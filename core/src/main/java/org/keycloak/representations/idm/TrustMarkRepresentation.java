package org.keycloak.representations.idm;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrustMarkRepresentation {
    private String id;
    private String trustMarkType;
    private Boolean selfSigned;
    @JsonProperty("sub")
    private String subject;
    @JsonProperty("iss")
    private String issuer;
    private Long iat;
    private Long exp;
    private String logoUri;
    private String ref;
    private String delegation;
    private String jwt;

    public TrustMarkRepresentation () {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTrustMarkType() {
        return trustMarkType;
    }

    public void setTrustMarkType(String trustMarkType) {
        this.trustMarkType = trustMarkType;
    }

    public Boolean isSelfSigned() {
        return selfSigned;
    }

    public void setSelfSigned(Boolean selfSigned) {
        this.selfSigned = selfSigned;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Long getIat() {
        return iat;
    }

    public void setIat(Long iat) {
        this.iat = iat;
    }

    public Long getExp() {
        return exp;
    }

    public void setExp(Long exp) {
        this.exp = exp;
    }

    public String getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(String logoUri) {
        this.logoUri = logoUri;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getDelegation() {
        return delegation;
    }

    public void setDelegation(String delegation) {
        this.delegation = delegation;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}
