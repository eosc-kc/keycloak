
package org.keycloak.models;

import java.io.Serializable;

public class IdentityProviderMapperModelSummary implements Serializable {

    protected String id;
    protected String identityProviderAlias;
    protected String name;


    public IdentityProviderMapperModelSummary(String id, String name, String identityProviderAlias) {
        this.id = id;
        this.name = name;
        this.identityProviderAlias = identityProviderAlias;
    }

    public IdentityProviderMapperModelSummary(IdentityProviderMapperModel model) {
        this.id = model.getId();
        this.identityProviderAlias = model.getIdentityProviderAlias();
        this.name = model.getName();
    }


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

    public String getIdentityProviderAlias() {
        return identityProviderAlias;
    }

    public void setIdentityProviderAlias(String identityProviderAlias) {
        this.identityProviderAlias = identityProviderAlias;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof IdentityProviderMapperModelSummary) {
            IdentityProviderMapperModelSummary s = (IdentityProviderMapperModelSummary)obj;
            return id.equals(s.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (id == null ? 0 : id.hashCode());
        return hash;
    }

}
