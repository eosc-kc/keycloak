package org.keycloak.services.scheduled;

import java.net.URI;
import java.time.Instant;

import jakarta.ws.rs.core.UriInfo;

import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.EnvKeycloakUriInfo;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.trustchain.TrustChainProcessor;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

public class OpenIdFederationIdPExpirationTask implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(OpenIdFederationIdPExpirationTask.class);

    public static final String KEYCLOAK_URL = "keycloakUrl";
    protected final String alias;
    protected final String realmId;

    public OpenIdFederationIdPExpirationTask(String alias, String realmId) {
        this.alias = alias;
        this.realmId = realmId;
    }

    @Override
    public void run(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm(realmId);
        if ( realm == null) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTask("OpenIdFederationIdPExpirationTask_" + alias);
            return;
        }
        session.getContext().setRealm(realm);
        IdentityProviderModel idp = session.identityProviders().getByAlias(alias);
        if (idp != null && "openid-federation".equals(idp.getProviderId()) && idp.getConfig().get(OIDCConfigAttributes.EXPIRATION_TIME) != null && Instant.now().getEpochSecond() >= Long.parseLong(idp.getConfig().get(OIDCConfigAttributes.EXPIRATION_TIME))) {
            logger.info(" OpenId Federation IdP with alias= " + alias + " has expired.");
            TrustChainProcessor trustChainProcessor = session.getProvider(TrustChainProcessor.class, "openid-federation");
            try {
                UriInfo fixedUriInfo = new EnvKeycloakUriInfo(URI.create(realm.getAttribute(KEYCLOAK_URL)));
                session.getContext().setRealm(realm);
                idp = trustChainProcessor.updateIdP(idp, realm, fixedUriInfo, fixedUriInfo);
            } catch (Exception e) {
                logger.warn("Error during updating OPenId Federation Identity Provider", e);
                idp.setEnabled(false);
                EventBuilder event = new EventBuilder(realm, session,  new DummyClientConnection("127.0.0.1"))
                        .event(EventType.FEDERATION_EXPIRATION)
                        .detail("type", "identity-provider")
                        .detail("alias", alias);
                event.success();
            }
            realm.updateIdentityProvider(idp);
        }
    }
}
