package org.keycloak.models.jpa;

import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.*;

import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.FederationEntity;
import org.keycloak.models.jpa.entities.IdentityProviderEntity;
import org.keycloak.models.jpa.entities.IdentityProviderMapperEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

//import com.google.common.collect.ImmutableSet;

public class JpaIdpProvider implements IdentityProviderProvider {
	
	protected static final Logger logger = Logger.getLogger(JpaIdpProvider.class);
	private final KeycloakSession session;
    protected EntityManager em;
    
    
    public JpaIdpProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
    }
    
	@Override
	public void close() {
		
	}

    
    @Override
	public List<String> getUsedIdentityProviderIdTypes(RealmModel realm){
    	TypedQuery<String> query = em.createNamedQuery("findUtilizedIdentityProviderTypesOfRealm", String.class);
    	query.setParameter("realmId", realm.getId());
    	return query.getResultList();
	}
    
    @Override
	public Long countIdentityProviders(RealmModel realm) {
    	TypedQuery<Long> query = em.createNamedQuery("countIdentityProvidersOfRealm", Long.class);
    	query.setParameter("realmId", realm.getId());
    	return query.getSingleResult();
    }


	@Override
	public Set<IdentityProviderModelSummary> getIdentityProvidersSummary(RealmModel realm) {
		Query query = em.createNamedQuery("findIdentityProviderSummaryByRealm");
		query.setParameter("realmId", realm.getId());
		List<IdentityProviderEntity> idps = query.getResultList();
		return idps.stream().map(entity -> new IdentityProviderModelSummary(entity.getInternalId(), entity.getAlias(), entity.getProviderId())).collect(Collectors.toCollection(HashSet::new));
	}
    
	@Override
	public List<IdentityProviderModel> getIdentityProviders(RealmModel realm) {
		TypedQuery<IdentityProviderEntity> query = em.createNamedQuery("findIdentityProviderByRealm", IdentityProviderEntity.class);
		query.setParameter("realmId", realm.getId());
		return query.getResultList().stream().map(entity -> entityToModel(entity, false)).collect(Collectors.toList());
	}
  
  
	@Override
	public List<IdentityProviderModel> searchIdentityProviders(RealmModel realm, String keyword, Integer firstResult, Integer maxResults) {
		TypedQuery<IdentityProviderEntity> query = (keyword!=null && !keyword.isEmpty()) ? 
				em.createNamedQuery("findIdentityProviderByRealmAndKeyword", IdentityProviderEntity.class) :
				em.createNamedQuery("findIdentityProviderByRealm", IdentityProviderEntity.class);
				
		query.setParameter("realmId", realm.getId());
		if (firstResult != null && firstResult >= 0)
			query.setFirstResult(firstResult);
		if (maxResults != null && maxResults > 0)
			query.setMaxResults(maxResults);
		
		if(keyword!=null && !keyword.isEmpty())
			query.setParameter("keyword", "%"+keyword.toLowerCase()+"%");
		
		return query.getResultList().stream().map(entity -> entityToModel(entity, false)).collect(Collectors.toList());
	}
  
  
  
	private IdentityProviderModel entityToModel(IdentityProviderEntity entity, boolean withFederations) {
		IdentityProviderModel identityProviderModel = new IdentityProviderModel();
		identityProviderModel.setProviderId(entity.getProviderId());
		identityProviderModel.setAlias(entity.getAlias());
		identityProviderModel.setDisplayName(entity.getDisplayName());

		identityProviderModel.setInternalId(entity.getInternalId());
		Map<String, String> config = entity.getConfig();
		Map<String, String> copy = new HashMap<>();
		copy.putAll(config);
		identityProviderModel.setConfig(copy);
		identityProviderModel.setEnabled(entity.isEnabled());
		identityProviderModel.setLinkOnly(entity.isLinkOnly());
		identityProviderModel.setTrustEmail(entity.isTrustEmail());
		identityProviderModel.setAuthenticateByDefault(entity.isAuthenticateByDefault());
		identityProviderModel.setFirstBrokerLoginFlowId(entity.getFirstBrokerLoginFlowId());
		identityProviderModel.setPostBrokerLoginFlowId(entity.getPostBrokerLoginFlowId());
		identityProviderModel.setStoreToken(entity.isStoreToken());
		identityProviderModel.setAddReadTokenRoleOnCreate(entity.isAddReadTokenRoleOnCreate());
		identityProviderModel.setFederations(entity.getFederations().stream().map(fe -> fe.getInternalId()).collect(Collectors.toCollection(HashSet::new)));
		return identityProviderModel;
	}
  
	
	private IdentityProviderEntity modelToEntity(IdentityProviderModel identityProvider) {
		IdentityProviderEntity entity = new IdentityProviderEntity();

		if (identityProvider.getInternalId() == null) {
			entity.setInternalId(KeycloakModelUtils.generateId());
		} else {
			entity.setInternalId(identityProvider.getInternalId());
		}
		modelToEntity(entity,identityProvider);
		if (identityProvider.getFederations() != null)
		entity.setFederations(identityProvider.getFederations().stream().map(id-> {
		    FederationEntity fed = new FederationEntity();
		    fed.setInternalId(id);
		    return fed;
		}).collect(Collectors.toSet()));

		return entity;
	}
	
	private void modelToEntity(IdentityProviderEntity entity, IdentityProviderModel identityProvider) {
	    entity.setAlias(identityProvider.getAlias());
        entity.setDisplayName(identityProvider.getDisplayName());
        entity.setProviderId(identityProvider.getProviderId());
        entity.setEnabled(identityProvider.isEnabled());
        entity.setStoreToken(identityProvider.isStoreToken());
        entity.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
        entity.setTrustEmail(identityProvider.isTrustEmail());
        entity.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
        entity.setFirstBrokerLoginFlowId(identityProvider.getFirstBrokerLoginFlowId());
        entity.setPostBrokerLoginFlowId(identityProvider.getPostBrokerLoginFlowId());
        entity.setConfig(identityProvider.getConfig());
        entity.setLinkOnly(identityProvider.isLinkOnly());
	}
	
	private FederationEntity modelToEntity(IdentityProvidersFederationModel identityProvidersFederationModel) {

		FederationEntity federationEntity = new FederationEntity();

		federationEntity.setInternalId(identityProvidersFederationModel.getInternalId());
		federationEntity.setAlias(identityProvidersFederationModel.getAlias());
		federationEntity.setProviderId(identityProvidersFederationModel.getProviderId());
		
		federationEntity.setLastMetadataRefreshTimestamp(new Date().getTime());
		federationEntity.setUrl(identityProvidersFederationModel.getUrl());
		federationEntity.setEntityIdBlackList(identityProvidersFederationModel.getEntityIdBlackList());
		federationEntity.setEntityIdWhiteList(identityProvidersFederationModel.getEntityIdWhiteList());
		federationEntity.setRegistrationAuthorityBlackList(identityProvidersFederationModel.getRegistrationAuthorityBlackList());
        federationEntity.setRegistrationAuthorityWhiteList(identityProvidersFederationModel.getRegistrationAuthorityWhiteList());
        federationEntity.setCategoryBlackList(identityProvidersFederationModel.getCategoryBlackList());
        federationEntity.setCategoryWhiteList(identityProvidersFederationModel.getCategoryWhiteList());
		federationEntity.setUpdateFrequencyInMins(identityProvidersFederationModel.getUpdateFrequencyInMins());
		federationEntity.setValidUntilTimestamp(identityProvidersFederationModel.getValidUntilTimestamp());
		return federationEntity;
	}

	// realm is not used in this implementation. Could also be null
	@Override
	public IdentityProviderModel getIdentityProviderById(String realmId, String internalId) {
		IdentityProviderEntity identityProvider = em.find(IdentityProviderEntity.class, internalId);
		return identityProvider != null ? entityToModel(identityProvider, false) : null;
	}
  
	private IdentityProviderEntity getIdentityProviderEntityByAlias(RealmModel realmModel, String alias) {
		TypedQuery<IdentityProviderEntity> query = em.createNamedQuery("findIdentityProviderByRealmAndAlias", IdentityProviderEntity.class);
      query.setParameter("alias", alias);
      query.setParameter("realmId", realmModel.getId());
      try {
      	return query.getSingleResult();
      }
      catch(NoResultException | NonUniqueResultException ex) {
      	return null;
      }
	}
  
	@Override
	public IdentityProviderModel getIdentityProviderByAlias(RealmModel realmModel, String alias) {
		IdentityProviderEntity identityProvider = getIdentityProviderEntityByAlias(realmModel, alias);
		return identityProvider != null ? entityToModel(identityProvider, false) : null;
	}

	@Override
	public void addIdentityProvider(RealmModel realmModel, IdentityProviderModel identityProvider) {
		IdentityProviderEntity entity = modelToEntity(identityProvider);
		
		RealmEntity realm = new RealmEntity();
		realm.setId(realmModel.getId());
		
		entity.setRealm(realm);

		em.persist(entity);
		em.flush();

		identityProvider.setInternalId(entity.getInternalId());
	}
	
	@Override
	public void removeIdentityProviderByAlias(RealmModel realmModel, String alias) {
		TypedQuery<IdentityProviderEntity> query = em.createNamedQuery("findIdentityProviderByRealmAndAlias", IdentityProviderEntity.class);
		query.setParameter("alias", alias);
		query.setParameter("realmId", realmModel.getId());
		IdentityProviderEntity identityProvider = query.getSingleResult();
		
		getIdentityProviderMappersByAlias(realmModel, identityProvider.getAlias()).stream()
			.forEach(identityProviderMapper -> removeIdentityProviderMapper(realmModel, identityProviderMapper));
		
		
		IdentityProviderModel model = entityToModel(identityProvider, false);
		
		em.remove(identityProvider);
		em.flush();
		
		session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderRemovedEvent() {

			@Override
			public RealmModel getRealm() {
				return realmModel;
			}

			@Override
			public IdentityProviderModel getRemovedIdentityProvider() {
				return model;
			}

			@Override
			public KeycloakSession getKeycloakSession() {
				return session;
			}
		});

	}

	@Override
	public void updateIdentityProvider(RealmModel realmModel, IdentityProviderModel identityProvider) {

		IdentityProviderEntity entity = em.find(IdentityProviderEntity.class, identityProvider.getInternalId());
		if (entity != null) {
		    modelToEntity(entity,identityProvider);
		}

		em.flush();

		session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderUpdatedEvent() {

			@Override
			public RealmModel getRealm() {
				return realmModel;
			}

			@Override
			public IdentityProviderModel getUpdatedIdentityProvider() {
				return identityProvider;
			}

			@Override
			public KeycloakSession getKeycloakSession() {
				return session;
			}
		});
		
	}



	@Override
	public boolean isIdentityFederationEnabled(RealmModel realmModel) {
		return countIdentityProviders(realmModel) > 0;
	}
	
	
	@Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappers(RealmModel realmModel) {
		TypedQuery<IdentityProviderMapperEntity> query = em.createNamedQuery("findIdentityProviderMappersByRealm", IdentityProviderMapperEntity.class);
		query.setParameter("realmId", realmModel.getId());
        return query.getResultList().stream().map(entity -> entityToModel(entity)).collect(Collectors.toCollection(HashSet::new));
    }

	@Override
	public Set<IdentityProviderMapperModelSummary> getIdentityProviderMappersSummary(RealmModel realmModel) {
		TypedQuery<IdentityProviderMapperEntity> query = em.createNamedQuery("findIdentityProviderMappersByRealm", IdentityProviderMapperEntity.class);
		query.setParameter("realmId", realmModel.getId());
		return query.getResultList().stream().map(entity -> new IdentityProviderMapperModelSummary(entity.getId(), entity.getName(), entity.getIdentityProviderAlias())).collect(Collectors.toCollection(HashSet::new));
	}

    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(RealmModel realmModel, String brokerAlias) {
    	TypedQuery<IdentityProviderMapperEntity> query = em.createNamedQuery("findIdentityProviderMappersByRealmAndAlias", IdentityProviderMapperEntity.class);
    	query.setParameter("alias", brokerAlias);
    	query.setParameter("realmId", realmModel.getId());
    	return query.getResultList().stream().map(entity -> entityToModel(entity)).collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public IdentityProviderMapperModel addIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel model) {
    	RealmEntity realm = em.find(RealmEntity.class, realmModel.getId());
        if (getIdentityProviderMapperByName(realmModel, model.getIdentityProviderAlias(), model.getName()) != null) {
            throw new RuntimeException("identity provider mapper name must be unique per identity provider");
        }
        String id = KeycloakModelUtils.generateId();
        IdentityProviderMapperEntity entity = new IdentityProviderMapperEntity();
        entity.setId(id);
        entity.setName(model.getName());
        entity.setIdentityProviderAlias(model.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(model.getIdentityProviderMapper());
        entity.setRealm(realm);
        entity.setConfig(model.getConfig());
        
        em.persist(entity);
        em.flush();
        
        return entityToModel(entity);
    }


    protected IdentityProviderMapperEntity getIdentityProviderMapperEntityByName(RealmModel realmModel, String alias, String name) {
    	TypedQuery<IdentityProviderMapperEntity> query = em.createNamedQuery("findIdentityProviderMappersByRealmAndAliasAndName", IdentityProviderMapperEntity.class);
    	query.setParameter("realmId", realmModel.getId());
    	query.setParameter("alias", alias);
    	query.setParameter("name", name);
    	try {
    		return query.getSingleResult();
    	}
    	catch(NoResultException | NonUniqueResultException e) {
    		return null;
    	}
    }

    @Override
    public void removeIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping) {
        IdentityProviderMapperEntity toDelete = em.find(IdentityProviderMapperEntity.class, mapping.getId());
        if (toDelete != null)
        	em.remove(toDelete);
    }

    @Override
    public void updateIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping) {
    	IdentityProviderMapperEntity entity = em.find(IdentityProviderMapperEntity.class, mapping.getId());
        entity.setIdentityProviderAlias(mapping.getIdentityProviderAlias());
        entity.setIdentityProviderMapper(mapping.getIdentityProviderMapper());
        if (entity.getConfig() == null) {
            entity.setConfig(mapping.getConfig());
        } else {
            entity.getConfig().clear();
            if (mapping.getConfig() != null) {
                entity.getConfig().putAll(mapping.getConfig());
            }
        }
        em.flush();

    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperById(String realmId, String id) {
    	IdentityProviderMapperEntity entity = em.find(IdentityProviderMapperEntity.class, id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(RealmModel realmModel, String alias, String name) {
        IdentityProviderMapperEntity entity = getIdentityProviderMapperEntityByName(realmModel, alias, name);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    
    protected IdentityProviderMapperModel entityToModel(IdentityProviderMapperEntity entity) {
        IdentityProviderMapperModel mapping = new IdentityProviderMapperModel();
        mapping.setId(entity.getId());
        mapping.setName(entity.getName());
        mapping.setIdentityProviderAlias(entity.getIdentityProviderAlias());
        mapping.setIdentityProviderMapper(entity.getIdentityProviderMapper());
        Map<String, String> config = new HashMap<String, String>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        mapping.setConfig(config);
        return mapping;
    }
	
	
    @Override
    public void saveFederationIdp(RealmModel realmModel, IdentityProviderModel idpModel) {

        RealmEntity realm = new RealmEntity();
        realm.setId(realmModel.getId());
        IdentityProviderEntity idpEntity = modelToEntity(idpModel);
        idpEntity.setRealm(realm);

        // only for new identity provider
        if (idpModel.getInternalId() == null) {
            idpModel.setInternalId(idpEntity.getInternalId());
            em.persist(idpEntity);
        } 
        
        em.flush();
    }
	
	@Override
	public boolean removeFederationIdp(RealmModel realmModel, IdentityProvidersFederationModel idpfModel, String idpAlias) {
		
		try {
			IdentityProviderEntity idpEntity = getIdentityProviderEntityByAlias(realmModel, idpAlias);
			if(idpEntity == null) return false;
			Set<FederationEntity> idpFeds = idpEntity.getFederations();
			FederationEntity fedEntity = idpFeds.stream().filter(e -> e.getInternalId().equals(idpfModel.getInternalId())).findAny().orElse(null);
			IdentityProviderModel idpModel = entityToModel(idpEntity, true);
			if(idpFeds.size() == 1) {
				em.remove(idpEntity);
				em.flush();
				
//				session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderRemovedEvent() {
//
//	                    @Override
//	                    public RealmModel getRealm() {
//	                        return RealmAdapter.this;
//	                    }
//
//	                    @Override
//	                    public IdentityProviderModel getRemovedIdentityProvider() {
//	                        return idpModel;
//	                    }
//
//	                    @Override
//	                    public KeycloakSession getKeycloakSession() {
//	                        return session;
//	                    }
//	                });
			}
			else if(idpFeds.size() > 1) {
				idpEntity.removeFromFederation(fedEntity);
				em.persist(idpEntity);
				em.flush();
				
//				session.getKeycloakSessionFactory().publish(new RealmModel.IdentityProviderUpdatedEvent() {
//
//		            @Override
//		            public RealmModel getRealm() {
//		                return RealmAdapter.this;
//		            }
//
//		            @Override
//		            public IdentityProviderModel getUpdatedIdentityProvider() {
//		                return idpModel;
//		            }
//
//		            @Override
//		            public KeycloakSession getKeycloakSession() {
//		                return session;
//		            }
//		        });
			}
			
			return true;
		}
		catch(Exception ex) {
			return false;
		}
		
	}
	
}
