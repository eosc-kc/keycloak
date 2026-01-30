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

package org.keycloak.authentication.requiredactions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.messages.Messages;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TermsAndConditions implements RequiredActionProvider, RequiredActionFactory {
    public static final String PROVIDER_ID = UserModel.RequiredAction.TERMS_AND_CONDITIONS.name();
    public static final String USER_ATTRIBUTE = "terms_and_conditions";
    public static String INTERVAL_NUM = "reset_every";
    public static String UNIT_MULTIPLIER = "reset_every_multiplier";

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


    @Override
    public void evaluateTriggers(RequiredActionContext context) {

    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        List<ProviderConfigProperty> config = new ArrayList<>(RequiredActionFactory.MAX_AUTH_AGE_CONFIG_PROPERTIES);

        config.addAll(ProviderConfigurationBuilder.create()
                .property()
                .name(INTERVAL_NUM)
                .label("resetInterval")
                .helpText("resetIntervalHelp")
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .add()
                .property()
                .name(UNIT_MULTIPLIER)
                .label("resetIntervalMultiplier")
                .helpText("resetIntervalMultiplierHelp")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(List.of("days","hours","months"))
                .add()
                .build());

        return config;
    }


    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = context.form()
            .setAttribute("user", context.getAuthenticationSession().getAuthenticatedUser())
            .createForm("terms.ftl");
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // Keycloak 21.0.0 changed the user attribute name from lowercase to uppercase
        // this change was reverted, but it is still possible some attributes created
        // in Keycloak 21.0.0 will be present in the database, we need to remove it too.
        // See https://github.com/keycloak/keycloak/issues/17277 for more details
        context.getUser().removeAttribute(USER_ATTRIBUTE.toUpperCase());

        if (context.getHttpRequest().getDecodedFormParameters().containsKey("cancel")) {
            context.getUser().removeAttribute(USER_ATTRIBUTE);
            context.failure(Messages.TERMS_AND_CONDITIONS_DECLINED);
            return;
        }

        context.getUser().setAttribute(USER_ATTRIBUTE, Arrays.asList(Integer.toString(Time.currentTime())));

        context.success();
    }

    @Override
    public String getDisplayText() {
        return "Terms and Conditions";
    }

    @Override
    public void close() {

    }
}
