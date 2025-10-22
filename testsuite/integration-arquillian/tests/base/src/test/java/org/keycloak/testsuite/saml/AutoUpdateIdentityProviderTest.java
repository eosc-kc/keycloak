package org.keycloak.testsuite.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.AbstractAdminTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.AdminEventPaths;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jboss.logging.Logger;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class AutoUpdateIdentityProviderTest extends AbstractAdminTest {

    private static final Logger log = Logger.getLogger(AutoUpdateIdentityProviderTest.class);

    @Test
    public void testAutoUpdatedSAMLIdP() {

        Undertow httpService = Undertow.builder().addHttpListener(8880, "localhost", new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                if (exchange.getRequestURI().endsWith("/saml-idp-metadata")) {
                    exchange.getResponseSender().send(StreamUtil.readString(getClass().getClassLoader().getResourceAsStream("identityprovider/saml-idp-metadata.xml")));
                }
            }
        }).build();
        httpService.start();

        try {
            // import metadata from url
            HashMap<String, Object> map = new HashMap<>();
            map.put("providerId", "saml");
            map.put("fromUrl", "http://localhost:8880/saml-idp-metadata");

            Map<String, String> result = realm.identityProviders().importFrom(map);
            assertSamlImport(result);

            // Create new SAML identity provider using configuration retrieved from import-config
            //change some values( postBindingLogout,postBindingAuthnRequest from true to false, enabled false)  - add autoupdated values
            result.put(IdentityProviderModel.AUTO_UPDATE, "true");
            result.put(IdentityProviderModel.METADATA_URL, "http://localhost:8880/saml-idp-metadata");
            result.put(IdentityProviderModel.REFRESH_PERIOD, String.valueOf(30));
            result.put(SAMLIdentityProviderConfig.POST_BINDING_LOGOUT, "false");
            result.put(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "false");
            create(createRep("saml", "saml", false, result));

            IdentityProviderResource provider = realm.identityProviders().get("saml");
            IdentityProviderRepresentation rep = provider.toRepresentation();
            Assert.assertNotNull("IdentityProviderRepresentation not null", rep);
            Assert.assertNotNull("internalId", rep.getInternalId());
            Assert.assertEquals("alias", "saml", rep.getAlias());
            Assert.assertEquals("providerId", "saml", rep.getProviderId());
            Assert.assertEquals("enabled", false, rep.isEnabled());
            assertSamlConfigAutoUpdated(rep.getConfig(), false);
            assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.identityProviderPath(rep.getAlias()), rep, ResourceType.IDENTITY_PROVIDER);

            sleep(80000);
            //autoupdated - check again Idp - see if values has changed
            provider = realm.identityProviders().get("saml");
            rep = provider.toRepresentation();
            Assert.assertEquals("enabled", true, rep.isEnabled());
            assertSamlConfigAutoUpdated(rep.getConfig(), true);

        } finally {
            httpService.stop();
        }
    }

    private void assertSamlImport(Map<String, String> config) {
        //firtsly check and remove enabledFromMetadata from config
        boolean enabledFromMetadata = Boolean.valueOf(config.get(SAMLIdentityProviderConfig.ENABLED_FROM_METADATA));
        config.remove(SAMLIdentityProviderConfig.ENABLED_FROM_METADATA);
        Assert.assertTrue(enabledFromMetadata);
        assertSamlConfig(config);
    }

    private void assertSamlConfig(Map<String, String> config) {
        // import endpoint simply converts IDPSSODescriptor into key value pairs.
        // check that saml-idp-metadata.xml was properly converted into key value pairs
        //System.out.println(config);
        List<String> keys = new ArrayList<>(List.of(
                "validateSignature",
                "singleLogoutServiceUrl",
                "postBindingLogout",
                "artifactBindingResponse",
                "postBindingAuthnRequest",
                "singleSignOnServiceUrl",
                "artifactResolutionServiceUrl",
                "wantAuthnRequestsSigned",
                "nameIDPolicyFormat",
                "signingCertificate",
                "addExtensionsElementWithKeyInfo",
                "loginHint",
                "idpEntityId",
                "entityAttributes",
                "hideOnLoginPage",
                "metadataDescriptorUrl"
        ));

        assertThat(config.keySet(), containsInAnyOrder(keys.toArray()));
        assertThat(config, hasEntry("validateSignature", "true"));
        assertThat(config, hasEntry("singleLogoutServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml"));
        assertThat(config, hasEntry("artifactResolutionServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml/resolve"));
        assertThat(config, hasEntry("artifactBindingResponse", "false"));
        assertThat(config, hasEntry("postBindingAuthnRequest", "true"));
        assertThat(config, hasEntry("singleSignOnServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml"));
        assertThat(config, hasEntry("wantAuthnRequestsSigned", "true"));
        assertThat(config, hasEntry("addExtensionsElementWithKeyInfo", "false"));
        assertThat(config, hasEntry("nameIDPolicyFormat", "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"));
        assertThat(config, hasEntry("idpEntityId", "http://localhost:8080/auth/realms/master"));
        assertThat(config, hasEntry(is("signingCertificate"), notNullValue()));
    }

    private void assertSamlConfigAutoUpdated(Map<String, String> config, boolean hasExecuted) {
        // import endpoint simply converts IDPSSODescriptor into key value pairs.
        // check that saml-idp-metadata.xml was properly converted into key value pairs
        //System.out.println(config);
        List<String> keys = new ArrayList<>(List.of("syncMode",
                "validateSignature",
                "singleLogoutServiceUrl",
                "postBindingLogout",
                "artifactBindingResponse",
                "postBindingAuthnRequest",
                "singleSignOnServiceUrl",
                "artifactResolutionServiceUrl",
                "wantAuthnRequestsSigned",
                "nameIDPolicyFormat",
                "signingCertificate",
                "addExtensionsElementWithKeyInfo",
                "loginHint",
                "idpEntityId",
                "entityAttributes",
                "metadataDescriptorUrl",
                "autoUpdate",
                "metadataUrl",
                "refreshPeriod"
        ));
        if (hasExecuted)
            keys.add("lastRefreshTime");

        assertThat(config.keySet(), containsInAnyOrder(keys.toArray()));
        assertThat(config, hasEntry("validateSignature", "true"));
        assertThat(config, hasEntry("singleLogoutServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml"));
        assertThat(config, hasEntry("artifactResolutionServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml/resolve"));
        assertThat(config, hasEntry("artifactBindingResponse", "false"));
        assertThat(config, hasEntry("postBindingAuthnRequest", "true"));
        assertThat(config, hasEntry("singleSignOnServiceUrl", "http://localhost:8080/auth/realms/master/protocol/saml"));
        assertThat(config, hasEntry("wantAuthnRequestsSigned", "true"));
        assertThat(config, hasEntry("addExtensionsElementWithKeyInfo", "false"));
        assertThat(config, hasEntry("nameIDPolicyFormat", "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"));
        assertThat(config, hasEntry("idpEntityId", "http://localhost:8080/auth/realms/master"));
        assertThat(config, hasEntry(is("signingCertificate"), notNullValue()));
        assertThat(config, hasEntry("autoUpdate", "true"));
        assertThat(config, hasEntry("metadataUrl", "http://localhost:8880/saml-idp-metadata"));
        assertThat(config, hasEntry("refreshPeriod", String.valueOf(60)));
    }


    @Test
    public void testAutoUpdatedOIDCIdP() {

        Undertow httpService = Undertow.builder().addHttpListener(8880, "localhost", new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                if (exchange.getRequestURI().endsWith("/oidc-idp")) {
                    exchange.getResponseSender().send(StreamUtil.readString(getClass().getClassLoader().getResourceAsStream("identityprovider/oidc-idp.json")));
                }
            }
        }).build();
        httpService.start();

        try {
            // import metadata from url
            HashMap<String, Object> map = new HashMap<>();
            map.put("providerId", "oidc");
            map.put("fromUrl", "http://localhost:8880/oidc-idp");

            Map<String, String> result = realm.identityProviders().importFrom(map);
            assertThat(result.keySet(), containsInAnyOrder("issuer", "authorizationUrl", "tokenUrl", "userInfoUrl", "validateSignature", "useJwksUrl", "jwksUrl", "metadataDescriptorUrl"));
            assertThat(result, hasEntry("authorizationUrl", "https://aai.egi.eu/oidc/authorize"));
            assertThat(result, hasEntry("tokenUrl", "https://aai.egi.eu/oidc/token"));

            // Create new OIDC identity provider using configuration retrieved from import-config
            //change some values( authorizationUrl,tokenUrl)  - add autoupdated values
            result.put(IdentityProviderModel.AUTO_UPDATE, "true");
            result.put(IdentityProviderModel.METADATA_URL, "http://localhost:8880/oidc-idp");
            result.put(IdentityProviderModel.REFRESH_PERIOD, String.valueOf(60));
            result.put("authorizationUrl", "https://aai.egi.eu/oidc/authorize/new");
            result.put("tokenUrl", "https://aai.egi.eu/oidc/token/new");
            create(createRep("auto-oidc", "oidc", true, result));

            IdentityProviderResource provider = realm.identityProviders().get("auto-oidc");
            IdentityProviderRepresentation rep = provider.toRepresentation();
            Assert.assertNotNull("IdentityProviderRepresentation not null", rep);
            Assert.assertNotNull("internalId", rep.getInternalId());
            Assert.assertEquals("alias", "auto-oidc", rep.getAlias());
            Assert.assertEquals("providerId", "oidc", rep.getProviderId());
            Assert.assertEquals("enabled", true, rep.isEnabled());
            assertThat(rep.getConfig(), hasEntry("authorizationUrl", "https://aai.egi.eu/oidc/authorize/new"));
            assertThat(rep.getConfig(), hasEntry("tokenUrl", "https://aai.egi.eu/oidc/token/new"));
            assertOidcConfig(rep.getConfig(), false);
            assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.identityProviderPath(rep.getAlias()), rep, ResourceType.IDENTITY_PROVIDER);

            sleep(80000);
            //autoupdated - check again Idp - see if values has changed
            provider = realm.identityProviders().get("auto-oidc");
            rep = provider.toRepresentation();
            Assert.assertEquals("enabled", true, rep.isEnabled());
            assertThat(rep.getConfig(), hasEntry("authorizationUrl", "https://aai.egi.eu/oidc/authorize"));
            assertThat(rep.getConfig(), hasEntry("tokenUrl", "https://aai.egi.eu/oidc/token"));
            assertOidcConfig(rep.getConfig(), true);

        } finally {
            httpService.stop();
        }
    }

    private void assertOidcConfig(Map<String, String> config, boolean hasExecuted) {
        Set fields = Stream.of("issuer", "authorizationUrl", "tokenUrl", "userInfoUrl", "validateSignature", "useJwksUrl", "jwksUrl", "metadataDescriptorUrl", "syncMode", "autoUpdate", "metadataUrl", "refreshPeriod").collect(Collectors.toSet());
        //autoupdated has been executed -  add lastRefreshTime
        if (hasExecuted)
            fields.add("lastRefreshTime");
        assertThat(config.keySet(), containsInAnyOrder(fields.toArray()));
        assertThat(config, hasEntry("issuer", "https://aai.egi.eu/oidc/"));
        assertThat(config, hasEntry("userInfoUrl", "https://aai.egi.eu/oidc/userinfo"));
        assertThat(config, hasEntry("validateSignature", "true"));
        assertThat(config, hasEntry("useJwksUrl", "true"));
        assertThat(config, hasEntry("jwksUrl", "https://aai.egi.eu/oidc/jwk"));
        assertThat(config, hasEntry("autoUpdate", "true"));
        assertThat(config, hasEntry("metadataUrl", "http://localhost:8880/oidc-idp"));
        assertThat(config, hasEntry("refreshPeriod", String.valueOf(60)));
    }

    private void create(IdentityProviderRepresentation idpRep) {
        Response response = realm.identityProviders().create(idpRep);
        Assert.assertNotNull(ApiUtil.getCreatedId(response));
        response.close();

        getCleanup().addIdentityProviderAlias(idpRep.getAlias());

    }

    private IdentityProviderRepresentation createRep(String id, String providerId,boolean enabled, Map<String, String> config) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();

        idp.setAlias(id);
        idp.setDisplayName(id);
        idp.setProviderId(providerId);
        idp.setEnabled(enabled);
        if (config != null) {
            idp.setConfig(config);
        }
        return idp;
    }

    private static void sleep(long ms) {
        try {
            log.infof("Sleeping for %d ms", ms);
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

}
