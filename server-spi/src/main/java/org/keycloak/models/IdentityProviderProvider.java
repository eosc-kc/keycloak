package org.keycloak.models;

import java.util.List;
import java.util.Set;

import org.keycloak.provider.Provider;

public interface IdentityProviderProvider extends Provider {

	void close();
	
	
	List<String> getUsedIdentityProviderIdTypes(RealmModel realm);
	Long countIdentityProviders(RealmModel realm);

	List<IdentityProviderModel> getIdentityProviders(RealmModel realm);
	List<IdentityProviderModel> searchIdentityProviders(RealmModel realm, String keyword, Integer firstResult, Integer maxResults);
	
	IdentityProviderModel getIdentityProviderById(String internalId);
	IdentityProviderModel getIdentityProviderByAlias(RealmModel realm, String alias);
	void addIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider);
	void removeIdentityProviderByAlias(RealmModel realm, String alias);
	void updateIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider);
	boolean isIdentityFederationEnabled(RealmModel realm);
	
	Set<IdentityProviderMapperModel> getIdentityProviderMappers(RealmModel realmModel);
	Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(RealmModel realmModel, String brokerAlias);
	IdentityProviderMapperModel addIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel model);
	void removeIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping);
	void updateIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping);
	IdentityProviderMapperModel getIdentityProviderMapperById(RealmModel realmModel, String id);
	IdentityProviderMapperModel getIdentityProviderMapperByName(RealmModel realmModel, String alias, String name);
	
	boolean addFederationIdp(RealmModel realmModel, IdentityProvidersFederationModel idpfModel, IdentityProviderModel idpModel);
    boolean removeFederationIdp(RealmModel realmModel, IdentityProvidersFederationModel identityProvidersFederationModel, String idpAlias);
	
}
