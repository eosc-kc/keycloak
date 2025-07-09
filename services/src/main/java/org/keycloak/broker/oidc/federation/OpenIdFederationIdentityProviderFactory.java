package org.keycloak.broker.oidc.federation;

import jakarta.ws.rs.BadRequestException;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class OpenIdFederationIdentityProviderFactory extends AbstractIdentityProviderFactory<OpenIdFederationIdentityProvider> {

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
        throw new BadRequestException("OpenId Federation does not support configuration");
    }

}