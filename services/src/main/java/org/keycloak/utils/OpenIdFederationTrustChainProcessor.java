package org.keycloak.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.TokenCategory;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
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
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.enums.EntityTypeEnum;
import org.keycloak.representations.openid_federation.AbstractMetadataPolicy;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.OPMetadataPolicy;
import org.keycloak.representations.openid_federation.RPMetadataPolicy;
import org.keycloak.representations.openid_federation.TrustChainResolution;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JWKSUtils;

import org.jboss.logging.Logger;

public class OpenIdFederationTrustChainProcessor implements TrustChainProcessor {

    private static final Logger logger = Logger.getLogger(OpenIdFederationTrustChainProcessor.class);
    private  final KeycloakSession session;

    public OpenIdFederationTrustChainProcessor(KeycloakSession session) {
        this.session = session;
    }

    /**
     * This should construct all possible trust chains from a given leaf node self-signed and encoded JWT to a set of trust anchor urls
     * @param leafEs  this is the EntityStatement of a leaf node (Relay party or Openid Provider)
     * @param trustAnchorIds this should hold the trust anchor ids
     * @return any valid trust chains from the leaf node JWT to the trust anchor.
     */
    @Override
    public TrustChainResolution constructTrustChains(EntityStatement leafEs, Set<String> trustAnchorIds, boolean forRp) {

        List<TrustChainResolution> trustChainResolutions = subTrustChains(leafEs.getSubject(), leafEs, trustAnchorIds, new HashSet<>(), forRp);

        for (TrustChainResolution trustChainResolution : trustChainResolutions) {

            //combine policies if valid till now
            List<EntityStatement> parsedChain = trustChainResolution.getParsedChain();
            try {
                AbstractMetadataPolicy combinedPolicy = parsedChain.get(parsedChain.size() - 1).getMetadataPolicy() == null ? null :
                        (forRp ? parsedChain.get(parsedChain.size() - 1).getMetadataPolicy().getRelyingPartyMetadataPolicy() : parsedChain.get(parsedChain.size() - 1).getMetadataPolicy().getOpenIdProviderMetadataPolicy());
                for (int i = parsedChain.size() - 2; i > 0; i--) {
                    combinedPolicy = MetadataPolicyUtils.combinePolicies(combinedPolicy, parsedChain.get(i).getMetadataPolicy() == null ? null :
                            (forRp ? parsedChain.get(i).getMetadataPolicy().getRelyingPartyMetadataPolicy() : parsedChain.get(i).getMetadataPolicy().getOpenIdProviderMetadataPolicy()));
                }

                if (forRp) {
                    trustChainResolution.setMetadataAfterPolicies(MetadataPolicyUtils.applyPoliciesToRPStatement(leafEs.getMetadata().getRelyingPartyMetadata(), (RPMetadataPolicy) combinedPolicy));
                } else {
                    trustChainResolution.setMetadataAfterPolicies(MetadataPolicyUtils.applyPoliciesToOPStatement(leafEs.getMetadata().getOpenIdProviderMetadata(), (OPMetadataPolicy) combinedPolicy));
                }

                trustChainResolution.setCombinedPolicy(combinedPolicy);
                trustChainResolution.setLeafId(trustChainResolution.getParsedChain().get(0).getSubject());
                return trustChainResolution;
            } catch (MetadataPolicyCombinationException | MetadataPolicyException e) {
                logger.warn(String.format("Cannot combine metadata policy for trust anchor : "+ trustChainResolution));
            }
        }
        return null;
    }

