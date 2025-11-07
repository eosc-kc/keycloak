package org.keycloak.protocol.saml;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.timer.TimerProvider;
import org.keycloak.timer.TimerProviderFactory;
import org.keycloak.timer.basic.BasicTimerProvider;
import org.keycloak.timer.basic.TimerTaskContextImpl;

import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultConfigureAutoUpdateSAMLClientFactory implements ConfigureAutoUpdateSAMLClientFactory {

    @Override
    public ConfigureAutoUpdateSAMLClient create(KeycloakSession session) {
        return new DefaultConfigureAutoUpdateSAMLClient(session);
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
    public String getId() {
        return "default";
    }


}
