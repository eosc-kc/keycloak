package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public abstract class AbstractMetadataPolicy {
    @JsonUnwrapped
    private CommonMetadataPolicy commonMetadataPolicy;

    public CommonMetadataPolicy getCommonMetadataPolicy() {
        return commonMetadataPolicy;
    }

    public void setCommonMetadataPolicy(CommonMetadataPolicy commonMetadataPolicy) {
        this.commonMetadataPolicy = commonMetadataPolicy;
    }
}
