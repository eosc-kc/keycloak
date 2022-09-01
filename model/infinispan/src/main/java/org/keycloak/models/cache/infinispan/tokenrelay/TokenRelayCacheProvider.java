package org.keycloak.models.cache.infinispan.tokenrelay;

import org.infinispan.Cache;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.DefaultInfinispanConnectionProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.customcache.CustomCacheProvider;



import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

public class TokenRelayCacheProvider implements CustomCacheProvider {

    private static final Logger logger = Logger.getLogger(TokenRelayCacheProvider.class);

    private static final Long LIFESPAN_SEC = 30L;
    private static final Long MAXIDLE_SEC = 30L;

    private static Cache<Object, Object> CACHE;

    public TokenRelayCacheProvider(KeycloakSession session) {

        if(CACHE != null) {
            logger.info("TokenRelayCache has already been initiated. Using that instance!");
            return;
        }

        EmbeddedCacheManager cacheManager;
        String tokenRelayCacheName = TokenRelayCacheProviderFactory.ID;

        try {
            Field field = DefaultInfinispanConnectionProvider.class.getDeclaredField("cacheManager");
            field.setAccessible(true);
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            cacheManager = (EmbeddedCacheManager) field.get(ispnConnections);
        }
        catch(Exception ex) {
            logger.error("Could not locate keycloak's cacheManager (this might fix with a restart of this node's keycloak service). Will initiate a local TokenRelayCache. If running in cluster mode, please restart this node to increase efficiency!");
            Configuration c = new ConfigurationBuilder().simpleCache(true).expiration().lifespan(LIFESPAN_SEC, TimeUnit.SECONDS).maxIdle(MAXIDLE_SEC, TimeUnit.SECONDS).build();
            cacheManager = new DefaultCacheManager();
            CACHE = cacheManager.createCache(tokenRelayCacheName, c);
            return;
        }

        //if reached at this point, then we can use the keycloak's cache manager (initiated during the bootstrap) to initiate a distributed cache

        GlobalConfiguration globalConfiguration = cacheManager.getCacheManagerConfiguration();

        logger.info("Keycloak is clustered? -> " + globalConfiguration.isClustered());

        Configuration config;
        /* DISABLING THE DISTRIBUTED CACHE FOR NOW. PLEASE, MAKE SURE THE DISTRIBUTED CACHE IS INITIALIZED WITH THE getOrCreateCache() */
//        if(globalConfiguration.isClustered()) {
//            logger.info("Initiating a cluster-aware TokenRelayCache");
//            config = new ConfigurationBuilder()
//                    .expiration().lifespan(LIFESPAN_SEC, TimeUnit.SECONDS).maxIdle(MAXIDLE_SEC, TimeUnit.SECONDS)
//                    .clustering()
//                    .cacheMode(CacheMode.REPL_SYNC)
//                    .memory().storage(StorageType.HEAP)
//                    .encoding().mediaType(MediaType.APPLICATION_SERIALIZED_OBJECT_TYPE)
//                    .build();
//            CACHE = cacheManager.administration()
//                    .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
//                    .getOrCreateCache(tokenRelayCacheName, config);
//        }
//        else {
            logger.info("Initiating a local TokenRelayCache");
            config = new ConfigurationBuilder().simpleCache(true).expiration().lifespan(LIFESPAN_SEC, TimeUnit.SECONDS).maxIdle(MAXIDLE_SEC, TimeUnit.SECONDS).build();
            CACHE = cacheManager.createCache(tokenRelayCacheName, config);
//        }

    }


    @Override
    public Object get(Object key) {
        return CACHE.get(key);
    }

    @Override
    public void put(Object key, Object obj) {
        CACHE.put(key, obj);
    }

    @Override
    public void close() {
        CACHE.shutdown();
    }

}
