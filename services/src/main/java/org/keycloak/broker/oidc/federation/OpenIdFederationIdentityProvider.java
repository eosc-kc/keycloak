package org.keycloak.broker.oidc.federation;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.Urls;

public class OpenIdFederationIdentityProvider extends OIDCIdentityProvider {

    public OpenIdFederationIdentityProvider(KeycloakSession session, OpenIdFederationIdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    public SimpleHttp generateTokenRequest(String authorizationCode, EventBuilder event) {
        return generateTokenRequest(authorizationCode, event, getConfig(), Urls.openIdFederationAuthnResponse(session.getContext().getUri().getBaseUri(), session.getContext().getRealm().getName()).toString());
    }

}