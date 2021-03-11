package org.keycloak.models.jpa.entities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.keycloak.models.jpa.converter.ListJsonConverter;

@Entity
@Table(name = "FEDERATION")
@NamedQueries({
    @NamedQuery(name="findFederationByAliasAndRealm", query="select f from FederationEntity f where f.alias = :alias and f.realm.id = :realmId")
})
public class FederationEntity {

	@Id
	@Column(name = "INTERNAL_ID", length = 36)
	@Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity. This avoids an extra SQL
	protected String internalId;

	@Column(name = "URL")
	private String url;
	
	@Column(name = "ALIAS")
	private String alias;
	
	@Column(name = "DISPLAY_NAME")
	private String displayName;

	@Column(name = "PROVIDER_ID")
	private String providerId;

	@Column(name = "UPDATE_FREQUENCY_IN_MINS")
	private Integer updateFrequencyInMins;

	@Column(name = "VALID_UNTIL_TIMESTAMP")
	private Long validUntilTimestamp;

	@Column(name = "LAST_METADATA_REFRESH_TIMESTAMP")
	private Long lastMetadataRefreshTimestamp;
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;
    
    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="FEDERATION_CONFIG", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
    private Map<String, String> config;
	
	@ElementCollection
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="ENTITYID_BLACKLIST", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
    private Set<String> entityIdBlackList = new HashSet<String>();
	
	@ElementCollection
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="ENTITYID_WHITELIST", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
    private Set<String> entityIdWhiteList = new HashSet<String>();
	
	@ElementCollection
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="REGISTRATION_AUTHORITY_BLACKLIST", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
    private Set<String> registrationAuthorityBlackList = new HashSet<String>();
    
    @ElementCollection
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="REGISTRATION_AUTHORITY_WHITELIST", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
    private Set<String> registrationAuthorityWhiteList = new HashSet<String>();
	
	@ManyToMany(mappedBy = "federations")
	private Set<IdentityProviderEntity> identityproviders = new HashSet<IdentityProviderEntity>();
	
	@ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="ENTITY_CATEGORY_BLACKLIST", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
	@Convert(converter = ListJsonConverter.class, attributeName = "value")
    private Map<String, List<String>> categoryBlackList;
	
	@ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="ENTITY_CATEGORY_WHITELIST", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
    @Convert(converter = ListJsonConverter.class, attributeName = "value")
    private Map<String, List<String>> categoryWhiteList;
	
	@OneToMany(cascade =CascadeType.REMOVE,  mappedBy = "federation")
	private List<FederationMapperEntity> federationMapperEntities = new ArrayList<FederationMapperEntity>();

	public String getInternalId() {
		return internalId;
	}

	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}	
	
	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	public RealmEntity getRealm() {
		return realm;
	}

	public void setRealm(RealmEntity realm) {
		this.realm = realm;
	}

	public Map<String, String> getConfig() {
		return config;
	}

	public void setConfig(Map<String, String> config) {
		this.config = config;
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

	public void setLastMetadataRefreshTimestamp(Long lastMetadataRefreshTimestamp ) {
		this.lastMetadataRefreshTimestamp = lastMetadataRefreshTimestamp ;
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

    public Set<IdentityProviderEntity> getIdentityproviders() {
		return identityproviders;
	}

	public void setIdentityproviders(Set<IdentityProviderEntity> identityproviders) {
		this.identityproviders = identityproviders;
	}
	
	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public Long getValidUntilTimestamp() {
		return validUntilTimestamp;
	}

	public void setValidUntilTimestamp(Long validUntilTimestamp) {
		this.validUntilTimestamp = validUntilTimestamp;
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

    public List<FederationMapperEntity> getFederationMapperEntities() {
        return federationMapperEntities;
    }

    public void setFederationMapperEntities(List<FederationMapperEntity> federationMapperEntities) {
        this.federationMapperEntities = federationMapperEntities;
    }

    @Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		if (!(o instanceof FederationEntity))
			return false;

		FederationEntity that = (FederationEntity) o;

		if (!internalId.equals(that.internalId))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return internalId.hashCode();
	}

}
