package org.keycloak.protocol.oidc;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.keycloak.authentication.authenticators.client.ISHAREClientAuthenticator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;

public class ClientCreationUtils {

    public static ClientModel createIshareClient(RealmModel realm, String clientId) {
        ClientModel client = realm.addClient(clientId);
        client.setClientId(clientId);

        Set<String> redirectUris = new HashSet<>();
        redirectUris.add("*");
        client.setRedirectUris(redirectUris);

        client.setBaseUrl("");
        client.setRootUrl("");
        client.setBearerOnly(false);
        client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        client.setClientAuthenticatorType(ISHAREClientAuthenticator.PROVIDER_ID);
        client.setConsentRequired(true);
        client.setAlwaysDisplayInConsole(false);

        OIDCAdvancedConfigWrapper oidc = OIDCAdvancedConfigWrapper.fromClientModel(client);
        oidc.setUseRefreshToken(false);
        oidc.setUserInfoSignedResponseAlg(Algorithm.RS256);
        oidc.setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST);
        List<String> acr_values = new LinkedList();
        acr_values.add("urn:http://eidas.europa.eu/LoA/NotNotified/substantial");
        oidc.setAttributeMultivalued(Constants.DEFAULT_ACR_VALUES, acr_values);

        client.setManagementUrl("");
        client.setPublicClient(false);
        client.setName("[ishare] " + clientId);
        client.setDescription("ishare client added via dynamic client discovery.");

        client.setFullScopeAllowed(true);
        client.setFrontchannelLogout(true);
        client.setEnabled(true);

        client.setDirectAccessGrantsEnabled(false);
        client.setImplicitFlowEnabled(false);
       // client.setServiceAccountsEnabled(false);
        client.setSurrogateAuthRequired(false);
        client.setAttribute(Constants.ISHARE_ENABLED, "true");
        ClientScopeModel ishareScope = realm.getClientScopesStream().filter(x -> Constants.ISHARE_SCOPE.equals(x.getName())).findAny().get();
        client.addClientScope(ishareScope, false);

        return client;
    }
}
