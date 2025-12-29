package org.keycloak.services.scheduled;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.trustchain.TrustChainProcessor;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

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
        session.getContext().setRealm(realm);
        IdentityProviderModel idp = session.identityProviders().getByAlias(alias);
        if (idp == null || !"openid-federation".equals(idp.getProviderId()) ) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTaskAndNotify("OpenIdFederationIdPExpirationTask_" + alias);
        } else {
            TrustChainProcessor trustChainProcessor = session.getProvider(TrustChainProcessor.class, "openid-federation");
            trustChainProcessor.updateIdP(idp, realm);
        }
    }
}
