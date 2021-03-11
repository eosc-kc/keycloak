package org.keycloak.models.cache.infinispan;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.*;
import org.keycloak.models.cache.CacheIdpProviderI;
import org.keycloak.models.cache.infinispan.events.*;

public class IdpCacheProvider implements CacheIdpProviderI {

    protected static final Logger logger = Logger.getLogger(IdpCacheProvider.class);

    private Cache<String, IdentityProviderModel> cacheIdp;
    private Cache<String, IdentityProviderMapperModel> cacheIdpMappers;

    private static Map<String, Set<IdentityProviderModelSummary>> idpSummaries = new ConcurrentHashMap<>();
    private static Map<String, Set<IdentityProviderMapperModelSummary>> idpMapperSummaries = new ConcurrentHashMap<>();


//	private static IdpIndex idpIndex = new IdpIndex();
//	private static IdpMapperIndex idpMapperIndex = new IdpMapperIndex();


    private ClusterProvider cluster;

    protected KeycloakSession session;
    protected IdentityProviderProvider identityProviderDelegate;


    public IdpCacheProvider(Cache<String, IdentityProviderModel> identityProvidersCache, Cache<String, IdentityProviderMapperModel> identityProviderMappersCache, KeycloakSession session) {
        this.cacheIdp = identityProvidersCache;
        this.cacheIdpMappers = identityProviderMappersCache;
        this.session = session;
        this.cluster = session.getProvider(ClusterProvider.class);
        lazyInitIdps();
        lazyInitIdpMappers();
    }


    private void lazyInitIdps() {
        if (idpSummaries.isEmpty()) {
            synchronized (idpSummaries) {
                if (idpSummaries.isEmpty())
                    session.realms().getRealmsStream().forEach(realm -> idpSummaries.put(realm.getId(), getIdentityProviderDelegate().getIdentityProvidersSummary(realm)));
            }
        }
    }

    private void lazyInitIdpMappers() {
        if (idpMapperSummaries.isEmpty()) {
            synchronized (idpMapperSummaries) {
                if (idpMapperSummaries.isEmpty())
                    session.realms().getRealmsStream().forEach(realm -> idpMapperSummaries.put(realm.getId(), getIdentityProviderDelegate().getIdentityProviderMappersSummary(realm)));
            }
        }
    }

    protected void resetCachingIdps(){
        cacheIdp.clear();
        idpSummaries.clear();
        lazyInitIdps();
    }

    protected void resetCachingIdpMappers(){
        cacheIdpMappers.clear();
        idpMapperSummaries.clear();
        lazyInitIdpMappers();
    }

    @Override
    public void clear() {
        resetCachingIdps();
        resetCachingIdpMappers();
        cluster.notify(InfinispanCacheIdpProviderFactory.IDP_CLEAR_CACHE_EVENT, new ClearCacheEvent(), true, ClusterProvider.DCNotify.ALL_DCS);
        cluster.notify(InfinispanCacheIdpProviderFactory.IDP_MAPPERS_CLEAR_CACHE_EVENT, new ClearCacheEvent(), true, ClusterProvider.DCNotify.ALL_DCS);
    }

    @Override
    public IdentityProviderProvider getIdentityProviderDelegate() {
        if (identityProviderDelegate != null) return identityProviderDelegate;
        identityProviderDelegate = session.identityProviderLocalStorage();
        return identityProviderDelegate;
    }


    @Deprecated
    @Override
    public void evict(RealmModel realm, IdentityProviderModel identityProvider) {
        //not used
    }

    @Deprecated
    @Override
    public void evict(RealmModel realm) {
        //not used
    }

    @Override
    public void close() {
        if (identityProviderDelegate != null) identityProviderDelegate.close();
    }


    @Override
    public List<String> getUsedIdentityProviderIdTypes(RealmModel realm) {
        Set<IdentityProviderModelSummary> summaries = getIdentityProvidersSummary(realm);
        if(summaries==null)
            return new ArrayList<>();
        Set<String> resp = new HashSet<>();
        summaries.stream().forEach(idp -> resp.add(idp.getProviderId()));
        return new ArrayList<>(resp);
    }


    @Override
    public Long countIdentityProviders(RealmModel realm) {
        Set<IdentityProviderModelSummary> summaries = getIdentityProvidersSummary(realm);
        //TODO: check if we can just return 0 if null
        return summaries!=null ? new Long(summaries.size()) : session.identityProviderLocalStorage().countIdentityProviders(realm);
    }


