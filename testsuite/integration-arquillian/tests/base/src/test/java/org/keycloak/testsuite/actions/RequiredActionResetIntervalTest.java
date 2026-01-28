/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.services.scheduled.RequiredActionsResetTask;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.TermsAndConditionsPage;
import org.keycloak.timer.TimerProvider;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RequiredActionResetIntervalTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected TermsAndConditionsPage termsPage;


    @Test
    public void testRequiredActionTnCIntervalReset() {

        String realmId = adminClient.realm("test").toRepresentation().getId();
        testingClient.server().run((session -> {
            //remove task for required actions
            TimerProvider timer = session.getProvider(TimerProvider.class);
            TimerProvider.TimerTaskContext context = timer.cancelTask("RequiredActionsResetTask_"+realmId);
        }));

        //ensure user has no pending required actions already
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        user.setRequiredActions(new ArrayList<>());
        adminClient.realm("test").users().get(user.getId()).update(user);

        //configure required action reset interval
        RequiredActionProviderRepresentation rep = adminClient.realm("test").flows().getRequiredAction(TermsAndConditions.PROVIDER_ID);
        rep.setEnabled(true);
        Map<String, String> config = new HashMap<>();
        config.put(RequiredActionsResetTask.INTERVAL_NUM, "1");
        config.put(RequiredActionsResetTask.UNIT_MULTIPLIER, "1");
        rep.setConfig(config);
        adminClient.realm("test").flows().updateRequiredAction(TermsAndConditions.PROVIDER_ID, rep);

        testingClient.server().run((session -> {
            //add again the scheduled task for required actions
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.schedule(new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), new RequiredActionsResetTask(realmId), 200), 200, "RequiredActionsResetTask_"+ realmId);
        }));


        sleep(30000);
        checkIfTermsAreReset();
        termsAndConditionsAccept("test-user@localhost");
    }

    private void checkIfTermsAreReset(){
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        boolean requiredActionIsReset = user.getRequiredActions().contains(TermsAndConditions.PROVIDER_ID);
        assertTrue(requiredActionIsReset);
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

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

}
