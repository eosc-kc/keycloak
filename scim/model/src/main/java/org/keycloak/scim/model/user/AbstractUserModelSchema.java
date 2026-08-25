package org.keycloak.scim.model.user;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.schema.AbstractModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.user.User;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;

import static java.util.Optional.ofNullable;

import static org.keycloak.scim.resource.schema.attribute.Attribute.getSchema;

public abstract class AbstractUserModelSchema extends AbstractModelSchema<UserModel ,User> {

    public static final String ANNOTATION_SCIM_SCHEMA_ATTRIBUTE = "kc.scim.schema.attribute";
    private final KeycloakSession session;

    public AbstractUserModelSchema(KeycloakSession session, String name) {
        super(name);
        this.session = session;
    }

    @Override
    protected Set<String> getModelAttributeNames() {
        UserProfile profile = getUserProfile();
        Attributes attributes = profile.getAttributes();
        Set<String> names = new HashSet<>(attributes.nameSet());

        names.add(UserModel.ENABLED);
        names.add("groups");

        return names;
    }

    @Override
    protected String getAttributeSchemaName(String name) {
        List<String> schemas = getAttributeSchemaNames(name);

        if (schemas == null || schemas.isEmpty()) {
            return null;
        }

        // For CRUD/PATCH, the first configured SCIM attribute is enough
        return schemas.get(0);
    }

    protected List<String> getAttributeSchemaNames(String name) {
        if ("groups".equals(name)) {
            return List.of(name);
        }

        return getScimAttributeValue(getAttributeAnnotations(name).get(ANNOTATION_SCIM_SCHEMA_ATTRIBUTE));
    }

    @Override
    protected Object getAttributeValue(UserModel model, String name) {
        if (UserModel.ENABLED.equals(name)) {
            return String.valueOf(model.isEnabled());
        }
        if ("groups".equals(name)) {
            return model.getGroupsStream().toList();
        }
        UserProfile profile = session.getProvider(UserProfileProvider.class).create(UserProfileContext.SCIM, model);
        Attributes attributes = profile.getAttributes();
        AttributeMetadata metadata = attributes.getMetadata(name);

        //support multivalued user attribute to scim as array
        if (metadata != null && metadata.isMultivalued()) {
            return attributes.get(name);
        }

        return attributes.getFirst(name);
    }

    private Map<String, Object> getAttributeAnnotations(String name) {
        AttributeMetadata metadata = getProfileAttributes().getMetadata(name);

        if (metadata == null) {
            return Map.of();
        }

        return ofNullable(metadata.getAnnotations()).orElse(Map.of());
    }

    private Attributes getProfileAttributes() {
        UserProfile profile = session.getProvider(UserProfileProvider.class).create(UserProfileContext.SCIM, Map.of());
        return profile.getAttributes();
    }

    protected String createModelAttributeResolver(Attribute<UserModel, User> attribute) {
        for (String name : getModelAttributeNames()) {
            List<String> scimNames = getAttributeSchemaNames(name);

            if (scimNames != null && scimNames.stream().anyMatch(scimName -> hasPath(attribute, scimName))) {
                return name;
            }
        }

        return null;
    }

    protected boolean hasSchema(String attributeName) {
        return getId().equals(getSchema(attributeName));
    }

    protected UserProfile getUserProfile() {
        return session.getProvider(UserProfileProvider.class).create(UserProfileContext.SCIM, Map.of());
    }

    protected List<String> getScimAttributeValue(Object value) {
        if (value instanceof String stringValue) {
            return List.of(stringValue);
        }
        if (value instanceof List<?> listValue) {
            return (List<String>) listValue;
        }
        return null;
    }
}
