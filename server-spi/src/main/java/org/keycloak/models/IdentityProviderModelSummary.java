package org.keycloak.models;

import java.io.Serializable;

public class IdentityProviderModelSummary implements Serializable {

    private String internalId;
    private String alias;
    private String providerId;

    public IdentityProviderModelSummary(String internalId, String alias, String providerId) {
        this.internalId = internalId;
        this.alias = alias;
        this.providerId = providerId;
    }

    public IdentityProviderModelSummary(IdentityProviderModel model) {
        this.internalId = model.getInternalId();
        this.alias = model.getAlias();
        this.providerId = model.getProviderId();
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof IdentityProviderModelSummary) {
            IdentityProviderModelSummary s = (IdentityProviderModelSummary)obj;
            return internalId.equals(s.internalId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (internalId == null ? 0 : internalId.hashCode());
        return hash;
    }


}
