package org.keycloak.userprofile.validator;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.validation.Validation;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidationResult;
import org.keycloak.validate.ValidatorConfig;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DuplicateAttributeValidator implements SimpleValidator, ConfiguredProvider {

    public static final String ID = "up-duplicate-attribute";
    public static final String DEFAULT_ERROR_MESSAGE = "error-user-attribute-unique";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        List<String> values = (List<String>) input;

        if (values.isEmpty()) {
            return context;
        }

        String value = values.get(0);

        if (Validation.isBlank(value) || values.size() >1)
            return context;

        KeycloakSession session = context.getSession();
        RealmModel realm = session.getContext().getRealm();
        UserModel user = UserProfileAttributeValidationContext.from(context).getAttributeContext().getUser();
        if (user == null) {
            return context;
        }
        //get user with same user attribute - exclude current user if he/she have been created
        List<UserModel> usersWithSame = session.users().searchForUserStream(realm, Map.of(inputHint, value, UserModel.EXACT, Boolean.TRUE.toString())).filter(x -> !x.getId().equals(user.getId()))
                .collect(Collectors.toList());

        if (!usersWithSame.isEmpty()) {
            context.addError(new ValidationError(ID, inputHint, DEFAULT_ERROR_MESSAGE));
        }

        return context;
    }

    @Override
    public ValidationResult validateConfig(KeycloakSession session, ValidatorConfig config) {
        return ValidationResult.OK;
    }

    @Override
    public String getHelpText() {
        return "Check for unique attribute existence. Applicable only to single value attributes.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

}
