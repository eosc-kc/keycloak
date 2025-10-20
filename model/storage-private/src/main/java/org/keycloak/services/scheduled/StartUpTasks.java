package org.keycloak.services.scheduled;

import java.time.Instant;

import org.keycloak.models.KeycloakSession;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

public class StartUpTasks implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(StartUpTasks.class);

    @Override
    public void run(KeycloakSession session) {
        TimerProvider timer = session.getProvider(TimerProvider.class);
        session.realms().getRealmsStream().forEach(realm -> {
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
        });
    }
}
