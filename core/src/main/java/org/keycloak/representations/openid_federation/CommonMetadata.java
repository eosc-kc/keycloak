package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.keycloak.jose.jwk.JSONWebKeySet;

import java.util.List;

public class CommonMetadata {

    @JsonProperty("signed_jwks_uri")
    private String signedJwksUri;

    @JsonProperty("organization_name")
    private String organizationName;

    @JsonProperty("homepage_uri")
    private String homepageUri;

    public String getSignedJwksUri() {
        return signedJwksUri;
    }

    public void setSignedJwksUri(String signedJwksUri) {
        this.signedJwksUri = signedJwksUri;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getHomepageUri() {
        return homepageUri;
    }

    public void setHomepageUri(String homepageUri) {
        this.homepageUri = homepageUri;
    }
}
