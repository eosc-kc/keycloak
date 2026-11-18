package org.keycloak.utils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.federation.OpenIdFederationAutomaticIdentityProviderConfig;
import org.keycloak.broker.oidc.federation.OpenIdFederationAutomaticIdentityProviderFactory;
import org.keycloak.broker.oidc.federation.OpenIdFederationExplicitIdentityProviderConfig;
import org.keycloak.broker.oidc.federation.OpenIdFederationExplicitIdentityProviderFactory;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OpenIdFederationGeneralConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.trustchain.TrustChainProcessor;
import org.keycloak.representations.openid_federation.CommonMetadata;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.EntityStatementExplicitResponse;
import org.keycloak.representations.openid_federation.OPMetadata;
import org.keycloak.representations.openid_federation.RPMetadata;
import org.keycloak.representations.openid_federation.TrustChainResolution;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.clientregistration.ClientRegistrationAuth;
import org.keycloak.services.clientregistration.openid_federation.OpenIdFederationClientRegistrationService;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.client.config.RequestConfig;
import org.jboss.logging.Logger;

public class OpenIdFederationUtils {

    private static final Logger logger = Logger.getLogger(OpenIdFederationUtils.class);
    private static final String WELL_KNOWN_SUBPATH = ".well-known/openid-federation";
    public static final String SUBJECT_TYPES_SUPPORTED = "subject_types_supported";
    public static List<String> OPENID_FEDERATION_IDPS = List.of(OpenIdFederationExplicitIdentityProviderFactory.PROVIDER_ID, OpenIdFederationAutomaticIdentityProviderFactory.PROVIDER_ID);

    public static String getSelfSignedToken(String issuer, KeycloakSession session) throws IOException {
        issuer = issuer.trim();
        if (!issuer.endsWith("/")) issuer += "/";
        SimpleHttpResponse response = createSimpleHttp(session).doGet((issuer + WELL_KNOWN_SUBPATH)).asResponse();
        int status = response.getStatus();
        if (status < 200 || status >= 400) {
            throw new BadRequestException(".well-known/openid-federation failed with response: " + response.asString() + " and status code : " + status);
        }
        return response.asString();
    }

    public static String getSubordinateToken(String fedApiUrl, String subject, KeycloakSession session) throws IOException {
        SimpleHttpResponse response = createSimpleHttp(session).doGet((fedApiUrl + "?sub=" + urlEncode(subject))).asResponse();
        int status = response.getStatus();
        if (status < 200 || status >= 400) {
            throw new BadRequestException("Fetch endpoint failed with response: " + response.asString() + " and status code : " + status);
        }
        return response.asString();
    }

    public static boolean containedInListEndpoint(String listApiUrl, String entityType, String issuer, KeycloakSession session) throws IOException {
        return listApiUrl != null && createSimpleHttp(session).doGet(listApiUrl + "?entity_type=" + entityType).asJson(new TypeReference<List<String>>(){}).contains(issuer);
    }

    public static SimpleHttp createSimpleHttp(KeycloakSession session) {
        return SimpleHttp.create(session).withRequestConfig(createRequestConfig(session.getContext().getRealm().getOpenIdFederationGeneralConfig()));
    }

    public static RequestConfig createRequestConfig(OpenIdFederationGeneralConfig config) {
        return RequestConfig.custom()
                .setConnectTimeout((int) TimeUnit.SECONDS.toMillis(config.getConnectionTimeout()))
                .setSocketTimeout((int) TimeUnit.SECONDS.toMillis(config.getSocketTimeout()))
                .setConnectionRequestTimeout((int) TimeUnit.SECONDS.toMillis(config.getConnectionRequestTimeout()))
                .build();
    }

