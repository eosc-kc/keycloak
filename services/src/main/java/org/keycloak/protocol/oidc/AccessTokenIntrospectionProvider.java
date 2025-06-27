/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.protocol.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.models.customcache.CustomCacheProvider;
import org.keycloak.models.customcache.CustomCacheProviderFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.services.util.UserSessionUtil;
import org.keycloak.util.JsonSerialization;
import org.keycloak.protocol.oidc.utils.Key;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AccessTokenIntrospectionProvider implements TokenIntrospectionProvider {

    private final KeycloakSession session;
    private final TokenManager tokenManager;
    private final RealmModel realm;
    private static final Logger logger = Logger.getLogger(AccessTokenIntrospectionProvider.class);
    private static final String wellKnown = "/.well-known/openid-configuration";
    private static final String PARAM_TOKEN = "token";
    private static final  String PROXIED_TOKEN_INTROSPECTION_FALLBACK_PROVIDERS = "proxiedTokenIntrospectionFallbackProviders";
    private static final ObjectMapper mapper = new ObjectMapper();

    private static CustomCacheProvider tokenRelayCache;

    public AccessTokenIntrospectionProvider(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.tokenManager = new TokenManager();
        initTokenCache();
    }

    private void initTokenCache(){
        if(tokenRelayCache != null)
            return;
        CustomCacheProviderFactory factory = (CustomCacheProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(CustomCacheProvider.class, "token-relay-cache");
        if(factory == null)
            throw new NotFoundException("Could not initate TokenRelayCacheProvider. Was not found");
        tokenRelayCache = factory.create(session);
    }

    public Response introspect(String token, EventBuilder eventBuilder) {
        try {
            String[] splitToken = token.split("\\.");
            String accessTokenStr = new String(Base64.getUrlDecoder().decode(splitToken[1]));
            JsonNode tokenJson = new ObjectMapper().readTree(accessTokenStr);
            String issuer = tokenJson.get("iss").asText();
            String realmUrl = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());
            if (realmUrl.equals(issuer)) {
                return introspectKeycloak(token, eventBuilder);
            } else {
                long exp = tokenJson.get("exp").asLong();
                if (isExpired(exp)) {
                    String clientId = tokenJson.get("azp").asText();
                    String description = String.format("Token introspection for %s client has expired  token. Token expiration = %d. Current time = %d", clientId, exp, Time.currentTime());
                    logger.warn(description);
                    eventBuilder.detail("Expired token", description);
                    ObjectNode tokenMetadata = JsonSerialization.createObjectNode();
                    tokenMetadata.put("active", false);
                    eventBuilder.error(Errors.INVALID_TOKEN);
                    return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
                }  else {
                    return introspectWithProxied(token, issuer, realm, eventBuilder);
                }
            }

        } catch (Exception e) {
            ObjectNode tokenMetadata = JsonSerialization.createObjectNode();
            tokenMetadata.put("active", false);
            eventBuilder.detail("Failure reason", e.getMessage());
            eventBuilder.error(Errors.TOKEN_INTROSPECTION_FAILED);
            try {
                return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
            } catch (IOException ioException) {
                throw new RuntimeException("Error creating token introspection response.", e);
            }
        }
    }

    protected Response introspectKeycloak (String token, EventBuilder eventBuilder) {

        AccessToken accessToken = null;
        try {

            accessToken = verifyAccessToken(token, eventBuilder);
            accessToken = transformAccessToken(accessToken);
            ObjectNode tokenMetadata;

            if (accessToken != null) {
                tokenMetadata = JsonSerialization.createObjectNode(accessToken);
                tokenMetadata.put("client_id", accessToken.getIssuedFor());

                String scope = accessToken.getScope();
                if (scope != null && scope.trim().isEmpty()) {
                    tokenMetadata.remove("scope");
                }

                if (!tokenMetadata.has("username")) {
                    if (accessToken.getPreferredUsername() != null) {
                        tokenMetadata.put("username", accessToken.getPreferredUsername());
                    } else {
                        UserModel userModel = session.users().getUserById(realm, accessToken.getSubject());
                        if (userModel != null) {
                            tokenMetadata.put("username", userModel.getUsername());
                        }
                    }
                }

                String sessionState = accessToken.getSessionState();

                if (sessionState != null) {
                    UserSessionModel userSession = session.sessions().getUserSession(realm, sessionState);

                    if (userSession != null) {
                        String actor = userSession.getNote(ImpersonationSessionNote.IMPERSONATOR_USERNAME.toString());

                        if (actor != null) {
                            // for token exchange delegation semantics when an entity (actor) other than the subject is the acting party to whom authority has been delegated
                            tokenMetadata.putObject("act").put("sub", actor);
                        }
                    }
                }

                tokenMetadata.put(OAuth2Constants.TOKEN_TYPE, accessToken.getType());

            } else {
                tokenMetadata = JsonSerialization.createObjectNode();
                logger.warn("Keycloak token introspection return false");
                eventBuilder.error(Errors.TOKEN_INTROSPECTION_FAILED);
            }

            tokenMetadata.put("active", accessToken != null);

            return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            String clientId = accessToken != null ? accessToken.getIssuedFor() : "unknown";
            logger.warnf(e, "Exception during Keycloak introspection for %s client in realm %s", clientId, realm.getName());
            eventBuilder.detail(Details.REASON, e.getMessage());
            eventBuilder.error(Errors.TOKEN_INTROSPECTION_FAILED);
            throw new RuntimeException("Error creating token introspection response.", e);
        }
    }

    private AccessToken transformAccessToken(AccessToken token) {
        if (token == null) {
            return null;
        }

        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        EventBuilder event = new EventBuilder(realm, session, session.getContext().getConnection())
                .event(EventType.INTROSPECT_TOKEN)
                .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);
        UserSessionModel userSession;
        try {
            userSession = UserSessionUtil.findValidSession(session, realm, token, event, client);
        } catch (Exception e) {
            logger.warnf("Can not get user session: %s", e.getMessage());
            return null;
        }
        if (userSession.getUser() == null) {
            logger.warnf("User not found");
            return null;
        }
        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
        ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionAndScopeParameter(clientSession, token.getScope(), session);
        AccessToken smallToken = getAccessTokenFromStoredData(token, userSession);
        return tokenManager.transformIntrospectionAccessToken(session, smallToken, userSession, clientSessionCtx, smallToken.getScope());
    }

    private AccessToken getAccessTokenFromStoredData(AccessToken token, UserSessionModel userSession) {
        // Copy just "basic" claims from the initial token. The same like filled in TokenManager.initToken. The rest should be possibly added by protocol mappers (only if configured for introspection response)
        AccessToken newToken = new AccessToken();
        newToken.id(token.getId());
        newToken.type(token.getType());
        newToken.subject(token.getSubject() != null ? token.getSubject() : userSession.getUser().getId());
        newToken.iat(token.getIat());
        newToken.exp(token.getExp());
        newToken.issuedFor(token.getIssuedFor());
        newToken.issuer(token.getIssuer());
        newToken.setNonce(token.getNonce());
        newToken.setScope(token.getScope());
        newToken.setAuth_time(token.getAuth_time());
        newToken.setSessionState(token.getSessionState());

        // In the case of a refresh token, aud is a basic claim.
        newToken.audience(token.getAudience());

        // The cnf is not a claim controlled by the protocol mapper.
        newToken.setCertConf(token.getCertConf());
        return newToken;
    }

    protected AccessToken verifyAccessToken(String token, EventBuilder eventBuilder) {
        AccessToken accessToken;

        try {
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(token, AccessToken.class)
                    .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

            SignatureVerifierContext verifierContext = session.getProvider(SignatureProvider.class, verifier.getHeader().getAlgorithm().name()).verifier(verifier.getHeader().getKeyId());
            verifier.verifierContext(verifierContext);

            accessToken = verifier.verify().getToken();
        } catch (VerificationException e) {
            logger.warnf("Introspection access token : JWT check failed: %s", e.getMessage());
            eventBuilder.detail(Details.REASON,"Access token JWT check failed");
            return null;
        }

        RealmModel realm = this.session.getContext().getRealm();

        return tokenManager.checkTokenValidForIntrospection(session, realm, accessToken, false, eventBuilder) ? accessToken : null;
    }

  protected Response introspectWithProxied(String token, String issuer, RealmModel realm, EventBuilder eventBuilder) throws IOException {

        try {
            String cachedToken = (String) tokenRelayCache.get(new Key(token, realm.getName()));
            if(cachedToken != null)
                return Response.ok(cachedToken).type(MediaType.APPLICATION_JSON_TYPE).build();

            IdentityProviderModel issuerIdp = realm.getIdentityProvidersStream().filter(idp -> issuer.equals(idp.getConfig().get("issuer")) && idp.isEnabled()).findAny().orElse(null);
            if (issuerIdp != null) {
                OIDCIdentityProviderConfig oidcIssuerIdp = new OIDCIdentityProviderConfig(issuerIdp);
                OIDCIdentityProvider oidcIssuerProvider = new OIDCIdentityProvider(session, oidcIssuerIdp);
                InputStream inputStream = session.getProvider(HttpClientProvider.class).get(new String(oidcIssuerIdp.getIssuer() + wellKnown));
                OIDCConfigurationRepresentation rep = JsonSerialization.readValue(inputStream, OIDCConfigurationRepresentation.class);
                if (rep.getIntrospectionEndpoint() != null) {
                    SimpleHttp.Response response = oidcIssuerProvider.authenticateTokenRequest(SimpleHttp.doPost(rep.getIntrospectionEndpoint(), session).param(PARAM_TOKEN, token)).asResponse();
                    if (response.getResponse().getStatusLine().getStatusCode() > 300) {
                        logger.warn("Remote introspection Idp return http status " + response.getResponse().getStatusLine().getStatusCode() + " with body :");
                        logger.warn(IOUtils.toString(response.getResponse().getEntity().getContent(), StandardCharsets.UTF_8));
                        ObjectNode tokenMetadata = JsonSerialization.createObjectNode();
                        tokenMetadata.put("active", false);
                        return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
                    }
                    String responseJson = IOUtils.toString(response.getResponse().getEntity().getContent(), Charset.defaultCharset());
                    tokenRelayCache.put(new Key(token, realm.getName()), responseJson);
                    return Response.status(response.getResponse().getStatusLine().getStatusCode()).type(MediaType.APPLICATION_JSON_TYPE).entity(responseJson).build();
                }
            } else if (realm.getAttribute(PROXIED_TOKEN_INTROSPECTION_FALLBACK_PROVIDERS) != null){
                //fallback IdP
                List<String> fallbackIdPsAlias = Arrays.asList(realm.getAttribute(PROXIED_TOKEN_INTROSPECTION_FALLBACK_PROVIDERS).split(","));
                HttpClientProvider httpClientProvider = session.getProvider(HttpClientProvider.class);
                for (String alias : fallbackIdPsAlias ){
                    IdentityProviderModel idp = realm.getIdentityProviderByAlias(alias);
                    if (idp != null && idp.isEnabled() ) {
                        OIDCIdentityProviderConfig oidcIssuerIdp = new OIDCIdentityProviderConfig(idp);
                        OIDCIdentityProvider oidcIssuerProvider = new OIDCIdentityProvider(session, oidcIssuerIdp);
                        InputStream inputStream = httpClientProvider.get(new String(oidcIssuerIdp.getIssuer() + wellKnown));
                        OIDCConfigurationRepresentation rep = JsonSerialization.readValue(inputStream, OIDCConfigurationRepresentation.class);
                        if (rep.getIntrospectionEndpoint() != null) {
                            SimpleHttp.Response response = oidcIssuerProvider.authenticateTokenRequest(SimpleHttp.doPost(rep.getIntrospectionEndpoint(), session).param(PARAM_TOKEN, token)).asResponse();
                            String responseJson = IOUtils.toString(response.getResponse().getEntity().getContent(), Charset.defaultCharset());
                            if (response.getResponse().getStatusLine().getStatusCode() < 300 && mapper.readTree(responseJson).path("active").asBoolean(false)) {
                                return Response.status(response.getResponse().getStatusLine().getStatusCode()).type(MediaType.APPLICATION_JSON_TYPE).entity(responseJson).build();
                            } else {
                                logger.warnf("IdP with alias %s responde to token introspection with status %d and body : %s", alias, response.getResponse().getStatusLine().getStatusCode(), responseJson);
                            }
                        }
                    }
                }
            }
            //if failed to find issuer in IdPs or IntrospectionEndpoint does not exist for specific Idp return false
            String problem = issuerIdp != null ? "Remote introspection: problem getting remote Idp with issuer " + issuer + "introspection endpoint" : "Remote introspection: Idp with issuer " + issuer + " does not exist";
            logger.warn(problem);
            eventBuilder.detail("Remote introspection problem", problem);
            eventBuilder.error(Errors.TOKEN_INTROSPECTION_FAILED);
            ObjectNode tokenMetadata = JsonSerialization.createObjectNode();
            tokenMetadata.put("active", false);
            return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            eventBuilder.detail("Remote introspection exception", e.getMessage());
            eventBuilder.error(Errors.TOKEN_INTROSPECTION_FAILED);
            logger.warn("Error during remote introspection", e);
            throw new RuntimeException("Error creating token introspection response.", e);
        }
    }

    private boolean isExpired(Long exp) {
        return exp != null && exp != 0 ? Time.currentTime() > exp : false;
    }

    @Override
    public void close() {

    }
}
