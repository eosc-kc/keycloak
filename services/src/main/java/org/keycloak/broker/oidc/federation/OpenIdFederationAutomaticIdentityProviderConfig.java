package org.keycloak.broker.oidc.federation;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Config for OpenID Federation Automatic Registration when Keycloak is the RP.
 * Forces asymmetric client authentication ({@code private_key_jwt}) and RP Entity ID as {@code client_id}.
 */
public class OpenIdFederationAutomaticIdentityProviderConfig extends OpenIdFederationIdentityProviderConfig {

    private static final Logger logger = Logger.getLogger(OpenIdFederationAutomaticIdentityProviderConfig.class);

    public OpenIdFederationAutomaticIdentityProviderConfig() {
        super();
    }

    public OpenIdFederationAutomaticIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

//        // Audience of private_key_jwt MUST be the OP Entity Identifier (§12.1.1.2).
//        if (StringUtil.isNotBlank(getIssuer())) {
//            setClientAssertionAudience(getIssuer());
//        }
 //   }

    //this is configuration during IdP creation
//    private void applyAutomaticDefaults() {
//        setClientAuthMethod(OIDCLoginProtocol.PRIVATE_KEY_JWT);
//        getConfig().remove(OAuth2IdentityProviderConfig.CLIENT_SECRET);
//    }

    /**
     * Optional pre-resolved trust chain (array of entity-statement JWTs) for the Request Object.
     * Discovery can populate this later; empty means the OP resolves the chain from the RP Entity ID.
     */
    @SuppressWarnings("unchecked")
    public List<String> getTrustChain() {
        String raw = getConfig().get(TokenUtil.TRUST_CHAIN);
        if (StringUtil.isBlank(raw)) {
            return Collections.emptyList();
        }
        try {
            return JsonSerialization.readValue(raw, List.class);
        } catch (Exception e) {
            logger.warn("Failed to parse trust_chain from IdP config", e);
            return Collections.emptyList();
        }
    }

    public void setTrustChain(List<String> trustChain) {
        if (trustChain == null || trustChain.isEmpty()) {
            getConfig().remove(TokenUtil.TRUST_CHAIN);
            return;
        }
        try {
            getConfig().put(TokenUtil.TRUST_CHAIN, JsonSerialization.writeValueAsString(trustChain));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize trust_chain", e);
        }
    }

    @Override
    public void validate(RealmModel realm) {
        if (StringUtil.isBlank(getClientAuthMethod()) || !OIDCLoginProtocol.PRIVATE_KEY_JWT.equals(getClientAuthMethod())) {
            throw new IllegalArgumentException("OpenID Federation automatic registration requires private_key_jwt client authentication");
        }
        super.validate(realm);
    }
}
