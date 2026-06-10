package org.keycloak.models.jpa.entities;


import java.util.Map;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name="FEDERATION_MAPPER")
@NamedQueries({
        @NamedQuery(name = "findByFederation", query = "select f from FederationMapperEntity f where f.federation.internalId = :federationId"),
        @NamedQuery(name = "countByFederationAndName", query = "select count(f) from FederationMapperEntity f where f.federation.internalId = :federationId and f.name = :name")
})
public class FederationMapperEntity {
    
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @Column(name="NAME")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FEDERATION_ID")
    private FederationEntity federation;
    @Column(name = "IDP_MAPPER_NAME")
    private String identityProviderMapper;

    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE")
    @CollectionTable(name="FEDERATION_MAPPER_CONFIG", joinColumns={ @JoinColumn(name="FEDERATION_MAPPER_ID") })
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

    public FederationEntity getFederation() {
        return federation;
    }

    public void setFederation(FederationEntity federation) {
        this.federation = federation;
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof FederationMapperEntity)) return false;

        FederationMapperEntity that = (FederationMapperEntity) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
