package org.keycloak.testsuite.oidc.federation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.TrustMarkResource;
import org.keycloak.admin.client.resource.TrustMarksResource;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.OpenIdFederationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.TrustMarkRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
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
    public void internalTrustMarks() {
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
    public void externalTrustMarks() {
        RealmResource testRealm = adminClient.realm("test");
            TrustMarksResource trustMarksResource = testRealm.trustMarksResource();
            long currentTime = Time.currentTime();

            TrustMarkRepresentation created = new TrustMarkRepresentation();
            created.setTrustMarkType("https://edugain.org/trust-mark/external");
            created.setSelfSigned(false);

            // Generate a dummy JWT representing the trust mark
            String jwt = createDummyJwt("https://edugain.org/issuer", "https://edugain.org/subject", currentTime - 100, currentTime + (24 * 60 * 60));
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
            String updatedJwt = createDummyJwt("https://edugain.org/issuer", "https://edugain.org/subject", currentTime, currentTime + (24 * 60 * 60));
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

            // Case 1: Missing trustMarkType
            TrustMarkRepresentation rep1 = new TrustMarkRepresentation();
            rep1.setSelfSigned(true);
            rep1.setIat(currentTime - 100);
            assertBadRequest(trustMarksResource.create(rep1));

            // Case 2: Self-signed but exp is in the past
            TrustMarkRepresentation rep2 = new TrustMarkRepresentation();
            rep2.setTrustMarkType("type");
            rep2.setSelfSigned(true);
            rep2.setIat(currentTime - 100);
            rep2.setExp(currentTime - 50);
            assertBadRequest(trustMarksResource.create(rep2));

            // Case 3: Not self-signed, but has extraneous fields (logoUri)
            TrustMarkRepresentation rep3 = new TrustMarkRepresentation();
            rep3.setTrustMarkType("type");
            rep3.setSelfSigned(false);
            rep3.setJwt(createDummyJwt("iss", "sub", currentTime, currentTime + (24 * 60 * 60)));
            rep3.setLogoUri("https://edugain.org/logo.png");
            assertBadRequest(trustMarksResource.create(rep3));

            // Case 4: Not self-signed, JWT missing mandatory 'iss' claim
            TrustMarkRepresentation rep4 = new TrustMarkRepresentation();
            rep4.setTrustMarkType("type");
            rep4.setSelfSigned(false);
            rep4.setJwt(createDummyJwt(null, "sub", currentTime, currentTime + (24 * 60 * 60)));
            assertBadRequest(trustMarksResource.create(rep4));

            // Case 5: 'subject' directly in the root representation
            TrustMarkRepresentation rep5 = new TrustMarkRepresentation();
            rep5.setTrustMarkType("type");
            rep5.setSelfSigned(true);
            rep5.setIat(currentTime - 100);
            rep5.setSubject("invalid-root-subject");
            assertBadRequest(trustMarksResource.create(rep5));


    }

    private void assertBadRequest(Response response) {
        try {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        } finally {
            response.close();
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
