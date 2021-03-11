package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IdentityProvidersFederationRepresentation {
	

    private String internalId;
 
    private String providerId;

    private String alias;
    
    private String url;
    
    private Integer updateFrequencyInMins;
    
    private String displayName;
    
    private Long validUntilTimestamp;
    
    private Long lastMetadataRefreshTimestamp;

    private Set<String> entityIdBlackList;
    
    private Set<String> entityIdWhiteList;
    
    private Set<String> registrationAuthorityBlackList;
    
    private Set<String> registrationAuthorityWhiteList;
    
    private Set<String> identityprovidersAlias;
    
    private Map<String, String> config;
    
    private Map<String, List<String>> categoryBlackList;
    
    private Map<String, List<String>> categoryWhiteList;
    
    private List<FederationMapperRepresentation> federationMappers = new ArrayList<>();
    
    public IdentityProvidersFederationRepresentation() {
    	this.entityIdBlackList = new HashSet<String>();
    	this.identityprovidersAlias = new HashSet<String>();
    }

	public String getInternalId() {
		return internalId;
	}

	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public Integer getUpdateFrequencyInMins() {
		return updateFrequencyInMins;
	}

	public void setUpdateFrequencyInMins(Integer updateFrequencyInMins) {
		this.updateFrequencyInMins = updateFrequencyInMins;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Long getLastMetadataRefreshTimestamp() {
		return lastMetadataRefreshTimestamp;
	}

	public void setLastMetadataRefreshTimestamp(Long lastMetadataRefreshTimestamp) {
		this.lastMetadataRefreshTimestamp = lastMetadataRefreshTimestamp;
	}

	public Set<String> getEntityIdBlackList() {
		return entityIdBlackList;
	}

	public void setEntityIdBlackList(Set<String> entityIdBlackList) {
		this.entityIdBlackList = entityIdBlackList;
	}	
	
	public Set<String> getEntityIdWhiteList() {
        return entityIdWhiteList;
    }

    public void setEntityIdWhiteList(Set<String> entityIdWhiteList) {
        this.entityIdWhiteList = entityIdWhiteList;
    }

    public Set<String> getRegistrationAuthorityBlackList() {
        return registrationAuthorityBlackList;
    }

    public void setRegistrationAuthorityBlackList(Set<String> registrationAuthorityBlackList) {
        this.registrationAuthorityBlackList = registrationAuthorityBlackList;
    }

    public Set<String> getRegistrationAuthorityWhiteList() {
        return registrationAuthorityWhiteList;
    }

    public void setRegistrationAuthorityWhiteList(Set<String> registrationAuthorityWhiteList) {
        this.registrationAuthorityWhiteList = registrationAuthorityWhiteList;
    }

    public Set<String> getIdentityprovidersAlias() {
		return identityprovidersAlias;
	}

	public void setIdentityprovidersAlias(Set<String> identityprovidersAlias) {
		this.identityprovidersAlias = identityprovidersAlias;
	}
	
	public Long getValidUntilTimestamp() {
		return validUntilTimestamp;
	}

	public void setValidUntilTimestamp(Long validUntilTimestamp) {
		this.validUntilTimestamp = validUntilTimestamp;
	}

	public Map<String, String> getConfig() {
		return config;
	}

	public void setConfig(Map<String, String> config) {
		this.config = config;
	}

    public Map<String, List<String>> getCategoryBlackList() {
        return categoryBlackList;
    }

    public void setCategoryBlackList(Map<String, List<String>> categoryBlackList) {
        this.categoryBlackList = categoryBlackList;
    }

    public Map<String, List<String>> getCategoryWhiteList() {
        return categoryWhiteList;
    }

    public void setCategoryWhiteList(Map<String, List<String>> categoryWhiteList) {
        this.categoryWhiteList = categoryWhiteList;
    }

    public List<FederationMapperRepresentation> getFederationMappers() {
        return federationMappers;
    }

    public void setFederationMappers(List<FederationMapperRepresentation> federationMappers) {
        this.federationMappers = federationMappers;
    }

}
