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
public class FederationModel implements Serializable {

    private String internalId;

    private String alias;
 
    private String providerId;
    
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
    
    private List<FederationMapperModel> federationMapperModels = new ArrayList<>();
    
    private List<String> idps;
    

    public FederationModel() {
		super();
		this.entityIdDenyList = new HashSet<String>();
	}

    public FederationModel(FederationModel model) {
		super();
		this.setInternalId(model.getInternalId());
		this.setAlias(model.getAlias());
		this.setProviderId(model.getProviderId());
		this.setUrl(model.getUrl());
		this.setUpdateFrequencyInMins(model.getUpdateFrequencyInMins());
		this.setDisplayName(model.getDisplayName());
		this.setCategory(model.getCategory());
		this.setEntityIdDenyList(model.getEntityIdDenyList() != null ? model.getEntityIdDenyList() : new HashSet<String>());
		this.setEntityIdAllowList(model.getEntityIdAllowList() != null ? model.getEntityIdAllowList() : new HashSet<String>());
		this.setRegistrationAuthorityDenyList(model.getRegistrationAuthorityDenyList() != null ? model.getRegistrationAuthorityDenyList() : new HashSet<String>());
        this.setRegistrationAuthorityAllowList(model.getRegistrationAuthorityAllowList() != null ? model.getRegistrationAuthorityAllowList() : new HashSet<String>());
        this.setCategoryDenyList(model.getCategoryDenyList() != null ? model.getCategoryDenyList() : new HashMap<String,List<String>>());
        this.setCategoryAllowList(model.getCategoryAllowList() != null ? model.getCategoryAllowList() : new HashMap<String,List<String>>());
		this.setLastMetadataRefreshTimestamp(model.getLastMetadataRefreshTimestamp());
        this.setValidUntilTimestamp(model.getValidUntilTimestamp());
		this.setConfig(model.getConfig());
		this.setFederationMapperModels(model.getFederationMapperModels());
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

    public List<FederationMapperModel> getFederationMapperModels() {
        return federationMapperModels;
    }

    public void setFederationMapperModels(List<FederationMapperModel> federationMapperModels) {
        this.federationMapperModels = federationMapperModels;
    }

    public List<String> getIdps() {
        return idps;
    }

    public void setIdps(List<String> idps) {
        this.idps = idps;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
