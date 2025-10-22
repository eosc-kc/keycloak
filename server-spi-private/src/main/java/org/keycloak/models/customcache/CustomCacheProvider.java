package org.keycloak.models.customcache;

import org.keycloak.provider.Provider;

public interface CustomCacheProvider extends Provider {

    public Object get(Object key);
    public void put(Object key, Object obj);

}
