package org.keycloak.broker.oidc.federation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.PkceUtils;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.Urls;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.Booleans;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.StringUtil;

/**
 * OpenID Federation Automatic Registration IdP (Keycloak as RP).
 * <p>
 * Differs from {@link OpenIdFederationIdentityProvider} (explicit registration) by:
 * <ul>
 *   <li>Authorization: signed JAR ({@code request}) with required federation claims; optional {@code trust_chain}</li>
 *   <li>Token endpoint: {@code private_key_jwt} with RP Entity ID as client_id / assertion iss+sub,
 *       audience = OP Entity Identifier</li>
 * </ul>
 * Native {@link org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider#authenticateTokenRequest}
 * already implements private_key_jwt once the config forces that client auth method.
 */
public class OpenIdFederationAutomaticIdentityProvider extends OpenIdFederationIdentityProvider {

    private static final String BROKER_NONCE_PARAM = "BROKER_NONCE";
    private static final String BROKER_CODE_CHALLENGE_PARAM = "BROKER_CODE_CHALLENGE";
    private static final String BROKER_CODE_CHALLENGE_METHOD_PARAM = "BROKER_CODE_CHALLENGE_METHOD";

    public OpenIdFederationAutomaticIdentityProvider(KeycloakSession session, OpenIdFederationAutomaticIdentityProviderConfig config) {
        super(session, config);
    }

    @Override
    public OpenIdFederationAutomaticIdentityProviderConfig getConfig() {
        return (OpenIdFederationAutomaticIdentityProviderConfig) super.getConfig();
    }

    /**
     * Build authorization URL with a signed Request Object (JAR) per OpenID Federation §12.1.1.1.
     * Outer query carries {@code client_id} and {@code request}; OIDC params live inside the JWT.
     */
    @Override
    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        OpenIdFederationAutomaticIdentityProviderConfig config = getConfig();
        config.setClientId(getConfig().getClientId());

        AuthenticationSessionModel authenticationSession = request.getAuthenticationSession();
        Map<String, Object> claims = new HashMap<>();

        claims.put(OAUTH2_PARAMETER_RESPONSE_TYPE, "code");
        claims.put(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId());
        claims.put(OAUTH2_PARAMETER_REDIRECT_URI, request.getRedirectUri());
        claims.put(OAUTH2_PARAMETER_STATE, request.getState().getEncoded());

        //correct???
        String scope = getConfig().getDefaultScope() != null ? getConfig().getDefaultScope() : OAuth2Constants.SCOPE_OPENID;
        claims.put(OAUTH2_PARAMETER_SCOPE, scope);

        if (!config.isDisableNonce()) {
            String nonce = Base64Url.encode(SecretGenerator.getInstance().randomBytes(16));
            authenticationSession.setClientNote(BROKER_NONCE_PARAM, nonce);
            claims.put(OIDCLoginProtocol.NONCE_PARAM, nonce);
        }

        String loginHint = authenticationSession.getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        if (Booleans.isTrue(config.isLoginHint()) && loginHint != null) {
            claims.put(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
        }

        if (config.isUiLocales()) {
            claims.put(OIDCLoginProtocol.UI_LOCALES_PARAM, session.getContext().resolveLocale(null).toLanguageTag());
        }

        String prompt = config.getPrompt();
        if (StringUtil.isBlank(prompt)) {
            prompt = authenticationSession.getClientNote(OAuth2Constants.PROMPT);
        }
        if (StringUtil.isNotBlank(prompt)) {
            claims.put(OAuth2Constants.PROMPT, prompt);
        }

        String maxAge = authenticationSession.getClientNote(OIDCLoginProtocol.MAX_AGE_PARAM);
        if (Booleans.isTrue(config.isPassMaxAge()) && maxAge != null) {
            claims.put(OIDCLoginProtocol.MAX_AGE_PARAM, maxAge);
        }

        if (config.isPkceEnabled()) {
            String codeVerifier = PkceUtils.generateCodeVerifier();
            String codeChallengeMethod = config.getPkceMethod();
            authenticationSession.setClientNote(BROKER_CODE_CHALLENGE_PARAM, codeVerifier);
            authenticationSession.setClientNote(BROKER_CODE_CHALLENGE_METHOD_PARAM, codeChallengeMethod);
            claims.put(OAuth2Constants.CODE_CHALLENGE, PkceUtils.encodeCodeChallenge(codeVerifier, codeChallengeMethod));
            claims.put(OAuth2Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod);
        }

        // Required Request Object claims (§12.1.1.1). sub MUST NOT be present.
        JsonWebToken requestObject = new JsonWebToken();
        requestObject.id(SecretGenerator.getInstance().generateSecureID());
        requestObject.issuer(getConfig().getClientId());
        requestObject.audience(config.getIssuer());
        requestObject.exp((long) Time.currentTime() + session.getContext().getRealm().getAccessCodeLifespan());
        requestObject.issuedNow();
        requestObject.getOtherClaims().putAll(claims);
        requestObject.getOtherClaims().put(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId());

//        List<String> trustChain = config.getTrustChain();
//        if (trustChain != null && !trustChain.isEmpty()) {
//            // Spec prefers trust_chain as a JWS header; claim form is still valid (§12.1.1.1).
//            requestObject.setOtherClaims(TokenUtil.TRUST_CHAIN, trustChain);
//        }

        String jar = new JWSBuilder()
                .type(TokenUtil.OAUTH_AUTHZ_REQUEST_JWT)
                .jsonContent(requestObject)
                .sign(getSignatureContext());

        return UriBuilder.fromUri(config.getAuthorizationUrl())
                .queryParam(OAUTH2_PARAMETER_CLIENT_ID, getConfig().getClientId())
                .queryParam(OAUTH2_PARAMETER_RESPONSE_TYPE, "code")
                .queryParam(OIDCLoginProtocol.REQUEST_PARAM, jar);
    }

    /**
     * private_key_jwt for automatic registration: aud MUST be the OP Entity Identifier only.
     */
    @Override
    protected JsonWebToken generateToken() {
        JsonWebToken jwt = super.generateToken();
        //needed????
        jwt.audience(getConfig().getIssuer());
        jwt.issuer(getConfig().getClientId());
        jwt.subject(getConfig().getClientId());
        return jwt;
    }

}