    private static String urlEncode(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    public static CommonMetadata commonMetadata(OpenIdFederationGeneralConfig realmConfig){
        CommonMetadata common = new CommonMetadata();
        common.setOrganizationUri(realmConfig.getOrganizationUri());
        common.setOrganizationName(realmConfig.getOrganizationName());
        return common;
    }

    public static RPMetadata createRPMetadata(OpenIdFederationGeneralConfig openIdFederationConfig, Stream<ClientRegistrationTypeEnum> registrationTypes, CommonMetadata common, String jwksUri, UriInfo frontendUriInfo, String realmName){
        RPMetadata rPMetadata = new RPMetadata();
        rPMetadata.setClientRegistrationTypes(registrationTypes.map(ClientRegistrationTypeEnum::getValue).collect(Collectors.toList()));
        rPMetadata.setContacts(openIdFederationConfig.getContacts());
        rPMetadata.setLogoUri(openIdFederationConfig.getLogoUri());
        rPMetadata.setPolicyUri(openIdFederationConfig.getPolicyUri());
        rPMetadata.setCommonMetadata(common);
        rPMetadata.setJwksUri(jwksUri);
        rPMetadata.setGrantTypes(Collections.singletonList(OAuth2Constants.AUTHORIZATION_CODE));
        rPMetadata.setResponseTypes(Stream.of("code").collect(Collectors.toList()));
        rPMetadata.setApplicationType("web");
        rPMetadata.setRedirectUris(Stream.of(Urls.openIdFederationAuthnResponse(frontendUriInfo.getBaseUri(), realmName).toString()).collect(Collectors.toList()));
        return rPMetadata;
    }

    public static IdentityProviderModel convertEntityStatementToIdp(IdentityProviderModel model, RealmModel realm, EntityStatementExplicitResponse entityStatement, Map<String, String> federationIdPConfig) {
        model = mapTrustAnchorIdPConfiguration(model, realm, federationIdPConfig);
        RPMetadata rp = entityStatement.getMetadata().getRelyingPartyMetadata();
        model.setDisplayName(rp.getClientName());
        if (rp.getPostLogoutRedirectUris() != null && !rp.getPostLogoutRedirectUris().isEmpty()) {
            model.getConfig().put(OIDCIdentityProviderConfig.LOGOUT_URL, rp.getPostLogoutRedirectUris().get(0));
        }
        model.getConfig().put(OAuth2IdentityProviderConfig.DEFAULT_SCOPE, rp.getScope());
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_ID, rp.getClientId());
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_SECRET, rp.getClientSecret());
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_AUTH_METHOD, rp.getTokenEndpointAuthMethod());
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_ASSERTION_SIGNING_ALG, rp.getTokenEndpointAuthSigningAlg());
        model.getConfig().put(OpenIdFederationExplicitIdentityProviderConfig.TRUST_ANCHOR_ID, entityStatement.getTrustAnchor());
        model.getConfig().put(OpenIdFederationExplicitIdentityProviderConfig.AUTHORITY_HINTS, entityStatement.getAuthorityHints().stream().collect(Collectors.joining("##")));
        model.getConfig().put(OIDCConfigAttributes.EXPIRATION_TIME, String.valueOf(entityStatement.getExp()));
        return model;
    }

    public static IdentityProviderModel createAutomaticIdentityProvider(IdentityProviderModel model, RealmModel realm, UriInfo frontendUriInfo, String activeAlg, Map<String, String> federationIdPConfig, TrustChainResolution trustChainResolution, Long exp) throws Exception {
        if (model.getInternalId() == null) {
            model= mapTrustAnchorIdPConfiguration(model, realm, federationIdPConfig);
        }
        OPMetadata op = (OPMetadata) trustChainResolution.getMetadataAfterPolicies();
        model = OIDCIdentityProviderFactory.parseOIDCConfig(op, OpenIdFederationAutomaticIdentityProviderConfig.class, model);
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_ID, Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName()));
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_AUTH_METHOD, OIDCLoginProtocol.PRIVATE_KEY_JWT);

        // 3. Client Assertion Signing Algorithm
        // Fetch the active signing algorithm for your realm's federation keys
        // Check if the OP supports it; fallback to RS256 if not specified, though in Fed it usually is.
        if (op.getTokenEndpointAuthSigningAlgValuesSupported() != null &&
                op.getTokenEndpointAuthSigningAlgValuesSupported().contains(activeAlg)) {
            model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_ASSERTION_SIGNING_ALG, activeAlg);
        } else {
            logger.warnf("OP %s does not support the active signing algorithm %s; falling back to RS256", op.getIssuer(), activeAlg);
            model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_ASSERTION_SIGNING_ALG, "RS256");
        }

        if (op.getScopesSupported() != null && !op.getScopesSupported().isEmpty()) {
            Set<String> realmDefinedScopes = realm.getClientScopesStream()
                    .map(ClientScopeModel::getName)
                    .collect(Collectors.toSet());
            String negotiatedScopes = op.getScopesSupported().stream()
                    .filter(scope -> "openid".equals(scope) || realmDefinedScopes.contains(scope))
                    .collect(Collectors.joining(" "));
            model.getConfig().put(OAuth2IdentityProviderConfig.DEFAULT_SCOPE, negotiatedScopes);
        } else {
            model.getConfig().put(OAuth2IdentityProviderConfig.DEFAULT_SCOPE, federationIdPConfig.get(OAuth2IdentityProviderConfig.DEFAULT_SCOPE));
        }
        model.getConfig().put(OpenIdFederationAutomaticIdentityProviderConfig.TRUST_ANCHOR_ID, trustChainResolution.getTrustAnchorId());
        model.getConfig().put(OpenIdFederationAutomaticIdentityProviderConfig.AUTHORITY_HINTS, trustChainResolution.getLeafId());
        model.getConfig().put(OIDCConfigAttributes.EXPIRATION_TIME, String.valueOf(exp));
        model.getConfig().put(IdentityProviderModel.LAST_REFRESH_TIME, String.valueOf(Instant.now().toEpochMilli()));

        return model;
    }

    private static IdentityProviderModel mapTrustAnchorIdPConfiguration(IdentityProviderModel model, RealmModel realm, Map<String, String> federationIdPConfig) {
        model.setProviderId(OpenIdFederationExplicitIdentityProviderFactory.PROVIDER_ID);
        model.setAddReadTokenRoleOnCreate(Boolean.valueOf(federationIdPConfig.get("addReadTokenRoleOnCreate")));
        model.setEnabled(true);
        model.setLinkOnly(Boolean.valueOf(federationIdPConfig.get("linkOnly")));
        model.setStoreToken(Boolean.valueOf(federationIdPConfig.get("storeToken")));
        model.setTrustEmail(Boolean.valueOf(federationIdPConfig.get("trustEmail")));
        model.setHideOnLogin(Boolean.valueOf(federationIdPConfig.get("hideOnLogin")));
        model.setSyncMode(federationIdPConfig.get("syncMode") != null ? IdentityProviderSyncMode.valueOf(federationIdPConfig.get("syncMode")) : IdentityProviderSyncMode.IMPORT);
        if (federationIdPConfig.get("firstBrokerLoginFlowAlias") == null || federationIdPConfig.get("postBrokerLoginFlowAlias").isEmpty()) {
            model.setFirstBrokerLoginFlowId(realm.getFlowByAlias(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW).getId());
        } else {
            AuthenticationFlowModel flowModel = realm.getFlowByAlias(federationIdPConfig.get("firstBrokerLoginFlowAlias"));
            if (flowModel == null) {
                throw new ModelException("No available First Broker Login authentication flow with alias: " + federationIdPConfig.get("firstBrokerLoginFlowAlias"));
            }
            model.setFirstBrokerLoginFlowId(flowModel.getId());
        }

        if (federationIdPConfig.get("postBrokerLoginFlowAlias") != null &&  !federationIdPConfig.get("postBrokerLoginFlowAlias").isEmpty()) {
            AuthenticationFlowModel flowModel = realm.getFlowByAlias(federationIdPConfig.get("postBrokerLoginFlowAlias"));
            if (flowModel == null) {
                throw new ModelException("No available Post Broker Login authentication flow with alias: " + federationIdPConfig.get("postBrokerLoginFlowAlias"));
            }
            model.setPostBrokerLoginFlowId(flowModel.getId());
        }

        federationIdPConfig.remove(OAuth2IdentityProviderConfig.DEFAULT_SCOPE);
        federationIdPConfig.remove(OAuth2IdentityProviderConfig.CLIENT_AUTH_METHOD);
        federationIdPConfig.remove("addReadTokenRoleOnCreate");
        federationIdPConfig.remove("linkOnly");
        federationIdPConfig.remove("storeToken");
        federationIdPConfig.remove("trustEmail");
        federationIdPConfig.remove("syncMode");
        federationIdPConfig.remove("firstBrokerLoginFlowAlias");
        federationIdPConfig.remove("postBrokerLoginFlowAlias");
        model.getConfig().putAll(federationIdPConfig);
        return model;
    }

    public static IdentityProviderModel updateIdPFromEntityStatement(IdentityProviderModel model, EntityStatementExplicitResponse entityStatement) {
        RPMetadata rp = entityStatement.getMetadata().getRelyingPartyMetadata();
        model.setDisplayName(rp.getClientName());
        model.setEnabled(true);
        if (rp.getPostLogoutRedirectUris() != null && !rp.getPostLogoutRedirectUris().isEmpty()) {
            model.getConfig().put(OIDCIdentityProviderConfig.LOGOUT_URL, rp.getPostLogoutRedirectUris().get(0));
        } else {
            model.getConfig().remove(OIDCIdentityProviderConfig.LOGOUT_URL);
        }
        model.getConfig().put(OAuth2IdentityProviderConfig.DEFAULT_SCOPE, rp.getScope());
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_ID, rp.getClientId());
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_SECRET, rp.getClientSecret());
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_AUTH_METHOD, rp.getTokenEndpointAuthMethod());
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_ASSERTION_SIGNING_ALG, rp.getTokenEndpointAuthSigningAlg());
        model.getConfig().put(OpenIdFederationExplicitIdentityProviderConfig.TRUST_ANCHOR_ID, entityStatement.getTrustAnchor());
        model.getConfig().put(OpenIdFederationExplicitIdentityProviderConfig.AUTHORITY_HINTS, entityStatement.getAuthorityHints().stream().collect(Collectors.joining("##")));
        model.getConfig().put(OIDCConfigAttributes.EXPIRATION_TIME, String.valueOf(entityStatement.getExp()));
        return model;
    }

    public static ClientModel createOrUpdateAutomaticClient(String clientId, KeycloakSession session, RealmModel realm) throws IOException {
        String rpMetadata = OpenIdFederationUtils.getSelfSignedToken(clientId, session);
        TrustChainProcessor trustChainProcessor = session.getProvider(TrustChainProcessor.class, OpenIdFederationTrustChainProcessorFactory.PROVIDER_ID);
        EntityStatement rpEntityStatement = trustChainProcessor.parseAndValidateSelfSigned(rpMetadata);
        trustChainProcessor.validationRules(rpEntityStatement, false);
        logger.info("Starting validating trust chain for issuer: " + rpEntityStatement.getIssuer() + " with automatic registration as RP for realm: " + realm.getName());
        TrustChainResolution validChain = trustChainProcessor.constructTrustChains(rpMetadata, rpEntityStatement, realm.getOpenIdFederationGeneralConfig(), true);

        if (validChain == null) {
            throw new ErrorResponseException(Errors.INVALID_TRUST_ANCHOR, "No trusted trust anchor could be found", Response.Status.NOT_FOUND);
        }

        EventBuilder event = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
        OpenIdFederationClientRegistrationService provider = new OpenIdFederationClientRegistrationService(session);
        provider.setEvent(event);
        provider.setAuth(new ClientRegistrationAuth(session, provider, event, "openid-connect"));
        provider.createOrUpdateClient(rpEntityStatement, (RPMetadata) validChain.getMetadataAfterPolicies());

        // Automatic registration (§12.1): mark client and enforce asymmetric client authentication.
        // No client secret is assigned; subsequent authz/token/userinfo use the automatic attribute.
        ClientModel client = realm.getClientByClientId(clientId);
        if (client != null) {
            applyAutomaticRegistrationClientSettings(client);
        }
        return client;
    }

    /**
     * OpenID Federation §12.1: asymmetric cryptography MUST be used for Automatic Registration;
     * the OP neither assigns nor returns a Client Secret. Also require Request Object on later
     * authorization requests (defense in depth alongside the federation request parser).
     */
    public static void applyAutomaticRegistrationClientSettings(ClientModel client) {
        client.setAttribute(OIDCConfigAttributes.OPENID_FEDERATION_REGISTRATION, OIDCConfigAttributes.OPENID_FEDERATION_AUTOMATIC);
        String authenticator = client.getClientAuthenticatorType();
        if (authenticator == null || "client-secret".equals(authenticator)) {
            client.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
        }
    }
}
