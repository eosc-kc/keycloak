/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.storage.datastore;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.broker.federation.FederationProvider;
import org.keycloak.broker.federation.SAMLFederationProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.executors.ExecutorsProvider;
import org.keycloak.migration.MigrationModelManager;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.protocol.saml.ConfigureAutoUpdateSAMLClient;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.services.scheduled.*;
import org.keycloak.storage.*;
import org.keycloak.storage.managers.UserStorageSyncManager;
import org.keycloak.timer.TimerProvider;

import java.util.concurrent.ExecutorService;

public class LegacyDatastoreProviderFactory implements DatastoreProviderFactory, ProviderEventListener, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(LegacyDatastoreProviderFactory.class);

    private static final String PROVIDER_ID = "legacy";
    private static final String SAML_AUTO_UPDATED = "saml.auto.updated";
    private long clientStorageProviderTimeout;
    private long roleStorageProviderTimeout;
    private Runnable onClose;

    @Override
    public DatastoreProvider create(KeycloakSession session) {
        return new LegacyDatastoreProvider(this, session);
    }

    @Override
    public void init(Scope config) {
        clientStorageProviderTimeout = Config.scope("client").getLong("storageProviderTimeout", 3000L);
        roleStorageProviderTimeout = Config.scope("role").getLong("storageProviderTimeout", 3000L);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(this);
        onClose = () -> factory.unregister(this);
    }

    @Override
    public void close() {
        if (onClose != null) {
            onClose.run();
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    public long getClientStorageProviderTimeout() {
        return clientStorageProviderTimeout;
    }

    public long getRoleStorageProviderTimeout() {
        return roleStorageProviderTimeout;
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof PostMigrationEvent) {
            setupScheduledTasks(((PostMigrationEvent) event).getFactory());
        } else if (event instanceof LegacyStoreSyncEvent) {
            LegacyStoreSyncEvent ev = (LegacyStoreSyncEvent) event;
            UserStorageSyncManager.notifyToRefreshPeriodicSyncAll(ev.getSession(), ev.getRealm(), ev.getRemoved());
        } else if (event instanceof LegacyStoreMigrateRepresentationEvent) {
            LegacyStoreMigrateRepresentationEvent ev = (LegacyStoreMigrateRepresentationEvent) event;
            MigrationModelManager.migrateImport(ev.getSession(), ev.getRealm(), ev.getRep(), ev.isSkipUserDependent());
        }
    }

    public static void setupScheduledTasks(final KeycloakSessionFactory sessionFactory) {
        long interval = Config.scope("scheduled").getLong("interval", 900L) * 1000;

        try (KeycloakSession session = sessionFactory.create()) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            if (timer != null) {
                timer.schedule(new ClusterAwareScheduledTaskRunner(sessionFactory, new ClearExpiredEvents(), interval), interval, "ClearExpiredEvents");
                timer.schedule(new ClusterAwareScheduledTaskRunner(sessionFactory, new ClearExpiredAdminEvents(), interval), interval, "ClearExpiredAdminEvents");
                timer.schedule(new ClusterAwareScheduledTaskRunner(sessionFactory, new ClearExpiredClientInitialAccessTokens(), interval), interval, "ClearExpiredClientInitialAccessTokens");
                timer.schedule(new ClusterAwareScheduledTaskRunner(sessionFactory, new ClearExpiredUserSessions(), interval), interval, ClearExpiredUserSessions.TASK_NAME);
                timer.schedule(new ClusterAwareScheduledTaskRunner(sessionFactory, new RequiredActionsResetTask(), interval), interval, "RequiredActionsResetTask");
                autoUpdateTasks(sessionFactory, timer);
                ExecutorService executor = session.getProvider(ExecutorsProvider.class).getExecutor("idp-scheduled tasks");
                StartIdPScheduledTasks idPScheduledTasks = new StartIdPScheduledTasks();
                ScheduledTaskRunner task = new ScheduledTaskRunner(sessionFactory, idPScheduledTasks);
                executor.submit(task);
                UserStorageSyncManager.bootstrapPeriodic(sessionFactory, timer);
            }
        }
    }

    private static void autoUpdateTasks(final KeycloakSessionFactory sessionFactory, final TimerProvider timer) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                ConfigureAutoUpdateSAMLClient conf = session.getProvider(ConfigureAutoUpdateSAMLClient.class);
                session.realms().getRealmsStream().forEach(realm -> realm.getSAMLFederations().stream().forEach(fedModel -> {
                    FederationProvider federationProvider = SAMLFederationProviderFactory.getSAMLFederationProviderFactoryById(session, fedModel.getProviderId()).create(session, fedModel, realm.getId());
                    federationProvider.enableUpdateTask();
                    session.clients().getClientsStream(realm).filter(clientModel ->"saml".equals(clientModel.getProtocol()) && clientModel.getAttributes() != null && Boolean.valueOf(clientModel.getAttributes().get(SAML_AUTO_UPDATED))).forEach( clientModel -> conf.configure(clientModel, realm));
                }));

            }
        });
    }

    @Override
    public boolean isSupported() {
        return !Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }

}
