package org.keycloak.broker.saml.aggregate.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticator;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticatorFactory;
import org.keycloak.broker.saml.aggregate.SAMLAggregateConstants;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;

public class SAMLAggregateIdentityProviderAuthenticator implements Authenticator  {

    private static final Logger LOG = Logger.getLogger(IdentityProviderAuthenticator.class);

    protected static final String ACCEPTS_PROMPT_NONE = "acceptsPromptNoneForwardFromClient";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (context.getUriInfo().getQueryParameters().containsKey(AdapterConstants.KC_IDP_HINT)) {
            String providerId = context.getUriInfo().getQueryParameters().getFirst(AdapterConstants.KC_IDP_HINT);
            if (providerId == null || providerId.equals("")) {
                LOG.tracef("Skipping: kc_idp_hint query parameter is empty");
                context.attempted();
            } else {
                LOG.tracef("Redirecting: %s set to %s", AdapterConstants.KC_IDP_HINT, providerId);
                redirect(context, providerId);
            }
        } else if (context.getAuthenticatorConfig() != null && context.getAuthenticatorConfig().getConfig().containsKey(SAMLAggregateIdentityProviderAuthenticatorFactory.DEFAULT_PROVIDER)) {
            String defaultProvider = context.getAuthenticatorConfig().getConfig().get(SAMLAggregateIdentityProviderAuthenticatorFactory.DEFAULT_PROVIDER);
            LOG.tracef("Redirecting: default provider set to %s", defaultProvider);
            redirect(context, defaultProvider);
        } else {
            LOG.tracef("No default provider set or %s query parameter provided", AdapterConstants.KC_IDP_HINT);
            context.attempted();
        }
    }

    private void redirect(AuthenticationFlowContext context, String providerId) {
        List<IdentityProviderModel> identityProviders = context.getRealm().getIdentityProviders();
        for (IdentityProviderModel identityProvider : identityProviders) {
            if (identityProvider.isEnabled() && providerId.equals(identityProvider.getAlias())) {
                String accessCode = new ClientSessionCode<>(context.getSession(), context.getRealm(), context.getAuthenticationSession()).getOrGenerateCode();
                String clientId = context.getAuthenticationSession().getClient().getClientId();
                String tabId = context.getAuthenticationSession().getTabId();
                URI location = Urls.identityProviderAuthnRequest(context.getUriInfo().getBaseUri(), providerId, context.getRealm().getName(), accessCode, clientId, tabId);
                if (context.getAuthenticationSession().getClientNote(OAuth2Constants.DISPLAY) != null) {
                    location = UriBuilder.fromUri(location).queryParam(OAuth2Constants.DISPLAY, context.getAuthenticationSession().getClientNote(OAuth2Constants.DISPLAY)).build();
                }
                if (context.getUriInfo().getQueryParameters().containsKey(SAMLAggregateConstants.REDIRECT_TO_ENTITY_ID)) {
                    String entityId = context.getUriInfo().getQueryParameters().get(SAMLAggregateConstants.REDIRECT_TO_ENTITY_ID).get(0);
                    location = UriBuilder.fromUri(location).queryParam(SAMLAggregateConstants.REDIRECT_TO_ENTITY_ID, entityId).build();
                }
                Response response = Response.seeOther(location)
                        .build();
                // will forward the request to the IDP with prompt=none if the IDP accepts forwards with prompt=none.
                if ("none".equals(context.getAuthenticationSession().getClientNote(OIDCLoginProtocol.PROMPT_PARAM)) &&
                        Boolean.valueOf(identityProvider.getConfig().get(ACCEPTS_PROMPT_NONE))) {
                    context.getAuthenticationSession().setAuthNote(AuthenticationProcessor.FORWARDED_PASSIVE_LOGIN, "true");
                }
                LOG.debugf("Redirecting to %s", providerId);
                context.forceChallenge(response);
                return;
            }
        }

        LOG.warnf("Provider not found or not enabled for realm %s", providerId);
        context.attempted();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }
}
