package org.keycloak.utils;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.keycloak.common.VerificationException;
import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.provider.Provider;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.TrustChainResolution;

public interface TrustChainProcessor extends Provider {

    List<TrustChainResolution> constructTrustChains(EntityStatement leafEs, Set<String> trustAnchorIds, boolean policyRequired, boolean forRp);
    <T extends EntityStatement> T parseAndValidateSelfSigned (String token, Class<T> clazz, JSONWebKeySet publicKey) throws IOException, JWSInputException, VerificationException;
    EntityStatement parseAndValidateSelfSigned(String token) throws InvalidTrustChainException;
    boolean validateEntityStatementFields(EntityStatement statement, String issuer, String subject);
    TrustChainResolution findAcceptableMetadataPolicyChain(List<TrustChainResolution> trustChainResolutions, EntityStatement statement);
    JSONWebKeySet getKeySet();
}
