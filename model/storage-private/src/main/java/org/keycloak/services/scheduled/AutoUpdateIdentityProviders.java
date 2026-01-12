package org.keycloak.services.scheduled;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

public class AutoUpdateIdentityProviders implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(AutoUpdateIdentityProviders.class);

    protected final String alias;
    protected final String realmId;

    public AutoUpdateIdentityProviders(String alias, String realmId) {
        this.alias = alias;
        this.realmId = realmId;
    }

    @Override
    public void run(KeycloakSession session) {
        logger.info(" Updating identity provider with alias= " + alias + " in realm= " + realmId);
        RealmModel realm = session.realms().getRealm(realmId);
        if ( realm == null) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTask(realmId + "_AutoUpdateIdP_" + alias);
        }
        session.getContext().setRealm(realm);
        IdentityProviderModel idp = realm.getIdentityProviderByAlias(alias);
        if (idp == null || idp.getConfig().get(IdentityProviderModel.METADATA_URL) == null) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTask(realmId + "_AutoUpdateIdP_" + alias);
            throw new NotFoundException();
        }
        try {
            String file = session.getProvider(HttpClientProvider.class).getString(idp.getConfig().get(IdentityProviderModel.METADATA_URL));
            idp = getProviderFactoryById(session, idp.getProviderId()).parseConfig(session, file, idp);
            idp.getConfig().remove(IdentityProviderModel.LEGACY_HIDE_ON_LOGIN_ATTR);
            idp.getConfig().put(IdentityProviderModel.LAST_REFRESH_TIME, String.valueOf(Instant.now().toEpochMilli()));
            realm.updateIdentityProvider(idp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private IdentityProviderFactory<?> getProviderFactoryById(KeycloakSession session, String providerId) {
        return getProviderFactories(session)
                .filter(providerFactory -> Objects.equals(providerId, providerFactory.getId()))
                .map(IdentityProviderFactory.class::cast)
                .findFirst()
                .orElse(null);
    }

    private Stream<ProviderFactory> getProviderFactories(KeycloakSession session) {
        return Stream.concat(session.getKeycloakSessionFactory().getProviderFactoriesStream(IdentityProvider.class),
                session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class));
    }

}
