package org.keycloak.protocol.oidc.federation;

import org.keycloak.common.VerificationException;
import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.provider.Provider;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.TrustChainForExplicit;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface TrustChainProcessor extends Provider {

    public List<TrustChainForExplicit> constructTrustChains(EntityStatement leafEs, Set<String> trustAnchorIds, boolean policyRequired);
    EntityStatement parseAndValidateSelfSigned (String token, JSONWebKeySet publicKey) throws IOException, JWSInputException, VerificationException;
    EntityStatement parseAndValidateSelfSigned(String token) throws InvalidTrustChainException;
    TrustChainForExplicit findAcceptableMetadataPolicyChain(List<TrustChainForExplicit> trustChainForExplicits, EntityStatement statement);
}
