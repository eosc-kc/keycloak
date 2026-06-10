package org.keycloak.models;

import java.io.Serializable;
import java.util.Map;


public class FederationMapperModel implements Serializable {
    
    private String id;
    private String name;
    private String federationId;
    private String identityProviderMapper;
    private Map<String, String> config;
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getFederationId() {
        return federationId;
    }
    public void setFederationId(String federationId) {
        this.federationId = federationId;
    }
    public String getIdentityProviderMapper() {
        return identityProviderMapper;
    }
    public void setIdentityProviderMapper(String identityProviderMapper) {
        this.identityProviderMapper = identityProviderMapper;
    }
    public Map<String, String> getConfig() {
        return config;
    }
    public void setConfig(Map<String, String> config) {
        this.config = config;
    }
    
    

}
