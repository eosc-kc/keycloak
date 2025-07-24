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

package org.keycloak.models.jpa.entities;

import org.hibernate.annotations.BatchSize;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.util.HashSet;
import jakarta.persistence.Index;
import java.util.Map;
import java.util.Set;

/**
 * @author Pedro Igor
 */
@Entity
@Table(
		name="IDENTITY_PROVIDER",
		indexes = {
				@Index(name = "IDP_PROVIDER_ALIAS_INDX", columnList = "PROVIDER_ALIAS")
		}
)
@NamedQueries({
		@NamedQuery(name="countIdentityProvidersOfRealm", query="select count(identityProvider.internalId) from IdentityProviderEntity identityProvider where identityProvider.realm.id = :realmId"),
        @NamedQuery(name="findIdentityProvidersByAlias", query="select identityProvider from IdentityProviderEntity identityProvider where identityProvider.alias = :alias"),
        @NamedQuery(name="findIdentityProviderByRealm", query="select identityProvider from IdentityProviderEntity identityProvider where identityProvider.realm.id = :realmId"),
        @NamedQuery(name="findIdentityProviderByRealmAndKeyword", query="select identityProvider from IdentityProviderEntity identityProvider where identityProvider.realm.id = :realmId and (lower(identityProvider.alias) like :keyword or lower(identityProvider.displayName) like :keyword )"),
        @NamedQuery(name="findIdentityProviderByRealmAndAlias", query="select identityProvider from IdentityProviderEntity identityProvider where identityProvider.alias = :alias and identityProvider.realm.id = :realmId"),
        @NamedQuery(name="findUtilizedIdentityProviderTypesOfRealm", query="select distinct identityProvider.providerId from IdentityProviderEntity identityProvider where identityProvider.realm.id = :realmId"),
        @NamedQuery(name="findIdPAliasByFederation", query="select identityProvider.alias from IdentityProviderEntity identityProvider join identityProvider.federations f where f.internalId = :federationId"),
        @NamedQuery(name="findIdPsByFederation", query="select identityProvider from IdentityProviderEntity identityProvider join identityProvider.federations f where f.internalId = :federationId"),
        @NamedQuery(name="findAutoUpdatedIdentityProviders", query="select i from IdentityProviderEntity i inner join i.config c where i.realm.id = :realmId and KEY(c) = 'autoUpdate' AND VALUE(c) = 'true'"),
        @NamedQuery(name="findEnabledIdentityProvidersByProviderId", query="select i from IdentityProviderEntity i where i.realm.id = :realmId and i.providerId = :providerId and i.enabled = true")
})
public class IdentityProviderEntity {

    @Id
    @Column(name="INTERNAL_ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String internalId;

    @BatchSize(size = 50)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @Column(name="PROVIDER_ID")
    private String providerId;

    @Column(name="PROVIDER_ALIAS")
    private String alias;

    @Column(name="PROVIDER_DISPLAY_NAME")
    private String displayName;

    @Column(name="ENABLED")
    private boolean enabled;

    @Column(name = "TRUST_EMAIL")
    private boolean trustEmail;

    @Column(name="STORE_TOKEN")
    private boolean storeToken;

    @Column(name="LINK_ONLY")
    private boolean linkOnly;

    @Column(name="ADD_TOKEN_ROLE")
    protected boolean addReadTokenRoleOnCreate;

    @Column(name="AUTHENTICATE_BY_DEFAULT")
    private boolean authenticateByDefault;

    @Column(name="FIRST_BROKER_LOGIN_FLOW_ID")
    private String firstBrokerLoginFlowId;

    @Column(name="POST_BROKER_LOGIN_FLOW_ID")
    private String postBrokerLoginFlowId;

    @BatchSize(size = 50)
    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="IDENTITY_PROVIDER_CONFIG", joinColumns={ @JoinColumn(name="IDENTITY_PROVIDER_ID") })
    private Map<String, String> config;
    
    @ManyToMany
    @BatchSize(size = 50)
    @JoinTable(
    	name = "federation_idps", 
		joinColumns = @JoinColumn(name = "identityprovider_id"), 
		inverseJoinColumns = @JoinColumn(name = "federation_id"))
    Set<FederationEntity> federations = new HashSet<FederationEntity>();

    public String getInternalId() {
        return this.internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getProviderId() {
        return this.providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public RealmEntity getRealm() {
        return this.realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isStoreToken() {
        return this.storeToken;
    }

    public void setStoreToken(boolean storeToken) {
        this.storeToken = storeToken;
    }

    public boolean isAuthenticateByDefault() {
        return authenticateByDefault;
    }

    public void setAuthenticateByDefault(boolean authenticateByDefault) {
        this.authenticateByDefault = authenticateByDefault;
    }

    public boolean isLinkOnly() {
        return linkOnly;
    }

    public void setLinkOnly(boolean linkOnly) {
        this.linkOnly = linkOnly;
    }

    public String getFirstBrokerLoginFlowId() {
        return firstBrokerLoginFlowId;
    }

    public void setFirstBrokerLoginFlowId(String firstBrokerLoginFlowId) {
        this.firstBrokerLoginFlowId = firstBrokerLoginFlowId;
    }

    public String getPostBrokerLoginFlowId() {
        return postBrokerLoginFlowId;
    }

    public void setPostBrokerLoginFlowId(String postBrokerLoginFlowId) {
        this.postBrokerLoginFlowId = postBrokerLoginFlowId;
    }

    public Map<String, String> getConfig() {
        return this.config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public boolean isAddReadTokenRoleOnCreate() {
        return addReadTokenRoleOnCreate;
    }

    public void setAddReadTokenRoleOnCreate(boolean addReadTokenRoleOnCreate) {
        this.addReadTokenRoleOnCreate = addReadTokenRoleOnCreate;
    }

    public boolean isTrustEmail() {
        return trustEmail;
    }

    public void setTrustEmail(boolean trustEmail) {
        this.trustEmail = trustEmail;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

	public Set<FederationEntity> getFederations() {
		return federations;
	}

	public void setFederations(Set<FederationEntity> federations) {
		this.federations = federations;
	}

	public void addToFederation(FederationEntity federation) {
		this.federations.add(federation);
	}
	
	public void removeFromFederation(FederationEntity federation) {
		this.federations.remove(federation);
	}
	
	
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof IdentityProviderEntity)) return false;

        IdentityProviderEntity that = (IdentityProviderEntity) o;

        if (!internalId.equals(that.internalId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return internalId.hashCode();
    }

}
