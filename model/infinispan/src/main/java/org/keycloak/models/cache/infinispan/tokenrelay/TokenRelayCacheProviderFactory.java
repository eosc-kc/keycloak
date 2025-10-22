package org.keycloak.models.cache.infinispan.tokenrelay;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.customcache.CustomCacheProviderFactory;


public class TokenRelayCacheProviderFactory implements CustomCacheProviderFactory {

    public final static String ID = "token-relay-cache";

    @Override
    public TokenRelayCacheProvider create(KeycloakSession session) {
        return new TokenRelayCacheProvider(session);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return ID;
    }
}
