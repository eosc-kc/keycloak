package org.keycloak.utils;

import java.io.IOException;
import java.util.Set;

import org.keycloak.common.VerificationException;
import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.provider.Provider;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.TrustChainResolution;

public interface TrustChainProcessor extends Provider {

    public TrustChainResolution constructTrustChains(EntityStatement leafEs, Set<String> trustAnchorIds, boolean forRp);
    <T extends EntityStatement> T parseAndValidateSelfSigned (String token, Class<T> clazz, JSONWebKeySet publicKey) throws IOException, JWSInputException, VerificationException;
    EntityStatement parseAndValidateSelfSigned(String token) throws InvalidTrustChainException;
    boolean validateEntityStatementFields(EntityStatement statement, String issuer, String subject);
    JSONWebKeySet getKeySet();
}
