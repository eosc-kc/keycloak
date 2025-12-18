package org.keycloak.protocol.oidc.federation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.crypto.RS256SignatureProviderFactory;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.events.Errors;
import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.exceptions.MetadataPolicyCombinationException;
import org.keycloak.exceptions.MetadataPolicyException;
import org.keycloak.jose.JOSEParser;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.RPMetadataPolicy;
import org.keycloak.representations.openid_federation.TrustChainForExplicit;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.util.JWKSUtils;

import org.jboss.logging.Logger;

public class OpenIdFederationTrustChainProcessor implements TrustChainProcessor {

    private static final Logger logger = Logger.getLogger(OpenIdFederationTrustChainProcessor.class);
    private  final KeycloakSession session;

    public OpenIdFederationTrustChainProcessor(KeycloakSession session) {
        this.session = session;
    }

    /**
     * This should construct all possible trust chains from a given leaf node url to a set of trust anchor urls
     * @param leafNodeBaseUrl  this url should point to the base path of the leaf node (without the .well-known discovery subpath)
     * @param trustAnchorIds this should hold the trust anchor ids
     * @return any valid trust chains from the leaf node to the trust anchor.
     * @throws IOException
     */
    public List<TrustChainForExplicit> constructTrustChainsFromUrl(String leafNodeBaseUrl, Set<String> trustAnchorIds, boolean policyRequired) throws IOException, InvalidTrustChainException {
        String encodedLeafES = OpenIdFederationUtils.getSelfSignedToken(leafNodeBaseUrl, session);
        EntityStatement statement = parseAndValidateSelfSigned(encodedLeafES);
        return constructTrustChains(statement, trustAnchorIds, policyRequired);
    }


