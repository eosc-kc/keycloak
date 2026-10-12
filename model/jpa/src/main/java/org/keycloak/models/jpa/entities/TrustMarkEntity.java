package org.keycloak.models.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "TRUST_MARK")
public class TrustMarkEntity {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    private RealmEntity realm;

    @Column(name = "TRUST_MARK_TYPE", nullable = false)
    private String trustMarkType;

    @Column(name = "SUBJECT", nullable = false)
    private String subject;

    @Column(name = "ISSUER", nullable = false)
    private String issuer;

    @Column(name = "ISSUED_AT", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "EXPIRATION")
    private LocalDateTime expiration;

    @Column(name = "LOGO_URI")
    private String logoUri;

    @Column(name = "REFERENCE")
    private String reference;

    @Column(name = "DELEGATION")
    private String delegation;

    @Column(name = "TRUST_MARK_JWT", nullable = false)
    private String trustMarkJwt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
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
