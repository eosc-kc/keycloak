package org.keycloak.protocol.trustchain;

import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.TrustChainResolution;

import java.util.List;
import java.util.Set;

public interface TrustChainProcessor  extends Provider {

    TrustChainProcessor session(KeycloakSession session);
    TrustChainResolution constructTrustChains(EntityStatement leafEs, Set<String> trustAnchorIds, boolean forRp);
    List<TrustChainResolution> subTrustChains(String initialEntity, EntityStatement leafEs, Set<String> trustAnchorIds, Set<String> visitedNodes, boolean forRp);
    EntityStatement parseAndValidateSelfSigned(String token) throws InvalidTrustChainException;
    <T extends EntityStatement> T parseAndValidateSelfSigned(String token, Class<T> clazz, JSONWebKeySet jwks) throws InvalidTrustChainException;
    boolean validateEntityStatementFields(EntityStatement statement, String issuer, String subject);
    void validationRules(EntityStatement statement, boolean checkAudience);
    JSONWebKeySet getKeySet();
    IdentityProviderModel updateIdP(IdentityProviderModel model, RealmModel realm) throws Exception;
    IdentityProviderModel rPexcplicitRegistration(String opIssuer, String trustAnchor, IdentityProviderModel model, RealmModel realm) throws Exception;

}