package org.keycloak.services.scheduled;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.timer.ScheduledTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequiredActionsResetTask implements ScheduledTask {

    public static String INTERVAL_NUM = "reset_every";
    public static String UNIT_MULTIPLIER = "reset_every_multiplier";

    @Override
    public void run(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(realmModel -> {
            realmModel.getRequiredActionProvidersStream().forEach(requiredActionProviderModel -> {
                if(!requiredActionProviderModel.isEnabled() || requiredActionProviderModel.getConfig().get(INTERVAL_NUM)==null || requiredActionProviderModel.getConfig().get(UNIT_MULTIPLIER)==null)
                    return;
                if(requiredActionProviderModel.getProviderId().equals(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name())){
                    session.users().searchForUserStream(realmModel, Map.of(UserModel.INCLUDE_SERVICE_ACCOUNT, "false")).forEach(user -> {
                        if(expiredOrFirsttime(user, requiredActionProviderModel))
                            user.addRequiredAction(UserModel.RequiredAction.TERMS_AND_CONDITIONS.name());
                    });
                }
            });
        });
    }



    private boolean expiredOrFirsttime(UserModel user, RequiredActionProviderModel requiredActionModel){
        //user attribute in database is saved as lower case
        List<String> attrList = user.getAttributes().get(requiredActionModel.getProviderId().toLowerCase());
        if(attrList==null || attrList.isEmpty()) //means that this user has not performed this required action in the past
            return true;
        long numOfExpired = attrList.stream().filter(attrVal -> {
            try {
                long userLastAcceptTime = Long.parseLong(attrVal);
                long every = Long.parseLong(requiredActionModel.getConfig().get(INTERVAL_NUM));
                long multiplier = Long.parseLong(requiredActionModel.getConfig().get(UNIT_MULTIPLIER));
                long expiryOffset = every * multiplier;
                long epochSecondsNow = System.currentTimeMillis() / 1000L;
                if(epochSecondsNow > expiryOffset + userLastAcceptTime)
                    return true; //required action expired
                else
                    return false;
            }
            catch(NumberFormatException ex){
                return false;
            }
        }).count(); //this should be 0 or 1
        return numOfExpired > 0;
    }

}