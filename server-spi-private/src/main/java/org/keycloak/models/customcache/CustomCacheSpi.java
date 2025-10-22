package org.keycloak.models.customcache;

import org.keycloak.models.customcache.CustomCacheProvider;
import org.keycloak.models.customcache.CustomCacheProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class CustomCacheSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "token-relay-spi";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return CustomCacheProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return CustomCacheProviderFactory.class;
    }
}
