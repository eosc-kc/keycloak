/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.broker.oidc;

import static org.keycloak.common.util.UriUtils.checkUrl;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;

/**
 * @author Pedro Igor
 */
public class OIDCIdentityProviderConfig extends OAuth2IdentityProviderConfig {

    public static final String JWKS_URL = "jwksUrl";

    public static final String USE_JWKS_URL = "useJwksUrl";
    public static final String VALIDATE_SIGNATURE = "validateSignature";
    public static final String IS_ACCESS_TOKEN_JWT = "isAccessTokenJWT";
    public static final String TOKEN_INTROSPECTION_URL = "tokenIntrospectionUrl";
    public static final String VALIDATE_REFRESH_TOKEN = "validateRefreshToken";
    public static final String CLAIMS_PARAMETER_SUPPORTED = "claimsParameterSupported";
    public static final String ISSUER = "issuer";
    public static final String LOGOUT_URL = "logoutUrl";

    public OIDCIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public OIDCIdentityProviderConfig() {
        super();
    }

    public String getPrompt() {
        return getConfig().get("prompt");
    }
    public void setPrompt(String prompt) {
        getConfig().put("prompt", prompt);
    }

    public String getIssuer() {
        return getConfig().get(ISSUER);
    }
    public void setIssuer(String issuer) {
        getConfig().put(ISSUER, issuer);
    }
    public String getLogoutUrl() {
        return getConfig().get(LOGOUT_URL);
    }
    public void setLogoutUrl(String url) {
        getConfig().put(LOGOUT_URL, url);
    }

    public String getPublicKeySignatureVerifier() {
        return getConfig().get("publicKeySignatureVerifier");
    }

    public void setPublicKeySignatureVerifier(String signingCertificate) {
        getConfig().put("publicKeySignatureVerifier", signingCertificate);
    }

    public String getPublicKeySignatureVerifierKeyId() {
        return getConfig().get("publicKeySignatureVerifierKeyId");
    }

    public void setPublicKeySignatureVerifierKeyId(String publicKeySignatureVerifierKeyId) {
        getConfig().put("publicKeySignatureVerifierKeyId", publicKeySignatureVerifierKeyId);
    }

    public boolean isValidateSignature() {
        return Boolean.valueOf(getConfig().get("validateSignature"));
    }

    public void setValidateSignature(boolean validateSignature) {
        getConfig().put(VALIDATE_SIGNATURE, String.valueOf(validateSignature));
    }

    public void setAccessTokenJwt(boolean accessTokenJwt) {
        getConfig().put(IS_ACCESS_TOKEN_JWT, String.valueOf(accessTokenJwt));
    }

    public boolean isAccessTokenJwt() {
        return Boolean.parseBoolean(getConfig().get(IS_ACCESS_TOKEN_JWT));
    }

    public boolean isUseJwksUrl() {
        return Boolean.valueOf(getConfig().get(USE_JWKS_URL));
    }

    public void setUseJwksUrl(boolean useJwksUrl) {
        getConfig().put(USE_JWKS_URL, String.valueOf(useJwksUrl));
    }

    public String getJwksUrl() {
        return getConfig().get(JWKS_URL);
    }

    public void setJwksUrl(String jwksUrl) {
        getConfig().put(JWKS_URL, jwksUrl);
    }

    public boolean isBackchannelSupported() {
        return Boolean.valueOf(getConfig().get("backchannelSupported"));
    }

    public void setBackchannelSupported(boolean backchannel) {
        getConfig().put("backchannelSupported", String.valueOf(backchannel));
    }

    public boolean isDisableUserInfoService() {
        String disableUserInfo = getConfig().get("disableUserInfo");
        return Boolean.parseBoolean(disableUserInfo);
    }

    public void setDisableUserInfoService(boolean disable) {
        getConfig().put("disableUserInfo", String.valueOf(disable));
    }

    public boolean isDisableNonce() {
        return Boolean.parseBoolean(getConfig().get("disableNonce"));
    }

    public void setDisableNonce(boolean disableNonce) {
        if (disableNonce) {
            getConfig().put("disableNonce", Boolean.TRUE.toString());
        } else {
            getConfig().remove("disableNonce");
        }
    }

    public int getAllowedClockSkew() {
        String allowedClockSkew = getConfig().get(ALLOWED_CLOCK_SKEW);
        if (allowedClockSkew == null || allowedClockSkew.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(getConfig().get(ALLOWED_CLOCK_SKEW));
        } catch (NumberFormatException e) {
            // ignore it and use default
            return 0;
        }
    }

    public boolean isAutoUpdate() {
        return Boolean.valueOf(getConfig().get(AUTO_UPDATE));
    }

    public void setAutoUpdate(boolean autoUpdate) {
        getConfig().put(AUTO_UPDATE, String.valueOf(autoUpdate));
    }

    public String getMetadataUrl() {
        return getConfig().get(METADATA_URL);
    }

    public void setMetadataUrl(String metadataUrl) {
        getConfig().put(METADATA_URL, metadataUrl);
    }

    public Long getRefreshPeriod() {
        return getConfig().get(AUTO_UPDATE) != null ? Long.valueOf(getConfig().get(REFRESH_PERIOD)) : null;
    }

    public void setRefreshPeriod(long refreshPeriod) {
        getConfig().put(REFRESH_PERIOD, String.valueOf(refreshPeriod));
    }

    public Long getLastRefreshTime() {
        return getConfig().get(LAST_REFRESH_TIME) != null ? Long.valueOf(getConfig().get(LAST_REFRESH_TIME)) : null;
    }

    public void setLastRefreshTime(long lastRefreshTime) {
        getConfig().put(LAST_REFRESH_TIME, String.valueOf(lastRefreshTime));
    }

    public String getTokenIntrospectionUrl() {
        return getConfig().get(TOKEN_INTROSPECTION_URL);
    }

    public void setTokenIntrospectionUrl(String tokenIntrospectionUrl) {
        getConfig().put(TOKEN_INTROSPECTION_URL, tokenIntrospectionUrl);
    }

    public boolean isValidateRefreshToken() {
        return Boolean.valueOf(getConfig().get(VALIDATE_REFRESH_TOKEN));
    }

    public void setValidateRefreshToken(boolean validateRefreshToken) {
        getConfig().put(VALIDATE_REFRESH_TOKEN, String.valueOf(validateRefreshToken));
    }

    public void setPassSetMfa(boolean passSetMfa) {
        getConfig().put(IdentityProviderModel.PASS_SET_MFA, String.valueOf(passSetMfa));
    }

    public boolean isPassSetMfa() {
        return Boolean.valueOf(getConfig().get(IdentityProviderModel.PASS_SET_MFA));
    }

    public void setClaimsParameterSupported(boolean claimsParameterSupported) {
        getConfig().put(CLAIMS_PARAMETER_SUPPORTED, String.valueOf(claimsParameterSupported));
    }

    public boolean isClaimsParameterSupported() {
        return Boolean.valueOf(getConfig().get(CLAIMS_PARAMETER_SUPPORTED));
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);
        SslRequired sslRequired = realm.getSslRequired();
        checkUrl(sslRequired, getJwksUrl(), "jwks_url");
        checkUrl(sslRequired, getLogoutUrl(), "logout_url");
        checkUrl(sslRequired, getMetadataUrl(), METADATA_URL);
    }
}
