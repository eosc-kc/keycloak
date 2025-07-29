package org.keycloak.services.scheduled;

import org.jboss.logging.Logger;
import org.keycloak.broker.federation.FederationProvider;
import org.keycloak.broker.federation.SAMLFederationProviderFactory;
import org.keycloak.models.FederationModel;
import org.keycloak.models.KeycloakSession;


import org.keycloak.models.RealmModel;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;


public class UpdateFederation implements ScheduledTask {
	
	protected static final Logger logger = Logger.getLogger(UpdateFederation.class);
	
	protected final String federationId;
	protected final String realmId;
	
	public UpdateFederation(String federationId, String realmId) {
		this.federationId = federationId;
		this.realmId = realmId;
	}

	@Override
	public void run(KeycloakSession session) {
		logger.info(" Updating identity providers of federation with id " + federationId + " and realm id " + realmId);
		RealmModel realm = session.realms().getRealm(realmId);
		if ( realm != null) {
				FederationModel federationModel = realm.getSAMLFederationById(federationId);
			try {
				SAMLFederationProviderFactory samlFederationProviderFactory = SAMLFederationProviderFactory.getSAMLFederationProviderFactoryById(session, federationModel.getProviderId());
				FederationProvider federationProvider = samlFederationProviderFactory.create(session, federationModel, realmId);
				federationProvider.updateSamlEntities();
			} catch(Exception e){
					e.printStackTrace();
					logger.warn("Problem during updating IdPs of federation (id): " + federationModel.getInternalId());
			}
		} else {
			//realm has been removed. remove this task
			TimerProvider timer = session.getProvider(TimerProvider.class);
			timer.cancelTaskAndNotify("UpdateFederation" + federationId);
		}
	}
	


}
