package org.keycloak.testsuite.saml.federation;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.saml.SamlPrincipalType;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.SAMLFederationRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.util.JsonSerialization;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import static org.keycloak.testsuite.util.Matchers.bodyHC;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;
import static org.keycloak.testsuite.util.SamlClient.Binding.POST;
import static org.keycloak.testsuite.util.SamlClient.Binding.REDIRECT;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public abstract class SamlFederationIdpTest extends AbstractSamlTest {

	private static Undertow SERVER;
	private static final Logger log = Logger.getLogger(SamlFederationIdpTest.class);
	private static final String loginIdp = "aHR0cHM6Ly9pZHAucmFzaC5hbC9zaW1wbGVzYW1sL3NhbWwyL2lkcC9tZXRhZGF0YS5waHA";
	private static final String brokerIdp2 = "aHR0cHM6Ly9odWMuaWRwLmtuYXcubmwvc2ltcGxlc2FtbC9zYW1sMi9pZHAvbWV0YWRhdGEucGhw";

	protected RealmResource realm;
	private String internalId;

	@Before
	public void createFederation() throws IOException {
		realm = adminClient.realm(REALM_NAME);
		internalId = createFederation("edugain-sample", "http://localhost:8880/sample-federation-authn.xml");
		
	}

	@After
	public void removeFederation() {
		realm.samlFederation().delete(internalId);
	}

	@BeforeClass
	public static void onBeforeClass() {
		SERVER = Undertow.builder().addHttpListener(8880, "localhost", new HttpHandler() {
			@Override
			public void handleRequest(HttpServerExchange exchange) throws Exception {
				writeResponse(exchange.getRequestURI(), exchange);
			}

			private void writeResponse(String file, HttpServerExchange exchange) throws IOException {
				exchange.getResponseSender().send(StreamUtil.readString(
						getClass().getResourceAsStream("/federation/saml" + file), Charset.defaultCharset()));
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

	public void testSamlPostBindingPageIdP() throws Exception {

		new SamlClientBuilder().idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build().login()
				.idp(loginIdp).build().execute(r -> {
					Assert.assertThat(r, statusCodeIsHC(Response.Status.OK));
					Assert.assertThat(r,
							bodyHC(allOf(containsString("Authentication Redirect"),
									containsString("<input type=\"hidden\" name=\"SAMLRequest\""),
									containsString("id=\"kc-page-title\">"))));
				});

	}

	public void testRedirectQueryParametersPreserved() throws IOException {

		updateIdpByAlias(brokerIdp2);
		
		// updateIdpForNameIdPolicyFormat(brokerIdp2);
		SAMLDocumentHolder samlResponse = new SamlClientBuilder()
				.authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
						SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
				.build().login().idp(brokerIdp2).build()

				// Virtually perform login at IdP (return artificial SAML response)
				.getSamlResponse(REDIRECT);

		assertThat(samlResponse.getSamlObject(), Matchers.instanceOf(AuthnRequestType.class));
		AuthnRequestType ar = (AuthnRequestType) samlResponse.getSamlObject();
		assertThat(ar.getDestination(), Matchers.equalTo(URI.create("https://saml.idp/?service=name&serviceType=prod")));

		Header[] headers = new SamlClientBuilder()
				.authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
						SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
				.build().login().idp(brokerIdp2).build().doNotFollowRedirects()
				.executeAndTransform(resp -> resp.getHeaders(HttpHeaders.LOCATION));

		assertThat(headers.length, Matchers.is(1));
		assertThat(headers[0].getValue(), Matchers.containsString("https://saml.idp/?service=name&serviceType=prod"));
		assertThat(headers[0].getValue(), Matchers.containsString("SAMLRequest"));

	}
	  
	private String createFederation(String alias, String url) throws NotFoundException, IOException {
		SAMLFederationRepresentation representation = new SAMLFederationRepresentation();
		representation.setAlias(alias);
		representation.setProviderId("saml");
		representation.setCategory("Identity Providers");
		representation.setUpdateFrequencyInMins(60);
		representation.setUrl(url);
		Map<String,String> map = new HashMap<>();
		map.put(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT, "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
		map.put(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "false");
		map.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_SIGNED, "false");
		map.put(SAMLIdentityProviderConfig.WANT_ASSERTIONS_ENCRYPTED, "false");
		map.put(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "true");
		LinkedList<SAMLIdentityProviderConfig.Principal> principals = new LinkedList<>();
		SAMLIdentityProviderConfig.Principal pr = new SAMLIdentityProviderConfig.Principal();
		pr.setPrincipalType(SamlPrincipalType.SUBJECT);
		pr.setNameIDPolicyFormat(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get());
		principals.add(pr);
		SAMLIdentityProviderConfig.Principal pr2 = new SAMLIdentityProviderConfig.Principal();
		pr2.setPrincipalType(SamlPrincipalType.ATTRIBUTE);
		pr2.setPrincipalAttribute("subject-id");
		principals.add(pr2);
		SAMLIdentityProviderConfig.Principal pr3 = new SAMLIdentityProviderConfig.Principal();
		pr3.setPrincipalType(SamlPrincipalType.SUBJECT);
		pr3.setNameIDPolicyFormat(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get());
		principals.add(pr3);
		map.put(SAMLIdentityProviderConfig.MULTIPLE_PRINCIPALS, JsonSerialization.writeValueAsString(principals));
		representation.setConfig(map);

		Response response = realm.samlFederation().create(representation);
		String id = ApiUtil.getCreatedId(response);
		response.close();

		if (id == null)
			throw new NotFoundException();

		sleep(70000);

		return id;
	}

	private IdentityProviderRepresentation updateIdpByAlias(String idpAlias) throws IOException {
		IdentityProviderResource identityProviderResource = realm.identityProviders().get(idpAlias);

		IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

		assertNotNull(representation);
		
		representation.getConfig().put(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get());
		representation.getConfig().put(SAMLIdentityProviderConfig.BACKCHANNEL_SUPPORTED, Boolean.FALSE.toString());

		identityProviderResource.update(representation);
		return representation;
	}

	private static void sleep(long ms) {
		try {
			log.infof("Sleeping for %d ms", ms);
			Thread.sleep(ms);
		} catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}
	}

	protected URI getSamlBrokerUrl(String realmName) {
		return URI.create(getAuthServerRealmBase(realmName).toString() + "/broker/endpoint");
	}

}
