package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.jose.jwk.JSONWebKeySet;

public class CommonMetadataPolicy {

    @JsonProperty("signed_jwks_uri")
    private  Policy<String> signedJwksUri;

    @JsonProperty("organization_name")
    private  Policy<String> organizationName;

    @JsonProperty("organization_uri")
    private  Policy<String> organizationUri;

    public Policy<String> getSignedJwksUri() {
        return signedJwksUri;
    }

    public void setSignedJwksUri(Policy<String> signedJwksUri) {
        this.signedJwksUri = signedJwksUri;
    }

    public Policy<String> getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(Policy<String> organizationName) {
        this.organizationName = organizationName;
    }

    public Policy<String> getOrganizationUri() {
        return organizationUri;
    }

    public void setOrganizationUri(Policy<String> organizationUri) {
        this.organizationUri = organizationUri;
    }
}
