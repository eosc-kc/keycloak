package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.keycloak.representations.oidc.OIDCClientRepresentation;

import java.util.List;

public class RPMetadata extends OIDCClientRepresentation {

    @JsonProperty("client_registration_types")
    private List<String> clientRegistrationTypes;

    @JsonUnwrapped
    private CommonMetadata commonMetadata;

    public List<String> getClientRegistrationTypes() {
        return clientRegistrationTypes;
    }

    public void setClientRegistrationTypes(List<String> clientRegistrationTypes) {
        this.clientRegistrationTypes = clientRegistrationTypes;
    }

    public CommonMetadata getCommonMetadata() {
        return commonMetadata;
    }

    public void setCommonMetadata(CommonMetadata commonMetadata) {
        this.commonMetadata = commonMetadata;
    }
}