    @Override
    public List<IdentityProviderModel> getIdentityProviders(RealmModel realm) {
        return idpSummaries.get(realm.getId()).stream().map(idpSummary -> getIdentityProviderById(realm, idpSummary.getInternalId())).collect(Collectors.toList());
    }

    @Override
    public Set<IdentityProviderModelSummary> getIdentityProvidersSummary(RealmModel realm) {
        return idpSummaries.get(realm.getId());
    }

    @Override
    public List<IdentityProviderModel> searchIdentityProviders(RealmModel realm, String keyword, Integer firstResult, Integer maxResults) {
        final String lowercaseKeyword = keyword.toLowerCase();
        Set<IdentityProviderModelSummary> summaries = idpSummaries.get(realm.getId());
        if(summaries==null)
            return new ArrayList<>();
        return summaries.stream()
                .map(idpSummary -> getIdentityProviderById(realm, idpSummary.getInternalId()))
                .filter(idp -> {
                    String name = idp.getDisplayName() == null ? "" : idp.getDisplayName();
                    return name.toLowerCase().contains(lowercaseKeyword) || idp.getAlias().toLowerCase().contains(lowercaseKeyword);
                })
                .skip(firstResult)
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    @Override
    public IdentityProviderModel getIdentityProviderById(RealmModel realm, String internalId) {
        IdentityProviderModel idp = cacheIdp.get(realm.getId() + internalId);
        if (idp == null) {
            idp = getIdentityProviderDelegate().getIdentityProviderById(realm, internalId);
            cacheIdp.put(realm.getId() + internalId, idp);
        }
        return idp;
    }

    @Override
    public IdentityProviderModel getIdentityProviderByAlias(RealmModel realm, String alias) {
        return idpSummaries.get(realm.getId()).stream().filter(idp -> idp.getAlias().equals(alias)).map(idpSummary -> getIdentityProviderById(realm, idpSummary.getInternalId())).findFirst().orElse(null);
    }


    @Override
    public void addIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
        getIdentityProviderDelegate().addIdentityProvider(realm, identityProvider);

//		synchronized (cacheIdp) {
        cacheIdp.put(realm.getId() + identityProvider.getInternalId(), identityProvider);
        addIdpSummary(realm.getId(), new IdentityProviderModelSummary(identityProvider));
//		}

        cluster.notify(IdpAddedEvent.EVENT_NAME, new IdpAddedEvent(realm.getId(), identityProvider), true, ClusterProvider.DCNotify.ALL_DCS);
    }


    @Override
    public void removeIdentityProviderByAlias(RealmModel realm, String alias) {

        //remove from database idp and mappers
        getIdentityProviderDelegate().removeIdentityProviderByAlias(realm, alias);

        //remove idp mappers from the cache
        Set<IdentityProviderMapperModel> idpMappers = getIdentityProviderMappersByAlias(realm, alias);
        idpMappers.forEach(idpMapper -> {
//            synchronized (cacheIdpMappers) {
                cacheIdpMappers.remove(realm.getId() + idpMapper.getId());
                removeIdpMapperSummary(realm.getId(), new IdentityProviderMapperModelSummary(idpMapper));
//            }
            cluster.notify(IdpMapperRemovedEvent.EVENT_NAME, new IdpMapperRemovedEvent(realm.getId(), idpMapper), true, ClusterProvider.DCNotify.ALL_DCS);
        });

        //remove the identity provider from the cache
        IdentityProviderModel idpModel = getIdentityProviderByAlias(realm, alias);
//        synchronized (cacheIdp) {
            cacheIdp.remove(realm.getId()+idpModel.getInternalId());
            removeIdpSummary(realm.getId(), new IdentityProviderModelSummary(idpModel));
//        }
        cluster.notify(IdpRemovedEvent.EVENT_NAME, new IdpRemovedEvent(realm.getId(), idpModel), true, ClusterProvider.DCNotify.ALL_DCS);
    }

    @Override
    public void updateIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
        if (identityProvider == null) return;
        getIdentityProviderDelegate().updateIdentityProvider(realm, identityProvider);

//		synchronized (cacheIdp) {
        cacheIdp.put(realm.getId() + identityProvider.getInternalId(), identityProvider);
        //no need to alter the summary, since its fields never change after creation
//		}

