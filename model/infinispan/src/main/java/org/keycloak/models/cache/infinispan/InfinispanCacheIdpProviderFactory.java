package org.keycloak.models.cache.infinispan;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.*;
import org.keycloak.models.cache.CacheIdpProviderFactoryI;
import org.keycloak.models.cache.CacheIdpProviderI;
import org.keycloak.models.cache.infinispan.events.*;

import java.util.Set;

public class InfinispanCacheIdpProviderFactory implements CacheIdpProviderFactoryI {

    private static final Logger log = Logger.getLogger(InfinispanCacheIdpProviderFactory.class);
    
    public static final String IDP_AND_MAPPERS_CLEAR_CACHE_EVENT = "IDP_AND_MAPPERS_CLEAR_CACHE_EVENT";

    protected Cache<String, IdentityProviderModel> idpCache;
    protected Cache<String, IdentityProviderMapperModel> idpMappersCache;
    
	protected volatile IdpCacheProvider idpCacheProvider;


	@Override
	public CacheIdpProviderI create(KeycloakSession session) {
		lazyInit(session);
		idpCacheProvider = new IdpCacheProvider(idpCache, idpMappersCache, session);
		return idpCacheProvider;
	}

	private void lazyInit(KeycloakSession session) {
        if (idpCache == null) {
            synchronized (this) {
                if (idpCache == null) {
                	idpCache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.IDP_CACHE_NAME);
					ClusterProvider cluster = session.getProvider(ClusterProvider.class);

					cluster.registerListener(IdpAddedEvent.EVENT_NAME, (ClusterEvent event) -> {
						IdpAddedEvent idpAddedEvent = (IdpAddedEvent) event;
						String realmId = idpAddedEvent.getRealmId();
						String idpId = idpAddedEvent.getIdpId();
						String idpAlias = idpAddedEvent.getIdpAlias();
						String idpProviderId = idpAddedEvent.getIdpProviderId();
						idpCacheProvider.addIdpSummary(realmId, new IdentityProviderModelSummary(idpId, idpAlias, idpProviderId));
					});

					cluster.registerListener(IdpUpdatedEvent.EVENT_NAME, (ClusterEvent event) -> {
						IdpUpdatedEvent idpUpdatedEvent = (IdpUpdatedEvent) event;
						String realmId = idpUpdatedEvent.getRealmId();
						String idpId = idpUpdatedEvent.getIdpId();
						String idpAlias = idpUpdatedEvent.getIdpAlias();
						String idpProviderId = idpUpdatedEvent.getIdpProviderId();
						idpCache.remove(realmId + idpId);
						idpCacheProvider.addIdpSummary(realmId, new IdentityProviderModelSummary(idpId, idpAlias, idpProviderId));
					});

					cluster.registerListener(IdpRemovedEvent.EVENT_NAME, (ClusterEvent event) -> {
						IdpRemovedEvent idpRemovedEvent = (IdpRemovedEvent) event;
						String realmId = idpRemovedEvent.getRealmId();
						String idpId = idpRemovedEvent.getIdpId();
						idpCache.remove(realmId + idpId);
						idpCacheProvider.removeIdpSummary(realmId, new IdentityProviderModelSummary(idpId, null, null));
					});

					cluster.registerListener(InfinispanCacheIdpProviderFactory.IDP_AND_MAPPERS_CLEAR_CACHE_EVENT, (ClusterEvent event) -> {
						idpCacheProvider.clearAllCaches();
					});
                }
            }
        }
        if (idpMappersCache == null) {
            synchronized (this) {
                if (idpMappersCache == null) {
                    idpMappersCache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.IDP_MAPPERS_CACHE_NAME);
					ClusterProvider cluster = session.getProvider(ClusterProvider.class);

					cluster.registerListener(IdpMapperAddedEvent.EVENT_NAME, (ClusterEvent event) -> {
						IdpMapperAddedEvent idpMapperAddedEvent = (IdpMapperAddedEvent) event;
						String realmId = idpMapperAddedEvent.getRealmId();
						String mapperId = idpMapperAddedEvent.getMapperId();
						String idpAlias = idpMapperAddedEvent.getIdpAlias();
						String mapperName = idpMapperAddedEvent.getMapperName();
						idpCacheProvider.addIdpMapperSummary(realmId, new IdentityProviderMapperModelSummary(mapperId, mapperName, idpAlias));
					});

					cluster.registerListener(IdpMapperUpdatedEvent.EVENT_NAME, (ClusterEvent event) -> {
						IdpMapperUpdatedEvent idpMapperUpdatedEvent = (IdpMapperUpdatedEvent) event;
						String realmId = idpMapperUpdatedEvent.getRealmId();
						String mapperId = idpMapperUpdatedEvent.getMapperId();
						String idpAlias = idpMapperUpdatedEvent.getIdpAlias();
						String mapperName = idpMapperUpdatedEvent.getMapperName();
						idpMappersCache.remove(realmId + mapperId);
						idpCacheProvider.addIdpMapperSummary(realmId, new IdentityProviderMapperModelSummary(mapperId, mapperName, idpAlias));
					});

					cluster.registerListener(IdpMapperRemovedEvent.EVENT_NAME, (ClusterEvent event) -> {
						IdpMapperRemovedEvent idpMapperRemovedEvent = (IdpMapperRemovedEvent) event;
						String realmId = idpMapperRemovedEvent.getRealmId();
						String mapperId = idpMapperRemovedEvent.getMapperId();
						idpMappersCache.remove(realmId + mapperId);
						idpCacheProvider.removeIdpMapperSummary(realmId, new IdentityProviderMapperModelSummary(mapperId, null, null));
					});


                }
            }
        }
    }


	@Override
	public void init(Scope config) {
		
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		
	}

	@Override
	public void close() {
		
	}

	@Override
	public String getId() {
		return "default";
	}

}
