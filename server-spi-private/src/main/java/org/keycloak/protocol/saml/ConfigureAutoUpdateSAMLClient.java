package org.keycloak.protocol.saml;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

public interface ConfigureAutoUpdateSAMLClient extends Provider {

    public void configure(ClientModel clientModel, RealmModel realm);
}
