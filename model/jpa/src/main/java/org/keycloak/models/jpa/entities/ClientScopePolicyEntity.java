package org.keycloak.models.jpa.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="CLIENT_SCOPE_POLICY", uniqueConstraints = {@UniqueConstraint(columnNames = {"CLIENT_SCOPE_ID", "USER_ATTRIBUTE"})})
public class ClientScopePolicyEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_SCOPE_ID")
    private ClientScopeEntity clientScope;

    @Column(name = "USER_ATTRIBUTE")
    private String userAttribute;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "clientScopePolicy")
    protected Collection<ClientScopePolicyValueEntity> clientScopePolicyValues;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ClientScopeEntity getClientScope() {
        return clientScope;
    }

    public void setClientScope(ClientScopeEntity clientScope) {
        this.clientScope = clientScope;
    }

    public String getUserAttribute() {
        return userAttribute;
    }

    public void setUserAttribute(String userAttribute) {
        this.userAttribute = userAttribute;
    }

    public Collection<ClientScopePolicyValueEntity> getClientScopePolicyValues() {
        if (clientScopePolicyValues == null) {
            clientScopePolicyValues = new ArrayList<>();
        }
        return clientScopePolicyValues;
    }

    public void setClientScopePolicyValues(Collection<ClientScopePolicyValueEntity> clientScopePolicyValues) {
        this.clientScopePolicyValues = clientScopePolicyValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof ClientScopePolicyEntity)) return false;

        ClientScopePolicyEntity that = (ClientScopePolicyEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
