package org.keycloak.protocol.oidc;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * Supports Translations for Proxied Token Introspection
 */
public class ProxiedTokenIntrospectionTranslationsSpi implements Spi {
    public static final String SPI_NAME = "proxied-token-introspection-translation";

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return SPI_NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ProxiedTokenIntrospectionTranslationsProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ProxiedTokenIntrospectionTranslationsProviderFactory.class;
    }
}
