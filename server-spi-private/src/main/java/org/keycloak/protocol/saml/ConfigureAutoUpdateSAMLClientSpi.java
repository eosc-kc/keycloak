package org.keycloak.protocol.saml;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class ConfigureAutoUpdateSAMLClientSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "autoupdateSAMLClient";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return ConfigureAutoUpdateSAMLClient.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return ConfigureAutoUpdateSAMLClientFactory.class;
    }
}