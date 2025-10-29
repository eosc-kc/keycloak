package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_2FA;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_BASIC_AUTH;
public class MigrateTo26_4_1_0_1 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("26.4.1-0.1");

    @Override
    public void migrate(KeycloakSession session) {

        session.realms().getRealmsStream().forEach(this::addExtraAccountRoles);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        addExtraAccountRoles(realm);
    }

    private void addExtraAccountRoles(RealmModel realm) {
        ClientModel accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (accountClient != null && accountClient.getRole(MANAGE_ACCOUNT_BASIC_AUTH) == null) {
            RoleModel viewAppRole = accountClient.addRole(MANAGE_ACCOUNT_BASIC_AUTH);
            viewAppRole.setDescription("${role_" + MANAGE_ACCOUNT_BASIC_AUTH + "}");
        }
        if (accountClient != null && accountClient.getRole(MANAGE_ACCOUNT_2FA) == null) {
            RoleModel manageAccount2fa = accountClient.addRole(MANAGE_ACCOUNT_2FA);
            manageAccount2fa.setDescription("${role_" + MANAGE_ACCOUNT_2FA + "}");
        }
    }


    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
