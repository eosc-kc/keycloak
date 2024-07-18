package org.keycloak.services.util;

import com.google.common.collect.Streams;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.models.KeycloakSession;

import java.util.Objects;

public class ResourcesUtil {

    public static IdentityProviderFactory getProviderFactoryById(KeycloakSession session, String providerId) {
        return Streams.concat(session.getKeycloakSessionFactory().getProviderFactoriesStream(IdentityProvider.class),
                        session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class))
                .filter(providerFactory -> Objects.equals(providerId, providerFactory.getId()))
                .map(IdentityProviderFactory.class::cast)
                .findFirst()
                .orElse(null);
    }
}
