package org.keycloak.protocol.trustchain;

import java.util.Set;

import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.TrustChainResolution;

public interface TrustChainProcessor extends Provider {

    TrustChainResolution constructTrustChains(EntityStatement leafEs, Set<String> trustAnchorIds, boolean forRp);
    EntityStatement parseAndValidateSelfSigned(String token) throws InvalidTrustChainException;
    boolean validateEntityStatementFields(EntityStatement statement, String issuer, String subject);
    void validationRules(EntityStatement statement, boolean checkAudience);
    JSONWebKeySet getKeySet();
    void updateIdP(IdentityProviderModel model, RealmModel realm);
    IdentityProviderModel rPexcplicitRegistration(String opIssuer, String trustAnchor, IdentityProviderModel model, RealmModel realm) throws Exception;

}
