/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.client.policies;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientRolesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateContextConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureClientAuthenticatorExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureRequestObjectExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureResponseTypeExecutor;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureSigningAlgorithmEnforceExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureSigningAlgorithmForSignedJwtEnforceExecutorConfig;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.BadRequestException;

import org.apache.http.HttpResponse;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.Profile;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.Constants;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AuthorizationResponseToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthenticatorExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureLogoutExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureParContentsExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutor;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureResponseTypeExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSessionEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmForSignedJwtExecutorFactory;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;
import org.keycloak.common.util.UriUtils;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;


import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.Metadata;
import org.keycloak.representations.openid_federation.CommonMetadata;
import org.keycloak.representations.openid_federation.RPMetadata;
import org.keycloak.services.clientpolicy.executor.AutomaticClientRegistrationExecutorFactory;
import org.keycloak.services.clientpolicy.condition.AutomaticClientRegistrationConditionFactory;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.services.clientpolicy.executor.AutomaticClientRegistrationExecutor.AuthorizationEndpointRequestObjectForAutomaticClientRegistration;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.executor.UseLightweightAccessTokenExecutorFactory;
import org.keycloak.representations.openid_federation.OpenIdFederationEntity;
import org.keycloak.representations.idm.OpenIdFederationRepresentation;
import org.keycloak.util.JWKSUtils;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.ServerECDSASignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;

import  org.keycloak.util.TokenUtil;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;


/**
 * This test class is for testing an executor of client policies.
 * 
 * @author
 */
public class AutomaticClientRegistrationExecutorTest extends AbstractClientPoliciesTest {

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected AppPage appPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LogoutConfirmPage logoutConfirmPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        realm.setOpenIdFederationEnabled(true);
        realm.setOpenIdFederationAuthorityHints(List.of("https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/oidfed-ia/"));
        realm.setOpenIdFederationOPClientRegistrationTypesSupported(Arrays.asList("AUTOMATIC","EXPLICIT"));
        realm.setOpenIdFederationEntityTypes(Arrays.asList("OPENID_RELYING_PARTY","OPENID_PROVIDER"));
        
        OpenIdFederationRepresentation openIdFederation = new OpenIdFederationRepresentation();
        openIdFederation.setInternalId("internalid");
        openIdFederation.setTrustAnchor("https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/oidfed-ta/");

        List<OpenIdFederationRepresentation> list = new ArrayList<>();
        list.add(openIdFederation); 

        realm.setOpenIdFederationList(list);

        List<UserRepresentation> users = realm.getUsers();

