package org.keycloak.models;

import java.io.Serializable;
import java.util.List;

import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.models.enums.EntityTypeEnum;

public class OpenIdFederationGeneralConfig implements Serializable {

    private String organizationName;
    private List<String> contacts;
    private String logoUri;
    private String policyUri;
    private String organizationUri;
    private List<String> authorityHints;
    private Integer lifespan;
    // default 1 day - duration
    private String federationResolveEndpoint;
    private String federationHistoricalKeysEndpoint;
    private Integer connectionTimeout;
    private Integer socketTimeout;
    private Integer connectionRequestTimeout;
    private Integer maxAuthorityHints;
    private Integer maxHops;
    private Integer totalTimeout;
    private List<OpenIdFederationConfig> openIdFederationList = List.of();
    private List<TrustMarkModel> trustMarkList = List.of();
    private List<EntityTypeEnum> entityTypes;
    private List<ClientRegistrationTypeEnum> opClientRegistrationTypesSupported;
    private List<ClientRegistrationTypeEnum> rpClientRegistrationTypesSupported;

    public OpenIdFederationGeneralConfig(){}

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

    public String getOrganizationUri() {
        return organizationUri;
    }

    public void setOrganizationUri(String organizationUri) {
        this.organizationUri = organizationUri;
    }

    public List<String> getAuthorityHints() {
        return authorityHints;
    }

    public void setAuthorityHints(List<String> authorityHints) {
        this.authorityHints = authorityHints;
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

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public Integer getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(Integer connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public Integer getMaxAuthorityHints() {
        return maxAuthorityHints;
    }

    public void setMaxAuthorityHints(Integer maxAuthorityHints) {
        this.maxAuthorityHints = maxAuthorityHints;
    }

    public Integer getMaxHops() {
        return maxHops;
    }

    public void setMaxHops(Integer maxHops) {
        this.maxHops = maxHops;
    }

    public Integer getTotalTimeout() {
        return totalTimeout;
    }

    public void setTotalTimeout(Integer totalTimeout) {
        this.totalTimeout = totalTimeout;
    }

    public List<OpenIdFederationConfig> getOpenIdFederationList() {
        return openIdFederationList;
    }

    public void setOpenIdFederationList(List<OpenIdFederationConfig> openIdFederationList) {
        this.openIdFederationList = openIdFederationList == null ? List.of() : List.copyOf(openIdFederationList);
    }

    public List<TrustMarkModel> getTrustMarkList() {
        return trustMarkList;
    }

    public void setTrustMarkList(List<TrustMarkModel> trustMarkList) {
        this.trustMarkList = trustMarkList == null ? List.of() : List.copyOf(trustMarkList);
    }

    public List<EntityTypeEnum> getEntityTypes() {
        return entityTypes;
    }

    public void setEntityTypes(List<EntityTypeEnum> entityTypes) {
        this.entityTypes = entityTypes;
    }

    public List<ClientRegistrationTypeEnum> getOpClientRegistrationTypesSupported() {
        return opClientRegistrationTypesSupported;
    }

    public void setOpClientRegistrationTypesSupported(List<ClientRegistrationTypeEnum> opClientRegistrationTypesSupported) {
        this.opClientRegistrationTypesSupported = opClientRegistrationTypesSupported;
    }

    public List<ClientRegistrationTypeEnum> getRpClientRegistrationTypesSupported() {
        return rpClientRegistrationTypesSupported;
    }

    public void setRpClientRegistrationTypesSupported(List<ClientRegistrationTypeEnum> rpClientRegistrationTypesSupported) {
        this.rpClientRegistrationTypesSupported = rpClientRegistrationTypesSupported;
    }
}
