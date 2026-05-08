package org.keycloak.authentication.authenticators.client;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.ishare.Ishare;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.util.BasicAuthHelper;

import org.jboss.logging.Logger;

public class ISHAREClientAuthenticator extends AbstractClientAuthenticator {

    private static final Logger logger = Logger.getLogger(ISHAREClientAuthenticator.class);

    public static final String PROVIDER_ID = "client-ishare";

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
            context.challenge(challengeResponse);
            return;
        }

        if (client_assertion == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Missing client_assertion parameter");
            context.challenge(challengeResponse);
            return;
        }

        Ishare iSHARE = new Ishare(context.getSession());

        String issuer = context.getRealm().getIssuer();
        if (issuer == null || issuer.isEmpty()) {
            context.attempted();
            return;
        }

        if (!iSHARE.verifyClientToken(issuer, client_assertion)) {
            logger.errorf("client assertion INVALID!");
            context.attempted();
            return;
        }
        logger.info("client assertion verified!");

        context.getEvent().client(client_id);

        ClientModel client = context.getSession().clients().getClientByClientId(context.getRealm(), client_id);
        if (client == null) {
            logger.info("iSHARE client does not exist");
            context.failure(AuthenticationFlowError.CLIENT_NOT_FOUND, null);
            return;
        }

        context.setClient(client);

        if (!client.isEnabled()) {
            context.failure(AuthenticationFlowError.CLIENT_DISABLED, null);
            return;
        }

        context.success();
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
    public Map<String, Object> getAdapterConfiguration(ClientModel client) {
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
