/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.oidc;

import jakarta.ws.rs.client.Client;
import org.junit.Test;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.protocol.oidc.representations.MTLSEndpointAliases;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.OPMetadata;
import org.keycloak.services.clientregistration.ClientRegistrationService;
import org.keycloak.services.clientregistration.oidc.OIDCClientRegistrationProviderFactory;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.testsuite.wellknown.CustomOIDCWellKnownProviderFactory;

import java.io.IOException;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCWellKnownProviderTest extends AbstractWellKnownProviderTest {

    protected String getWellKnownProviderId() {
        return OIDCWellKnownProviderFactory.PROVIDER_ID;
    }

//    @Test
//    public void testOpenIdFederationDiscovery() {
//        Client client = AdminClientUtil.createResteasyClient();
//        try {
//            RealmResource testRealm = adminClient.realm("test");
//            RealmRepresentation realmRep = testRealm.toRepresentation();
//            realmRep.setOpenIdFederationEnabled(true);
//            realmRep.setOpenIdFederationOrganizationName("Keycloak");
//            realmRep.setOpenIdFederationResolveEndpoint("https://edugain.org/resolve");
//            realmRep.setOpenIdFederationTrustAnchors(Stream.of("https://edugain.org/trust-anchor").collect(Collectors.toList()));
//            realmRep.setOpenIdFederationAuthorityHints(Stream.of("https://edugain.org/federation").collect(Collectors.toList()));
//            realmRep.setOpenIdFederationClientRegistrationTypesSupported(Stream.of("EXPLICIT").collect(Collectors.toList()));
//            testRealm.update(realmRep);
//
//            //When Open Id Federation is configured
//            EntityStatement statement = getOIDCFederationDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);
//            Assert.assertNotNull("Entity Statement can not desirialize", statement);
//            String mainUrl = RealmsResource.realmBaseUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString();
//            assertEquals(mainUrl, statement.getIssuer());
//            assertEquals(mainUrl, statement.getSubject());
//            assertEquals("https://edugain.org/federation", statement.getAuthorityHints().get(0));
//            Assert.assertNotNull(statement.getMetadata().getFederationEntity());
//            assertEquals("Keycloak", statement.getMetadata().getFederationEntity().getCommonMetadata().getOrganizationName());
//            assertEquals("https://edugain.org/resolve", statement.getMetadata().getFederationEntity().getFederationResolveEndpoint());
//            OPMetadata op = statement.getMetadata().getOpenIdProviderMetadata();
//            assertEquals(1, op.getClientRegistrationTypes().size());
//            assertEquals("explicit", op.getClientRegistrationTypes().get(0));
//            assertEquals(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT).path(RealmsResource.class).path(RealmsResource.class, "getOpenIdFederationClientsService").build("test").toString(), op.getFederationRegistrationEndpoint());
//            testOidc(op);
//            String x = RealmAttributes.CLAIMS_SUPPORTED;
//
//            realmRep.getAttributes().put(RealmAttributes.OPENID_FEDERATION_ENABLED, Boolean.FALSE.toString());
//            testRealm.update(realmRep);
//        } finally {
//            client.close();
//        }
//    }
//
//    @Test
//    public void testWithoutOpenIdFederationDiscovery() {
//        Client client = AdminClientUtil.createResteasyClient();
//        try {
//            //When no Open Id Federation is configured
//            int responseStatus = getOIDCFederationDiscoveryConfiguration(client, OAuthClient.AUTH_SERVER_ROOT).getStatus();
//            assertEquals(responseStatus, 404);
//        } finally {
//            client.close();
//        }
//    }

    @Test
    public void testDefaultProviderCustomizations() throws IOException {
        Client client = AdminClientUtil.createResteasyClient();
        try {
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);

            // Assert that CustomOIDCWellKnownProvider was used as a prioritized provider over default OIDCWellKnownProvider
            MTLSEndpointAliases mtlsEndpointAliases = oidcConfig.getMtlsEndpointAliases();
            Assert.assertEquals("https://placeholder-host-set-by-testsuite-provider/registration", mtlsEndpointAliases.getRegistrationEndpoint());
            Assert.assertEquals("bar", oidcConfig.getOtherClaims().get("foo"));

            // Assert some configuration was overriden
            Assert.assertEquals("some-new-property-value", oidcConfig.getOtherClaims().get("some-new-property"));
            Assert.assertEquals("nested-value", ((Map) oidcConfig.getOtherClaims().get("some-new-property-compound")).get("nested1"));
            Assert.assertNames(oidcConfig.getIntrospectionEndpointAuthMethodsSupported(), "private_key_jwt", "client_secret_jwt", "tls_client_auth", "custom_nonexisting_authenticator");

            // Exact names already tested in OIDC
            assertScopesSupportedMatchesWithRealm(oidcConfig);

            // Temporarily disable client scopes
            getTestingClient().testing().setSystemPropertyOnServer(CustomOIDCWellKnownProviderFactory.INCLUDE_CLIENT_SCOPES, "false");
            oidcConfig = getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);
            Assert.assertNull(oidcConfig.getScopesSupported());
        } finally {
            getTestingClient().testing().setSystemPropertyOnServer(CustomOIDCWellKnownProviderFactory.INCLUDE_CLIENT_SCOPES, null);
            client.close();
        }
    }

}
