package org.keycloak.services.scheduled;


import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

import org.jboss.logging.Logger;

public class RequiredActionsResetTask implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(RequiredActionsResetTask.class);

    public static String INTERVAL_NUM = "reset_every";
    public static String UNIT_MULTIPLIER = "reset_every_multiplier";
    //use 3600 for hours, 86400 for days, 2592000 for months
    protected final String realmId;

    public RequiredActionsResetTask(String realmId) {
        this.realmId = realmId;
    }

    @Override
    public void run(KeycloakSession session) {
        logger.info(" Starting reset required action for realm " + realmId);
        RealmModel realm = session.realms().getRealm(realmId);
        TimerProvider timer = session.getProvider(TimerProvider.class);
        if (realm == null) {
            timer.cancelTaskAndNotify("RequiredActionsResetTask_" + realmId);
            return;
        }
        session.getContext().setRealm(realm);
        RequiredActionProviderModel requiredActionProviderModel = realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name());

        if (requiredActionProviderModel != null && requiredActionProviderModel.isEnabled() && requiredActionProviderModel.getConfig().get(INTERVAL_NUM) != null && !"0".equals(requiredActionProviderModel.getConfig().get(INTERVAL_NUM)) && requiredActionProviderModel.getConfig().get(UNIT_MULTIPLIER) != null) {
            session.users().searchUsersForRenewTermsAndConditions(realm, calculateExpirationTime(requiredActionProviderModel.getConfig())).forEach(user -> {
                    user.addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name());
            });
        }

    }

    private String calculateExpirationTime(Map<String, String> config) {
        return String.valueOf(System.currentTimeMillis() / 1000L - ( Long.valueOf(config.get(INTERVAL_NUM)) * getMultiplierFromUnit(config.get(UNIT_MULTIPLIER))));
    }

    private  long getMultiplierFromUnit(String unit) {
        if (unit == null) return 86400L;

        switch (unit.toLowerCase().trim()) {
            case "hours":
                return 3600L;
            case "months":
                return 2592000L;
            default:
                return 86400L;
        }
    }

}
