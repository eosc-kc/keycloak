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
package org.keycloak.tests.keys;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyCategory;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedRsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies protocol-specific key selection with fallback to general keys.
 */
@KeycloakIntegrationTest
@DatabaseTest
public class KeyCategoryTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void defaultKeysAreGeneral() {
        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();
        assertNotNull(keys.getKeys());
        keys.getKeys().stream()
                .filter(k -> KeyUse.SIG.equals(k.getUse()) && Algorithm.RS256.equals(k.getAlgorithm()))
                .forEach(k -> assertEquals(KeyCategory.GENERAL.getSpecName(), k.getCategory()));
    }

    @Test
    public void oidcSpecificKeyPreferredOverGeneral() throws Exception {
        String generalKid = findActiveGeneralKid();

        String oidcProviderId = createRsaKey("oidc-sig", KeyCategory.OIDC, "200");
        KeysMetadataRepresentation.KeyMetadataRepresentation oidcKey = findKeyByProviderId(oidcProviderId);
        assertEquals(KeyCategory.OIDC.getSpecName(), oidcKey.getCategory());
        assertNotEquals(generalKid, oidcKey.getKid());

        String realmName = realm.getName();
        String oidcKid = oidcKey.getKid();
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            KeyWrapper activeOidc = session.keys().getActiveKey(realmModel, KeyUse.SIG, Algorithm.RS256, KeyCategory.OIDC);
            assertEquals(oidcKid, activeOidc.getKid());
            assertEquals(KeyCategory.OIDC, activeOidc.getCategory());

            // SAML should still use general when no SAML-specific key exists
            KeyWrapper activeSaml = session.keys().getActiveKey(realmModel, KeyUse.SIG, Algorithm.RS256, KeyCategory.SAML);
            assertEquals(KeyCategory.GENERAL, activeSaml.getCategory());
            assertNotEquals(oidcKid, activeSaml.getKid());
        });
    }

    @Test
    public void samlSpecificKeyPreferredOverGeneral() throws Exception {
        String generalKid = findActiveGeneralKid();

        String samlProviderId = createRsaKey("saml-sig", KeyCategory.SAML, "200");
        KeysMetadataRepresentation.KeyMetadataRepresentation samlKey = findKeyByProviderId(samlProviderId);
        assertEquals(KeyCategory.SAML.getSpecName(), samlKey.getCategory());
        assertNotEquals(generalKid, samlKey.getKid());

        String realmName = realm.getName();
        String samlKid = samlKey.getKid();
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            KeyWrapper activeSaml = session.keys().getActiveKey(realmModel, KeyUse.SIG, Algorithm.RS256, KeyCategory.SAML);
            assertEquals(samlKid, activeSaml.getKid());
            assertEquals(KeyCategory.SAML, activeSaml.getCategory());

            // OIDC should still use general when no OIDC-specific key exists
            KeyWrapper activeOidc = session.keys().getActiveKey(realmModel, KeyUse.SIG, Algorithm.RS256, KeyCategory.OIDC);
            assertEquals(KeyCategory.GENERAL, activeOidc.getCategory());
            assertNotEquals(samlKid, activeOidc.getKid());
        });
    }

    @Test
    public void fallsBackToGeneralWhenSpecificMissing() {
        String realmName = realm.getName();
        String generalKid = findActiveGeneralKid();
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            KeyWrapper activeOidc = session.keys().getActiveKey(realmModel, KeyUse.SIG, Algorithm.RS256, KeyCategory.OIDC);
            KeyWrapper activeSaml = session.keys().getActiveKey(realmModel, KeyUse.SIG, Algorithm.RS256, KeyCategory.SAML);
            assertEquals(generalKid, activeOidc.getKid());
            assertEquals(generalKid, activeSaml.getKid());
            assertEquals(KeyCategory.GENERAL, activeOidc.getCategory());
            assertEquals(KeyCategory.GENERAL, activeSaml.getCategory());
        });
    }

    private String findActiveGeneralKid() {
        return realm.admin().keys().getKeyMetadata().getKeys().stream()
                .filter(k -> KeyUse.SIG.equals(k.getUse())
                        && Algorithm.RS256.equals(k.getAlgorithm())
                        && KeyCategory.GENERAL.getSpecName().equals(k.getCategory())
                        && "ACTIVE".equals(k.getStatus()))
                .findFirst()
                .orElseThrow()
                .getKid();
    }

    private KeysMetadataRepresentation.KeyMetadataRepresentation findKeyByProviderId(String providerId) {
        return realm.admin().keys().getKeyMetadata().getKeys().stream()
                .filter(k -> providerId.equals(k.getProviderId()))
                .findFirst()
                .orElseThrow();
    }

    private String createRsaKey(String name, KeyCategory category, String priority) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(name);
        rep.setParentId(realm.getId());
        rep.setProviderId(GeneratedRsaKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle(Attributes.PRIORITY_KEY, priority);
        rep.getConfig().putSingle(Attributes.KEY_CATEGORY, category.getSpecName());

        Response response = realm.admin().components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        response.close();
        realm.cleanup().add(r -> r.components().component(id).remove());
        return id;
    }
}
