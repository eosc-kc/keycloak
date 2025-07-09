package org.keycloak.broker.oidc.federation;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.models.KeycloakSession;

public class OpenIdFederationIdentityProvider extends OIDCIdentityProvider {

    public OpenIdFederationIdentityProvider(KeycloakSession session, OpenIdFederationIdentityProviderConfig config) {
        super(session, config);
    }

}