package org.keycloak.broker.oidc.federation;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.keycloak.common.util.UriUtils.checkUrl;

public class OpenIdFederationIdentityProviderConfig extends OIDCIdentityProviderConfig {

    public static final String AUTHORITY_HINTS = "authorityHints";

    public static final String TRUST_ANCHOR_ID = "trustAnchorId";

    public OpenIdFederationIdentityProviderConfig() {
        super();
    }

    public OpenIdFederationIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public List<String> getAuthorityHints() {
        return Arrays.asList(getConfig().get(AUTHORITY_HINTS).split("##"));
    }

    public void setAuthorityHints(List<String> authorityHints) {
        getConfig().put(AUTHORITY_HINTS, authorityHints.stream().collect(Collectors.joining("##")));

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

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);
        getAuthorityHints().forEach(auth -> checkUrl(SslRequired.NONE, auth, "Authority hints"));
        checkUrl(SslRequired.NONE, getTrustAnchorId(), "Trust anchors id");

    }

}
