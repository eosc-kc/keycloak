package org.keycloak.models;

import org.jboss.logging.Logger;
import org.keycloak.models.enums.ClientRegistrationTypeEnum;

import java.io.Serializable;
import java.util.List;

public class OpenIdFederationConfig  implements Serializable {

    protected static final Logger logger = Logger.getLogger(OpenIdFederationConfig.class);

    private String organizationName;
    private List<String> contacts;
    private String logoUri;
    private String policyUri;
    private String homepageUri;
    private List<String> authorityHints;
    private List<String> trustAnchors;

    private List<ClientRegistrationTypeEnum> clientRegistrationTypesSupported;

    private Integer lifespan;
    // default 1 day - duration
    //exp = now + duration

    private String federationResolveEndpoint;
    private String federationHistoricalKeysEndpoint;

    public OpenIdFederationConfig (){}

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public String getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(String logoUri) {
        this.logoUri = logoUri;
    }

    public String getPolicyUri() {
        return policyUri;
    }

    public void setPolicyUri(String policyUri) {
        this.policyUri = policyUri;
    }

    public String getHomepageUri() {
        return homepageUri;
    }

    public void setHomepageUri(String homepageUri) {
        this.homepageUri = homepageUri;
    }

    public List<ClientRegistrationTypeEnum> getClientRegistrationTypesSupported() {
        return clientRegistrationTypesSupported;
    }

    public void setClientRegistrationTypesSupported(List<ClientRegistrationTypeEnum> clientRegistrationTypesSupported) {
        this.clientRegistrationTypesSupported = clientRegistrationTypesSupported;
    }

    public List<String> getAuthorityHints() {
        return authorityHints;
    }

    public void setAuthorityHints(List<String> authorityHints) {
        this.authorityHints = authorityHints;
    }

    public List<String> getTrustAnchors() {
        return trustAnchors;
    }

    public void setTrustAnchors(List<String> trustAnchors) {
        this.trustAnchors = trustAnchors;
    }

    public Integer getLifespan() {
        return lifespan;
    }

    public void setLifespan(Integer lifespan) {
        this.lifespan = lifespan;
    }

    public String getFederationResolveEndpoint() {
        return federationResolveEndpoint;
    }

    public void setFederationResolveEndpoint(String federationResolveEndpoint) {
        this.federationResolveEndpoint = federationResolveEndpoint;
    }

    public String getFederationHistoricalKeysEndpoint() {
        return federationHistoricalKeysEndpoint;
    }

    public void setFederationHistoricalKeysEndpoint(String federationHistoricalKeysEndpoint) {
        this.federationHistoricalKeysEndpoint = federationHistoricalKeysEndpoint;
    }
}
