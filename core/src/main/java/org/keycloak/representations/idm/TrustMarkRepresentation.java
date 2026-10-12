package org.keycloak.representations.idm;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;

public class TrustMarkRepresentation {
    private String id;
    private String trustMarkType;
    private String subject;
    private String issuer;
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern= "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime issuedAt;
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern= "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime expiration;
    private String logoUri;
    private String reference;
    private String delegation;
    private String trustMarkJwt;

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

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getExpiration() {
        return expiration;
    }

    public void setExpiration(LocalDateTime expiration) {
        this.expiration = expiration;
    }

    public String getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(String logoUri) {
        this.logoUri = logoUri;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDelegation() {
        return delegation;
    }

    public void setDelegation(String delegation) {
        this.delegation = delegation;
    }

    public String getTrustMarkJwt() {
        return trustMarkJwt;
    }

    public void setTrustMarkJwt(String trustMarkJwt) {
        this.trustMarkJwt = trustMarkJwt;
    }
}
