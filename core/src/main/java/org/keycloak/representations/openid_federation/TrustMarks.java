package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TrustMarks {

    private String trustMarkType;
    private String trustMark;

    public TrustMarks() {
    }

    public TrustMarks(String trustMarkType, String trustMark) {
        this.trustMarkType = trustMarkType;
        this.trustMark = trustMark;
    }

    @JsonProperty("trust_mark_type")
    public String getTrustMarkType() {
        return trustMarkType;
    }

    @JsonProperty("trust_mark_type")
    public void setTrustMarkType(String trustMarkType) {
        this.trustMarkType = trustMarkType;
    }

    @JsonProperty("trust_mark")
    public String getTrustMark() {
        return trustMark;
    }

    @JsonProperty("trust_mark")
    public void setTrustMark(String trustMark) {
        this.trustMark = trustMark;
    }
}
