package org.keycloak.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.TokenCategory;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.exceptions.MetadataPolicyCombinationException;
import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.exceptions.MetadataPolicyException;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.federation.MetadataPolicyUtils;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.RPMetadataPolicy;
import org.keycloak.representations.openid_federation.TrustChainResolution;
import org.keycloak.services.ErrorResponseException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.keycloak.events.Errors;
import java.util.Collections;
import java.util.stream.Stream;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import org.keycloak.util.TokenUtil;

public class OpenIdFederationTrustChainProcessor {

    private static final Logger logger = Logger.getLogger(OpenIdFederationTrustChainProcessor.class);
    private  final KeycloakSession session;

    private static ObjectMapper om = new ObjectMapper();

    public OpenIdFederationTrustChainProcessor(KeycloakSession session) {
        this.session = session;
    }

    /**
     * This should construct all possible trust chains from a given leaf node self-signed and encoded JWT to a set of trust anchor urls
     * @param leafEs  this is the EntityStatement of a leaf node (Relay party or Openid Provider)
     * @param trustAnchorIds this should hold the trust anchor ids
     * @return any valid trust chains from the leaf node JWT to the trust anchor.
     */
    public List<TrustChainResolution> constructTrustChains(EntityStatement leafEs, Set<String> trustAnchorIds, boolean policyRequired, boolean forRp) {

        List<TrustChainResolution> trustChainResolutions = subTrustChains(leafEs, leafEs, trustAnchorIds, new HashSet<>(), forRp);

        return trustChainResolutions.stream().map(trustChainResolution -> {

                    //combine policies if valid till now
                    List<EntityStatement> parsedChain = trustChainResolution.getParsedChain();
                    if (trustChainResolution != null && policyRequired) {
                        try {
                            RPMetadataPolicy combinedPolicy = parsedChain.get(parsedChain.size() - 1).getMetadataPolicy() == null ? null : parsedChain.get(parsedChain.size() - 1).getMetadataPolicy().getRelyingPartyMetadataPolicy();
                            for (int i = parsedChain.size() - 2; i > 0; i--) {
                                combinedPolicy = MetadataPolicyUtils.combineClientPolicies(combinedPolicy, parsedChain.get(i).getMetadataPolicy().getRelyingPartyMetadataPolicy());
                            }

                            trustChainResolution.setCombinedPolicy(combinedPolicy);
                            trustChainResolution.setTrustAnchorId(trustChainResolution.getParsedChain().get(trustChainResolution.getParsedChain().size() - 1).getIssuer());
                            trustChainResolution.setLeafId(trustChainResolution.getParsedChain().get(0).getIssuer());
                        } catch (MetadataPolicyCombinationException e) {
                            logger.warn(String.format("Cannot combine metadata policy"));
                            trustChainResolution = null;
                        }

                    } else if (trustChainResolution != null) {
                        trustChainResolution.setTrustAnchorId(trustChainResolution.getParsedChain().get(trustChainResolution.getParsedChain().size() - 1).getIssuer());
                        trustChainResolution.setLeafId(trustChainResolution.getParsedChain().get(0).getIssuer());
                    }

                    return trustChainResolution;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

    }

    private List<TrustChainResolution> subTrustChains(EntityStatement initialEntity, EntityStatement leafEs, Set<String> trustAnchorIds, Set<String> visitedNodes, boolean forRp) {

        List<TrustChainResolution> chainsList = new ArrayList<>();
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
                    EntityStatement subNodeSubordinateES = parseAndValidateSelfSigned(encodedSubNodeSubordinate, EntityStatement.class, subNodeSelfES.getJwks());
                    if (!validateEntityStatementFields(subNodeSubordinateES, authHint, leafEs.getIssuer())) {
                        throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
                    }
                    logger.debug(String.format("EntityStatement of %s about %s. AuthHints: %s", subNodeSubordinateES.getIssuer(), subNodeSubordinateES.getSubject(), subNodeSubordinateES.getAuthorityHints()));

                    visitedNodes.add(subNodeSelfES.getIssuer());
                    if (trustAnchorIds.contains(authHint)) {
                        TrustChainResolution trustAnchor = new TrustChainResolution();
                        trustAnchor.getParsedChain().add(0, subNodeSelfES);
                        if (initialEntity.getSubject().equals(subNodeSubordinateES.getSubject())) {
                            //set initial entity if no intermediates entities between trust achor and initial entity
                            if ((forRp && subNodeSubordinateES.getMetadata().getRelyingPartyMetadata() == null) || (!forRp && subNodeSubordinateES.getMetadata().getOpenIdProviderMetadata() == null ))
                                throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
                            trustAnchor.setInitialEntity(subNodeSubordinateES);
                        } else {
                            String initialEntityFetch = OpenIdFederationUtils.getSubordinateToken(fedApiUrl, initialEntity.getIssuer(), session);
                            EntityStatement initialEntityFetchStatement = parseAndValidateSelfSigned(initialEntityFetch, EntityStatement.class, subNodeSelfES.getJwks());
                            if (!validateEntityStatementFields(subNodeSubordinateES, authHint, leafEs.getIssuer()) || (forRp && initialEntityFetchStatement.getMetadata().getRelyingPartyMetadata() == null) || (!forRp && initialEntityFetchStatement.getMetadata().getOpenIdProviderMetadata() == null )) {
                                throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
                            }
                            trustAnchor.setInitialEntity(initialEntityFetchStatement);
                        }
                        chainsList.add(trustAnchor);
                    } else {
                        List<TrustChainResolution> subList = subTrustChains(initialEntity, subNodeSelfES, trustAnchorIds, visitedNodes, forRp);
                        for (TrustChainResolution tcr : subList) {
                            tcr.getParsedChain().add(0, subNodeSelfES);
                            chainsList.add(tcr);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            });

        } else if (trustAnchorIds.contains(leafEs.getIssuer())) {
            TrustChainResolution trustAnchor = new TrustChainResolution();
            trustAnchor.getParsedChain().add(0, leafEs);
            chainsList.add(trustAnchor);
        }

        return chainsList;

    }

    public EntityStatement parseAndValidateSelfSigned(String token) throws InvalidTrustChainException {
        EntityStatement statement = parse(token, EntityStatement.class);
        validateToken(token, statement.getJwks());
        return statement;
    }

    public <T extends EntityStatement> T parseAndValidateSelfSigned(String token, Class<T> clazz, JSONWebKeySet jwks) throws InvalidTrustChainException {
        T statement = parse(token, clazz);
        validateToken(token, jwks);
        return statement;
    }

    private void validateToken(String token, JSONWebKeySet jwks){
        try{
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = produceJwtProcessor(jwks);
            jwtProcessor.process(token, null);

        } catch(IOException | ParseException | BadJOSEException | JOSEException ex) {
            ex.printStackTrace();
            throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
        }
    }

    private ConfigurableJWTProcessor<SecurityContext> produceJwtProcessor(JSONWebKeySet jwks) throws IOException, ParseException {
        String jsonKey = om.writeValueAsString(jwks);
        JWKSet jwkSet = JWKSet.load(new ByteArrayInputStream(jsonKey.getBytes()));
        JWKSource<SecurityContext> keySource = new ImmutableJWKSet<>(jwkSet);
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

        Set<JWSAlgorithm> algs = jwkSet.getKeys().stream()
                .map(key -> {
                    Object alg = key.getAlgorithm();
                    if (alg instanceof JWSAlgorithm) {
                        return (JWSAlgorithm) alg;
                    } else if (alg instanceof Algorithm) {
                        try {
                            return JWSAlgorithm.parse(((Algorithm) alg).getName());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    } else if (alg instanceof String) {
                        try {
                            return JWSAlgorithm.parse((String) alg);
                        } catch (Exception e) {
                            return null;
                        }
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (algs.isEmpty()) {
            algs = Collections.singleton(JWSAlgorithm.RS256); // Default to RS256
        }

        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(algs, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);
        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(Stream.of(new JOSEObjectType(TokenUtil.ENTITY_STATEMENT_JWT), new JOSEObjectType(TokenUtil.EXPLICIT_REGISTRATION_RESPONSE_JWT)).collect(Collectors.toSet())));
        return jwtProcessor;
    }

    public boolean validateEntityStatementFields(EntityStatement statement, String issuer, String subject) {
        return statement.getIssuer() == null || statement.getIssuer().equals(issuer) || statement.getSubject() == null || statement.getSubject().equals(subject) || statement.getIat() == null || LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) > statement.getIat() || statement.getExp() == null || LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) < statement.getExp();
    }

    public <T extends EntityStatement> T parse(String token, Class<T> clazz) throws InvalidTrustChainException {
        String[] splits = token.split("\\.");
        if (splits.length != 3)
            throw new InvalidTrustChainException("Trust chain contains a chain-link which does not abide to the dot-delimited format of xxx.yyy.zzz");
        try {
            return om.readValue(Base64.getDecoder().decode(splits[1]), clazz);
        } catch (IOException e) {
            throw new InvalidTrustChainException("Trust chain does not contain a valid Entity Statement");
        }
    }

    public TrustChainResolution findAcceptableMetadataPolicyChain(List<TrustChainResolution> trustChainResolutions, EntityStatement statement) {
        TrustChainResolution validChain = null;
        EntityStatement current = statement;
        for (TrustChainResolution chain : trustChainResolutions) {
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

}
