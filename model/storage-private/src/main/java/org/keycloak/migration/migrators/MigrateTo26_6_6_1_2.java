/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.migration.migrators;

import org.keycloak.crypto.KeyCategory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.DefaultKeyProviders;

/**
 * Migrates existing realm key providers to the default {@link KeyCategory#GENERAL} category.
 */
public class MigrateTo26_6_6_1_2 extends RealmMigration {

    public static final ModelVersion VERSION = new ModelVersion("26.6.6");

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void migrateRealm(KeycloakSession session, RealmModel realm) {
        realm.getComponentsStream(realm.getId(), KeyProvider.class.getName()).forEach(component -> {
            if (component.getConfig().getFirst(DefaultKeyProviders.KEY_CATEGORY) == null) {
                component.getConfig().putSingle(DefaultKeyProviders.KEY_CATEGORY, KeyCategory.GENERAL.getSpecName());
                realm.updateComponent(component);
            }
        });
    }
}
