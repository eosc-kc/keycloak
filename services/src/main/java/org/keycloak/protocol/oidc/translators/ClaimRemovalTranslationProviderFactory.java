package org.keycloak.protocol.oidc.translators;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.AccessTokenIntrospectionProvider;
import org.keycloak.protocol.oidc.ProxiedTokenIntrospectionTranslationsProvider;
import org.keycloak.protocol.oidc.ProxiedTokenIntrospectionTranslationsProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class ClaimRemovalTranslationProviderFactory implements ProxiedTokenIntrospectionTranslationsProviderFactory {

    public static final String PROVIDER_ID = "claim-removal";

    public static final String IS_REGEX = "isRegex";
    public static final String CLAIM = "claim";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public ProxiedTokenIntrospectionTranslationsProvider create(KeycloakSession session, ComponentModel model) {
        return new ClaimRemovalTranslationProvider(model);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getHelpText() {
        return "Translates or renames claims within a proxied remote token introspection response.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(AccessTokenIntrospectionProvider.ORDER)
                .label("Order")
                .helpText("Order of the translation provider.")
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .add()
                .property()
                .name(CLAIM)
                .label("Claim Name")
                .helpText("Claim name, or a regular expression if isRegex is true, to be removed.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(IS_REGEX)
                .label("Is Regular Expression")
                .helpText("Whether oldClaim and newClaim should be evaluated as regex expressions.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()
                .build();
    }
}
