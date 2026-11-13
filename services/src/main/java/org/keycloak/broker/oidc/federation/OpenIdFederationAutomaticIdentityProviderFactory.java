package org.keycloak.broker.oidc.federation;

import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

/**
 * Factory for OpenID Federation Automatic Registration IdP (Keycloak as RP).
 * Distinct from {@link OpenIdFederationIdentityProviderFactory} (explicit registration).
 */
public class OpenIdFederationAutomaticIdentityProviderFactory extends OIDCIdentityProviderFactory {

    public static final String PROVIDER_ID = "openid-federation-automatic";

    @Override
    public String getName() {
        return "OpenID Federation Automatic";
    }

    @Override
    public OpenIdFederationAutomaticIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new OpenIdFederationAutomaticIdentityProvider(session, new OpenIdFederationAutomaticIdentityProviderConfig(model));
    }

    @Override
    public OpenIdFederationAutomaticIdentityProviderConfig createConfig() {
        return new OpenIdFederationAutomaticIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public IdentityProviderModel parseConfig(KeycloakSession session, String config, IdentityProviderModel model) {
        return parseOIDCConfig(config, model, OpenIdFederationAutomaticIdentityProviderConfig.class);
    }
}
