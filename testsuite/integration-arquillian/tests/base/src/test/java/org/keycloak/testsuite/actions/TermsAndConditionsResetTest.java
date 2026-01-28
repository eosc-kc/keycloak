package org.keycloak.testsuite.actions;

import java.util.List;

import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.TermsAndConditionsPage;
import org.keycloak.testsuite.util.UserBuilder;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TermsAndConditionsResetTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected TermsAndConditionsPage termsPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void addTermsAndConditionsRequiredActionAndSecondUser() {
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        UserBuilder.edit(user).requiredAction(TermsAndConditions.PROVIDER_ID);
        adminClient.realm("test").users().get(user.getId()).update(user);

        RequiredActionProviderRepresentation rep = adminClient.realm("test").flows().getRequiredAction(TermsAndConditions.PROVIDER_ID);
        rep.setEnabled(true);
        adminClient.realm("test").flows().updateRequiredAction(TermsAndConditions.PROVIDER_ID, rep);

        createUser("test","test-user2@localhost", "password",TermsAndConditions.PROVIDER_ID);
        UserRepresentation user2 = ActionUtil.findUserWithAdminClient(adminClient, "test-user2@localhost");
    }

    @Test
    public void termsAcceptedAndRefreshed() {

        termsAndConditionsAccept("test-user@localhost");
        deleteAllSessionsInRealm("test");
        termsAndConditionsAccept("test-user2@localhost");
        deleteAllSessionsInRealm("test");

        adminClient.realm("test").flows().resetRequiredAction(TermsAndConditions.PROVIDER_ID);

        termsAndConditionsAccept("test-user@localhost");
        deleteAllSessionsInRealm("test");
        termsAndConditionsAccept("test-user2@localhost");
        deleteAllSessionsInRealm("test");

    }

    private void termsAndConditionsAccept(String userName){
        loginPage.open();
        loginPage.login(userName, "password");
        assertTrue(termsPage.isCurrent());
        termsPage.acceptTerms();
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, userName);
        List<String> userReqAct = user.getRequiredActions();
        assertTrue(userReqAct!=null && userReqAct.isEmpty());
    }

}
