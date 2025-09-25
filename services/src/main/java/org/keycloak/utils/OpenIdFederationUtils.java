package org.keycloak.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.federation.OpenIdFederationIdentityProviderConfig;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OpenIdFederationConfig;
import org.keycloak.models.OpenIdFederationGeneralConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.federation.OpenIdFederationWellKnownProviderFactory;
import org.keycloak.protocol.trustchain.TrustChainProcessor;
import org.keycloak.representations.openid_federation.CommonMetadata;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.EntityStatementExplicitResponse;
import org.keycloak.representations.openid_federation.RPMetadata;
import org.keycloak.representations.openid_federation.TrustChainResolution;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.clientregistration.ClientRegistrationAuth;
import org.keycloak.services.clientregistration.openid_federation.OpenIdFederationClientRegistrationService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenIdFederationUtils {

    private static final Logger logger = Logger.getLogger(OpenIdFederationUtils.class);
    private static final String WELL_KNOWN_SUBPATH = ".well-known/openid-federation";
    public static final String SUBJECT_TYPES_SUPPORTED = "subject_types_supported";

    public static String getSelfSignedToken(String issuer, KeycloakSession session) throws IOException {
        issuer = issuer.trim();
        if (!issuer.endsWith("/")) issuer += "/";
        return SimpleHttp.doGet((issuer + WELL_KNOWN_SUBPATH), session).asString();
    }

    public static String getSubordinateToken(String fedApiUrl, String subject, KeycloakSession session) throws IOException {
        return SimpleHttp.doGet((fedApiUrl + "?sub=" + urlEncode(subject)),session).asString();
    }

    public static boolean containedInListEndpoint(String listApiUrl, String entityType, String issuer, KeycloakSession session) throws IOException {
        return listApiUrl != null && SimpleHttp.doGet(listApiUrl + "?entity_type=" + entityType, session).asJson(new TypeReference<List<String>>(){}).contains(issuer);
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

    public static void convertEntityStatementToIdp(IdentityProviderModel model, RealmModel realm, EntityStatementExplicitResponse entityStatement, Map<String, String> federationIdPConfig) {
        RPMetadata rp = entityStatement.getMetadata().getRelyingPartyMetadata();

        model.setProviderId(OpenIdFederationWellKnownProviderFactory.PROVIDER_ID);
        model.setAddReadTokenRoleOnCreate(Boolean.valueOf(federationIdPConfig.get("addReadTokenRoleOnCreate")));
        model.setDisplayName(rp.getClientName());
        model.setEnabled(true);
        model.setLinkOnly(Boolean.valueOf(federationIdPConfig.get("linkOnly")));
        model.setStoreToken(Boolean.valueOf(federationIdPConfig.get("storeToken")));
        model.setTrustEmail(Boolean.valueOf(federationIdPConfig.get("trustEmail")));
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
        if (rp.getPostLogoutRedirectUris() != null && !rp.getPostLogoutRedirectUris().isEmpty()) {
            model.getConfig().put(OIDCIdentityProviderConfig.LOGOUT_URL, rp.getPostLogoutRedirectUris().get(0));
        }
        model.getConfig().put(OAuth2IdentityProviderConfig.DEFAULT_SCOPE, rp.getScope());
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_ID, rp.getClientId());
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_SECRET, rp.getClientSecret());
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_AUTH_METHOD, rp.getTokenEndpointAuthMethod());
        model.getConfig().put(OAuth2IdentityProviderConfig.CLIENT_ASSERTION_SIGNING_ALG, rp.getTokenEndpointAuthSigningAlg());
        model.getConfig().put(OpenIdFederationIdentityProviderConfig.TRUST_ANCHOR_ID, entityStatement.getTrustAnchor());
        model.getConfig().put(OpenIdFederationIdentityProviderConfig.AUTHORITY_HINTS, entityStatement.getAuthorityHints().stream().collect(Collectors.joining("##")));
        model.getConfig().put(OIDCConfigAttributes.EXPIRATION_TIME, String.valueOf(entityStatement.getExp()));
        model.getConfig().putAll(federationIdPConfig);
    }

    public static ClientModel createOrUpdateAutomaticClient(String clientId, KeycloakSession session, RealmModel realm) throws IOException, InvalidTrustChainException {
        String rpMetadata = OpenIdFederationUtils.getSelfSignedToken(clientId, session);
        TrustChainProcessor trustChainProcessor = session.getProvider(TrustChainProcessor.class, OpenIdFederationTrustChainProcessorFactory.PROVIDER_ID);
        EntityStatement rpEntityStatement = trustChainProcessor.parseAndValidateSelfSigned(rpMetadata);
        trustChainProcessor.validationRules(rpEntityStatement, false);
        logger.info("starting validating trust chains");
        TrustChainResolution validChain = trustChainProcessor.constructTrustChains(rpEntityStatement, realm.getOpenIdFederations().stream().map(OpenIdFederationConfig::getTrustAnchor).collect(Collectors.toSet()), true);

        if (validChain == null) {
            throw new ErrorResponseException(Errors.INVALID_TRUST_ANCHOR, "No trusted trust anchor could be found", Response.Status.NOT_FOUND);
        }

        EventBuilder event = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
        OpenIdFederationClientRegistrationService provider = new OpenIdFederationClientRegistrationService(session);
        provider.setEvent(event);
        provider.setAuth(new ClientRegistrationAuth(session, provider, event, "openid-connect"));
        provider.createOrUpdateClient(rpEntityStatement, (RPMetadata) validChain.getMetadataAfterPolicies());
        return realm.getClientByClientId(clientId);
    }
}
