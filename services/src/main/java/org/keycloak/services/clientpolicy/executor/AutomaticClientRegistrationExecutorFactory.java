package org.keycloak.services.clientpolicy.executor;

import java.util.Collections;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class AutomaticClientRegistrationExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "client-creation-for-oidfed-automatic-registration";

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new AutomaticClientRegistrationExecutor(session);
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Clients will be created for Automatic Client Registration of OIDFED";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return true;
    }

}
