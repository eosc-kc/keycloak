package org.keycloak.protocol.oidc;

import org.keycloak.provider.Provider;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface ProxiedTokenIntrospectionTranslationsProvider extends Provider {

    /**
     * Translates claims within the proxied token introspection JSON response.
     *
     * @param introspectionResponse mutable Jackson ObjectNode of the response
     */
    void translate(ObjectNode introspectionResponse);
}
