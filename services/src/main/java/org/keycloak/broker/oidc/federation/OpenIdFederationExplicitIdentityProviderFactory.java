package org.keycloak.broker.oidc.federation;

import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

public class OpenIdFederationExplicitIdentityProviderFactory extends OIDCIdentityProviderFactory {

    public static final String PROVIDER_ID = "openid-federation-explicit";

    @Override
    public String getName() {
        return "OpenID Federation (explicit)";
    }

    @Override
    public OpenIdFederationExplicitIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new OpenIdFederationExplicitIdentityProvider(session, new OpenIdFederationExplicitIdentityProviderConfig(model));
    }

    @Override
    public OpenIdFederationExplicitIdentityProviderConfig createConfig() {
        return new OpenIdFederationExplicitIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public IdentityProviderModel parseConfig(KeycloakSession session, String config, IdentityProviderModel model) {
        return parseOIDCConfig(config, model, OpenIdFederationExplicitIdentityProviderConfig.class);
    }

}
