package org.keycloak.authentication.authenticators.client;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.ishare.Ishare;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.ClientCreationUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.util.BasicAuthHelper;

import org.jboss.logging.Logger;

public class ISHAREClientAuthenticator extends AbstractClientAuthenticator {

    private static final Logger logger = Logger.getLogger(ISHAREClientAuthenticator.class);

    public static final String PROVIDER_ID = "client-ishare";
    private static final List<EventType> EVENTS_FOR_CREATE_OR_ENABLE= Stream.of(EventType.CODE_TO_TOKEN, EventType.CLIENT_LOGIN, EventType.OAUTH2_DEVICE_AUTH).toList();

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        logger.debug("auth ISHARE");

        String client_id = null;
        String client_assertion = null;
        String client_assertion_type = null;

        String authorizationHeader = context.getHttpRequest().getHttpHeaders().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        MediaType mediaType = context.getHttpRequest().getHttpHeaders().getMediaType();
        boolean hasFormData = mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        MultivaluedMap<String, String> formData = hasFormData ? context.getHttpRequest().getDecodedFormParameters() : null;

        if (authorizationHeader != null) {
            String[] usernameSecret = BasicAuthHelper.RFC6749.parseHeader(authorizationHeader);
            if (usernameSecret != null) {
                client_id = usernameSecret[0];
            } else {
                // Don't send 401 if client_id parameter was sent in request. For example IE may automatically send "Authorization: Negotiate" in XHR requests even for public clients
                if (formData != null && !formData.containsKey(OAuth2Constants.CLIENT_ID)) {
                    Response challengeResponse = Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"" + context.getRealm().getName() + "\"").build();
                    context.challenge(challengeResponse);
                    return;
                }
            }
        }

        if (formData != null) {
            // even if basic challenge response exist, we check if client id was explicitly set in the request as a form param,
            // so we can also support clients overriding flows and using challenges (e.g: basic) to authenticate their users
            if (formData.containsKey(OAuth2Constants.CLIENT_ID)) {
                client_id = formData.getFirst(OAuth2Constants.CLIENT_ID);
            }

            if (formData.containsKey(OAuth2Constants.CLIENT_ASSERTION)) {
                client_assertion = formData.getFirst(OAuth2Constants.CLIENT_ASSERTION);
            }

            if (formData.containsKey(OAuth2Constants.CLIENT_ASSERTION_TYPE)) {
                client_assertion_type = formData.getFirst(OAuth2Constants.CLIENT_ASSERTION_TYPE);
            }
        }

        if (client_id == null) {
            client_id = context.getSession().getAttribute("client_id", String.class);
        }

        if (client_id == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Missing client_id parameter");
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
        }

        RealmModel realm = context.getRealm();
        ClientModel client = context.getSession().clients().getClientByClientId(realm, client_id);
        if( !isIShareEnabled(context.getRealm(), client)) {
            //TO BE REMOVED logging
            logger.info("No iSHARE enabled, skipping iSHARE client authentication");
            context.attempted();
            return;
        }

        if (client_assertion == null) {
            failBadRequest(Errors.INVALID_CLIENT_CREDENTIALS, "Missing client_assertion parameter");
        }

        if (!Ishare.CLIENT_ASSERTION_TYPE.equals(client_assertion_type) ) {
            failBadRequest(Errors.INVALID_CLIENT_CREDENTIALS,"client_assertion_type must be urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        }

        String scopeValue = formData.getFirst(OAuth2Constants.SCOPE);
        if (scopeValue == null || Stream.of(scopeValue).noneMatch(c -> c.equals(Constants.ISHARE_SCOPE))){
            failBadRequest(Errors.INVALID_REQUEST,"scope parameter does not contain iSHARE scope");
        }

        context.getEvent().client(client_id);
        try {
            Ishare iSHARE = new Ishare(context.getSession());
            if (!iSHARE.verifyClientToken(realm.getIssuer(),  realm.getAttribute(Constants.PR_ISSUER), client_assertion, client_id)) {
                failBadRequest(Errors.INVALID_REQUEST,"Ishare client assertion verification failed");
            }
        } catch (Exception e) {
            failBadRequest(Errors.INVALID_REQUEST,"Ishare verification failed: " + e.getMessage());
        }
        logger.info("client assertion verified!");

        if (client == null && EVENTS_FOR_CREATE_OR_ENABLE.contains(context.getEvent().getEvent().getType())) {
            logger.info("Create iSHARE client that does not exist");
            try {
                client = ClientCreationUtils.createIshareClient(realm, client_id);
                EventType eventType = context.getEvent().getEvent().getType();
                switch (eventType) {
                    case CODE_TO_TOKEN:
                        client.setStandardFlowEnabled(true);
                        break;
                    case CLIENT_LOGIN:
                        client.setStandardFlowEnabled(false);
                        client.setServiceAccountsEnabled(true);
                        break;
                    case OAUTH2_DEVICE_AUTH:
                        client.setStandardFlowEnabled(false);
                        client.setAttribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true");
                        break;
                    default:
                        client.setStandardFlowEnabled(false);
                        break;
                }
                client.updateClient();
                client = realm.getClientByClientId(client_id);
                if (EventType.CLIENT_LOGIN.equals(eventType)) {
                    new ClientManager(new RealmManager(context.getSession())).enableServiceAccount(client);
                }

            } catch (Exception e) {
                failBadRequest(Errors.CLIENT_NOT_FOUND, "Ishare client creation failed: " + e.getMessage());
            }
        } else if (client == null) {
            failBadRequest(Errors.CLIENT_NOT_FOUND, "iSHARE client not found and creation not allowed");
        }

        if (!client.isEnabled()) {
            //To be changed if expiration exists
            failBadRequest(Errors.CLIENT_DISABLED, "Ishare disable client with clientId : " + client_id);
        }

        context.setClient(client);
        context.success();
    }

    private boolean isIShareEnabled(RealmModel realm, ClientModel client) {
        return realm.getAttribute(Constants.ISHARE_ENABLED, false) && realm.getIssuer() != null && !realm.getIssuer().isEmpty() && (client == null || Boolean.valueOf(client.getAttribute(Constants.ISHARE_ENABLED)));
    }

    /**
     * iSHARE expects HTTP 400, throw specific AuthenticationFlowError.ISHARE_ERROR and return response
     */
    private static void failBadRequest(String error, String errorDescription) {
        logger.error(errorDescription);
        Response response = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), error, errorDescription);
        throw new AuthenticationFlowException(AuthenticationFlowError.ISHARE_ERROR, response);
    }

    @Override
    public void init(Config.Scope config) {
        super.init(config);
    }

    @Override
    public String getDisplayType() {
        return Constants.ISHARE_SCOPE;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getHelpText() {
        return "iSHARE Client Authenticator";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new LinkedList<>();
    }

    @Override
    public List<ProviderConfigProperty> getConfigPropertiesPerClient() {
        // doesnt seem to work yet:
        // https://keycloak.discourse.group/t/custom-per-client-configurable-clientauthenticator/24226
        // return configMetadata;
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getAdapterConfiguration(KeycloakSession session, ClientModel client)  {
        return Collections.emptyMap();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Set<String> getProtocolAuthenticatorMethods(String loginProtocol) {
        return Collections.emptySet();
    }
}