        cluster.notify(IdpUpdatedEvent.EVENT_NAME, new IdpUpdatedEvent(realm.getId(), identityProvider), true, ClusterProvider.DCNotify.ALL_DCS);
    }


    @Override
    public void saveFederationIdp(RealmModel realmModel, IdentityProviderModel idpModel) {
        if (idpModel.getInternalId() != null) {
            IdentityProviderModel existingModel = getIdentityProviderById(realmModel, idpModel.getInternalId());
            if (existingModel.equalsPreviousVersion(idpModel))
                return;
        }

        getIdentityProviderDelegate().saveFederationIdp(realmModel, idpModel);

//		synchronized (cacheIdp) {
        cacheIdp.put(realmModel.getId() + idpModel.getInternalId(), idpModel);
        addIdpSummary(realmModel.getId(), new IdentityProviderModelSummary(idpModel));
//		}

        cluster.notify(IdpUpdatedEvent.EVENT_NAME, new IdpUpdatedEvent(realmModel.getId(), idpModel), true, ClusterProvider.DCNotify.ALL_DCS);
    }


    @Override
    public boolean removeFederationIdp(RealmModel realmModel, IdentityProvidersFederationModel idpfModel, String idpAlias) {
        boolean result = getIdentityProviderDelegate().removeFederationIdp(realmModel, idpfModel, idpAlias);
        if (result) {
            IdentityProviderModel idpModel = getIdentityProviderByAlias(realmModel, idpAlias);
            if (idpModel != null) {
                if (idpModel.getFederations().size() == 1) { //belongs to only one federation, this one, so remove it entirely from cache

//                    synchronized (cacheIdp) {
                        cacheIdp.remove(realmModel.getId() + idpModel.getInternalId());
                        removeIdpSummary(realmModel.getId(), new IdentityProviderModelSummary(idpModel));
//                    }
                    cluster.notify(IdpRemovedEvent.EVENT_NAME, new IdpRemovedEvent(realmModel.getId(), idpModel), true, ClusterProvider.DCNotify.ALL_DCS);

                    //remove also its mappers
                    Set<IdentityProviderMapperModel> idpMappers = getIdentityProviderMappersByAlias(realmModel, idpAlias);
//                    synchronized (cacheIdpMappers) {
                        idpMappers.forEach(mapper -> {
                            cacheIdpMappers.remove(realmModel.getId() + mapper.getId());
                            removeIdpMapperSummary(realmModel.getId(), new IdentityProviderMapperModelSummary(mapper));
                        });
//                    }
                    idpMappers.forEach(idpMapper -> {
                        cluster.notify(IdpMapperRemovedEvent.EVENT_NAME, new IdpMapperRemovedEvent(realmModel.getId(), idpMapper), true, ClusterProvider.DCNotify.ALL_DCS);
                    });
                } else if (idpModel.getFederations().size() > 1) {
                    idpModel.getFederations().remove(idpfModel.getInternalId());
//                    synchronized (cacheIdp) {
                        cacheIdp.put(realmModel.getId()+idpModel.getInternalId(), idpModel);
//                    }
                    cluster.notify(IdpUpdatedEvent.EVENT_NAME, new IdpUpdatedEvent(realmModel.getId(), idpModel), true, ClusterProvider.DCNotify.ALL_DCS);
                } else //means it's zero. should never happen normally
                    logger.errorf("Cache inconsistency! Trying to remove from cache an identity provider (alias= %s) which does not belong to the expected federation (alias= %s)", idpAlias, idpfModel.getAlias());
            } else
                logger.errorf("Cache inconsistency! Could not locate within the federation (alias = %s) the identity provider (alias = %s) to delete", idpfModel.getAlias(), idpAlias);
        }
        return result;
    }

    @Override
    public boolean isIdentityFederationEnabled(RealmModel realm) {
        return idpSummaries.get(realm.getId()).size() > 0;
    }


    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappers(RealmModel realmModel) {
        return idpMapperSummaries.get(realmModel.getId()).stream().map(idpMapperSummary -> getIdentityProviderMapperById(realmModel, idpMapperSummary.getId())).collect(Collectors.toSet());
    }

    @Override
    public Set<IdentityProviderMapperModelSummary> getIdentityProviderMappersSummary(RealmModel realmModel) {
        return idpMapperSummaries.get(realmModel.getId());
    }

    @Override
    public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(RealmModel realmModel, String brokerAlias) {
        Set<IdentityProviderMapperModelSummary> summaries = getIdentityProviderMappersSummary(realmModel);
        if(summaries==null)
            return new HashSet<>();
        return summaries.stream().filter(mapperSummary -> mapperSummary.getIdentityProviderAlias().equals(brokerAlias)).map(mapperSummary -> getIdentityProviderMapperById(realmModel, mapperSummary.getId())).collect(Collectors.toSet());
    }

    @Override
    public IdentityProviderMapperModel addIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel model) {
        //add to db
        model = getIdentityProviderDelegate().addIdentityProviderMapper(realmModel, model);
        //add to cache
//        synchronized (cacheIdpMappers) {
            cacheIdpMappers.put(realmModel.getId()+model.getId(), model);
            addIdpMapperSummary(realmModel.getId(), new IdentityProviderMapperModelSummary(model));
//        }
        cluster.notify(IdpMapperAddedEvent.EVENT_NAME, new IdpMapperAddedEvent(realmModel.getId(), model), true, ClusterProvider.DCNotify.ALL_DCS);
        return model;
    }

    @Override
    public void removeIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapper) {
        getIdentityProviderDelegate().removeIdentityProviderMapper(realmModel, mapper);

//        synchronized (cacheIdpMappers) {
            cacheIdpMappers.remove(realmModel.getId()+mapper.getId());
            removeIdpMapperSummary(realmModel.getId(), new IdentityProviderMapperModelSummary(mapper));
//        }

        cluster.notify(IdpMapperRemovedEvent.EVENT_NAME, new IdpMapperRemovedEvent(realmModel.getId(), mapper), true, ClusterProvider.DCNotify.ALL_DCS);
    }

    @Override
    public void updateIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapper) {
        if (mapper == null) return;
        getIdentityProviderDelegate().updateIdentityProviderMapper(realmModel, mapper);

//        synchronized (cacheIdpMappers) {
            cacheIdpMappers.put(realmModel.getId()+mapper.getId(), mapper);
            //no need to update the mapper summaries, since their attributes are defined upon creation and never change
//        }

        cluster.notify(IdpMapperUpdatedEvent.EVENT_NAME, new IdpMapperUpdatedEvent(realmModel.getId(), mapper), true, ClusterProvider.DCNotify.ALL_DCS);
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperById(RealmModel realmModel, String id) {
        IdentityProviderMapperModel idpMapper = cacheIdpMappers.get(realmModel.getId() + id);
        if (idpMapper == null) {
            idpMapper = getIdentityProviderDelegate().getIdentityProviderMapperById(realmModel, id);
            cacheIdpMappers.put(realmModel.getId() + id, idpMapper);
        }
        return idpMapper;
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(RealmModel realmModel, String alias, String name) {
        return idpMapperSummaries.get(realmModel.getId()).stream().filter(m -> m.getIdentityProviderAlias().equals(alias) && m.getName().equals(name)).map(m -> getIdentityProviderMapperById(realmModel, m.getId())).findFirst().orElse(null);
    }




    protected void addIdpSummary(String realmId, IdentityProviderModelSummary summary) {
        Set<IdentityProviderModelSummary> summaries = idpSummaries.get(realmId);
        if (summaries == null) {
            summaries = new HashSet<>();
            idpSummaries.put(realmId, summaries);
        }
        summaries.add(summary);
    }

    protected void removeIdpSummary(String realmId, IdentityProviderModelSummary summary) {
        Set<IdentityProviderModelSummary> summaries = idpSummaries.get(realmId);
        if (summaries == null) return;
        summaries.remove(summary);
    }

    protected void addIdpMapperSummary(String realmId, IdentityProviderMapperModelSummary summary) {
        Set<IdentityProviderMapperModelSummary> summaries = idpMapperSummaries.get(realmId);
        if (summaries == null) {
            summaries = new HashSet<>();
            idpMapperSummaries.put(realmId, summaries);
        }
        summaries.add(summary);
    }

    protected void removeIdpMapperSummary(String realmId, IdentityProviderMapperModelSummary summary) {
        Set<IdentityProviderMapperModelSummary> summaries = idpMapperSummaries.get(realmId);
        if (summaries == null) return;
        summaries.remove(summary);
    }


}
