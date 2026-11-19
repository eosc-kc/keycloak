package org.keycloak.testsuite.oidc.federation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.TrustMarkResource;
import org.keycloak.admin.client.resource.TrustMarksResource;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.OpenIdFederationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.TrustMarkRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.utils.OpenIdFederationUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TrustMarksTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void setupFederationResource() {
        RealmResource testRealmResource = adminClient.realm("test");
        RealmRepresentation testRealm = testRealmResource.toRepresentation();
        testRealm.setOpenIdFederationEnabled(true);
        testRealm.setOpenIdFederationOrganizationName("Keycloak");
        testRealm.setOpenIdFederationResolveEndpoint("https://edugain.org/resolve");
        testRealm.setOpenIdFederationAuthorityHints(Stream.of("https://edugain.org/federation").collect(Collectors.toList()));
        testRealm.setOpenIdFederationOPClientRegistrationTypesSupported(Stream.of("EXPLICIT").collect(Collectors.toList()));
        testRealm.setOpenIdFederationRPClientRegistrationTypesSupported(Stream.of("EXPLICIT").collect(Collectors.toList()));
        testRealm.setOpenIdFederationEntityTypes(Stream.of("OPENID_PROVIDER", "OPENID_RELYING_PARTY").collect(Collectors.toList()));
        testRealmResource.update(testRealm);


        OpenIdFederationRepresentation openIdFederationRepresentation = new OpenIdFederationRepresentation();
        openIdFederationRepresentation.setTrustAnchor("https://edugain.org/trust-anchor");
        JSONWebKeySet jwks = new JSONWebKeySet();
        JWK jwk = new JWK();
        jwk.setKeyId("test-ta-key");
        jwk.setKeyType("RSA");
        jwk.setPublicKeyUse("sig");
        jwk.setAlgorithm("RS256");
        jwks.setKeys(new JWK[]{jwk});
        openIdFederationRepresentation.setJwks(jwks);
        Map<String, String> idpConfiguration = Map.of(OpenIdFederationUtils.SUBJECT_TYPES_SUPPORTED, "public");
        openIdFederationRepresentation.setIdpConfiguration(idpConfiguration);

        testRealmResource.openIdFederationsResource().create(openIdFederationRepresentation).close();
    }

    @After
    public void cleanupFederationResource() {
        RealmResource testRealm = adminClient.realm("test");
        RealmRepresentation realmRep = testRealm.toRepresentation();
        realmRep.setOpenIdFederationEnabled(false);
        testRealm.update(realmRep);
    }

    @Test
    public void selfSignedTrustMarks() {
        RealmResource testRealm = adminClient.realm("test");
            TrustMarksResource trustMarksResource = testRealm.trustMarksResource();
            long currentTime = Time.currentTime();

            TrustMarkRepresentation created = new TrustMarkRepresentation();
            created.setTrustMarkType("https://edugain.org/trust-mark/member");
            created.setSelfSigned(true);
            created.setIat(currentTime - 100);
            created.setExp(currentTime + (24 * 60 * 60));
            created.setLogoUri("https://edugain.org/logo.png");
            created.setRef("https://edugain.org/trust-mark/ref");
            created.setDelegation("https://edugain.org/delegation");

            String trustMarkId;
            try (Response response = trustMarksResource.create(created)) {
                assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
                trustMarkId = ApiUtil.getCreatedId(response);
            }
            assertNotNull(trustMarkId);

            List<TrustMarkRepresentation> listed = trustMarksResource.list();
            assertEquals(1, listed.size());
            assertTrustMark(created, listed.get(0), trustMarkId);

            TrustMarkResource trustMarkResource = trustMarksResource.getTrustMark(trustMarkId);
            TrustMarkRepresentation fetched = trustMarkResource.getTrustMark();
            assertTrustMark(created, fetched, trustMarkId);

            fetched.setLogoUri("https://edugain.org/logo2.png");

            try (Response response = trustMarkResource.update(fetched)) {
                assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            }
            TrustMarkRepresentation afterUpdate = trustMarkResource.getTrustMark();
            assertTrustMark(fetched, afterUpdate, trustMarkId);

            try (Response response = trustMarkResource.delete()) {
                assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            }

            listed = trustMarksResource.list();
            assertTrue(listed.isEmpty());

    }

    @Test
    public void noSelfSignedTrustMarks() {
        RealmResource testRealm = adminClient.realm("test");
            TrustMarksResource trustMarksResource = testRealm.trustMarksResource();
            long currentTime = Time.currentTime();
            String realmSubject = RealmsResource.realmBaseUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString();

            TrustMarkRepresentation created = new TrustMarkRepresentation();
            created.setTrustMarkType("https://edugain.org/trust-mark/external");
            created.setSelfSigned(false);

            // Generate a dummy JWT representing the trust mark
            String jwt = createDummyJwt("https://edugain.org/issuer", realmSubject, currentTime - 100, currentTime + (24 * 60 * 60));
            created.setJwt(jwt);

            String trustMarkId;
            try (Response response = trustMarksResource.create(created)) {
                assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
                trustMarkId = ApiUtil.getCreatedId(response);
            }
            assertNotNull(trustMarkId);

            List<TrustMarkRepresentation> listed = trustMarksResource.list();
            assertEquals(1, listed.size());
            assertTrustMark(created, listed.get(0), trustMarkId);

            TrustMarkResource trustMarkResource = trustMarksResource.getTrustMark(trustMarkId);
            TrustMarkRepresentation fetched = trustMarkResource.getTrustMark();
            assertTrustMark(created, fetched, trustMarkId);

            // Update: Supply a new JWT (e.g., extended expiration)
            String updatedJwt = createDummyJwt("https://edugain.org/issuer", realmSubject, currentTime, currentTime + (24 * 60 * 60));
            fetched.setJwt(updatedJwt);

            try (Response response = trustMarkResource.update(fetched)) {
                assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            }
            TrustMarkRepresentation afterUpdate = trustMarkResource.getTrustMark();
            assertTrustMark(fetched, afterUpdate, trustMarkId);

            try (Response response = trustMarkResource.delete()) {
                assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            }

            listed = trustMarksResource.list();
            assertTrue(listed.isEmpty());

    }

    @Test
    public void trustMarksValidationErrors() {
        RealmResource testRealm = adminClient.realm("test");
        TrustMarksResource trustMarksResource = testRealm.trustMarksResource();
        long currentTime = Time.currentTime();
        String realmSubject = RealmsResource.realmBaseUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString();

        // Missing trustMarkType
        TrustMarkRepresentation rep1 = new TrustMarkRepresentation();
        rep1.setSelfSigned(true);
        rep1.setIat(currentTime - 100);
        assertBadRequest(trustMarksResource.create(rep1),
                "trustMarkType and selfSigned are required.");

        // Self-signed missing iat
        TrustMarkRepresentation rep3 = new TrustMarkRepresentation();
        rep3.setTrustMarkType("https://edugain.org/trust-mark/member");
        rep3.setSelfSigned(true);
        assertBadRequest(trustMarksResource.create(rep3),
                "iat is required when selfSigned is true.");

        // Self-signed with exp in the past
        TrustMarkRepresentation rep4 = new TrustMarkRepresentation();
        rep4.setTrustMarkType("https://edugain.org/trust-mark/member");
        rep4.setSelfSigned(true);
        rep4.setIat(currentTime - 100);
        rep4.setExp(currentTime - 50);
        assertBadRequest(trustMarksResource.create(rep4),
                "exp must not be before current time.");

        // Not self-signed missing jwt
        TrustMarkRepresentation rep5 = new TrustMarkRepresentation();
        rep5.setTrustMarkType("https://edugain.org/trust-mark/external");
        rep5.setSelfSigned(false);
        assertBadRequest(trustMarksResource.create(rep5),
                "jwt is required when selfSigned is false.");

        // Not self-signed with extraneous fields (logoUri)
        TrustMarkRepresentation rep6 = new TrustMarkRepresentation();
        rep6.setTrustMarkType("https://edugain.org/trust-mark/external");
        rep6.setSelfSigned(false);
        rep6.setJwt(createDummyJwt("https://edugain.org/issuer", realmSubject, currentTime, currentTime + (24 * 60 * 60)));
        rep6.setLogoUri("https://edugain.org/logo.png");
        assertBadRequest(trustMarksResource.create(rep6),
                "Only jwt and trustMarkType are allowed when selfSigned is false.");

        // Not self-signed with invalid JWT format
        TrustMarkRepresentation rep7 = new TrustMarkRepresentation();
        rep7.setTrustMarkType("https://edugain.org/trust-mark/external");
        rep7.setSelfSigned(false);
        rep7.setJwt("not-a-valid-jwt");
        assertBadRequest(trustMarksResource.create(rep7),
                "Invalid JWT format.");

        // Not self-signed, JWT missing 'iss' claim
        TrustMarkRepresentation rep8 = new TrustMarkRepresentation();
        rep8.setTrustMarkType("https://edugain.org/trust-mark/external");
        rep8.setSelfSigned(false);
        rep8.setJwt(createDummyJwt(null, realmSubject, currentTime, currentTime + (24 * 60 * 60)));
        assertBadRequest(trustMarksResource.create(rep8),
                "JWT must contain the 'iss' claim.");

        // Not self-signed, JWT 'sub' does not match realm issuer
        TrustMarkRepresentation rep9 = new TrustMarkRepresentation();
        rep9.setTrustMarkType("https://edugain.org/trust-mark/external");
        rep9.setSelfSigned(false);
        rep9.setJwt(createDummyJwt("https://edugain.org/issuer", "https://edugain.org/wrong-subject", currentTime, currentTime + (24 * 60 * 60)));
        assertBadRequest(trustMarksResource.create(rep9),
                "JWT must contain realm issuer as the 'sub' claim.");

        // Not self-signed, JWT missing 'iat' claim
        TrustMarkRepresentation rep10 = new TrustMarkRepresentation();
        rep10.setTrustMarkType("https://edugain.org/trust-mark/external");
        rep10.setSelfSigned(false);
        rep10.setJwt(createDummyJwt("https://edugain.org/issuer", realmSubject, null, currentTime + (24 * 60 * 60)));
        assertBadRequest(trustMarksResource.create(rep10),
                "JWT must contain the 'iat' claim.");

        // Not self-signed, JWT 'exp' in the past
        TrustMarkRepresentation rep11 = new TrustMarkRepresentation();
        rep11.setTrustMarkType("https://edugain.org/trust-mark/external");
        rep11.setSelfSigned(false);
        rep11.setJwt(createDummyJwt("https://edugain.org/issuer", realmSubject, currentTime - 100, currentTime - 50));
        assertBadRequest(trustMarksResource.create(rep11),
                "JWT 'exp' claim must not be before current time.");

    }

    private void assertBadRequest(Response response, String expectedErrorMessage) {
        try (response) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
            assertNotNull("Expected ErrorRepresentation in response body", error);
            assertEquals(expectedErrorMessage, error.getErrorMessage());
        }
    }

    private String createDummyJwt(String iss, String sub, Long iat, Long exp) {
        JsonWebToken token = new JsonWebToken();
        if (iss != null) token.issuer(iss);
        if (sub != null) token.subject(sub);
        if (iat != null) token.iat(iat);
        if (exp != null) token.exp(exp);

        // Uses "none" algorithm to bypass actual crypto for structural tests
        return new JWSBuilder().type("JWT").jsonContent(token).none();
    }

    private void assertTrustMark(TrustMarkRepresentation expected, TrustMarkRepresentation actual, String expectedId) {
        assertEquals(expectedId, actual.getId());
        assertEquals(expected.isSelfSigned(), actual.isSelfSigned());
        assertEquals(expected.getTrustMarkType(), actual.getTrustMarkType());
        assertEquals(expected.getIat(), actual.getIat());
        assertEquals(expected.getExp(), actual.getExp());
        assertEquals(expected.getLogoUri(), actual.getLogoUri());
        assertEquals(expected.getRef(), actual.getRef());
        assertEquals(expected.getDelegation(), actual.getDelegation());
        assertEquals(expected.getJwt(), actual.getJwt());
    }
}
