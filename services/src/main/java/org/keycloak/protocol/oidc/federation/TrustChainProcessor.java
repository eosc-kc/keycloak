package org.keycloak.protocol.oidc.federation;

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
import org.keycloak.exceptions.MetadataPolicyCombinationException;
import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.exceptions.MetadataPolicyException;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.RPMetadataPolicy;
import org.keycloak.representations.openid_federation.TrustChainForExplicit;
import org.keycloak.services.ErrorResponseException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.keycloak.events.Errors;
import java.util.Collections;
import java.util.stream.Stream;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;

public class TrustChainProcessor {

    private static final Logger logger = Logger.getLogger(TrustChainProcessor.class);
    private  final KeycloakSession session;

    private static ObjectMapper om = new ObjectMapper();

    public TrustChainProcessor (KeycloakSession session) {
        this.session = session;
    }

    /**
     * This should construct all possible trust chains from a given leaf node url to a set of trust anchor urls
     * @param leafNodeBaseUrl  this url should point to the base path of the leaf node (without the .well-known discovery subpath)
     * @param trustAnchorIds this should hold the trust anchor ids
     * @return any valid trust chains from the leaf node to the trust anchor.
     * @throws IOException
     */
    public List<TrustChainForExplicit> constructTrustChainsFromUrl(String leafNodeBaseUrl, List<String> trustAnchorIds, boolean policyRequired) throws IOException, InvalidTrustChainException {
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
    public List<TrustChainForExplicit> constructTrustChains(EntityStatement leafEs, List<String> trustAnchorIds, boolean policyRequired) {

        List<TrustChainForExplicit> trustChainForExplicits = subTrustChains(leafEs, trustAnchorIds, new HashSet<>());

        return trustChainForExplicits.stream().map(trustChainForExplicit -> {
//                    //parse chain nodes
//                    List<EntityStatement> parsedChain = trustChain.getChain().stream().map(x -> {
//                                try {
//                                    return parse(x);
//                                } catch (InvalidTrustChainException e) {
//                                    return null;
//                                }
//                            })
//                            .filter(Objects::nonNull)
//                            .collect(Collectors.toList());
//                    if (parsedChain.size() == trustChain.getChain().size()) {

                    // trustChain.setParsedChain(parsedChain);

//                    } else {
//                        trustChain = null;
//                    }

                    //combine policies if valid till now
                    List<EntityStatement> parsedChain = trustChainForExplicit.getParsedChain();
                    if (trustChainForExplicit != null && parsedChain.size() > 1 && policyRequired) {
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

    private List<TrustChainForExplicit> subTrustChains(EntityStatement leafEs, List<String> trustAnchorIds, Set<String> visitedNodes) {

        List<TrustChainForExplicit> chainsList = new ArrayList<>();
        visitedNodes.add(leafEs.getIssuer());

        if (leafEs.getAuthorityHints() != null && !leafEs.getAuthorityHints().isEmpty()) {
            leafEs.getAuthorityHints().forEach(authHint -> {
                try {
                    if (visitedNodes.contains(authHint) && !trustAnchorIds.contains(authHint))
                        return;
                    String encodedSubNodeSelf = OpenIdFederationUtils.getSelfSignedToken(authHint, session);
                    EntityStatement subNodeSelfES = parseAndValidateSelfSigned(encodedSubNodeSelf);
                    logger.debug(String.format("EntityStatement of %s about %s. AuthHints: %s", subNodeSelfES.getIssuer(), subNodeSelfES.getSubject(), subNodeSelfES.getAuthorityHints()));

                    String fedApiUrl = subNodeSelfES.getMetadata().getFederationEntity().getFederationFetchEndpoint();
                    String encodedSubNodeSubordinate = OpenIdFederationUtils.getSubordinateToken(fedApiUrl, subNodeSelfES.getIssuer(), leafEs.getIssuer(), session);
                    EntityStatement subNodeSubordinateES = parse(encodedSubNodeSubordinate);
                    validate(encodedSubNodeSubordinate, subNodeSelfES.getJwks());
                    logger.debug(String.format("EntityStatement of %s about %s. AuthHints: %s", subNodeSubordinateES.getIssuer(), subNodeSubordinateES.getSubject(), subNodeSubordinateES.getAuthorityHints()));
                    visitedNodes.add(subNodeSelfES.getIssuer());
                    if (trustAnchorIds.contains(subNodeSelfES.getIssuer())) {
                        TrustChainForExplicit trustAnchor = new TrustChainForExplicit();
                        trustAnchor.getParsedChain().add(0, subNodeSelfES);
                        chainsList.add(trustAnchor);
                    } else {
                        List<TrustChainForExplicit> subList = subTrustChains(subNodeSelfES, trustAnchorIds, visitedNodes);
                        for (TrustChainForExplicit tcr : subList) {
                            // tcr.getChain().add(0, encodedSubNodeSubordinate);
                            tcr.getParsedChain().add(0, subNodeSubordinateES);
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

    /**
     * This validates the whole trustChain signature
     * @param trustChain
     * @return the outcome of the validation (true/false)
     * @throws InvalidTrustChainException
     */
//    public void validateTrustChain(TrustChain trustChain) throws InvalidTrustChainException, UnparsableException, RemoteFetchingException, BadSigningOrEncryptionException {
//        List<String> trustChainRaw = trustChain.getChain();
//
//        if(trustChainRaw.size() < 2)
//            throw new InvalidTrustChainException("A trust chain should contain at least 2 elements.");
//
//        String trustAnchorUri = parse(trustChainRaw.get(trustChainRaw.size()-1)).getIssuer();
//        String trustAnchorSelfSigned;
//        try {
//            trustAnchorSelfSigned = OpenIdFederationUtils.getSelfSignedToken(trustAnchorUri, session);
//        } catch (IOException e) {
//            throw new RemoteFetchingException(e.getMessage());
//        }
//        JSONWebKeySet trustAnchorKeys = parse(trustAnchorSelfSigned).getJwks();
//
//        String trustAnchorSubordinate = trustChainRaw.get(trustChainRaw.size()-1);
//        validate(trustAnchorSubordinate, trustAnchorKeys);
//
//        List<EntityStatement> parsedTrustChain = new ArrayList<EntityStatement>();
//        for(String entityRaw : trustChainRaw)
//            parsedTrustChain.add(parse(entityRaw));
//
//        boolean correctlyLinked = true;
//        for(int i=parsedTrustChain.size()-1 ; i>0 ; i--) {
//            if(!parsedTrustChain.get(i).getSubject().trim().equals(parsedTrustChain.get(i-1).getIssuer().trim())) {
//                correctlyLinked = false;
//                break;
//            }
//        }
//        if(!correctlyLinked)
//            throw new InvalidTrustChainException("The trust chain should have entity[i].iss == entity[i+1].sub");
//
//        boolean allIntermediatesValid = true;
//        for(int i=trustChainRaw.size()-2 ; i>0 ; i--)
//            validate(trustChainRaw.get(i), parsedTrustChain.get(i+1).getJwks());
//
//        if(!allIntermediatesValid)
//            throw new InvalidTrustChainException("The trust chain has one or more invalid signed ");
//
//    }
//

    public void validate(String token, JSONWebKeySet publicKey) throws IOException, ParseException, BadJOSEException, JOSEException {
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = produceJwtProcessor(publicKey);
            jwtProcessor.process(token, null);
    }

    public EntityStatement parseAndValidateSelfSigned(String token) throws InvalidTrustChainException {
        EntityStatement statement = parse(token);
        try{
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = produceJwtProcessor(statement.getJwks());
            jwtProcessor.process(token, null);

        } catch(IOException | ParseException | BadJOSEException | JOSEException ex) {
            ex.printStackTrace();
            throw new ErrorResponseException(Errors.INVALID_TRUST_CHAIN, "Trust chain is not valid", Response.Status.BAD_REQUEST);
        }

        return statement;
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
                            // Not a valid JWSAlgorithm
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
        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(Stream.of(new JOSEObjectType("entity-statement+jwt")).collect(Collectors.toSet())));
        return jwtProcessor;
    }

    public EntityStatement parse(String token) throws InvalidTrustChainException {
        String [] splits = token.split("\\.");
        if(splits.length != 3)
            throw new InvalidTrustChainException("Trust chain contains a chain-link which does not abide to the dot-delimited format of xxx.yyy.zzz");
        try {
            return om.readValue(Base64.getDecoder().decode(splits[1]), EntityStatement.class);
        } catch (IOException e) {
            throw new InvalidTrustChainException("Trust chain does not contain a valid Entity Statement");
        }
    }

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

}
