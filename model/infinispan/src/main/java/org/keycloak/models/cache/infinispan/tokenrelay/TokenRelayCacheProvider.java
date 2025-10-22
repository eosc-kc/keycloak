package org.keycloak.models.cache.infinispan.tokenrelay;

import org.infinispan.Cache;
import org.infinispan.commons.api.CacheContainerAdmin;
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

    private static final Long LIFESPAN_SEC = 60L;
    private static final Long MAXIDLE_SEC = 60L;

    private Cache<Object, Object> cache;

    public TokenRelayCacheProvider(KeycloakSession session){

        EmbeddedCacheManager cacheManager;
        String tokenRelayCacheName = TokenRelayCacheProviderFactory.ID;

        try {
            Field field = DefaultInfinispanConnectionProvider.class.getDeclaredField("cacheManager");
            field.setAccessible(true);
            InfinispanConnectionProvider ispnConnections = session.getProvider(InfinispanConnectionProvider.class);
            cacheManager = (EmbeddedCacheManager) field.get(ispnConnections);
        }
        catch(Exception ex){
            logger.error("Could not locate keycloak's cacheManager. Will initiate a local TokenRelayCache");
            Configuration c = new ConfigurationBuilder().simpleCache(true).expiration().lifespan(LIFESPAN_SEC, TimeUnit.SECONDS).maxIdle(MAXIDLE_SEC, TimeUnit.SECONDS).build();
            cacheManager = new DefaultCacheManager();
            cache = cacheManager.createCache(tokenRelayCacheName, c);
            return;
        }

        GlobalConfiguration globalConfiguration = cacheManager.getCacheManagerConfiguration();

        logger.info("Keycloak is clustered? -> " + globalConfiguration.isClustered());

        Configuration config;
        if(globalConfiguration.isClustered()) {
            logger.info("Initiating a cluster-aware TokenRelayCache");
            config = new ConfigurationBuilder()
                    .expiration().lifespan(LIFESPAN_SEC, TimeUnit.SECONDS).maxIdle(MAXIDLE_SEC, TimeUnit.SECONDS)
                    .clustering()
                    .cacheMode(CacheMode.REPL_SYNC)
                    .memory().storage(StorageType.HEAP)
                    .build();
            cache = cacheManager.administration()
                    .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
                    .createCache(tokenRelayCacheName, config);
        }
        else {
            logger.info("Initiating a local TokenRelayCache");
            config = new ConfigurationBuilder().simpleCache(true).expiration().lifespan(LIFESPAN_SEC, TimeUnit.SECONDS).maxIdle(MAXIDLE_SEC, TimeUnit.SECONDS).build();
            EmbeddedCacheManager ecm = new DefaultCacheManager();
            cache = ecm.createCache(tokenRelayCacheName, config);
        }

    }


    @Override
    public Object get(Object key) {
        return cache.get(key);
    }

    @Override
    public void put(Object key, Object obj) {
        cache.put(key, obj);
    }

    @Override
    public void close() {
        cache.shutdown();
    }

}
