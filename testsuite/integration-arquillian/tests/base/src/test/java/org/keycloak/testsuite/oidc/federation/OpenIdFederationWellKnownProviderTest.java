package org.keycloak.testsuite.oidc.federation;

import java.io.IOException;
import java.net.URI;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.federation.OpenIdFederationWellKnownProviderFactory;
import org.keycloak.representations.idm.OpenIdFederationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.TrustMarkRepresentation;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.OPMetadata;
import org.keycloak.representations.openid_federation.RPMetadata;
import org.keycloak.representations.openid_federation.TrustMarks;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.oidc.AbstractWellKnownProviderTest;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.OpenIdFederationUtils;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class OpenIdFederationWellKnownProviderTest extends AbstractWellKnownProviderTest {

    protected String getWellKnownProviderId() {
        return OpenIdFederationWellKnownProviderFactory.PROVIDER_ID;
    }

    @Test
    public void testOpenIdFederationDiscovery() {
        Client client = AdminClientUtil.createResteasyClient();
        try {
            RealmResource testRealm = adminClient.realm("test");
            RealmRepresentation realmRep = testRealm.toRepresentation();
            realmRep.setOpenIdFederationEnabled(true);
            realmRep.setOpenIdFederationOrganizationName("Keycloak");
            realmRep.setOpenIdFederationResolveEndpoint("https://edugain.org/resolve");
            realmRep.setOpenIdFederationAuthorityHints(Stream.of("https://edugain.org/federation").collect(Collectors.toList()));
            realmRep.setOpenIdFederationOPClientRegistrationTypesSupported(Stream.of("EXPLICIT").collect(Collectors.toList()));
            realmRep.setOpenIdFederationRPClientRegistrationTypesSupported(Stream.of("EXPLICIT").collect(Collectors.toList()));
            realmRep.setOpenIdFederationEntityTypes(Stream.of("OPENID_PROVIDER", "OPENID_RELYING_PARTY").collect(Collectors.toList()));
            testRealm.update(realmRep);

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
            testRealm.openIdFederationsResource().create(openIdFederationRepresentation);

            long currentTime = Time.currentTime();
            TrustMarkRepresentation created = new TrustMarkRepresentation();
            created.setTrustMarkType("https://edugain.org/trust-mark/member");
            created.setSelfSigned(true);
            created.setIat(currentTime - 100);
            created.setExp(currentTime + (24 * 60 * 60));
            created.setLogoUri("https://edugain.org/logo.png");
            created.setRef("https://edugain.org/trust-mark/ref");
            created.setDelegation("https://edugain.org/delegation");
            testRealm.trustMarksResource().create(created);

            String jwtFederation;
            try (Response response = getOIDCFederationDiscoveryConfiguration(client, OAuthClient.AUTH_SERVER_ROOT)) {
                // HTTP response checks (OpenID Federation 1.0 / 1.1)
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
//                Assert.assertNotNull("Content-Type header must be present", response.getMediaType());
//                assertEquals("application", response.getMediaType().getType());
//                assertEquals("entity-statement+jwt", response.getMediaType().getSubtype());
                jwtFederation = response.readEntity(String.class);
            }

            // Entity Configuration JWS header checks
            JWSInput entityJws = new JWSInput(jwtFederation);
            JWSHeader entityHeader = entityJws.getHeader();
            assertEquals(TokenUtil.ENTITY_STATEMENT_JWT, entityHeader.getType());
            assertEquals("Entity Configuration alg must match realm signature algorithm", realmRep.getDefaultSignatureAlgorithm() != null ? realmRep.getDefaultSignatureAlgorithm() : Constants.DEFAULT_SIGNATURE_ALGORITHM, entityHeader.getRawAlgorithm());
            Assert.assertNotEquals("alg must not be none", "none", entityHeader.getRawAlgorithm());
            Assert.assertNotNull("Entity Configuration kid must be present", entityHeader.getKeyId());

            // Deserialize and verify Entity Configuration signature via OAuthClient and its own JWKS
            EntityStatement statement = oauth.verifyToken(jwtFederation, EntityStatement.class);
            Assert.assertNotNull("Entity Statement can not deserialize", statement);
            Assert.assertNotNull("Entity Statement JWKS must be present", statement.getJwks());

            JWK entityJwk = Arrays.stream(statement.getJwks().getKeys())
                    .filter(k -> entityHeader.getKeyId().equals(k.getKeyId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No matching JWK found in Entity Statement JWKS for kid: " + entityHeader.getKeyId()));
            assertEquals("Entity Configuration alg must match JWK alg", entityJwk.getAlgorithm(), entityHeader.getRawAlgorithm());
            PublicKey entityPublicKey = JWKParser.create(entityJwk).toPublicKey();
            Assert.assertTrue("Entity Configuration signature must verify against its own JWKS",
                    RSAProvider.verify(entityJws, entityPublicKey));

            String mainUrl = RealmsResource.realmBaseUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString();
            assertEquals(mainUrl, statement.getIssuer());
            assertEquals(mainUrl, statement.getSubject());
            assertEquals("https://edugain.org/federation", statement.getAuthorityHints().get(0));
            Assert.assertNotNull(statement.getMetadata().getFederationEntity());
            assertEquals("Keycloak", statement.getMetadata().getFederationEntity().getCommonMetadata().getOrganizationName());

            // Trust Marks checks
            List<TrustMarks> trustMarks = statement.getTrustMarks();
            Assert.assertNotNull("Trust marks claim must not be null", trustMarks);
            assertEquals("There should be exactly one trust mark", 1, trustMarks.size());
            TrustMarks trustMark = trustMarks.get(0);
            assertEquals("https://edugain.org/trust-mark/member", trustMark.getTrustMarkType());
            String selfSignedJwt = trustMark.getTrustMark();
            Assert.assertNotNull("Self-signed trust mark should generate a JWT", selfSignedJwt);

            JWSInput trustMarkJws = new JWSInput(selfSignedJwt);
            JWSHeader trustMarkHeader = trustMarkJws.getHeader();

            // 1. Trust Mark header checks
            assertEquals("trust-mark+jwt", trustMarkHeader.getType());
            assertEquals("Trust mark alg must match Entity Configuration alg", entityHeader.getRawAlgorithm(), trustMarkHeader.getRawAlgorithm());
            Assert.assertNotNull("kid must be present", trustMarkHeader.getKeyId());

            // 2. Cryptographic signature check against the Entity Configuration's JWKS
            JWK matchingJwk = Arrays.stream(statement.getJwks().getKeys())
                    .filter(k -> trustMarkHeader.getKeyId().equals(k.getKeyId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No matching JWK found for kid: " + trustMarkHeader.getKeyId()));
            assertEquals("Trust mark alg must match JWK alg", matchingJwk.getAlgorithm(), trustMarkHeader.getRawAlgorithm());
            PublicKey trustMarkPublicKey = JWKParser.create(matchingJwk).toPublicKey();
            Assert.assertTrue("Self-signed trust mark signature must be valid",
                    RSAProvider.verify(trustMarkJws, trustMarkPublicKey));

            // 3. Payload claim checks using TrustMarkRepresentation
            TrustMarkRepresentation decodedTrustMark = JsonSerialization.readValue(trustMarkJws.getContent(), TrustMarkRepresentation.class);
            assertEquals(mainUrl, decodedTrustMark.getIssuer());
            assertEquals(mainUrl, decodedTrustMark.getSubject());
            assertEquals("https://edugain.org/trust-mark/member", decodedTrustMark.getTrustMarkType());
            assertEquals(Long.valueOf(currentTime - 100), decodedTrustMark.getIat());
            assertEquals(Long.valueOf(currentTime + (24 * 60 * 60)), decodedTrustMark.getExp());
            assertEquals("https://edugain.org/logo.png", decodedTrustMark.getLogoUri());
            assertEquals("https://edugain.org/trust-mark/ref", decodedTrustMark.getRef());
            assertEquals("https://edugain.org/delegation", decodedTrustMark.getDelegation());

            OPMetadata op = statement.getMetadata().getOpenIdProviderMetadata();
            assertEquals(1, op.getClientRegistrationTypesSupported().size());
            assertEquals("explicit", op.getClientRegistrationTypesSupported().get(0));
            assertEquals(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT).path(RealmsResource.class).path(RealmsResource.class, "getOpenIdFederationClientsService").build("test").toString(), op.getFederationRegistrationEndpoint());
            testOidc(op);

            RPMetadata rp = statement.getMetadata().getRelyingPartyMetadata();
            Assert.assertNotNull("RPMetadata can not be null", rp);
            assertEquals(1, rp.getClientRegistrationTypes().size());
            assertEquals("explicit", rp.getClientRegistrationTypes().get(0));
            assertEquals("web", rp.getApplicationType());
            assertEquals(1, rp.getGrantTypes().size());
            assertEquals(OAuth2Constants.AUTHORIZATION_CODE, rp.getGrantTypes().get(0));
            assertEquals(1, rp.getRedirectUris().size());
            assertEquals(IdentityBrokerService.openidFederationRedirectUri(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString(), rp.getRedirectUris().get(0));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            RealmResource testRealm = adminClient.realm("test");
            RealmRepresentation realmRep = testRealm.toRepresentation();
            realmRep.setOpenIdFederationEnabled(false);
            testRealm.update(realmRep);
            client.close();
        }
    }

    @Test
    public void testWithoutOpenIdFederationDiscovery() {
        Client client = AdminClientUtil.createResteasyClient();
        try {
            // When no OpenID Federation is configured
            try (Response response = getOIDCFederationDiscoveryConfiguration(client, OAuthClient.AUTH_SERVER_ROOT)) {
                assertEquals(404, response.getStatus());
            }
        } finally {
            client.close();
        }
    }

    @Test
    @Ignore
    public void testDiscovery() {
    }

    @Test
    @Ignore
    public void testHttpDiscovery() {
    }

    @Test
    @Ignore
    public void testIssuerMatches() throws Exception {
    }

    @Test
    @Ignore
    public void corsTest() {
    }

    @Test
    @Ignore
    public void certs() throws IOException {
    }

    @Test
    @Ignore
    public void certsWithJwks() throws IOException {
    }

    @Test
    @Ignore
    public void testIntrospectionEndpointClaim() throws IOException {
    }

    @Test
    @Ignore
    public void testAcrValuesSupported() throws IOException {
    }

    @Test
    @Ignore
    public void testDpopSigningAlgValuesSupportedWithDpop() throws IOException {
    }

    @Test
    @Ignore
    public void testChangeClaimsSupported() throws IOException {
    }

    @Test
    @Ignore
    public void testDefaultProviderCustomizations() throws IOException {
    }

    private EntityStatement getOIDCFederationDiscoveryRepresentation(Client client, String uriTemplate) {
        try (Response response = getOIDCFederationDiscoveryConfiguration(client, uriTemplate)) {
            assertEquals("Expected HTTP 200 for Accept: application/entity-statement+jwt", 200, response.getStatus());
            assertEquals("Invalid Content-Type header", TokenUtil.APPLICATION_ENTITY_STATEMENT_JWT, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

            String jwtFederation = response.readEntity(String.class);
            Assert.assertNotNull("Response body must not be null", jwtFederation);
            Assert.assertFalse("Response body must be a compact JWT, not JSON", jwtFederation.trim().startsWith("{"));
            assertEquals("Compact JWT must have 3 dot-separated parts", 3, jwtFederation.split("\\.").length);

            return oauth.verifyToken(jwtFederation, EntityStatement.class);
        }
    }

    private Response getOIDCFederationDiscoveryConfiguration(Client client, String uriTemplate) {
        UriBuilder builder = UriBuilder.fromUri(uriTemplate);
        URI oidcDiscoveryUri = RealmsResource.wellKnownProviderUrl(builder).build("test", OpenIdFederationWellKnownProviderFactory.PROVIDER_ID);
        WebTarget oidcDiscoveryTarget = client.target(oidcDiscoveryUri);

        return oidcDiscoveryTarget.request(TokenUtil.APPLICATION_ENTITY_STATEMENT_JWT).get();
    }
}
