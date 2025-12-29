package org.keycloak.utils;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.http.entity.StringEntity;
import org.jboss.logging.Logger;
import org.keycloak.TokenCategory;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.federation.OpenIdFederationIdentityProviderConfig;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.crypto.RS256SignatureProviderFactory;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.exceptions.MetadataPolicyCombinationException;
import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.exceptions.MetadataPolicyException;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.jose.JOSEParser;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OpenIdFederationConfig;
import org.keycloak.models.OpenIdFederationGeneralConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.models.enums.EntityTypeEnum;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.protocol.trustchain.TrustChainProcessor;
import org.keycloak.representations.openid_federation.AbstractMetadataPolicy;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.EntityStatementExplicitResponse;
import org.keycloak.representations.openid_federation.Metadata;
import org.keycloak.representations.openid_federation.OPMetadata;
import org.keycloak.representations.openid_federation.OPMetadataPolicy;
import org.keycloak.representations.openid_federation.RPMetadata;
import org.keycloak.representations.openid_federation.RPMetadataPolicy;
import org.keycloak.representations.openid_federation.TrustChainResolution;
import org.keycloak.services.ErrorResponseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.events.Errors;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.TokenUtil;

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
        if (checkAudience && !statement.hasAudience(Urls.realmIssuer(session.getContext().getUri(UrlType.FRONTEND).getBaseUri(), session.getContext().getRealm().getName()))) {
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
    public void updateIdP(IdentityProviderModel model, RealmModel realm){
        try {
            rPexcplicitRegistration(model.getConfig().get(OIDCIdentityProviderConfig.ISSUER), model.getConfig().get(OpenIdFederationIdentityProviderConfig.TRUST_ANCHOR_ID), model, realm);
            model.setEnabled(true);
        } catch (Exception e) {
            model.setEnabled(false);
        }
        realm.updateIdentityProvider(model);
    }

    @Override
    public void rPexcplicitRegistration(String opIssuer, String trustAnchor, IdentityProviderModel model, RealmModel realm) throws Exception {
        OpenIdFederationGeneralConfig federationGeneralConfig = realm.getOpenIdFederationGeneralConfig();
        OpenIdFederationConfig federationConfig = federationGeneralConfig.getOpenIdFederationList().stream().filter(x -> trustAnchor.equals(x.getTrustAnchor())).findAny().orElseThrow(() -> new NotFoundException("Trust anchor does not exist"));
        EntityStatement opStatement = parseAndValidateSelfSigned(OpenIdFederationUtils.getSelfSignedToken(opIssuer, session));
        if (!validateEntityStatementFields(opStatement, opIssuer, opIssuer) || opStatement.getMetadata().getOpenIdProviderMetadata() == null || !opStatement.getMetadata().getOpenIdProviderMetadata().getClientRegistrationTypesSupported().contains("explicit") || opStatement.getMetadata().getOpenIdProviderMetadata().getFederationRegistrationEndpoint() == null) {
            throw new BadRequestException("No valid OP Entity Statement");
        }
        TrustChainResolution trustChainResolution = constructTrustChains(opStatement, Stream.of(federationConfig.getTrustAnchor()).collect(Collectors.toSet()),  false);
        if (trustChainResolution == null) {
            throw new BadRequestException("No common trust chain found");
        }
        OPMetadata op = (OPMetadata) trustChainResolution.getMetadataAfterPolicies();
        model = OIDCIdentityProviderFactory.parseOIDCConfig(op,  OpenIdFederationIdentityProviderConfig.class, model);

        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
        JSONWebKeySet jwks = getKeySet();
        EntityStatement entityStatement = new EntityStatement(Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName()), Long.valueOf(federationGeneralConfig.getLifespan()), Stream.of(trustChainResolution.getLeafId()).collect(Collectors.toList()), jwks);
        entityStatement.addAudience(opIssuer);
        Metadata metadata = new Metadata();
        // Use RP client registration types from general config
        Stream<ClientRegistrationTypeEnum> rpRegistrationTypes = federationGeneralConfig.getRpClientRegistrationTypesSupported() != null ?
                federationGeneralConfig.getRpClientRegistrationTypesSupported().stream() :
                Stream.empty();
        RPMetadata rPMetadata = OpenIdFederationUtils.createRPMetadata(federationGeneralConfig, rpRegistrationTypes, OpenIdFederationUtils.commonMetadata(federationGeneralConfig), RealmsResource.protocolUrl(backendUriInfo).clone().path(OIDCLoginProtocolService.class, "certs").build(realm.getName(),
                OIDCLoginProtocol.LOGIN_PROTOCOL).toString(), frontendUriInfo, realm.getName());
        metadataFromOP(rPMetadata, federationConfig.getIdpConfiguration(), op, opStatement.getSubject());
        metadataFromFederation(rPMetadata, federationConfig.getIdpConfiguration());
        rPMetadata.setPostLogoutRedirectUris(Stream.of(OIDCIdentityProvider.getLogoutResponse(frontendUriInfo, realm.getName(), model.getAlias())).collect(Collectors.toList()));
        metadata.setRelyingPartyMetadata(rPMetadata);
        entityStatement.setMetadata(metadata);
        StringEntity entity = new StringEntity(session.tokens().encodeForOpenIdFederation(entityStatement), TokenUtil.APPLICATION_ENTITY_STATEMENT_JWT);
        SimpleHttpResponse response = SimpleHttp.create(session).doPost(op.getFederationRegistrationEndpoint())
                .header("Content-Type", "application/entity-statement+jwt")
                .entity(entity)
                .asResponse();
        if (response.getStatus() < 200 || response.getStatus() >= 400) {
            throw new BadRequestException("Error during explicit client registration with body : "+ response.asString());
        }
        EntityStatementExplicitResponse statementResponse = parseAndValidateSelfSigned(response.asString(), EntityStatementExplicitResponse.class, opStatement.getJwks());
        if (!validateEntityStatementFields(statementResponse, opIssuer, opIssuer) || statementResponse.getTrustAnchor() == null || LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) > statementResponse.getExp() ) {
            throw new BadRequestException("No valid OP Entity Statement");
        }
        if ( model.getInternalId() == null) {
            OpenIdFederationUtils.convertEntityStatementToIdp(model, realm, statementResponse, new HashMap<>(federationConfig.getIdpConfiguration()));
        } else {

        }
    }

    private void metadataFromFederation(RPMetadata rPMetadata, Map<String, String> federationConfig){
        rPMetadata.setScope(federationConfig.get(OAuth2IdentityProviderConfig.DEFAULT_SCOPE));
    }

    private void metadataFromOP(RPMetadata rPMetadata, Map<String, String> federationConfig, OPMetadata opMetadata, String subject) {
        List<String> subjectTypesSupported = federationConfig.get(OpenIdFederationUtils.SUBJECT_TYPES_SUPPORTED) == null
                ? OIDCWellKnownProvider.DEFAULT_SUBJECT_TYPES_SUPPORTED
                : Arrays.asList(federationConfig.get(OpenIdFederationUtils.SUBJECT_TYPES_SUPPORTED).split("##"));

        rPMetadata.setSubjectType(subjectTypesSupported.stream()
                .filter(x -> opMetadata.getSubjectTypesSupported().contains(x))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No subject type common exists")));
        rPMetadata.setClientName(opMetadata.getCommonMetadata().getOrganizationName() != null ? opMetadata.getCommonMetadata().getOrganizationName() : subject);
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
