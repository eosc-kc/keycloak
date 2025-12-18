package org.keycloak.protocol.oidc.federation;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

import java.util.Map;

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

