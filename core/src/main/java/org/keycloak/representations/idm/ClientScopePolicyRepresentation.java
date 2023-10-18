package org.keycloak.representations.idm;

import java.util.Collection;

public class ClientScopePolicyRepresentation {

    private String id;
    private String userAttribute;
    private Collection<ClientScopePolicyValueRepresentation> clientScopePolicyValues;

    public ClientScopePolicyRepresentation(){};

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

    public Collection<ClientScopePolicyValueRepresentation> getClientScopePolicyValues() {
        return clientScopePolicyValues;
    }

    public void setClientScopePolicyValues(Collection<ClientScopePolicyValueRepresentation> clientScopePolicyValues) {
        this.clientScopePolicyValues = clientScopePolicyValues;
    }
}
