package org.keycloak.models.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TRUST_MARK")
public class TrustMarkEntity {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    private RealmEntity realm;

    @Column(name = "TRUST_MARK_TYPE")
    private String trustMarkType;

    @Column(name = "SELF_SIGNED")
    private boolean selfSigned;

    @Column(name = "IAT")
    private Long iat;

    @Column(name = "EXP")
    private Long exp;

    @Column(name = "LOGO_URI")
    private String logoUri;

    @Column(name = "REF")
    private String ref;

    @Column(name = "DELEGATION")
    private String delegation;

    @Column(name = "JWT")
    private String jwt;

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

    public boolean isSelfSigned() {
        return selfSigned;
    }

    public void setSelfSigned(boolean selfSigned) {
        this.selfSigned = selfSigned;
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
