package org.keycloak.models.jpa.entities;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="CLIENT_SCOPE_POLICY_VALUE")
public class ClientScopePolicyValueEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_SCOPE_POLICY_ID")
    private ClientScopePolicyEntity clientScopePolicy;

    @Column(name = "VALUE")
    private String value;

    @Column(name = "NEGATE_OUTPUT")
    private Boolean negateOutput;

    @Column(name = "REGEX")
    private Boolean regex;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ClientScopePolicyEntity getClientScopePolicy() {
        return clientScopePolicy;
    }

    public void setClientScopePolicy(ClientScopePolicyEntity clientScopePolicy) {
        this.clientScopePolicy = clientScopePolicy;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Boolean getNegateOutput() {
        return negateOutput;
    }

    public void setNegateOutput(Boolean negateOutput) {
        this.negateOutput = negateOutput;
    }

    public Boolean getRegex() {
        return regex;
    }

    public void setRegex(Boolean regex) {
        this.regex = regex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof ClientScopePolicyValueEntity)) return false;

        ClientScopePolicyValueEntity that = (ClientScopePolicyValueEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
