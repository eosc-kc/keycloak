package org.keycloak.userprofile.validator;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.userprofile.AttributeContext;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

import static org.keycloak.common.util.CollectionUtil.collectionEquals;
import static org.keycloak.validate.BuiltinValidators.notBlankValidator;

public class NoEditableAttributeValidator implements SimpleValidator, ConfiguredProvider {

    public static final String ID = "up-no-editable-attribute";

    private static final String DEFAULT_ERROR_MESSAGE = "error-user-attribute-no-editable";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {
        UserProfileAttributeValidationContext ac = (UserProfileAttributeValidationContext) context;
        AttributeContext attributeContext = ac.getAttributeContext();
        UserModel user = attributeContext.getUser();

        if (user == null) {
            return context;
        }

        KeycloakSession session = context.getSession();
        RealmModel realm = session.getContext().getRealm();
        UserModel dbUser = session.users().getUserById(realm, user.getId());
        if (dbUser == null) {
            return context;
        }

        Stream<String> rawValues = dbUser.getAttributeStream(inputHint).filter(Objects::nonNull);

        List<String> currentValue = rawValues.collect(Collectors.toList());
        List<String> values = (List<String>) input;

        if (!collectionEquals(currentValue, values)) {
            if (currentValue.isEmpty() && !notBlankValidator().validate(values).isValid()) {
                return context;
            }
            context.addError(new ValidationError(ID, inputHint, DEFAULT_ERROR_MESSAGE));
        }

        return context;
    }


    @Override
    public String getHelpText() {
        return "Do not allow this attribute value to be updated.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }
}
