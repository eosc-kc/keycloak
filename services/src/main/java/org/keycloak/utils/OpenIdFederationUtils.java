package org.keycloak.utils;

import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OpenIdFederationGeneralConfig;
import org.keycloak.representations.openid_federation.CommonMetadata;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OpenIdFederationUtils {

    private static final String WELL_KNOWN_SUBPATH = ".well-known/openid-federation";

    public static String getSelfSignedToken(String issuer, KeycloakSession session) throws IOException {
        issuer = issuer.trim();
        if (!issuer.endsWith("/")) issuer += "/";
        return SimpleHttp.doGet((issuer + WELL_KNOWN_SUBPATH), session).asString();
    }

    public static String getSubordinateToken(String fedApiUrl, String subject, KeycloakSession session) throws IOException {
        return SimpleHttp.doGet((fedApiUrl + "?sub=" + urlEncode(subject)),session).asString();
    }

    private static String urlEncode(String url) throws UnsupportedEncodingException {
        return URLEncoder.encode(url, StandardCharsets.UTF_8.toString());
    }

    public static CommonMetadata commonMetadata(OpenIdFederationGeneralConfig realmConfig){
        CommonMetadata common = new CommonMetadata();
        common.setOrganizationUri(realmConfig.getOrganizationUri());
        common.setOrganizationName(realmConfig.getOrganizationName());
        return common;
    }
}
