package org.keycloak.testsuite.saml;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.Matchers.isSamlLogoutRequest;
import static org.keycloak.testsuite.util.Matchers.isSamlResponse;
import static org.keycloak.testsuite.util.Matchers.isSamlStatusResponse;
import static org.keycloak.testsuite.util.SamlClient.Binding.POST;
import static org.keycloak.testsuite.util.SamlClient.Binding.REDIRECT;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.SamlPrincipalType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.SAMLFederationRepresentation;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.SamlClientBuilder;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.keycloak.util.JsonSerialization;

public class SamlFederationIdpLogoutTest extends AbstractSamlTest {

	private static Undertow SERVER;
	private static final Logger log = Logger.getLogger(SamlFederationIdpLogoutTest.class);
	private static final String brokerIdp = "aHR0cHM6Ly9odWMuaWRwLmtuYXcubmwvc2ltcGxlc2FtbC9zYW1sMi9pZHAvbWV0YWRhdGEucGhw";

	protected RealmResource realm;
	private String internalId;

	private ClientRepresentation salesRep;
	private final AtomicReference<NameIDType> nameIdRef = new AtomicReference<>();
	private final AtomicReference<String> sessionIndexRef = new AtomicReference<>();

//	@Before
//	public void createFederation() throws IOException {
//		realm = adminClient.realm(REALM_NAME);
//		internalId = createFederation("edugain-sample", "http://localhost:8880/sample-federation-authn.xml");
//
//		salesRep = realm.clients().findByClientId(SAML_CLIENT_ID_SALES_POST).get(0);
//
//		adminClient.realm(REALM_NAME).clients().get(salesRep.getId())
//				.update(ClientBuilder.edit(salesRep).frontchannelLogout(true)
//						.attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "http://url").build());
//
//		nameIdRef.set(null);
//		sessionIndexRef.set(null);
//
//		adminClient.realm(REALM_NAME).clearEvents();
//
//	}
//
//	@After
//	public void removeFederation() {
//		realm.samlFederation().delete(internalId);
//	}
//
//	@BeforeClass
//	public static void onBeforeClass() {
//		SERVER = Undertow.builder().addHttpListener(8880, "localhost", new HttpHandler() {
//			@Override
//			public void handleRequest(HttpServerExchange exchange) throws Exception {
//				writeResponse(exchange.getRequestURI(), exchange);
//			}
//
//			private void writeResponse(String file, HttpServerExchange exchange) throws IOException {
//				exchange.getResponseSender().send(StreamUtil.readString(
//						getClass().getResourceAsStream("/federation/saml" + file), Charset.defaultCharset()));
//			}
//		}).build();
//
//		SERVER.start();
//	}
//
//	@AfterClass
//	public static void onAfterClass() {
//		if (SERVER != null) {
//			SERVER.stop();
//		}
//	}
//
//	@Override
//	protected boolean isImportAfterEachMethod() {
//		return true;
//	}
//
////	@Test
////	public void testLogoutPropagatesToSamlIdentityProvider() throws IOException {
////
////
////		try (Closeable sales = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
////				.setFrontchannelLogout(true).removeAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE)
////				.setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "http://url").update();
////
////		) {
////
////			updateIdpByAlias(brokerIdp);
////
////			SAMLDocumentHolder samlResponse = new SamlClientBuilder()
////					.authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
////							SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
////					.build()
////
////					// Virtually perform login at IdP (return artificial SAML response)
////					.login().idp(brokerIdp).build().processSamlResponse(REDIRECT)
////					.transformObject(this::createAuthnResponse).targetAttributeSamlResponse()
////					.targetUri(getSamlBrokerUrl(REALM_NAME)).build().updateProfile().username("a").email("a@b.c")
////					.firstName("A").lastName("B").build().followOneRedirect()
////
////					// Now returning back to the app
////					.processSamlResponse(POST).transformObject(this::extractNameIdAndSessionIndexAndTerminate).build()
////
////					// ----- Logout phase ------
////
////					// Logout initiated from the app
////					.logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, REDIRECT)
////					.nameId(nameIdRef::get).sessionIndex(sessionIndexRef::get).build()
////
////					// Should redirect now to logout from IdP
////					.processSamlResponse(REDIRECT).transformObject(this::createIdPLogoutResponse)
////					.targetAttributeSamlResponse().targetUri(getSamlBrokerUrl(REALM_NAME)).build()
////
////					.getSamlResponse(REDIRECT);
////
////			assertThat(samlResponse.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
////		} finally {
////			realm.clients().get(salesRep.getId()).update(salesRep);
////		}
////	}
////
////	@Test
////	public void testLogoutPropagatesToSamlIdentityProviderNameIdPreserved() throws IOException {
////
////		try (Closeable sales = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_SALES_POST)
////				.setFrontchannelLogout(true).removeAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE)
////				.setAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "http://url").update();
////
////		) {
////
////			updateIdpByAlias(brokerIdp);
////
////			SAMLDocumentHolder samlResponse = new SamlClientBuilder()
////					.authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
////							SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
////					.build()
////
////					// Virtually perform login at IdP (return artificial SAML response)
////					.login().idp(brokerIdp).build().processSamlResponse(REDIRECT)
////					.transformObject(this::createAuthnResponse).targetAttributeSamlResponse()
////					.targetUri(getSamlBrokerUrl(REALM_NAME)).build().updateProfile().username("a").email("a@b.c")
////					.firstName("A").lastName("B").build().followOneRedirect()
////
////					// Now returning back to the app
////					.processSamlResponse(POST).transformObject(this::extractNameIdAndSessionIndexAndTerminate).build()
////
////					// ----- Logout phase ------
////
////					// Logout initiated from the app
////					.logoutRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, REDIRECT)
////					.nameId(nameIdRef::get).sessionIndex(sessionIndexRef::get).build()
////
////					.getSamlResponse(REDIRECT);
////
////			assertThat(samlResponse.getSamlObject(), isSamlLogoutRequest("https://saml.idp/SLO/saml"));
////			LogoutRequestType lr = (LogoutRequestType) samlResponse.getSamlObject();
////			NameIDType logoutRequestNameID = lr.getNameID();
////			assertThat(logoutRequestNameID.getFormat(), is(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.getUri()));
////			assertThat(logoutRequestNameID.getValue(), is("a@b.c"));
////			assertThat(logoutRequestNameID.getNameQualifier(), is("nameQualifier"));
////			assertThat(logoutRequestNameID.getSPProvidedID(), is("spProvidedId"));
////			assertThat(logoutRequestNameID.getSPNameQualifier(), is("spNameQualifier"));
////		} finally {
////			realm.clients().get(salesRep.getId()).update(salesRep);
////		}
////	}
//
//	private SAML2Object createAuthnResponse(SAML2Object so) {
//		AuthnRequestType req = (AuthnRequestType) so;
//		try {
//			final ResponseType res = new SAML2LoginResponseBuilder().requestID(req.getID())
//					.destination(req.getAssertionConsumerServiceURL().toString()).issuer("https://idp.rash.al/simplesaml/saml2/idp/metadata.php")
//					.assertionExpiration(1000000).subjectExpiration(1000000)
//					.requestIssuer(getAuthServerRealmBase(REALM_NAME).toString())
//					.nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get(), "a@b.c")
//					.authMethod(JBossSAMLURIConstants.AC_UNSPECIFIED.get()).sessionIndex("idp:" + UUID.randomUUID())
//					.buildModel();
//
//			NameIDType nameId = (NameIDType) res.getAssertions().get(0).getAssertion().getSubject().getSubType()
//					.getBaseID();
//			nameId.setNameQualifier("nameQualifier");
//			nameId.setSPNameQualifier("spNameQualifier");
//			nameId.setSPProvidedID("spProvidedId");
//
//			return res;
//		} catch (ConfigurationException | ProcessingException ex) {
//			throw new RuntimeException(ex);
//		}
//	}
//
//	protected SAML2Object extractNameIdAndSessionIndexAndTerminate(SAML2Object so) {
//		assertThat(so, isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
//		ResponseType loginResp1 = (ResponseType) so;
//		final AssertionType firstAssertion = loginResp1.getAssertions().get(0).getAssertion();
//		assertThat(firstAssertion, org.hamcrest.Matchers.notNullValue());
//		assertThat(firstAssertion.getSubject().getSubType().getBaseID(), instanceOf(NameIDType.class));
//
//		NameIDType nameId = (NameIDType) firstAssertion.getSubject().getSubType().getBaseID();
//		AuthnStatementType firstAssertionStatement = (AuthnStatementType) firstAssertion.getStatements().iterator()
//				.next();
//
//		nameIdRef.set(nameId);
//		sessionIndexRef.set(firstAssertionStatement.getSessionIndex());
//
//		return null;
//	}
//
//	private SAML2Object createIdPLogoutResponse(SAML2Object so) {
//		LogoutRequestType req = (LogoutRequestType) so;
//		try {
//			return new SAML2LogoutResponseBuilder().logoutRequestID(req.getID())
//					.destination(getSamlBrokerUrl(REALM_NAME).toString()).issuer("https://idp.rash.al/simplesaml/saml2/idp/metadata.php")
//					.buildModel();
//		} catch (ConfigurationException ex) {
//			throw new RuntimeException(ex);
//		}
//	}
//
//	private String createFederation(String alias, String url) throws NotFoundException, IOException {
//		SAMLFederationRepresentation representation = new SAMLFederationRepresentation();
//		representation.setAlias(alias);
//		representation.setProviderId("saml");
//		representation.setCategory("Identity Providers");
//		representation.setUpdateFrequencyInMins(60);
//		representation.setUrl(url);
//		Map<String,String> map = new HashMap<>();
//		map.put(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT, "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
//		map.put(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "false");
//		map.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "false");
//		map.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "false");
//		map.put(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "false");
//		LinkedList<SAMLIdentityProviderConfig.Principal> principals = new LinkedList<>();
//		SAMLIdentityProviderConfig.Principal pr = new SAMLIdentityProviderConfig.Principal();
//		pr.setPrincipalType(SamlPrincipalType.SUBJECT);
//		pr.setNameIDPolicyFormat(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get());
//		principals.add(pr);
//		SAMLIdentityProviderConfig.Principal pr2 = new SAMLIdentityProviderConfig.Principal();
//		pr2.setPrincipalType(SamlPrincipalType.ATTRIBUTE);
//		pr2.setPrincipalAttribute("subject-id");
//		principals.add(pr2);
//		SAMLIdentityProviderConfig.Principal pr3 = new SAMLIdentityProviderConfig.Principal();
//		pr3.setPrincipalType(SamlPrincipalType.SUBJECT);
//		pr3.setNameIDPolicyFormat(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get());
//		principals.add(pr3);
//		map.put(SAMLIdentityProviderConfig.MULTIPLE_PRINCIPALS, JsonSerialization.writeValueAsString(principals));
//		representation.setConfig(map);
//
//		Response response = realm.samlFederation().create(representation);
//		String id = ApiUtil.getCreatedId(response);
//		response.close();
//
//		if (id == null)
//			throw new NotFoundException();
//
//		sleep(70000);
//
//		return id;
//	}
//
//	private IdentityProviderRepresentation updateIdpByAlias(String idpAlias) throws IOException {
//		IdentityProviderResource identityProviderResource = realm.identityProviders().get(idpAlias);
//
//		IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();
//
//		assertNotNull(representation);
//
//		representation.getConfig().put(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT,
//				JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get());
//		representation.getConfig().put(SAMLIdentityProviderConfig.BACKCHANNEL_SUPPORTED, Boolean.FALSE.toString());
//
//		identityProviderResource.update(representation);
//		return representation;
//	}
//
//	private static void sleep(long ms) {
//		try {
//			log.infof("Sleeping for %d ms", ms);
//			Thread.sleep(ms);
//		} catch (InterruptedException ie) {
//			throw new RuntimeException(ie);
//		}
//	}
//
//	protected URI getSamlBrokerUrl(String realmName) {
//		return URI.create(getAuthServerRealmBase(realmName).toString() + "/broker/endpoint");
//	}

}
