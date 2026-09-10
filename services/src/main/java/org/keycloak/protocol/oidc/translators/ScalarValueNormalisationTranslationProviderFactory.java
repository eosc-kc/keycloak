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

public class ScalarValueNormalisationTranslationProviderFactory implements ProxiedTokenIntrospectionTranslationsProviderFactory {

    public static final String PROVIDER_ID = "scalar-value-normalisation";

    public static final String IS_REGEX = "isRegex";
    public static final String CLAIM = "claim";
    public static final String OLD_VALUE = "oldValue";
    public static final String NEW_VALUE = "newValue";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public ProxiedTokenIntrospectionTranslationsProvider create(KeycloakSession session, ComponentModel model) {
        return new ScalarValueNormalisationTranslationProvider(model);
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
        return "Normalisation of single-value claims";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(IS_REGEX)
                .label("Is Regular Expression")
                .helpText("Whether oldClaim and newClaim should be evaluated as regex expressions.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()
                .property()
                .name(CLAIM)
                .label("Claim Name")
                .helpText("Claim name to be normalised")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(OLD_VALUE)
                .label("Old Value")
                .helpText("The original value of the claim.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(NEW_VALUE)
                .label("New Value")
                .helpText("The replacement value for the claim.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .build();
    }
}
