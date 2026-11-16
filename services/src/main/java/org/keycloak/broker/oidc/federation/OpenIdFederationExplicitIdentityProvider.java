package org.keycloak.broker.oidc.federation;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.Urls;

public class OpenIdFederationExplicitIdentityProvider extends OIDCIdentityProvider {

    public OpenIdFederationExplicitIdentityProvider(KeycloakSession session, OpenIdFederationExplicitIdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    public SimpleHttpRequest generateTokenRequest(String authorizationCode, EventBuilder event) {
        return generateTokenRequest(authorizationCode, event, getConfig(), Urls.openIdFederationAuthnResponse(session.getContext().getUri().getBaseUri(), session.getContext().getRealm().getName()).toString());
    }

}