    private List<TrustChainResolution> subTrustChains(String initialEntity, EntityStatement leafEs, Set<String> trustAnchorIds, Set<String> visitedNodes, boolean forRp) {

        List<TrustChainResolution> chainsList = new ArrayList<>();
        visitedNodes.add(leafEs.getSubject());

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
                    String encodedSubNodeSubordinate = OpenIdFederationUtils.getSubordinateToken(fedApiUrl, leafEs.getSubject(), session);
                    EntityStatement subNodeSubordinateES = parseAndValidateSelfSigned(encodedSubNodeSubordinate, EntityStatement.class, subNodeSelfES.getJwks());
                    if (!validateEntityStatementFields(subNodeSubordinateES, authHint, leafEs.getSubject())) {
                        throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
                    }
                    if (leafEs.getSubject().equals(initialEntity) && (subNodeSubordinateES.getMetadata() == null ||
                            ((forRp && subNodeSubordinateES.getMetadata().getRelyingPartyMetadata() == null) || (!forRp && subNodeSubordinateES.getMetadata().getOpenIdProviderMetadata() == null)))
                            && !OpenIdFederationUtils.containedInListEndpoint(subNodeSelfES.getMetadata().getFederationEntity().getFederationListEndpoint(), forRp ? EntityTypeEnum.OPENID_RELYING_PARTY.getValue() : EntityTypeEnum.OPENID_PROVIDER.getValue(), initialEntity, session)) {
                        //check that RP is registered as RP in immediate superior
                        throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
                    }
                    logger.debug(String.format("EntityStatement of %s about %s. AuthHints: %s", subNodeSubordinateES.getIssuer(), subNodeSubordinateES.getSubject(), subNodeSubordinateES.getAuthorityHints()));

                    visitedNodes.add(subNodeSelfES.getSubject());
                    if (trustAnchorIds.contains(authHint)) {
                        TrustChainResolution trustAnchor = new TrustChainResolution();
                        trustAnchor.getParsedChain().add(0, subNodeSelfES);
                        trustAnchor.setTrustAnchorId(authHint);
                        chainsList.add(trustAnchor);
                    } else {
                        List<TrustChainResolution> subList = subTrustChains(initialEntity, subNodeSelfES, trustAnchorIds, visitedNodes, forRp);
                        for (TrustChainResolution tcr : subList) {
                            tcr.getParsedChain().add(0, subNodeSelfES);
                            chainsList.add(tcr);
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("Problem during trust chain resolution for "+initialEntity, ex);
                }

            });

        } else if (trustAnchorIds.contains(leafEs.getSubject())) {
            TrustChainResolution trustAnchor = new TrustChainResolution();
            trustAnchor.getParsedChain().add(0, leafEs);
            trustAnchor.setTrustAnchorId(leafEs.getSubject());
            chainsList.add(trustAnchor);
        }

        return chainsList;

    }

    @Override
    public <T extends EntityStatement> T parseAndValidateSelfSigned (String token, Class<T> clazz, JSONWebKeySet publicKey) throws IOException, JWSInputException, VerificationException {
        JWSInput jws = (JWSInput)JOSEParser.parse(token);
        T statement = jws.readJsonContent(clazz);
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
    public boolean validateEntityStatementFields(EntityStatement statement, String issuer, String subject) {
        return statement.getIssuer() == null || statement.getIssuer().equals(issuer) || statement.getSubject() == null || statement.getSubject().equals(subject) || statement.getIat() == null || LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) > statement.getIat() || statement.getExp() == null || LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) < statement.getExp();
    }

    @Override
    public void validationRules(EntityStatement statement, boolean checkAudience) {
        if (statement.getIssuer() == null) {
            throw new ErrorResponseException(Errors.INVALID_ISSUER, "No issuer in the request.", Response.Status.NOT_FOUND);
        }
        if (statement.getSubject() == null) {
            throw new ErrorResponseException(Errors.INVALID_SUBJECT, "No issuer in the request.", Response.Status.NOT_FOUND);
        }
        if (statement.getIat() == null && LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) > statement.getIat()) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "Iat must exist and be before now.", Response.Status.BAD_REQUEST);
        }
        if (statement.getExp() == null && LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) < statement.getExp()){
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "Exp must exist and be before now.", Response.Status.BAD_REQUEST);
        }
        if (statement.getAuthorityHints() == null || statement.getAuthorityHints().isEmpty()) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "No authorityHints in the request.", Response.Status.BAD_REQUEST);
        }
        if (statement.getMetadata() == null || statement.getMetadata().getRelyingPartyMetadata() == null) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "No relaying party metadata in the request.", Response.Status.BAD_REQUEST);
        }
        if (!statement.getIssuer().trim().equals(statement.getSubject().trim())) {
            throw new ErrorResponseException(Errors.INVALID_ISSUER, "The registration request issuer differs from the subject.", Response.Status.NOT_FOUND);
        }
        if (checkAudience && statement.getAudience() == null || !statement.hasAudience(Urls.realmIssuer(session.getContext().getUri(UrlType.FRONTEND).getBaseUri(), session.getContext().getRealm().getName()))) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "Aud must contain OP entity Identifier", Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public JSONWebKeySet getKeySet() {
        List<JWK> keys = new LinkedList<>();
        session.keys().getKeysStream(session.getContext().getRealm())
                .filter(k -> k.getStatus().isEnabled() && k.getUse().equals(KeyUse.SIG) && k.getPublicKey() != null && k.getAlgorithm().equals(session.tokens().signatureAlgorithm(TokenCategory.ENTITY_STATEMENT)))
                .forEach(k -> {
                    JWKBuilder b = JWKBuilder.create().kid(k.getKid()).algorithm(k.getAlgorithm());
                    if (k.getType().equals(KeyType.RSA)) {
                        keys.add(b.rsa(k.getPublicKey(), k.getCertificate()));
                    } else if (k.getType().equals(KeyType.EC)) {
                        keys.add(b.ec(k.getPublicKey()));
                    }
                });

        JSONWebKeySet keySet = new JSONWebKeySet();

        JWK[] k = new JWK[keys.size()];
        k = keys.toArray(k);
        keySet.setKeys(k);
        return keySet;
    }

    @Override
    public void close() {

    }

    //nimbus implementation - to be removed
//    public EntityStatement parseAndValidateSelfSigned(String token) throws InvalidTrustChainException {
//        EntityStatement statement = parse(token, EntityStatement.class);
//        validateToken(token, statement.getJwks());
//        return statement;
//    }
//
//    public <T extends EntityStatement> T parseAndValidateSelfSigned(String token, Class<T> clazz, JSONWebKeySet jwks) throws InvalidTrustChainException {
//        T statement = parse(token, clazz);
//        validateToken(token, jwks);
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
//        String jsonKey = JsonSerialization.writeValueAsString(jwks);
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
//        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(Stream.of(new JOSEObjectType(TokenUtil.ENTITY_STATEMENT_JWT), new JOSEObjectType(TokenUtil.EXPLICIT_REGISTRATION_RESPONSE_JWT)).collect(Collectors.toSet())));
//        return jwtProcessor;
//    }
//
//
//
//    public <T extends EntityStatement> T parse(String token, Class<T> clazz) throws InvalidTrustChainException {
//        String[] splits = token.split("\\.");
//        if (splits.length != 3)
//            throw new InvalidTrustChainException("Trust chain contains a chain-link which does not abide to the dot-delimited format of xxx.yyy.zzz");
//        try {
//            return JsonSerialization.readValue(Base64.getUrlDecoder().decode(splits[1]), clazz);
//        } catch (IOException e) {
//            throw new InvalidTrustChainException("Trust chain does not contain a valid Entity Statement");
//        }
//    }



}
