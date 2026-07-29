package org.keycloak.scim.model.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.user.User;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfile;

import static org.keycloak.scim.resource.Scim.ENTERPRISE_USER_SCHEMA;
import static org.keycloak.scim.resource.Scim.USER_CORE_SCHEMA;
import static org.keycloak.userprofile.UserProfileUtil.isRootAttribute;

public class UserExtensionModelSchema extends AbstractUserModelSchema {

    public static final String KEYCLOAK_USER_SCHEMA = "urn:keycloak:params:scim:schemas:extension:realm:1.0:User";

    public UserExtensionModelSchema(KeycloakSession session) {
        this(session, KEYCLOAK_USER_SCHEMA);
    }

    public UserExtensionModelSchema(KeycloakSession session, String schema) {
        super(session, schema);
    }

    @Override
    public String getName() {
        return "RealmUser";
    }

    @Override
    public String getDescription() {
        return "Realm User";
    }

    @Override
    public boolean isCore() {
        return false;
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public boolean supports(Set<String> schemas) {
        for (Attribute<UserModel, User> value : getAttributes().values()) {
            String schema = value.getSchema();

            if (schema != null && schemas.contains(schema)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected Set<String> getModelAttributeNames() {
        if (isCore()) {
            return Set.of();
        }

        Set<String> names = new HashSet<>();
        UserProfile profile = getUserProfile();

        for (String name : profile.getAttributes().nameSet()) {
            if (isRootAttribute(name)) {
                continue;
            }

            AttributeMetadata metadata = profile.getAttributes().getMetadata(name);

            if (metadata == null || metadata.getAnnotations() == null) {
                continue;
            }

            List<String> scimNames = getScimAttributeValue(metadata.getAnnotations().get(ANNOTATION_SCIM_SCHEMA_ATTRIBUTE));

            if (scimNames == null || scimNames.isEmpty() || scimNames.stream().noneMatch(n -> n.contains(":"))) {
                continue;
            }

            names.add(name);
        }

        return names;
    }

    @Override
    protected Map<String, Attribute<UserModel, User>> getAttributeMappers() {
        Map<String, Attribute<UserModel, User>> mappers = new HashMap<>();
        Attributes attributes = getUserProfile().getAttributes();

        for (String name : getModelAttributeNames()) {
            AttributeMetadata metadata = attributes.getMetadata(name);

            if (metadata == null || metadata.getAnnotations() == null) {
                continue;
            }

            List<String> scimNames = getScimAttributeValue(metadata.getAnnotations().get(ANNOTATION_SCIM_SCHEMA_ATTRIBUTE));

            if (scimNames == null) {
                continue;
            }

            for (String scimName : scimNames) {
                if (hasSchema(scimName)) {
                    // Using name + "/" + scimName allows multiple Keycloak attributes to map to the same SCIM field.
                    // For PATCH, AbstractModelSchema will pick the first matched SCIM path it finds.
                    mappers.put(name + "/" + scimName, createCustomAttribute(name, scimName));
                }
            }
        }

        return mappers;
    }

    @Override
    protected Attribute<UserModel, User> getAttributeMapperByModelAttribute(String name) {
        UserProfile profile = getUserProfile();
        Attributes attributes = profile.getAttributes();
        AttributeMetadata metadata = attributes.getMetadata(name);

        if (metadata == null) {
            return null;
        }

        Map<String, Object> annotations = metadata.getAnnotations();

        if (annotations == null) {
            return null;
        }

        List<String> scimNames = getScimAttributeValue(annotations.get(ANNOTATION_SCIM_SCHEMA_ATTRIBUTE));

        if (scimNames == null) {
            return null;
        }

        for (String scimName : scimNames) {
            if (hasSchema(scimName)) {
                return createCustomAttribute(name, scimName);
            }
        }

        return null;
    }

    @Override
    protected boolean hasSchema(String attributeName) {
        String schema = Attribute.getSchema(attributeName);

        // it should be possible to query other schemas from the providers
        return schema != null && !List.of(USER_CORE_SCHEMA, ENTERPRISE_USER_SCHEMA).contains(schema);
    }

    private Attribute<UserModel,  User> createCustomAttribute(String boundModelName, Object scimName) {
        return Attribute.<UserModel, User>simple(scimName.toString())
                .modelAttributeResolver(attribute -> {
                    if (isCore()) {
                        return null;
                    }
                    return boundModelName;
                })
                .withSetters((model, name, value) -> {
                    if (isCore()) {
                        return;
                    }
                    if (getAttributeMapperByModelAttribute(name) == null) {
                        return;
                    }
                    if (value == null) {
                        model.removeAttribute(name);
                    } else {
                        model.setSingleAttribute(name, value.toString());
                    }
                }, (attribute, user, value) -> {
                    if (isCore()) {
                        return;
                    }

                    if (value == null || (value instanceof Collection && ((Collection<?>) value).isEmpty()) || (value instanceof String && ((String) value).isEmpty())) {
                        return;
                    }

                    String schema = attribute.getSchema();

                    if (schema == null) {
                        return;
                    }

                    String attributeName = attribute.getSimpleName();
                    Map<String, Object> extensions = user.getExtensions();

                    if (extensions == null) {
                        extensions = new HashMap<>();
                        user.setExtensions(extensions);
                    }

                    Map<String, Object> subAttributes = (Map<String, Object>) extensions.computeIfAbsent(schema, k -> new HashMap<>());

                    user.addSchema(schema);

                    int subSubAttribute = attributeName.indexOf('.');

                    if (subSubAttribute != -1) {
                        String parentAttributeName = attributeName.substring(0, subSubAttribute);
                        attributeName = attributeName.substring(parentAttributeName.length() + 1);

                        // Handle multivalued attributes annotated as "<parent>.value"
                        if ("value".equals(attributeName) && value instanceof Collection<?> values) {
                            List<Map<String, Object>> newValues = values.stream()
                                    .filter(Objects::nonNull)
                                    .map(v -> Map.<String, Object>of("value", v))
                                    .toList();

                            if (subAttributes.containsKey(parentAttributeName)) {
                                Object existing = subAttributes.get(parentAttributeName);
                                if (existing instanceof Collection) {
                                    List<Map<String, Object>> mergedValues = new ArrayList<>((Collection<Map<String, Object>>) existing);

                                    for (Map<String, Object> nv : newValues) {
                                        if (!mergedValues.contains(nv)) {
                                            mergedValues.add(nv);
                                        }
                                    }
                                    subAttributes.put(parentAttributeName, mergedValues);
                                } else {
                                    subAttributes.put(parentAttributeName, newValues);
                                }
                            } else {
                                subAttributes.put(parentAttributeName, newValues);
                            }
                            return;
                        }

                        subAttributes = (Map<String, Object>) subAttributes.computeIfAbsent(parentAttributeName, k -> new HashMap<>());

                    }

                    if (subAttributes.containsKey(attributeName)) {
                        Object existing = subAttributes.get(attributeName);
                        if (existing instanceof Collection<?> && value instanceof Collection<?>) {
                            List<Object> mergedValues = new ArrayList<>((Collection<?>) existing);

                            for (Object v : (Collection<?>) value) {
                                if (!mergedValues.contains(v)) {
                                    mergedValues.add(v);
                                }
                            }
                            subAttributes.put(attributeName, mergedValues);
                        } else {
                            subAttributes.put(attributeName, value);
                        }
                    } else {
                        subAttributes.put(attributeName, value);
                    }
                }).build().get(0);
    }
}
