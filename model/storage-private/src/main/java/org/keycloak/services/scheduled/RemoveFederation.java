package org.keycloak.services.scheduled;

import org.keycloak.broker.federation.FederationProvider;
import org.keycloak.broker.federation.SAMLFederationProviderFactory;
import org.keycloak.models.FederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.timer.ScheduledTask;

import org.jboss.logging.Logger;


public class RemoveFederation implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(RemoveFederation.class);

    private final String federationId;
    private final String realmId;

    public RemoveFederation(String federationId, String realmId) {
        this.federationId = federationId;
        this.realmId = realmId;
    }

    @Override
    public void run(KeycloakSession session) {
        logger.info(" Task Removing federation with id " + federationId + " and realm id " + realmId);
        RealmModel realm = session.realms().getRealm(realmId);
        if ( realm != null) {
            FederationModel model = realm.getSAMLFederationById(federationId);
            FederationProvider federationProvider = SAMLFederationProviderFactory.getSAMLFederationProviderFactoryById(session, model.getProviderId()).create(session,model, realm.getId());

            session.identityProviders().getIdentityProvidersByFederation(model.getInternalId()).forEach(idp -> federationProvider.removeIdP(idp, realm, session.identityProviders()));
            federationProvider.removeClients(session.clients().getFederationClientsStream(realm, model.getInternalId()), realm);

            realm.removeSAMLFederation(model.getInternalId());
            logger.info(" Finishing federation removal with id " + federationId + " and realm id " + realmId);

        }
    }

}
