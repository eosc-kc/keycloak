package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Arrays;
import java.util.List;

import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_2FA;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_BASIC_AUTH;
public class MigrateTo22_0_0_1_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("22.0.0-1.0");
    public static final List<String> DEFAULT_CLAIMS_SUPPORTED= Arrays.asList("aud", "sub", "iss", IDToken.AUTH_TIME, IDToken.NAME, IDToken.GIVEN_NAME, IDToken.FAMILY_NAME, IDToken.PREFERRED_USERNAME, IDToken.EMAIL, IDToken.ACR);

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
        realm.setClaimsSupported(DEFAULT_CLAIMS_SUPPORTED);
    }


    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
