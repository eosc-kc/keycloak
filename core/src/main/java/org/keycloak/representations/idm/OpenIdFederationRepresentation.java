package org.keycloak.representations.idm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenIdFederationRepresentation {

    private String internalId;
    private String trustAnchor;
    private List<String> clientRegistrationTypesSupported;

    private List<String> entityTypes;

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

    public List<String> getClientRegistrationTypesSupported() {
        return clientRegistrationTypesSupported;
    }

    public void setClientRegistrationTypesSupported(List<String> clientRegistrationTypesSupported) {
        this.clientRegistrationTypesSupported = clientRegistrationTypesSupported;
    }

    public List<String> getEntityTypes() {
        return entityTypes;
    }

    public void setEntityTypes(List<String> entityTypes) {
        this.entityTypes = entityTypes;
    }

    public Map<String, String> getIdpConfiguration() {
        return idpConfiguration;
    }

    public void setIdpConfiguration(Map<String, String> idpConfiguration) {
        this.idpConfiguration = idpConfiguration;
    }
}
