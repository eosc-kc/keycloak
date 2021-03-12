package org.keycloak.models;

import java.util.List;
import java.util.Set;

import org.keycloak.provider.Provider;

public interface IdentityProviderProvider extends Provider {

	void close();
	
	
	List<String> getUsedIdentityProviderIdTypes(RealmModel realm);
	Long countIdentityProviders(RealmModel realm);

	List<IdentityProviderModel> getIdentityProviders(RealmModel realm);
	Set<IdentityProviderModelSummary> getIdentityProvidersSummary(RealmModel realm);
	List<IdentityProviderModel> searchIdentityProviders(RealmModel realm, String keyword, Integer firstResult, Integer maxResults);
	
	IdentityProviderModel getIdentityProviderById(String realmId, String internalId);
	IdentityProviderModel getIdentityProviderByAlias(RealmModel realm, String alias);
	void addIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider);
	void removeIdentityProviderByAlias(RealmModel realm, String alias);
	void updateIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider);
	boolean isIdentityFederationEnabled(RealmModel realm);
	
	Set<IdentityProviderMapperModel> getIdentityProviderMappers(RealmModel realmModel);
	Set<IdentityProviderMapperModelSummary> getIdentityProviderMappersSummary(RealmModel realmModel);
	Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(RealmModel realmModel, String brokerAlias);
	IdentityProviderMapperModel addIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel model);
	void removeIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping);
	void updateIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping);
	IdentityProviderMapperModel getIdentityProviderMapperById(String realmId, String id);
	IdentityProviderMapperModel getIdentityProviderMapperByName(RealmModel realmModel, String alias, String name);
	
	void saveFederationIdp(RealmModel realmModel, IdentityProviderModel idpModel);
    boolean removeFederationIdp(RealmModel realmModel, IdentityProvidersFederationModel identityProvidersFederationModel, String idpAlias);
	
}