    /**
     * This should construct all possible trust chains from a given leaf node self-signed and encoded JWT to a set of trust anchor urls
     * @param leafEs  this is the EntityStatement of a leaf node (Relay party or Openid Provider)
     * @param trustAnchorIds this should hold the trust anchor ids
     * @return any valid trust chains from the leaf node JWT to the trust anchor.
     */
    @Override
    public List<TrustChainForExplicit> constructTrustChains(EntityStatement leafEs, Set<String> trustAnchorIds, boolean policyRequired) {

        List<TrustChainForExplicit> trustChainForExplicits = subTrustChains(leafEs, trustAnchorIds, new HashSet<>());

        return trustChainForExplicits.stream().map(trustChainForExplicit -> {
                    //combine policies if valid till now
                    List<EntityStatement> parsedChain = trustChainForExplicit.getParsedChain();
                    if (trustChainForExplicit != null && policyRequired) {
                        RPMetadataPolicy combinedPolicy = parsedChain.get(parsedChain.size() - 1).getMetadataPolicy().getRelyingPartyMetadataPolicy();
                        for (int i = parsedChain.size() - 2; i > 0; i--) {
                            try {
                                combinedPolicy = MetadataPolicyUtils.combineClientPolicies(combinedPolicy, parsedChain.get(i).getMetadataPolicy().getRelyingPartyMetadataPolicy());
                            } catch (MetadataPolicyCombinationException e) {
                                logger.debug(String.format("Cannot combine metadata policy of iss=%s sub=%s and its inferiors", parsedChain.get(i).getIssuer(), parsedChain.get(i).getSubject()));
                                combinedPolicy = null;
                            }
                        }
                        if (combinedPolicy != null) {
                            trustChainForExplicit.setCombinedPolicy(combinedPolicy);
                            trustChainForExplicit.setTrustAnchorId(trustChainForExplicit.getParsedChain().get(trustChainForExplicit.getParsedChain().size() - 1).getIssuer());
                            trustChainForExplicit.setLeafId(trustChainForExplicit.getParsedChain().get(0).getIssuer());
                        } else {
                            trustChainForExplicit = null;
                        }
                    }

                    return trustChainForExplicit;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

    }

    private List<TrustChainForExplicit> subTrustChains(EntityStatement leafEs, Set<String> trustAnchorIds, Set<String> visitedNodes) {

        List<TrustChainForExplicit> chainsList = new ArrayList<>();
        visitedNodes.add(leafEs.getIssuer());

        if (leafEs.getAuthorityHints() != null && !leafEs.getAuthorityHints().isEmpty()) {
            leafEs.getAuthorityHints().forEach(authHint -> {
                try {
                    if (visitedNodes.contains(authHint) && !trustAnchorIds.contains(authHint))
                        return;
                    String encodedSubNodeSelf = OpenIdFederationUtils.getSelfSignedToken(authHint, session);
                    EntityStatement subNodeSelfES = parseAndValidateSelfSigned(encodedSubNodeSelf);
                    if (!validateEntityStatementFields(subNodeSelfES, authHint, authHint)) {
                        throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
                    }
                    logger.debug(String.format("EntityStatement of %s about %s. AuthHints: %s", subNodeSelfES.getIssuer(), subNodeSelfES.getSubject(), subNodeSelfES.getAuthorityHints()));

                    String fedApiUrl = subNodeSelfES.getMetadata().getFederationEntity().getFederationFetchEndpoint();
                    String encodedSubNodeSubordinate = OpenIdFederationUtils.getSubordinateToken(fedApiUrl, leafEs.getIssuer(), session);
                    EntityStatement subNodeSubordinateES = parseAndValidateSelfSigned(encodedSubNodeSubordinate, subNodeSelfES.getJwks());
                    //fetch endpoint contains jwks of leafEs. So, validate based on subNodeSelfES.
                    if (!validateEntityStatementFields(subNodeSubordinateES, authHint, leafEs.getIssuer())) {
                        throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
                    }
                    logger.debug(String.format("EntityStatement of %s about %s. AuthHints: %s", subNodeSubordinateES.getIssuer(), subNodeSubordinateES.getSubject(), subNodeSubordinateES.getAuthorityHints()));
                    visitedNodes.add(subNodeSelfES.getIssuer());
                    if (trustAnchorIds.contains(authHint)) {
                        TrustChainForExplicit trustAnchor = new TrustChainForExplicit();
                        trustAnchor.getParsedChain().add(0, subNodeSelfES);
                        chainsList.add(trustAnchor);
                    } else {
                        List<TrustChainForExplicit> subList = subTrustChains(subNodeSelfES, trustAnchorIds, visitedNodes);
                        for (TrustChainForExplicit tcr : subList) {
                            tcr.getParsedChain().add(0, subNodeSelfES);
                            chainsList.add(tcr);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            });

        } else if (trustAnchorIds.contains(leafEs.getIssuer())) {
            TrustChainForExplicit trustAnchor = new TrustChainForExplicit();
            trustAnchor.getParsedChain().add(0, leafEs);
            chainsList.add(trustAnchor);
        }

        return chainsList;

    }

    @Override
    public EntityStatement parseAndValidateSelfSigned (String token, JSONWebKeySet publicKey) throws IOException, JWSInputException, VerificationException {
        JWSInput jws = (JWSInput)JOSEParser.parse(token);
        EntityStatement statement = jws.readJsonContent(EntityStatement.class);
        String signatureAlgorithm = jws.getHeader().getAlgorithm().name();
        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);

        if (signatureProvider == null) {
            signatureProvider = session.getProvider(SignatureProvider.class, RS256SignatureProviderFactory.ID);
        }

        PublicKeysWrapper pkw = JWKSUtils.getKeyWrappersForUse(publicKey, JWK.Use.SIG);
        if (! signatureProvider.verifier(pkw.getKeys().get(0)).
                verify(jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8), jws.getSignature())) {
            throw new IOException ("No verified token");
        }

        return statement;
    }

    @Override
    public EntityStatement parseAndValidateSelfSigned(String token) throws InvalidTrustChainException {
        try {
            JWSInput jws = (JWSInput) JOSEParser.parse(token);
            EntityStatement statement = jws.readJsonContent(EntityStatement.class);
            String signatureAlgorithm = jws.getHeader().getAlgorithm().name();
            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);

            if (signatureProvider == null) {
                signatureProvider = session.getProvider(SignatureProvider.class, RS256SignatureProviderFactory.ID);
            }

            PublicKeysWrapper pkw = JWKSUtils.getKeyWrappersForUse(statement.getJwks(), JWK.Use.SIG);
            if (!signatureProvider.verifier(pkw.getKeys().get(0)).
                    verify(jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8), jws.getSignature())){
                throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
            }
            return statement;
        } catch (JWSInputException | VerificationException e) {
            throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
        }

    }

    @Override
    public TrustChainForExplicit findAcceptableMetadataPolicyChain(List<TrustChainForExplicit> trustChainForExplicits, EntityStatement statement) {
        TrustChainForExplicit validChain = null;
        EntityStatement current = statement;
        for (TrustChainForExplicit chain : trustChainForExplicits) {
            try {
                current = MetadataPolicyUtils.applyPoliciesToRPStatement(current, chain.getCombinedPolicy());
                validChain = chain;
                break;
            } catch (MetadataPolicyCombinationException | MetadataPolicyException e) {
                e.printStackTrace();
            }
        }
        return validChain;
    }

    private boolean validateEntityStatementFields(EntityStatement statement, String issuer, String subject) {
        return statement.getIssuer() == null || statement.getIssuer().equals(issuer) || statement.getSubject() == null || statement.getSubject().equals(subject) || statement.getIat() == null || LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) > statement.getIat() || statement.getExp() == null || LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) < statement.getExp();
    }

    @Override
    public void close() {

    }

    //nimbus implementation - to be removed
