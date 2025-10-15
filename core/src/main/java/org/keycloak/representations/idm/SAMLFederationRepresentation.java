package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SAMLFederationRepresentation {
	

    private String internalId;
 
    private String providerId;

    private String alias;
    
    private String url;
    
    private Integer updateFrequencyInMins;
    
    private String displayName;

	private String category;
    
    private Long validUntilTimestamp;
    
    private Long lastMetadataRefreshTimestamp;

    private Set<String> entityIdDenyList;
    
    private Set<String> entityIdAllowList;
    
    private Set<String> registrationAuthorityDenyList;
    
    private Set<String> registrationAuthorityAllowList;
    
    private Map<String, String> config;
    
    private Map<String, List<String>> categoryDenyList;
    
    private Map<String, List<String>> categoryAllowList;
    
    private List<FederationMapperRepresentation> federationMappers = new ArrayList<>();
    
    public SAMLFederationRepresentation() {
    	this.entityIdDenyList = new HashSet<String>();
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

	public Set<String> getEntityIdDenyList() {
		return entityIdDenyList;
	}

	public void setEntityIdDenyList(Set<String> entityIdDenyList) {
		this.entityIdDenyList = entityIdDenyList;
	}	
	
	public Set<String> getEntityIdAllowList() {
        return entityIdAllowList;
    }

    public void setEntityIdAllowList(Set<String> entityIdAllowList) {
        this.entityIdAllowList = entityIdAllowList;
    }

    public Set<String> getRegistrationAuthorityDenyList() {
        return registrationAuthorityDenyList;
    }

    public void setRegistrationAuthorityDenyList(Set<String> registrationAuthorityDenyList) {
        this.registrationAuthorityDenyList = registrationAuthorityDenyList;
    }

    public Set<String> getRegistrationAuthorityAllowList() {
        return registrationAuthorityAllowList;
    }

    public void setRegistrationAuthorityAllowList(Set<String> registrationAuthorityAllowList) {
        this.registrationAuthorityAllowList = registrationAuthorityAllowList;
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

    public Map<String, List<String>> getCategoryDenyList() {
        return categoryDenyList;
    }

    public void setCategoryDenyList(Map<String, List<String>> categoryDenyList) {
        this.categoryDenyList = categoryDenyList;
    }

    public Map<String, List<String>> getCategoryAllowList() {
        return categoryAllowList;
    }

    public void setCategoryAllowList(Map<String, List<String>> categoryAllowList) {
        this.categoryAllowList = categoryAllowList;
    }

    public List<FederationMapperRepresentation> getFederationMappers() {
        return federationMappers;
    }

    public void setFederationMappers(List<FederationMapperRepresentation> federationMappers) {
        this.federationMappers = federationMappers;
    }

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
}
