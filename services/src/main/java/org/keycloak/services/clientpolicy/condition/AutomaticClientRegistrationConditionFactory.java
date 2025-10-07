package org.keycloak.services.clientpolicy.condition;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

public class AutomaticClientRegistrationConditionFactory extends AbstractClientPolicyConditionProviderFactory {

    public static final String PROVIDER_ID = "automatic-registable-client";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        addCommonConfigProperties(configProperties);
    }

    @Override
    public ClientPolicyConditionProvider create(KeycloakSession session) {
        return new AutomaticClientRegistrationCondition(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "The condition for automatic client registration to be trigerred";
    }


    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
}
