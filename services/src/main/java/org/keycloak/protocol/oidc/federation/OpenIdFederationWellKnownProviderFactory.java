package org.keycloak.protocol.oidc.federation;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.wellknown.WellKnownProvider;


public class OpenIdFederationWellKnownProviderFactory extends OIDCWellKnownProviderFactory {

    public static final String PROVIDER_ID = "openid-federation";

    @Override
    public WellKnownProvider create(KeycloakSession session) {
        return new OpenIdFederationWellKnownProvider(session, getOpenidConfigOverride());
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
