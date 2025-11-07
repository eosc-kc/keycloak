package org.keycloak.protocol.saml;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.services.scheduled.AutoUpdateSAMLClient;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

import java.time.Instant;

public class DefaultConfigureAutoUpdateSAMLClient implements ConfigureAutoUpdateSAMLClient{

    private final TimerProvider timer;
    private final KeycloakSessionFactory sessionFactory;

    public DefaultConfigureAutoUpdateSAMLClient(KeycloakSession session){
        timer = session.getProvider(TimerProvider.class);
        sessionFactory = session.getKeycloakSessionFactory();
    }

    @Override
    public void configure(ClientModel clientModel, RealmModel realm) {
        AutoUpdateSAMLClient autoUpdateProvider = new AutoUpdateSAMLClient(clientModel.getId(), realm.getId());
        Long intervalClient = Long.parseLong(clientModel.getAttributes().get(SamlConfigAttributes.SAML_REFRESH_PERIOD)) * 1000;
        Long delay = clientModel.getAttributes().get(SamlConfigAttributes.SAML_LAST_REFRESH_TIME) == null ? 1000 : Long.parseLong(clientModel.getAttributes().get(SamlConfigAttributes.SAML_LAST_REFRESH_TIME) )+ Long.parseLong(clientModel.getAttributes().get(SamlConfigAttributes.SAML_REFRESH_PERIOD)) * 1000 - Instant.now().toEpochMilli();
        ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(sessionFactory, autoUpdateProvider, intervalClient);
        timer.schedule(taskRunner, delay > 1000 ? delay : 1000, intervalClient, "AutoUpdateSAMLClient_" + clientModel.getId());
    }

    @Override
    public void close() {

    }
}
