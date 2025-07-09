package org.keycloak.broker.oidc.federation;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.List;

import static org.keycloak.common.util.UriUtils.checkUrl;

public class OpenIdFederationIdentityProviderConfig extends OIDCIdentityProviderConfig {

    public static final String AUTHORITY_HINTS = "authorityHints";

    public static final String TRUST_ANCHOR_ID = "trustAnchorId";

    public static final String OP_ENTITY_IDENTIFIER = "opEntityIdntifier";

    public OpenIdFederationIdentityProviderConfig() {
        super();
    }

    public OpenIdFederationIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public List<String> getAuthorityHints() {
        try {
            return JsonSerialization.readValue(getConfig().get(AUTHORITY_HINTS), List.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setAuthorityHints(List<String> authorityHints) {
        try {
            getConfig().put(AUTHORITY_HINTS, JsonSerialization.writeValueAsString(authorityHints));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getTrustAnchorId() {
        return getConfig().get(TRUST_ANCHOR_ID);
    }

    public void setTrustAnchorId(String trustAnchorId) {
        getConfig().put(TRUST_ANCHOR_ID, trustAnchorId);
    }

    public Long getExpirationTime() {
        return Long.valueOf(getConfig().get(OIDCConfigAttributes.EXPIRATION_TIME));
    }

    public void setExpirationTime(Long expirationTime) {
        getConfig().put(OIDCConfigAttributes.EXPIRATION_TIME, expirationTime.toString());
    }

    public String getOpEntityIdentifier() {
        return getConfig().get(OP_ENTITY_IDENTIFIER);
    }

    public void setOpEntityIdentifier(String opEntityIdentifier) {
        getConfig().put(OP_ENTITY_IDENTIFIER, opEntityIdentifier);
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);
        if (getAuthorityHints() == null || getAuthorityHints().isEmpty()) {
            throw new IllegalArgumentException("Authority Hints are required");
        }
        if (getTrustAnchorId() == null ) {
            throw new IllegalArgumentException("Trust anchors ids is required");
        }
        if (getOpEntityIdentifier() == null ) {
            throw new IllegalArgumentException("OP Entity Identifier is required");
        }
        if (getExpirationTime() == null ) {
            throw new IllegalArgumentException("Expiration time is required");
        }
        getAuthorityHints().forEach(auth -> checkUrl(SslRequired.NONE, auth, "Authority hints"));
        checkUrl(SslRequired.NONE, getTrustAnchorId(), "Trust anchors id");
        checkUrl(SslRequired.NONE, getOpEntityIdentifier(), "OP entity Identifier");


    }

}
