package org.keycloak.services.scheduled;


import java.time.Instant;
import java.util.Map;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.ConfigureAutoUpdateSAMLClient;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

public class StartUpTasks implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(StartUpTasks.class);
    private static final String SAML_AUTO_UPDATED = "saml.auto.updated";

    @Override
    public void run(KeycloakSession session) {
        TimerProvider timer = session.getProvider(TimerProvider.class);
        ConfigureAutoUpdateSAMLClient conf = session.getProvider(ConfigureAutoUpdateSAMLClient.class);
        session.realms().getRealmsStream().forEach(realm -> {
            session.getContext().setRealm(realm);
            realm.getSAMLFederations().stream().forEach(model -> {
                if (model.getLastMetadataRefreshTimestamp() == null) {
                    model.setLastMetadataRefreshTimestamp(0L);
                }
                UpdateFederation updateFederation = new UpdateFederation(model.getInternalId(), realm.getId());
                ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), updateFederation, model.getUpdateFrequencyInMins() * 60 * 1000);
                long delay = model.getLastMetadataRefreshTimestamp() + (model.getUpdateFrequencyInMins() * 60 * 1000) - Instant.now().toEpochMilli();
                timer.schedule(taskRunner, delay > 60 * 1000 ? delay : 60 * 1000, model.getUpdateFrequencyInMins() * 60 * 1000, "UpdateFederation" + model.getInternalId());
                logger.info("Initiating update task of federation with id: " + model.getInternalId());
            });
            session.identityProviders().getAllStream(Map.of(IdentityProviderModel.AUTO_UPDATE, "true"), null, null).forEach(idp -> {
                AutoUpdateIdentityProviders autoUpdateProvider = new AutoUpdateIdentityProviders(idp.getAlias(), realm.getId());
                ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), autoUpdateProvider, Long.valueOf(idp.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000);
                long delay = idp.getConfig().get(IdentityProviderModel.LAST_REFRESH_TIME) == null ? Long.parseLong(idp.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000 : Long.parseLong(idp.getConfig().get(IdentityProviderModel.LAST_REFRESH_TIME)) + (Long.parseLong(idp.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000) - Instant.now().toEpochMilli();
                timer.schedule(taskRunner, delay > 60 * 1000 ? delay : 60 * 1000, Long.valueOf(idp.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000, realm.getId() + "_AutoUpdateIdP_" + idp.getAlias());
            });
            session.clients().getClientsStream(realm).filter(clientModel ->"saml".equals(clientModel.getProtocol()) && clientModel.getAttributes() != null && Boolean.valueOf(clientModel.getAttributes().get(SAML_AUTO_UPDATED))).forEach( clientModel -> conf.configure(clientModel, realm));
        });
    }
}
