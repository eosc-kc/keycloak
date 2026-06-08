package org.keycloak.tests.oauth;

import org.keycloak.common.Profile;
import org.keycloak.protocol.oidc.resourceindicators.ResourceIndicatorsPostProcessor;
import org.keycloak.representations.AccessToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuthErrorException.INVALID_TARGET;
import static org.keycloak.protocol.oidc.resourceindicators.ResourceIndicatorConstants.ERROR_INVALID_RESOURCE;

@KeycloakIntegrationTest(config = ResourceIndicatorsWithoutCheckTest.ResourceIndicatorServerConfig.class)
public class ResourceIndicatorsWithoutCheckTest {

    @InjectRealm(config = ResourceIndicatorsWithoutCheckTest.ResourceIndicatorsWithoutCheckRealm.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = ResourceIndicatorsWithoutCheckTest.OAuthClientConfig.class)
    OAuthClient oauth;

    @TestSetup
    public void loginUser() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("urn:client:theservice").doLogin("user", "pass");
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());
    }

    @Test
    public void testValidResourceByClientUrn() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("urn:client:theservice").send();
        assertValidResponse(tokenResponse, "urn:client:theservice");
    }

    @Test
    public void testValidResourceByUrl() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("https://theservice").send();
        assertValidResponse(tokenResponse, "https://theservice");
    }

    @Test
    public void testInvalidResourceSyntax() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("/theservice2").send();
        assertErrorResponse(tokenResponse, INVALID_TARGET, ERROR_INVALID_RESOURCE);
    }

    @Test
    public void testAuthzInvalidResourceParam() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("/invalid").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());
        Assertions.assertEquals(authorizationEndpointResponse.getError(), INVALID_TARGET);
        Assertions.assertEquals(authorizationEndpointResponse.getErrorDescription(), ERROR_INVALID_RESOURCE);
    }

    @Test
    public void testAuthzResourceInTokenRequest() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("urn:client:theservice").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).resource("urn:client:theservice").send();
        assertValidResponse(accessTokenResponse, "urn:client:theservice");
    }

    @Test
    public void testAuthzNoResourceInTokenRequest() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("urn:client:theservice").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).send();
        assertValidResponse(accessTokenResponse, "urn:client:theservice");
    }

    @Test
    public void testAuthzDifferentResource() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("urn:client:otherservice").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).resource("urn:client:theservice").send();
        assertValidResponse(accessTokenResponse,  "urn:client:theservice");
    }

    @Test
    public void testRefreshWithNoResource() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("urn:client:theservice").send();
        Assertions.assertTrue(tokenResponse.isSuccess());

        AccessTokenResponse refreshResponse = oauth.refreshRequest(tokenResponse.getRefreshToken()).send();
        assertValidResponse(refreshResponse, "urn:client:theservice");
    }

    @Test
    public void testRefreshWithResourceByClientUrl() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("urn:client:theservice").send();
        Assertions.assertTrue(tokenResponse.isSuccess());

        AccessTokenResponse refreshResponse = oauth.refreshRequest(tokenResponse.getRefreshToken()).resource("urn:client:theservice").send();
        assertValidResponse(refreshResponse,  "urn:client:theservice");
    }

    @Test
    public void testRefreshWithDifferentResource() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("https://theservice").send();
        Assertions.assertTrue(tokenResponse.isSuccess());

        AccessTokenResponse refreshResponse = oauth.refreshRequest(tokenResponse.getRefreshToken()).resource("https://otherservice").send();
        assertValidResponse(refreshResponse,  "https://otherservice");
    }

    private static final class ResourceIndicatorsWithoutCheckRealm implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {

            realm.addUser("user").firstName("user").lastName("user").password("pass").email("the@email.localhost");

            realm.attribute(ResourceIndicatorsPostProcessor.RESOURCE_CHECK_IN_TOKEN_AUDIENCE, "false");

            return realm;
        }
    }

    private static final class OAuthClientConfig extends DefaultOAuthClientConfiguration {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return super.configure(client).fullScopeEnabled(true);
        }
    }

    protected static final class ResourceIndicatorServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.RESOURCE_INDICATORS);
        }
    }

    private void assertValidResponse(AccessTokenResponse response, String... expectedAudience) {
        Assertions.assertTrue(response.isSuccess());

        AccessToken accessToken = oauth.parseToken(response.getAccessToken(), AccessToken.class);
        MatcherAssert.assertThat(accessToken.getAudience(), Matchers.arrayContainingInAnyOrder(expectedAudience));
    }

    private void assertErrorResponse(AccessTokenResponse response, String expectedError, String expectedErrorDescription) {
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals(expectedError, response.getError());
        Assertions.assertEquals(expectedErrorDescription, response.getErrorDescription());
    }

}
