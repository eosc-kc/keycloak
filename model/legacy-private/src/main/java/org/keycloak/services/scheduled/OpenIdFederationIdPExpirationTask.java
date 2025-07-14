package org.keycloak.services.scheduled;

import org.jboss.logging.Logger;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

public class OpenIdFederationIdPExpirationTask implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(OpenIdFederationIdPExpirationTask.class);

    protected final String alias;
    protected final String realmId;

    public OpenIdFederationIdPExpirationTask(String alias, String realmId) {
        this.alias = alias;
        this.realmId = realmId;
    }

    @Override
    public void run(KeycloakSession session) {
        logger.info(" OpenId Federation IdP with alias= " + alias + " has expired.");
        RealmModel realm = session.realms().getRealm(realmId);
        if ( realm == null) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTask("OpenIdFederationIdPExpirationTask_" + alias);
            return;
        }
        IdentityProviderModel idp = realm.getIdentityProviderByAlias(alias);
        if (idp == null || !"openid-federation".equals(idp.getProviderId()) ) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTask("OpenIdFederationIdPExpirationTask_" + alias);
        } else {
            //TODO retry for updating IdP
            idp.setEnabled(false);
            realm.updateIdentityProvider(idp);
        }
    }
}


