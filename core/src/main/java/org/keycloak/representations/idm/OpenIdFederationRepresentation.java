package org.keycloak.representations.idm;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.jose.jwk.JSONWebKeySet;

public class OpenIdFederationRepresentation {

    private String internalId;
    private String trustAnchor;
    private JSONWebKeySet jwks;
    private Map<String, String> idpConfiguration  = new HashMap<>();

    public OpenIdFederationRepresentation(){}

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getTrustAnchor() {
        return trustAnchor;
    }

    public void setTrustAnchor(String trustAnchor) {
        this.trustAnchor = trustAnchor;
    }

    public JSONWebKeySet getJwks() {
        return jwks;
    }

    public void setJwks(JSONWebKeySet jwks) {
        this.jwks = jwks;
    }

    public Map<String, String> getIdpConfiguration() {
        return idpConfiguration;
    }

    public void setIdpConfiguration(Map<String, String> idpConfiguration) {
        this.idpConfiguration = idpConfiguration;
    }
}
