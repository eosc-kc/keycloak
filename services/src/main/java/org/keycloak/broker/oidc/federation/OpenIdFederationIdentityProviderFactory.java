package org.keycloak.broker.oidc.federation;

import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

import java.io.InputStream;

public class OpenIdFederationIdentityProviderFactory extends OIDCIdentityProviderFactory {

    public static final String PROVIDER_ID = "openid-federation";

    @Override
    public String getName() {
        return "OpenId Connect Federation";
    }

    @Override
    public OpenIdFederationIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new OpenIdFederationIdentityProvider(session, new OpenIdFederationIdentityProviderConfig(model));
    }

    @Override
    public OpenIdFederationIdentityProviderConfig createConfig() {
        return new OpenIdFederationIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public IdentityProviderModel parseConfig(KeycloakSession session, InputStream inputStream, IdentityProviderModel model) {
        return parseOIDCConfig(inputStream, model, OpenIdFederationIdentityProviderConfig.class);
    }

}