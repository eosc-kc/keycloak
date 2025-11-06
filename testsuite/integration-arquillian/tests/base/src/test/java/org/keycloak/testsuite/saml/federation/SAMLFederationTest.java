package org.keycloak.testsuite.saml.federation;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlPrincipalType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.UserAttributeStatementMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.FederationMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.SAMLFederationRepresentation;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.testsuite.AbstractAdminTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.util.JsonSerialization;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.apache.tools.ant.filters.StringInputStream;
import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SAMLFederationTest extends AbstractAdminTest {

    private static Undertow SERVER;

    private static final Logger log = Logger.getLogger(SAMLFederationTest.class);

    private static final Set<String> aliasSet = new HashSet<>(
            Arrays.asList(new String[]{"aHR0cHM6Ly9pZHAucmFzaC5hbC9zaW1wbGVzYW1sL3NhbWwyL2lkcC9tZXRhZGF0YS5waHA",
                    "aHR0cHM6Ly9odWMuaWRwLmtuYXcubmwvc2ltcGxlc2FtbC9zYW1sMi9pZHAvbWV0YWRhdGEucGhw", "aHR0cHM6Ly9pZHAuYWRtaW4uZ3JuZXQuZ3IvaWRwL3NoaWJib2xldGg"}));
    private static final String entityIdIdP = "https://idp.rash.al/simplesaml/saml2/idp/metadata.php";
    private static final String hashEntityIdIdP = "aHR0cHM6Ly9odWMuaWRwLmtuYXcubmwvc2ltcGxlc2FtbC9zYW1sMi9pZHAvbWV0YWRhdGEucGhw";
    private static final String authority = "http://aai.grnet.gr/";
    private static final String attributeName = "http://macedir.org/entity-category-support";
    private static final String attributeValue = "http://clarin.eu/category/clarin-member";
    private static final String entityIdClient1 = "loadbalancer-9.siroe.com";
    private static final String entityIdClient2 = "https://test-sp.tuke.sk/shibboleth";

    @BeforeClass
    public static void onBeforeClass() {
        SERVER = Undertow.builder().addHttpListener(8880, "localhost", new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                writeResponse(exchange.getRequestURI(), exchange);
            }

            private void writeResponse(String file, HttpServerExchange exchange) throws IOException {
                exchange.getResponseSender().send(
                        StreamUtil.readString(getClass().getResourceAsStream("/federation/saml" + file), Charset.defaultCharset()));
            }
        }).build();

        SERVER.start();

    }

    @AfterClass
    public static void onAfterClass() {
        if (SERVER != null) {
            SERVER.stop();
        }
    }


    public void testCreateUpdateAndRemoveAll() throws IOException {

        String internalId = createFederation("edugain-sample", "http://localhost:8880/edugain-sample-test.xml", "All", new HashSet<>(),
                new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashMap<>());
        createMapper(internalId);

        sleep(70000);

        // first execute trigger update idps and then get identity providers federation
        SAMLFederationRepresentation representation = realm.samlFederation()
                .getSAMLFederation(internalId);
        assertNotNull(representation);

        assertEquals("wrong federation alias", "edugain-sample", representation.getAlias());
        assertEquals("not saml federation", "saml", representation.getProviderId());
        assertEquals("wrong url", "http://localhost:8880/edugain-sample-test.xml",
                representation.getUrl());

        // must be three saml idps
        List<IdentityProviderRepresentation> idps = realm.identityProviders().findAll();
        log.infof("Identity Providers are %s", idps.size());
        idps.stream().filter(idp -> idp.getFederations() != null && !idp.getFederations().isEmpty()).forEach(idp -> {
            assertTrue("wrong IdPs", aliasSet.contains(idp.getAlias()));
            assertEquals("not saml IdP", "saml", idp.getProviderId());
            assertNotNull("empty IdP config", idp.getConfig());
            assertTrue("IdP singleSignOnServiceUrl not exist", idp.getConfig().containsKey("singleSignOnServiceUrl"));
            assertTrue("IdP postBindingAuthnRequest not exist", idp.getConfig().containsKey("postBindingAuthnRequest"));
            assertTrue(Boolean.valueOf(idp.getConfig().get("postBindingAuthnRequest")));
            //change postBindingAuthnRequest to false
            idp.getConfig().put("postBindingAuthnRequest", "false");
            IdentityProviderResource identityProviderResource = realm.identityProviders().get(idp.getAlias());
            identityProviderResource.update(idp);
            idp = identityProviderResource.toRepresentation();
            assertFalse(Boolean.valueOf(idp.getConfig().get("postBindingAuthnRequest")));
            //check that its idp has one attribute importer mapper
            List<IdentityProviderMapperRepresentation> mappers = identityProviderResource.getMappers();
            assertEquals(1, mappers.size());
            IdentityProviderMapperRepresentation mapper = mappers.get(0);
            assertEquals("my_mapper", mapper.getName());
            assertEquals("saml-user-attribute-idp-mapper", mapper.getIdentityProviderMapper());
            assertEquals("givenname", mapper.getConfig().get("attribute.name"));
            assertEquals("firstname", mapper.getConfig().get("user.attribute"));
        });

        //2 SAML Clients also
        assertExistSAMLClients(new ArrayList<>());

        //update federation in order to update idps based on xml
        representation.setUpdateFrequencyInMins(1);
        Response response = realm.samlFederation().create(representation);
        sleep(70000);
        //postBindingResponse must be true again
        representation = realm.samlFederation()
                .getSAMLFederation(internalId);
        assertNotNull(representation);
        idps = realm.identityProviders().findAll();
        idps.stream().filter(idp -> idp.getFederations() != null && !idp.getFederations().isEmpty()).forEach(idp -> {
            assertTrue("IdP postBindingResponse not exist", idp.getConfig().containsKey("postBindingAuthnRequest"));
            assertTrue(Boolean.valueOf(idp.getConfig().get("postBindingAuthnRequest")));
        });

    }


    public void testCreateClientsWithDefaultScopes() throws IOException {

        String scopeId = createClientScope("saml-scope");

        List<ProtocolMapperRepresentation> defaultMappers = new ArrayList<>();
        defaultMappers.add(makeSamlAttributeMapper(scopeId, "sn", "urn:oid:2.5.4.40"));
        defaultMappers.add(makeSamlAttributeMapper(scopeId, "email", "urn:oid:0.9.2342.19200300.100.1.3"));
        realm.addDefaultOptionalClientScope(scopeId);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.defaultOptionalClientScopePath(scopeId), ResourceType.CLIENT_SCOPE);

        String internalId = createFederation("edugain-sample", "http://localhost:8880/edugain-sample-test.xml", "Clients", new HashSet<>(),
                new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashMap<>());

        sleep(70000);
        try {
            // first execute trigger update idps and then get identity providers federation
            SAMLFederationRepresentation representation = realm.samlFederation()
                    .getSAMLFederation(internalId);
            assertNotNull(representation);

            assertEquals("wrong federation alias", "edugain-sample", representation.getAlias());
            assertEquals("not saml federation", "saml", representation.getProviderId());
            assertEquals("wrong url", "http://localhost:8880/edugain-sample-test.xml",
                    representation.getUrl());

            // must be three saml idps
            List<IdentityProviderRepresentation> idps = realm.identityProviders().findAll();
            log.infof("Identity Providers are %s", idps.size());
            assertTrue(idps.stream().noneMatch(idp -> idp.getFederations() != null && !idp.getFederations().isEmpty()));

            assertExistSAMLClients(defaultMappers);

        } finally {
            realm.clientScopes().get(scopeId).remove();
        }

    }


    public void testCreateWithDenyListandRemove() throws IOException {
        //blacklist entityIdIdP entity id and registrationAuthority="http://aai.grnet.gr/"
        String scopeId = createClientScope("saml-scope");

        List<ProtocolMapperRepresentation> defaultMappers = new ArrayList<>();
        defaultMappers.add(makeSamlAttributeMapper(scopeId, "sn", "urn:oid:2.5.4.40"));
        defaultMappers.add(makeSamlAttributeMapper(scopeId, "email", "urn:oid:0.9.2342.19200300.100.1.3"));
        realm.addDefaultOptionalClientScope(scopeId);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.defaultOptionalClientScopePath(scopeId), ResourceType.CLIENT_SCOPE);

        //create with excluding one idp
        Set<String> entityIdDenyList = new HashSet<>();
        entityIdDenyList.add(entityIdIdP);
        Set<String> authorityDenyList = new HashSet<>();
        authorityDenyList.add(authority);
        String internalId = createFederation("edugain-sample",
                "http://localhost:8880/edugain-sample-test.xml", "All", entityIdDenyList, new HashSet<>(), authorityDenyList, new HashSet<>(), new HashMap<>());

        sleep(70000);

        try {
            // first execute trigger update idps and then get identity providers federation
            SAMLFederationRepresentation representation = realm.samlFederation()
                    .getSAMLFederation(internalId);
            assertNotNull(representation);

            assertEquals("wrong federation alias", "edugain-sample", representation.getAlias());
            assertEquals("not saml federation", "saml", representation.getProviderId());
            assertEquals("wrong url", "http://localhost:8880/edugain-sample-test.xml",
                    representation.getUrl());

            //only client2 must be created
            List<ClientRepresentation> clients = realm.clients().findByClientId(entityIdClient1);
            assertEquals("Not expected to found " + entityIdClient1 + " client", clients.size(), 0);
            clients = realm.clients().findByClientId(entityIdClient2);
            assertEquals("Expected to found " + entityIdClient2 + " client", clients.size(), 1);
            ClientRepresentation client2 = clients.get(0);
            assertClient(client2, "Technical University of Kosice", null, "https://test-sp.tuke.sk/Shibboleth.sso/SAML2/Artifact", "https://test-sp.tuke.sk/Shibboleth.sso/SLO/Redirect");
            assertEquals(client2.getProtocolMappers().size(), 1);
            //check for equality for email mapper for client2
            ProtocolMapperRepresentation mapperEmail = client2.getProtocolMappers().get(0);
            assertEquals(mapperEmail.getName(), "email");
            assertEquals(mapperEmail.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE), "email");
            assertEquals(mapperEmail.getConfig().get(AttributeStatementHelper.SAML_ATTRIBUTE_NAME), "urn:oid:0.9.2342.19200300.100.1.3");
            assertEquals(mapperEmail.getConfig().get(AttributeStatementHelper.FRIENDLY_NAME), "email");

            // must be one idp
            List<IdentityProviderRepresentation> idps = realm.identityProviders().findAll();
            log.infof("Identity Providers are %s", idps.size());
            idps.stream().filter(idp -> idp.getFederations() != null && !idp.getFederations().isEmpty()).forEach(idp -> {
                assertEquals("wrong IdP", hashEntityIdIdP, idp.getAlias());
                assertEquals("not saml IdP", "saml", idp.getProviderId());
                assertNotNull("empty IdP config", idp.getConfig());
                assertTrue("IdP singleSignOnServiceUrl not exist", idp.getConfig().containsKey("singleSignOnServiceUrl"));
                assertTrue("IdP postBindingAuthnRequest not exist", idp.getConfig().containsKey("postBindingAuthnRequest"));
                assertTrue(Boolean.valueOf(idp.getConfig().get("postBindingAuthnRequest")));
            });

        } finally {
            realm.clientScopes().get(scopeId).remove();
        }
    }


    public void testCreateWithAllowListandRemove() throws IOException {
        //whitelist entityIdIdP entity id and registrationAuthority="http://aai.grnet.gr/"

        //create with excluding one idp
        Set<String> entityIdAllowList = new HashSet<>();
        entityIdAllowList.add(entityIdIdP);
        Set<String> authorityAllowList = new HashSet<>();
        authorityAllowList.add(authority);
        String internalId = createFederation("edugain-sample", "http://localhost:8880/edugain-sample-test.xml", "All", new HashSet<>(),
                entityIdAllowList, new HashSet<>(), authorityAllowList, new HashMap<>());

        sleep(70000);

        // first execute trigger update idps and then get identity providers federation
        SAMLFederationRepresentation representation = realm.samlFederation()
                .getSAMLFederation(internalId);
        assertNotNull(representation);

        assertEquals("wrong federation alias", "edugain-sample", representation.getAlias());
        assertEquals("not saml federation", "saml", representation.getProviderId());
        assertEquals("wrong url", "http://localhost:8880/edugain-sample-test.xml",
                representation.getUrl());

        //only client1 must be created
        List<ClientRepresentation> clients = realm.clients().findByClientId(entityIdClient2);
        assertEquals("Not expected to found " + entityIdClient2 + " client", clients.size(), 0);
        clients = realm.clients().findByClientId(entityIdClient1);
        assertEquals("Expected to found " + entityIdClient1 + " client", clients.size(), 1);
        ClientRepresentation client1 = clients.get(0);
        assertClient(client1, "siroe", "loadbalancer siroen client", "https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/Artifact", "https://LoadBalancer-9.siroe.com:3443/federation/SPSloRedirect/metaAlias/sp");
        assertNull(client1.getProtocolMappers());

        // must be two saml idps and not contain
        List<IdentityProviderRepresentation> idps = realm.identityProviders().findAll();
        log.infof("Identity Providers are %s", idps.size());
        idps.stream().filter(idp -> idp.getFederations() != null && !idp.getFederations().isEmpty()).forEach(idp -> {
            assertTrue("wrong IdPs", aliasSet.contains(idp.getAlias()));
            assertEquals("not saml IdP", "saml", idp.getProviderId());
            assertNotNull("empty IdP config", idp.getConfig());
            assertTrue("IdP singleSignOnServiceUrl not exist", idp.getConfig().containsKey("singleSignOnServiceUrl"));
            assertTrue("IdP postBindingAuthnRequest not exist", idp.getConfig().containsKey("postBindingAuthnRequest"));
            assertTrue(Boolean.valueOf(idp.getConfig().get("postBindingAuthnRequest")));
        });

    }


    public void testCreateWithCategoryDenyListandRemove() throws IOException {
        //blacklist with attributeName and attributeValue exclude only hashEntityIdIdP

        Map<String, List<String>> blackMap = new HashMap<>();
        blackMap.put(attributeName, Stream.of(attributeValue).collect(Collectors.toList()));
        String internalId = createFederation("edugain-sample", "http://localhost:8880/edugain-sample-test.xml", "Identity Providers", new HashSet<>(),
                new HashSet<>(), new HashSet<>(), new HashSet<>(), blackMap);

        sleep(70000);

        // first execute trigger update idps and then get identity providers federation
        SAMLFederationRepresentation representation = realm.samlFederation()
                .getSAMLFederation(internalId);
        assertNotNull(representation);

        assertEquals("wrong federation alias", "edugain-sample", representation.getAlias());
        assertEquals("not saml federation", "saml", representation.getProviderId());
        assertEquals("wrong url", "http://localhost:8880/edugain-sample-test.xml",
                representation.getUrl());

        assertNoSAMLClients();

        // must be two saml idps and not contain
        List<IdentityProviderRepresentation> idps = realm.identityProviders().findAll();
        log.infof("Identity Providers are %s", idps.size());
        idps.stream().filter(idp -> idp.getFederations() != null && !idp.getFederations().isEmpty()).forEach(idp -> {
            assertTrue("wrong IdPs", aliasSet.contains(idp.getAlias()));
            assertEquals("not saml IdP", "saml", idp.getProviderId());
            assertNotNull("empty IdP config", idp.getConfig());
            assertTrue("IdP singleSignOnServiceUrl not exist", idp.getConfig().containsKey("singleSignOnServiceUrl"));
            assertTrue("IdP postBindingAuthnRequest not exist", idp.getConfig().containsKey("postBindingAuthnRequest"));
            assertTrue(Boolean.valueOf(idp.getConfig().get("postBindingAuthnRequest")));
        });


    }


    public void testCreateAndExportWithoutMappers() throws IOException, ParsingException {

        String internalId = createFederation("edugain-sample", "http://localhost:8880/edugain-sample-test.xml", "Identity Providers", new HashSet<>(),
                new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashMap<>());

        SAMLFederationRepresentation representation = realm.samlFederation()
                .getSAMLFederation(internalId);
        Assert.assertNotNull(representation);
        String spDescriptorString = realm.samlFederation().export(representation.getAlias()).readEntity(String.class);
        SAMLParser parser = SAMLParser.getInstance();
        EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
        SPSSODescriptorType spDescriptor = o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();

        //attribute mappers do  exists-  AttributeConsumingService exist
        Assert.assertEquals(spDescriptor.getAssertionConsumerService().size(), 1);
        Assert.assertEquals(spDescriptor.getSingleLogoutService().size(), 1);
        Assert.assertEquals(spDescriptor.getNameIDFormat().size(), 1);
        Assert.assertEquals(spDescriptor.getNameIDFormat().get(0), "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
        Assert.assertTrue(spDescriptor.getAttributeConsumingService().isEmpty());
    }


    public void testCreateAndExportWithMappers() throws IOException, ParsingException {

        String internalId = createFederation("edugain-sample", "http://localhost:8880/edugain-sample-test.xml", "Identity Providers", new HashSet<>(),
                new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashMap<>());
        createMapper(internalId);

        SAMLFederationRepresentation representation = realm.samlFederation()
                .getSAMLFederation(internalId);
        Assert.assertNotNull(representation);
        String spDescriptorString = realm.samlFederation().export(representation.getAlias()).readEntity(String.class);
        SAMLParser parser = SAMLParser.getInstance();
        EntityDescriptorType o = (EntityDescriptorType) parser.parse(new StringInputStream(spDescriptorString));
        SPSSODescriptorType spDescriptor = o.getChoiceType().get(0).getDescriptors().get(0).getSpDescriptor();

        //attribute mappers do not exist- no AttributeConsumingService
        Assert.assertEquals(spDescriptor.getAssertionConsumerService().size(), 1);
        Assert.assertEquals(spDescriptor.getSingleLogoutService().size(), 1);
        Assert.assertEquals(spDescriptor.getNameIDFormat().size(), 1);
        Assert.assertEquals(spDescriptor.getNameIDFormat().get(0), "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
        Assert.assertFalse(spDescriptor.getAttributeConsumingService().isEmpty());
        AttributeConsumingServiceType attributeConsuming = spDescriptor.getAttributeConsumingService().get(0);
        Assert.assertEquals(attributeConsuming.getIndex(), 3);
        Assert.assertFalse(attributeConsuming.getRequestedAttribute().isEmpty());
        Assert.assertEquals(attributeConsuming.getRequestedAttribute().get(0).getName(), "givenname");
        Assert.assertEquals(attributeConsuming.getRequestedAttribute().get(0).getFriendlyName(), "given name");
        Assert.assertEquals(attributeConsuming.getRequestedAttribute().get(0).getNameFormat(), JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get());
        Assert.assertTrue(attributeConsuming.getServiceName() != null);
        Assert.assertEquals(attributeConsuming.getServiceName().get(0).getValue(), "federation");
    }


    public void testFederationMappers() throws IOException {

        String internalId = createFederation("edugain-sample", "http://localhost:8880/edugain-sample-test.xml", "Identity Providers", new HashSet<>(),
                new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashMap<>());
        String mapperId = createMapper(internalId);

        FederationMapperRepresentation mapper = realm.samlFederation().getIdentityProviderFederationMapper(internalId, mapperId);
        assertEquals("my_mapper", mapper.getName());
        assertEquals("saml-user-attribute-idp-mapper", mapper.getIdentityProviderMapper());
        assertEquals("givenname", mapper.getConfig().get("attribute.name"));
        assertEquals("firstname", mapper.getConfig().get("user.attribute"));

        //update and remove mapper
        mapper.getConfig().put("user.attribute", "name");
        realm.samlFederation().updateMapper(internalId, mapperId, mapper);
        mapper = realm.samlFederation().getIdentityProviderFederationMapper(internalId, mapperId);
        assertEquals("my_mapper", mapper.getName());
        assertEquals("saml-user-attribute-idp-mapper", mapper.getIdentityProviderMapper());
        assertEquals("givenname", mapper.getConfig().get("attribute.name"));
        assertEquals("name", mapper.getConfig().get("user.attribute"));

        realm.samlFederation().deleteMapper(internalId, mapperId);
        try {
            realm.samlFederation().getIdentityProviderFederationMapper(internalId, mapperId);
            Assert.fail("Not expected to found federation");

        } catch (NotFoundException nfe) {
            // Expected
        }
    }


    public void testFederationMappersActions() throws IOException {

        String internalId = createFederation("edugain-sample", "http://localhost:8880/edugain-sample-test.xml", "Identity Providers", new HashSet<>(),
                new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashMap<>());

        sleep(70000);
        // first execute trigger update idps and then get identity providers federation
        SAMLFederationRepresentation representation = realm.samlFederation()
                .getSAMLFederation(internalId);
        assertNotNull(representation);

        assertEquals("wrong federation alias", "edugain-sample", representation.getAlias());
        assertEquals("not saml federation", "saml", representation.getProviderId());
        assertEquals("wrong url", "http://localhost:8880/edugain-sample-test.xml",
                representation.getUrl());

        //add mapper to Idp
        String mapperId = createMapper(internalId);
        realm.samlFederation().massIdPMapperAction(internalId, mapperId, "add");

        // must be three saml idps
        List<IdentityProviderRepresentation> idps = realm.identityProviders().findAll();
        log.infof("Identity Providers are %s", idps.size());

        idps.stream().filter(idp -> idp.getFederations() != null && !idp.getFederations().isEmpty()).forEach(idp -> {
            assertTrue("wrong IdPs", aliasSet.contains(idp.getAlias()));
            assertEquals("not saml IdP", "saml", idp.getProviderId());
            assertNotNull("empty IdP config", idp.getConfig());
            assertEquals("not saml IdP", "saml", idp.getProviderId());
            assertNotNull("empty IdP config", idp.getConfig());
            IdentityProviderResource identityProviderResource = realm.identityProviders().get(idp.getAlias());
            //check mapper
            List<IdentityProviderMapperRepresentation> mappers = identityProviderResource.getMappers();
            assertEquals(1, mappers.size());
            IdentityProviderMapperRepresentation mapper = mappers.get(0);
            assertEquals("my_mapper", mapper.getName());
            assertEquals("saml-user-attribute-idp-mapper", mapper.getIdentityProviderMapper());
            assertEquals("givenname", mapper.getConfig().get("attribute.name"));
            assertEquals("firstname", mapper.getConfig().get("user.attribute"));
        });

        //update mapper to Idp
        FederationMapperRepresentation fedMapper = realm.samlFederation().getIdentityProviderFederationMapper(internalId, mapperId);
        fedMapper.getConfig().put("user.attribute", "name");
        realm.samlFederation().updateMapper(internalId, mapperId, fedMapper);
        realm.samlFederation().massIdPMapperAction(internalId, mapperId, "update");
        idps = realm.identityProviders().findAll();
        idps.stream().filter(idp -> idp.getFederations() != null && !idp.getFederations().isEmpty()).forEach(idp -> {
            assertEquals("not saml IdP", "saml", idp.getProviderId());
            assertNotNull("empty IdP config", idp.getConfig());
            IdentityProviderResource identityProviderResource = realm.identityProviders().get(idp.getAlias());
            //check mapper
            List<IdentityProviderMapperRepresentation> mappers = identityProviderResource.getMappers();
            assertEquals(1, mappers.size());
            IdentityProviderMapperRepresentation mapper = mappers.get(0);
            assertEquals("my_mapper", mapper.getName());
            assertEquals("saml-user-attribute-idp-mapper", mapper.getIdentityProviderMapper());
            assertEquals("givenname", mapper.getConfig().get("attribute.name"));
            assertEquals("name", mapper.getConfig().get("user.attribute"));
        });

        //remove mapper from Idp
        realm.samlFederation().massIdPMapperAction(internalId, mapperId, "remove");
        idps = realm.identityProviders().findAll();
        idps.stream().filter(idp -> idp.getFederations() != null && !idp.getFederations().isEmpty()).forEach(idp -> {
            assertEquals("not saml IdP", "saml", idp.getProviderId());
            assertNotNull("empty IdP config", idp.getConfig());
            IdentityProviderResource identityProviderResource = realm.identityProviders().get(idp.getAlias());
            //check mapper
            List<IdentityProviderMapperRepresentation> mappers = identityProviderResource.getMappers();
            assertEquals(0, mappers.size());
        });

    }


    private String createFederation(String alias, String url, String category, Set<String> denyList, Set<String> whitelist,
                                    Set<String> registrationAuthorityDenyList, Set<String> registrationAuthorityWhitelist, Map<String, List<String>> categoryDenyList) throws IOException {
        SAMLFederationRepresentation representation = new SAMLFederationRepresentation();
        representation.setAlias(alias);
        representation.setProviderId("saml");
        representation.setCategory(category);
        representation.setUpdateFrequencyInMins(60);
        representation.setUrl(url);
        representation.setEntityIdDenyList(denyList);
        representation.setEntityIdAllowList(whitelist);
        representation.setRegistrationAuthorityDenyList(registrationAuthorityDenyList);
        representation.setRegistrationAuthorityAllowList(registrationAuthorityWhitelist);
        representation.setCategoryDenyList(categoryDenyList);

        if (!"Clients".equals(category)) {
            Map<String, String> config = new HashMap<>();
            config.put("nameIDPolicyFormat", "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
            LinkedList<SAMLIdentityProviderConfig.Principal> principals = new LinkedList<>();
            SAMLIdentityProviderConfig.Principal pr = new SAMLIdentityProviderConfig.Principal();
            pr.setPrincipalType(SamlPrincipalType.SUBJECT);
            pr.setNameIDPolicyFormat(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get());
            principals.add(pr);
            SAMLIdentityProviderConfig.Principal pr2 = new SAMLIdentityProviderConfig.Principal();
            pr2.setPrincipalType(SamlPrincipalType.SUBJECT);
            pr2.setNameIDPolicyFormat(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get());
            principals.add(pr2);
            SAMLIdentityProviderConfig.Principal pr3 = new SAMLIdentityProviderConfig.Principal();
            pr3.setPrincipalType(SamlPrincipalType.ATTRIBUTE);
            pr3.setPrincipalAttribute("subject-id");
            principals.add(pr3);
            config.put(SAMLIdentityProviderConfig.MULTIPLE_PRINCIPALS, JsonSerialization.writeValueAsString(principals));
            config.put("wantAssertionsEncrypted", "optional");
            config.put("wantAssertionsSigned", "true");
            config.put("postBindingResponse", "true");
            config.put("postBindingLogoutReceivingRequest", "true");
            config.put("attributeConsumingServiceIndex", "3");
            config.put("attributeConsumingServiceName", "federation");
            representation.setConfig(config);
        }

        Response response = realm.samlFederation().create(representation);
        String id = ApiUtil.getCreatedId(response);
        Assert.assertNotNull(id);
        response.close();

        getCleanup().addIdentityProviderFederationId(id);

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.identityProvidersFederationPath(alias),
                representation, ResourceType.IDENTITY_PROVIDERS_FEDERATION);

        return id;
    }

    private void removeFederation(String id) {
        realm.samlFederation().delete(id);

        // federation and its idps must be deleted
        try {
            realm.samlFederation().getSAMLFederation(id);
            Assert.fail("Not expected to found federation");

        } catch (NotFoundException nfe) {
            // Expected
        }

        aliasSet.stream().forEach(idp -> {
            try {
                IdentityProviderResource resource = realm.identityProviders().get(idp);
                IdentityProviderRepresentation idpRes = resource.toRepresentation();
                Assert.fail("Not expected to found idp");

            } catch (NotFoundException nfe) {
                // Expected
            }
        });

        assertNoSAMLClients();

    }

    private void assertNoSAMLClients() {
        List<ClientRepresentation> clients = realm.clients().findByClientId(entityIdClient1);
        assertEquals("Not expected to found " + entityIdClient1 + " client", clients.size(), 0);
        clients = realm.clients().findByClientId(entityIdClient2);
        assertEquals("Not expected to found " + entityIdClient2 + " client", clients.size(), 0);
    }

    private void assertExistSAMLClients(List<ProtocolMapperRepresentation> defaultMappers) {
        List<ClientRepresentation> clients = realm.clients().findByClientId(entityIdClient1);
        assertEquals("Expected to found " + entityIdClient1 + " client", 1, clients.size());
        ClientRepresentation client1 = clients.get(0);
        assertClient(client1, "siroe", "loadbalancer siroen client", "https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/Artifact", "https://LoadBalancer-9.siroe.com:3443/federation/SPSloRedirect/metaAlias/sp");
        assertEquals(defaultMappers.size(), client1.getProtocolMappers() != null ? client1.getProtocolMappers().size() : 0);
        clients = realm.clients().findByClientId(entityIdClient2);
        assertEquals("Expected to found " + entityIdClient2 + " client", clients.size(), 1);
        ClientRepresentation client2 = clients.get(0);
        assertClient(client2, "Technical University of Kosice", null, "https://test-sp.tuke.sk/Shibboleth.sso/SAML2/Artifact", "https://test-sp.tuke.sk/Shibboleth.sso/SLO/Redirect");
        assertEquals(defaultMappers.isEmpty() ? 0 : 1, client2.getProtocolMappers() != null ? client2.getProtocolMappers().size() : 0);
        if (!defaultMappers.isEmpty()) {
            //check for equality for email mapper for client2
            ProtocolMapperRepresentation mapperEmail = client2.getProtocolMappers().stream().filter(mapper -> "email".equals(mapper.getName())).findAny().get();
            assertNotNull(mapperEmail);
            assertEquals("email", mapperEmail.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE));
            assertEquals("urn:oid:0.9.2342.19200300.100.1.3", mapperEmail.getConfig().get(AttributeStatementHelper.SAML_ATTRIBUTE_NAME));
            assertEquals("email", mapperEmail.getConfig().get(AttributeStatementHelper.FRIENDLY_NAME));
        }
    }

    private void assertClient(ClientRepresentation client, String name, String description, String artifactUrl, String logoutPostUrl) {
        assertEquals("False SAML Protocol", "saml", client.getProtocol());
        assertTrue("Enabled Client", client.isEnabled());
        assertTrue("Full Scope Allowed", client.isFullScopeAllowed());
        assertEquals("two redirect uris", 2, client.getRedirectUris().size());
        assertEquals("False Name", name, client.getName());
        assertEquals("False Description", description, client.getDescription());
        assertThat(client.getAttributes().keySet(), containsInAnyOrder(
                "saml.assertion.signature",
                "saml.force.post.binding",
                "saml_single_logout_service_url_redirect",
                "saml.encrypt",
                "saml_assertion_consumer_url_post",
                "saml.server.signature",
                "saml.server.signature.keyinfo.ext",
                "saml.signing.certificate",
                "saml.artifact.binding.identifier",
                "saml.signature.algorithm",
                "saml_force_name_id_format",
                "saml.client.signature",
                "saml.encryption.certificate",
                "saml.authnstatement",
                "saml_name_id_format",
                "saml_artifact_binding_url",
                "saml_signature_canonicalization_method",
                "saml.allow.ecp.flow",
                "client.secret.creation.time",
                "realm_client"
        ));
        assertEquals("true", client.getAttributes().get(SamlConfigAttributes.SAML_SERVER_SIGNATURE));
        assertEquals(SignatureAlgorithm.RSA_SHA256.toString(), client.getAttributes().get(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM));
        assertEquals("true", client.getAttributes().get(SamlConfigAttributes.SAML_AUTHNSTATEMENT));
        assertEquals("false", client.getAttributes().get(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE));
        assertEquals("persistent", client.getAttributes().get(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE));
        assertEquals("true", client.getAttributes().get(SamlConfigAttributes.SAML_ENCRYPT));
        assertEquals("false", client.getAttributes().get(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE));
        assertEquals(artifactUrl, client.getAttributes().get(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE));
        assertEquals(logoutPostUrl, client.getAttributes().get(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE));
    }

    private String createMapper(String federatedId) {
        FederationMapperRepresentation mapper = new FederationMapperRepresentation();
        mapper.setName("my_mapper");
        mapper.setIdentityProviderMapper("saml-user-attribute-idp-mapper");
        Map<String, String> config = new HashMap<>();
        config.put("attribute.name", "givenname");
        config.put("attribute.friendly.name", "given name");
        config.put("user.attribute", "firstname");
        config.put("attribute.name.format", JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.name());
        mapper.setConfig(config);

        Response response = realm.samlFederation().createMapper(federatedId, mapper);
        String id = ApiUtil.getCreatedId(response);
        Assert.assertNotNull(id);
        response.close();
        return id;
    }

    private String createClientScope(String name) {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName(name);
        scopeRep.setProtocol("saml");

        Response resp = realm.clientScopes().create(scopeRep);
        org.junit.Assert.assertEquals(201, resp.getStatus());
        resp.close();
        String clientScopeId = ApiUtil.getCreatedId(resp);
        getCleanup().addClientScopeId(clientScopeId);

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientScopeResourcePath(clientScopeId), scopeRep, ResourceType.CLIENT_SCOPE);

        return clientScopeId;
    }

    protected ProtocolMapperRepresentation makeSamlAttributeMapper(String clientScopeId, String friendlyName, String name) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(friendlyName);
        mapper.setProtocol("saml");
        mapper.setProtocolMapper(UserAttributeStatementMapper.PROVIDER_ID);
        Map<String, String> config = new HashMap<>();
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE, friendlyName);
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, name);
        config.put(AttributeStatementHelper.FRIENDLY_NAME, friendlyName);
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get());
        mapper.setConfig(config);
        Response resp = realm.clientScopes().get(clientScopeId).getProtocolMappers().createMapper(mapper);
        resp.close();
        String mapperId = ApiUtil.getCreatedId(resp);
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.clientScopeProtocolMapperPath(clientScopeId, mapperId), mapper, ResourceType.PROTOCOL_MAPPER);

        mapper.setId(mapperId);
        return mapper;
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
