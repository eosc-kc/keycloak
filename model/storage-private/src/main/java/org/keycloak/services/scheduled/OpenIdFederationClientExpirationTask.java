package org.keycloak.services.scheduled;

import java.time.Instant;

import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;


public class OpenIdFederationClientExpirationTask implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(OpenIdFederationClientExpirationTask.class);
    public static final String TASK_NAME = "OpenIdFederationClientExpirationTask_";

    protected final String id;
    protected final String realmId;

    public OpenIdFederationClientExpirationTask(String id, String realmId) {
        this.id = id;
        this.realmId = realmId;
    }

    @Override
    public void run(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm(realmId);
        if ( realm == null) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTask(OpenIdFederationClientExpirationTask.TASK_NAME + id);
            return;
        }
        ClientModel client = session.clients().getClientById(realm, id);
        if (client != null && client.getAttribute(OIDCConfigAttributes.EXPIRATION_TIME) != null && Instant.now().getEpochSecond() >= Long.parseLong(client.getAttribute(OIDCConfigAttributes.EXPIRATION_TIME))) {
            logger.info(" OpenId Federation Client with id= " + id + " has expired.");
            client.setEnabled(false);
            client.updateClient();
            EventBuilder event = new EventBuilder(realm, session,  new DummyClientConnection("127.0.0.1"))
                    .event(EventType.FEDERATION_EXPIRATION)
                    .client(client)
                    .detail("type", "client");
            event.success();
        }
    }
}