        LinkedList<CredentialRepresentation> credentials = new LinkedList<>();
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue("password");
        credentials.add(password);

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername("manage-clients");
        user.setCredentials(credentials);
        user.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID, Collections.singletonList(AdminRoles.MANAGE_CLIENTS)));

        users.add(user);

        user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername("create-clients");
        user.setCredentials(credentials);
        user.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID, Collections.singletonList(AdminRoles.CREATE_CLIENT)));
        user.setGroups(List.of("topGroup")); // defined in testrealm.json

        users.add(user);

        realm.setUsers(users);

        List<ClientRepresentation> clients = realm.getClients();

        ClientRepresentation app = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("test-device")
                .secret("secret")
                .attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
                .attribute(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+")
                .build();
        clients.add(app);

        ClientRepresentation appPublic = ClientBuilder.create().id(KeycloakModelUtils.generateId()).publicClient()
                .clientId(DEVICE_APP_PUBLIC)
                .attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
                .attribute(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+")
                .build();
        clients.add(appPublic);

        userId = KeycloakModelUtils.generateId();
        UserRepresentation deviceUser = UserBuilder.create()
                .id(userId)
                .username("device-login")
                .email("device-login@localhost")
                .password("password")
                .build();
        users.add(deviceUser);

        testRealms.add(realm);
    }

    protected AuthorizationEndpointRequestObjectForAutomaticClientRegistration 
                                createRequestObjectForAutomaticClientRegistration(String clientId) {

        CommonMetadata commonMetadata = new CommonMetadata();
        commonMetadata.setOrganizationName("SUNET");
        commonMetadata.setSignedJwksUri("https://openid.sunet.se/rp/signed_jwks.jose"); 
        // Should this be the endpoint which provides a publickey for validating request object? 
             
        RPMetadata rpMetadata = new RPMetadata();
        rpMetadata.setClientRegistrationTypes(Arrays.asList("automatic", "explicit"));
        rpMetadata.setCommonMetadata(commonMetadata);
        rpMetadata.setRequestObjectSigningAlgValuesSupported(Arrays.asList( "ES256","RS256"));
        rpMetadata.setRedirectUris(Arrays.asList("https://localhost:8543/auth/realms/master/app/auth"));
        rpMetadata.setTokenEndpointAuthMethod(OIDCLoginProtocol.PRIVATE_KEY_JWT);

        Metadata metadata = new Metadata();
        metadata.setRelyingPartyMetadata(rpMetadata);

        AuthorizationEndpointRequestObjectForAutomaticClientRegistration requestObject = new AuthorizationEndpointRequestObjectForAutomaticClientRegistration();
        requestObject.issuer("https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/");
        requestObject.issuedNow();
        requestObject.exp(requestObject.getIat()+(long) Time.currentTime()+300L);
        requestObject.setAuthorityHints(Arrays.asList("https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/oidfed-ia/"));
        requestObject.type(TokenUtil.ENTITY_STATEMENT_JWT);
        requestObject.setMetadata(metadata);
        String authServerBaseUrl = UriUtils.getOrigin(oauth.getRedirectUri()) + "/auth";
        requestObject.addAudience(KeycloakUriBuilder.fromUri(authServerBaseUrl).path(ServiceUrlConstants.REALM_INFO_PATH).build(REALM_NAME).toString());
        // set no subject here
        requestObject.setClientId(clientId);
        requestObject.id(KeycloakModelUtils.generateId());
        requestObject.nbf(requestObject.getIat());
        requestObject.setResponseType("code");
        requestObject.setRedirectUriParam(oauth.getRedirectUri());
        requestObject.setScope("openid");
        String state = KeycloakModelUtils.generateId();
        requestObject.setState(state);
        requestObject.setNonce(KeycloakModelUtils.generateId());

        return requestObject;
    }
    
    protected void registerRequestObject(AuthorizationEndpointRequestObjectForAutomaticClientRegistration requestObject, String clientId, String sigAlg) throws IOException {

        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // generate and register client keypair
        oidcClientEndpointsResource.generateKeys(sigAlg);

        JSONWebKeySet jwks = oidcClientEndpointsResource.getJwks();
        requestObject.setJwks(jwks);
        
        // register request object
        byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
        String encodedRequestObject = Base64Url.encode(contentBytes);
        oidcClientEndpointsResource.registerOIDCRequestForOIDFED(encodedRequestObject, sigAlg);
        
        // register RP's entity configuration
        EntityStatement entityStatement = (EntityStatement) requestObject;
        entityStatement.subject(entityStatement.getIssuer()); // subject of Entity Configuration is equal to issuer
        byte[] contentBytes1 = JsonSerialization.writeValueAsBytes(entityStatement);
        String encodedEntityStatement = Base64Url.encode(contentBytes1);
        oidcClientEndpointsResource.registerEntityConfiguration(encodedEntityStatement);

        request = oidcClientEndpointsResource.getOIDCRequest();
    }
    
    protected EntityStatement createEntityConfigurationForIA(){
        // create metadata of IA
        CommonMetadata commonMetadata = new CommonMetadata();
        commonMetadata.setOrganizationName("oidfed-ia");
        
        OpenIdFederationEntity federationMetadata = new OpenIdFederationEntity();
        federationMetadata.setCommonMetadata(commonMetadata);
        federationMetadata.setFederationFetchEndpoint(TestApplicationResourceUrls.oidfedIAFederationEndpoint());

        Metadata metadata = new Metadata();
        metadata.setFederationEntity(federationMetadata);

        EntityStatement entityStatement = new EntityStatement(
                "https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/oidfed-ia/",
                (long) Time.currentTime()+300L, 
                Arrays.asList("https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/oidfed-ta/"), 
                null, //JSONWebKeySet is set after key generation 
                metadata
       );

       return entityStatement;
     }

     protected EntityStatement createSubordibnateStatementOfIA(){
        // create metadata of RP
        CommonMetadata commonMetadata = new CommonMetadata();
        commonMetadata.setOrganizationName("SUNET");
        commonMetadata.setSignedJwksUri("https://openid.sunet.se/rp/signed_jwks.jose");
        // Should this be changed as same as the entity configuration of RP?
             
        RPMetadata rpMetadata = new RPMetadata();
        rpMetadata.setClientRegistrationTypes(Arrays.asList("automatic", "explicit"));
        rpMetadata.setCommonMetadata(commonMetadata);
        rpMetadata.setRequestObjectSigningAlgValuesSupported(Arrays.asList( "ES256","RS256"));
        rpMetadata.setRedirectUris(Arrays.asList("https://localhost:8543/auth/realms/master/app/auth"));
        rpMetadata.setTokenEndpointAuthMethod(OIDCLoginProtocol.PRIVATE_KEY_JWT);
        
        Metadata metadata = new Metadata();
        metadata.setRelyingPartyMetadata(rpMetadata);

        EntityStatement entityStatement = new EntityStatement(
                "https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/oidfed-ia/", 
                (long) Time.currentTime()+300L, 
                null, //authority hints 
                null, //JSONWebKeySet is set later 
                metadata
       );
       entityStatement.subject("https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/");

       return entityStatement;
     }

     protected EntityStatement createEntityConfigurationForTA(){
        CommonMetadata commonMetadata = new CommonMetadata();
        commonMetadata.setOrganizationName("oidfed-ta");
        
        OpenIdFederationEntity federationMetadata = new OpenIdFederationEntity();
        federationMetadata.setCommonMetadata(commonMetadata);
        federationMetadata.setFederationFetchEndpoint(TestApplicationResourceUrls.oidfedTAFederationEndpoint());

        Metadata metadata = new Metadata();
        metadata.setFederationEntity(federationMetadata);

        EntityStatement entityStatement = new EntityStatement(
                "https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/oidfed-ta/",
                (long) Time.currentTime()+300L, 
                null,     //no authority hints for TA, 
                null,     //JSONWebKeySet is set after key generation 
                metadata
       );

       return entityStatement;
     }

     protected EntityStatement createSubordibnateStatementOfTA(){
        CommonMetadata commonMetadata = new CommonMetadata();
        commonMetadata.setOrganizationName("oidfed-ia");
        
        OpenIdFederationEntity federationMetadata = new OpenIdFederationEntity();
        federationMetadata.setCommonMetadata(commonMetadata);
        
        Metadata metadata = new Metadata();
        metadata.setFederationEntity(federationMetadata);

        EntityStatement entityStatement = new EntityStatement(
                "https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/oidfed-ta/", 
                (long) Time.currentTime()+300L, 
                null, //authority hints 
                null, //JSONWebKeySet is set later
                metadata
       );
       entityStatement.subject("https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/oidfed-ia/");

       return entityStatement;
     }


    @Test
    public void testAutomaticClientRegistrationExecutor() throws Exception {
        // prepare client policy
        String json;
        try{
            json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                        (new ClientPoliciesUtil.ClientProfileBuilder())
                                .createProfile(PROFILE_NAME, "Automatic Client Registration")
                                .addExecutor(AutomaticClientRegistrationExecutorFactory.PROVIDER_ID,null)
                                .toRepresentation()
            ).toString();
            updateProfiles(json);

            json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                        (new ClientPoliciesUtil.ClientPolicyBuilder())
                        .createPolicy(POLICY_NAME, "Automatic Client Registration policy", Boolean.TRUE)
                        .addCondition(AutomaticClientRegistrationConditionFactory.PROVIDER_ID,createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
            ).toString();
            updatePolicies(json);
        } catch (Exception e) {
                fail();
        }

        String clientId = "https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/";
        oauth.client(clientId);
        
        // prepare request object for OIDFED Auhtomatic Client Registration
        AuthorizationEndpointRequestObjectForAutomaticClientRegistration requestObject;
        requestObject = createRequestObjectForAutomaticClientRegistration(clientId);
        // registrer the request object to the mock server of Client
        registerRequestObject(requestObject, clientId, Algorithm.ES256);

        // For the mock server of IA
        EntityStatement iaEntityConfiguration = createEntityConfigurationForIA();
        byte[] contentBytes = JsonSerialization.writeValueAsBytes(iaEntityConfiguration);
        String encodedIAEntityConfiguration = Base64Url.encode(contentBytes);
        
        EntityStatement subordinateStatementOfIA = createSubordibnateStatementOfIA();
        contentBytes = JsonSerialization.writeValueAsBytes(subordinateStatementOfIA);
        String encodedSubordinateStatementOfIA = Base64Url.encode(contentBytes);
        
        // register entity statments into the mock server of IA
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.registerEntityStatementsForIA(encodedIAEntityConfiguration,
                                                        encodedSubordinateStatementOfIA,Algorithm.ES256);

        // For the mock server of TA
        EntityStatement taEntityConfiguration = createEntityConfigurationForTA();
        contentBytes = JsonSerialization.writeValueAsBytes(taEntityConfiguration);
        String encodedTAEntityConfiguration = Base64Url.encode(contentBytes);
        
        EntityStatement subordinateStatementOfTA = createSubordibnateStatementOfTA();
        contentBytes = JsonSerialization.writeValueAsBytes(subordinateStatementOfTA);
        String encodedSubordinateStatementOfTA = Base64Url.encode(contentBytes);
        
        // register entity statements into the mock server of TA
        oidcClientEndpointsResource.registerEntityStatementsForTA(encodedTAEntityConfiguration,
                                                        encodedSubordinateStatementOfTA,Algorithm.ES256);
        
        
        // sending authorization request with request object
        oauth.loginForm().request(request).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.parseLoginResponse();

        assertNotNull(authorizationEndpointResponse.getCode());

        AccessTokenResponse response = doAccessTokenRequestWithSignedJWT(clientId,
                        authorizationEndpointResponse.getCode(), createEncodedTokenForClientAuth(clientId));

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getIdToken());

        String [] splits = response.getIdToken().split("\\.");        
        byte[] serializedIDtoken = Base64.getDecoder().decode(splits[1]);
        IDToken idtoken = null;
        try {
            idtoken = JsonSerialization.readValue(serializedIDtoken, IDToken.class);
        } catch (IOException e) {
            fail();
        }

        // aud in IDtoken should be clientID
        assertEquals("["+clientId+"]", Arrays.toString(idtoken.getAudience())); 
            
    }

    protected String createEncodedTokenForClientAuth(String clientId) {
        String authServerBaseUrl = UriUtils.getOrigin(oauth.getRedirectUri()) + "/auth";
        String realmInfoUrl = KeycloakUriBuilder.fromUri(authServerBaseUrl).path(ServiceUrlConstants.REALM_INFO_PATH).build(REALM_NAME).toString();

        JsonWebToken reqToken = new JsonWebToken();
        reqToken.id(KeycloakModelUtils.generateId());
        reqToken.issuer(clientId);
        reqToken.subject(clientId);
        reqToken.audience(realmInfoUrl);

        long now = Time.currentTime();
        reqToken.iat(now);
        reqToken.exp(now + 10);
        reqToken.nbf(now);

        byte[] contentBytes = null;
        String encodedRequestToken = null;
        try{
            contentBytes = JsonSerialization.writeValueAsBytes(reqToken);
            encodedRequestToken = Base64Url.encode(contentBytes);
        } catch (Exception e) {
            fail();
        }

        return testingClient.testApp().oidcClientEndpoints().signTokenForClientAuth(encodedRequestToken);

    }

    protected AccessTokenResponse doAccessTokenRequestWithSignedJWT(String client_id, String code, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));
        
        CloseableHttpResponse response = sendRequest(oauth.getEndpoints().getToken(), parameters);
        return new AccessTokenResponse(response);
    }

    private CloseableHttpResponse sendRequest(String requestUrl, List<NameValuePair> parameters) throws Exception {
        try (CloseableHttpClient client = new DefaultHttpClient()) {
            HttpPost post = new HttpPost(requestUrl);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
            post.setEntity(formEntity);
            return client.execute(post);
        }
    }

}
