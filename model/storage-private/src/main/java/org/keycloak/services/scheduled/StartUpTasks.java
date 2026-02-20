package org.keycloak.services.scheduled;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.keycloak.common.util.Time;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
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
                long delay = model.getLastMetadataRefreshTimestamp() + (model.getUpdateFrequencyInMins() * 60 * 1000) - Time.currentTimeMillis();
                timer.schedule(taskRunner, delay > 900 * 1000 ? delay : 900 * 1000, model.getUpdateFrequencyInMins() * 60 * 1000, "UpdateFederation" + model.getInternalId());
                logger.info("Initiating update task of federation with id: " + model.getInternalId());
            });
            session.identityProviders().getAllStream(Map.of(IdentityProviderModel.AUTO_UPDATE, "true"), null, null).forEach(idp -> {
                AutoUpdateIdentityProviders autoUpdateProvider = new AutoUpdateIdentityProviders(idp.getAlias(), realm.getId());
                ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), autoUpdateProvider, Long.valueOf(idp.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000);
                long delay = idp.getConfig().get(IdentityProviderModel.LAST_REFRESH_TIME) == null ? Long.parseLong(idp.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000 : Long.parseLong(idp.getConfig().get(IdentityProviderModel.LAST_REFRESH_TIME)) + (Long.parseLong(idp.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000) - Time.currentTimeMillis();
                timer.schedule(taskRunner, delay > 60 * 1000 ? delay : 60 * 1000, Long.valueOf(idp.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000, realm.getId() + "_AutoUpdateIdP_" + idp.getAlias());
            });
            session.identityProviders().getAllStream(Map.of(IdentityProviderModel.ENABLED, "true", IdentityProviderModel.PROVIDER_ID, "openid-federation"), null, null).forEach(identityProvider -> {
                OpenIdFederationIdPExpirationTask task = new OpenIdFederationIdPExpirationTask(identityProvider.getAlias(), realm.getId());
                long expiration = Long.valueOf(identityProvider.getConfig().get(OIDCConfigAttributes.EXPIRATION_TIME)) * 1000 - Time.currentTimeMillis();
                ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), task, expiration);
                timer.scheduleOnce(taskRunner, expiration > 60 * 1000 ? expiration : 60 * 1000, "OpenIdFederationIdPExpirationTask_" + identityProvider.getAlias());
            });
            session.clients().getClientsStream(realm).forEach(clientModel -> {
                if ("saml".equals(clientModel.getProtocol()) && clientModel.getAttributes() != null && Boolean.valueOf(clientModel.getAttributes().get(SAML_AUTO_UPDATED))) {
                    conf.configure(clientModel, realm);
                } else if (clientModel.getAttributes().get(OIDCConfigAttributes.EXPIRATION_TIME) != null) {
                    OpenIdFederationClientExpirationTask federationTask = new OpenIdFederationClientExpirationTask(clientModel.getId(), realm.getId());
                    long expiration = (LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - Long.valueOf(clientModel.getAttribute(OIDCConfigAttributes.EXPIRATION_TIME))) * 1000;
                    ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), federationTask, expiration > 60 * 1000 ? expiration : 60 * 1000);
                    timer.scheduleOnce(taskRunner, expiration > 60 * 1000 ? expiration : 60 * 1000, "OpenidFederationExplicitClient_" + clientModel.getId());
                }
            });
            long requiredPeriod = 21600 * 1000;
            timer.schedule(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new RequiredActionsResetTask(realm.getId()), requiredPeriod), 900 * 1000, requiredPeriod, "RequiredActionsResetTask_" + realm.getId());

        });
    }
}
