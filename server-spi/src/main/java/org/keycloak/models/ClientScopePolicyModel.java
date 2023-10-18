package org.keycloak.models;

import java.io.Serializable;
import java.util.Collection;

public class ClientScopePolicyModel implements Serializable {

    private String id;
    private String userAttribute;
    private Collection<ClientScopePolicyValueModel> clientScopePolicyValues;

    public ClientScopePolicyModel() {
        super();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserAttribute() {
        return userAttribute;
    }

    public void setUserAttribute(String userAttribute) {
        this.userAttribute = userAttribute;
    }

    public Collection<ClientScopePolicyValueModel> getClientScopePolicyValues() {
        return clientScopePolicyValues;
    }

    public void setClientScopePolicyValues(Collection<ClientScopePolicyValueModel> clientScopePolicyValues) {
        this.clientScopePolicyValues = clientScopePolicyValues;
    }
}
