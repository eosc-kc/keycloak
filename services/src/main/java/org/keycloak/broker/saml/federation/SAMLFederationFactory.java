package org.keycloak.broker.saml.federation;

import org.keycloak.Config.Scope;
import org.keycloak.broker.federation.FederationProvider;
import org.keycloak.broker.federation.SAMLFederationProviderFactory;
import org.keycloak.models.FederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;


public class SAMLFederationFactory implements SAMLFederationProviderFactory {

	public static final String FEDERATION_PROVIDER_ID = "saml";  //same as the SAMLIdentityProviderFactory.PROVIDER_ID
	
	
	@Override
	public String getId() {
		return FEDERATION_PROVIDER_ID;
	}
	
	@Override
    public String getName() {
        return "SAML v2.0 federation";
    }


	
	@Override
	public SAMLFederationProvider create(KeycloakSession session, FederationModel model, String realmId) {
		return new SAMLFederationProvider(session, new SAMLFederationModel(model),realmId);
	}


    @Override
    public FederationProvider create(KeycloakSession session) {
        return null;
    }

    @Override
	public void init(Scope config) {
		// TODO Auto-generated method stub
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		// TODO Auto-generated method stub
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}

}
