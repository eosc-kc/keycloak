package org.keycloak.protocol.oidc.translators;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.ProxiedTokenIntrospectionTranslationsProvider;
import org.keycloak.protocol.oidc.ProxiedTokenIntrospectionTranslationsProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class ClaimRenamingTranslationProviderFactory implements ProxiedTokenIntrospectionTranslationsProviderFactory {

    public static final String PROVIDER_ID = "claim-renaming";

    public static final String IS_REGEX = "isRegex";
    public static final String OVERRIDE_NEW_CLAIM = "overrideNewClaim";
    public static final String OLD_CLAIM = "oldClaim";
    public static final String NEW_CLAIM = "newClaim";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public ProxiedTokenIntrospectionTranslationsProvider create(KeycloakSession session, ComponentModel model) {
        return new ClaimRenamingTranslationProvider(model);
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
                .name(OLD_CLAIM)
                .label("Old Claim Name")
                .helpText("The original claim name, or a regular expression if isRegex is true.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(NEW_CLAIM)
                .label("New Claim Name")
                .helpText("The replacement claim name, or a replacement expression (e.g., $1) if isRegex is true.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(IS_REGEX)
                .label("Is Regular Expression")
                .helpText("Whether oldClaim and newClaim should be evaluated as regex expressions.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()
                .property()
                .name(OVERRIDE_NEW_CLAIM)
                .label("Override Existing New Claim")
                .helpText("If true and target claim already exists, overwrite it. If false, proxied token introspection does not change.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()
                .build();
    }
}
