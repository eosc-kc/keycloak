/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * <p>A model type for the identity provider federations.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentityProvidersFederationModel implements Serializable {

    private String internalId;

    private String alias;
 
    private String providerId;
    
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
    
    private List<FederationMapperModel> federationMapperModels = new ArrayList<>();
    

    public IdentityProvidersFederationModel() {
		super();
		this.entityIdBlackList = new HashSet<String>();
		this.identityprovidersAlias = new HashSet<String>();
	}

    public IdentityProvidersFederationModel(IdentityProvidersFederationModel model) {
		super();
		this.setInternalId(model.getInternalId());
		this.setAlias(model.getAlias());
		this.setProviderId(model.getProviderId());
		this.setUrl(model.getUrl());
		this.setUpdateFrequencyInMins(model.getUpdateFrequencyInMins());
		this.setDisplayName(model.getDisplayName());
		this.setEntityIdBlackList(model.getEntityIdBlackList() != null ? model.getEntityIdBlackList() : new HashSet<String>());
		this.setEntityIdWhiteList(model.getEntityIdWhiteList() != null ? model.getEntityIdWhiteList() : new HashSet<String>());
		this.setRegistrationAuthorityBlackList(model.getRegistrationAuthorityBlackList() != null ? model.getRegistrationAuthorityBlackList() : new HashSet<String>());
        this.setRegistrationAuthorityWhiteList(model.getRegistrationAuthorityWhiteList() != null ? model.getRegistrationAuthorityWhiteList() : new HashSet<String>());
        this.setCategoryBlackList(model.getRegistrationAuthorityBlackList() != null ? model.getCategoryBlackList() : new HashMap<String,List<String>>());
        this.setCategoryWhiteList(model.getRegistrationAuthorityWhiteList() != null ? model.getCategoryWhiteList() : new HashMap<String,List<String>>());
		this.setLastMetadataRefreshTimestamp(model.getLastMetadataRefreshTimestamp());
		this.setIdentityprovidersAlias(model.getIdentityprovidersAlias());
		this.setConfig(model.getConfig());
		this.setFederationMapperModels(model.getFederationMapperModels());
	}

	public IdentityProvidersFederationModel(String internalId, String alias, String providerId, String url,
			Integer refreshEveryMinutes, String displayName, Long lastMetadataRefreshTimestamp,Set<String> blackList,Set<String> whiteList,Set<String> identityprovidersAlias) {
		super();
		this.internalId = internalId;
		this.alias = alias;
		this.providerId = providerId;
		this.url = url;
		this.updateFrequencyInMins = refreshEveryMinutes;
		this.displayName = displayName;
		this.entityIdBlackList = (blackList != null) ? blackList : new HashSet<String>();
		this.entityIdWhiteList = (whiteList != null) ? whiteList : new HashSet<String>();
		this.lastMetadataRefreshTimestamp = lastMetadataRefreshTimestamp;
		this.identityprovidersAlias = identityprovidersAlias;
	}



	public String getInternalId() {
        return this.internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String id) {
        this.alias = id;
    }

    public String getProviderId() {
        return this.providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public List<FederationMapperModel> getFederationMapperModels() {
        return federationMapperModels;
    }

    public void setFederationMapperModels(List<FederationMapperModel> federationMapperModels) {
        this.federationMapperModels = federationMapperModels;
    }

}
