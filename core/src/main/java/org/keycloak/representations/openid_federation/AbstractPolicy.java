package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.keycloak.exceptions.MetadataPolicyCombinationException;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractPolicy <T> {

    protected Boolean essential;

    protected Map<String, T> otherClaims = new HashMap<>();

    protected AbstractPolicy() {

    }

    protected AbstractPolicy<T> combinePolicyCommon(AbstractPolicy<T> inferior) throws MetadataPolicyCombinationException {

        //combine essential
        if (this.essential != null && inferior.getEssential() != null) {
            this.essential = this.essential || inferior.getEssential();
        } else  if (inferior.getEssential() != null) {
            this.essential = inferior.getEssential();
        }
        return this;
    }

    public Boolean getEssential() {
        return essential;
    }

    public void setEssential(Boolean essential) {
        this.essential = essential;
    }

    @JsonAnyGetter
    public Map<String, T> getOtherClaims() {
        return otherClaims;
    }

    @JsonAnySetter
    public void setOtherClaims(Map<String, T> otherClaims) {
        this.otherClaims = otherClaims;
    }

}