//    public EntityStatement parseAndValidateSelfSigned(String token) throws InvalidTrustChainException {
//        EntityStatement statement = parse(token);
//        validateToken(token, statement.getJwks());
//        return statement;
//    }
//
//    private void validateToken(String token, JSONWebKeySet jwks){
//        try{
//            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = produceJwtProcessor(jwks);
//            jwtProcessor.process(token, null);
//
//        } catch(IOException | ParseException | BadJOSEException | JOSEException ex) {
//            ex.printStackTrace();
//            throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
//        }
//    }
//
//    private ConfigurableJWTProcessor<SecurityContext> produceJwtProcessor(JSONWebKeySet jwks) throws IOException, ParseException {
//        String jsonKey = om.writeValueAsString(jwks);
//        JWKSet jwkSet = JWKSet.load(new ByteArrayInputStream(jsonKey.getBytes()));
//        JWKSource<SecurityContext> keySource = new ImmutableJWKSet<>(jwkSet);
//        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
//
//        Set<JWSAlgorithm> algs = jwkSet.getKeys().stream()
//                .map(key -> {
//                    Object alg = key.getAlgorithm();
//                    if (alg instanceof JWSAlgorithm) {
//                        return (JWSAlgorithm) alg;
//                    } else if (alg instanceof Algorithm) {
//                        try {
//                            return JWSAlgorithm.parse(((Algorithm) alg).getName());
//                        } catch (IllegalArgumentException e) {
//                            // Not a valid JWSAlgorithm
//                            return null;
//                        }
//                    } else if (alg instanceof String) {
//                        try {
//                            return JWSAlgorithm.parse((String) alg);
//                        } catch (Exception e) {
//                            return null;
//                        }
//                    } else {
//                        return null;
//                    }
//                })
//                .filter(Objects::nonNull)
//                .collect(Collectors.toSet());
//
//        if (algs.isEmpty()) {
//            algs = Collections.singleton(JWSAlgorithm.RS256); // Default to RS256
//        }
//
//        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(algs, keySource);
//        jwtProcessor.setJWSKeySelector(keySelector);
//        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(Stream.of(new JOSEObjectType("entity-statement+jwt")).collect(Collectors.toSet())));
//        return jwtProcessor;
//    }
//
//
//
//    public EntityStatement parse(String token) throws InvalidTrustChainException {
//        String [] splits = token.split("\\.");
//        if(splits.length != 3)
//            throw new InvalidTrustChainException("Trust chain contains a chain-link which does not abide to the dot-delimited format of xxx.yyy.zzz");
//        try {
//            return om.readValue(Base64.getDecoder().decode(splits[1]), EntityStatement.class);
//        } catch (IOException e) {
//            throw new InvalidTrustChainException("Trust chain does not contain a valid Entity Statement");
//        }
//    }


}
