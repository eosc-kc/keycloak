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

package org.keycloak.broker.oidc.mappers;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.federation.OpenIdFederationIdentityProviderFactory;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAttributeMapper extends AbstractClaimMapper {

    public static final String[] COMPATIBLE_PROVIDERS = {KeycloakOIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID, OpenIdFederationIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String USER_ATTRIBUTE = "user.attribute";
    public static final String EMAIL = "email";
    public static final String EMAIL_VERIFIED = "emailVerified";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String RELATED_SCOPES = "related.scopes";
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    static {
        ProviderConfigProperty property;
        ProviderConfigProperty property1;
        property1 = new ProviderConfigProperty();
        property1.setName(CLAIM);
        property1.setLabel("Claim");
        property1.setHelpText("Name of claim to search for in token. You can reference nested claims using a '.', i.e. 'address.locality'. To use dot (.) literally, escape it with backslash (\\.)");
        property1.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property1);
        ProviderConfigProperty relatedScopesProperty = new ProviderConfigProperty();
        relatedScopesProperty.setName(RELATED_SCOPES);
        relatedScopesProperty.setLabel("Related scopes");
        relatedScopesProperty.setHelpText("Specify the scopes that can be used for requesting the claim when the 'Pass Scope' option is enabled. If none of the listed scopes are either set as default client scopes or present in the scope parameter from the initial application request, the user attribute will be preserved even if sync mode is set to 'FORCE'. If 'Pass Scope' is not enabled, any existing user attribute values will be removed if the corresponding claim is missing from the Identity Provider response.");
        relatedScopesProperty.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        relatedScopesProperty.setStringify(Boolean.TRUE);
        relatedScopesProperty.setDefaultValue("");
        configProperties.add(relatedScopesProperty);
        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText("User attribute name to store claim.  Use email, lastName, and firstName to map to those predefined user properties.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "oidc-user-attribute-idp-mapper";

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Attribute Importer";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        if(StringUtil.isNullOrEmpty(attribute)){
            return;
        }
        Object value = getClaimValue(mapperModel, context);
        if (EMAIL_VERIFIED.equalsIgnoreCase(attribute)) {
            boolean verified = value == null ? false : Boolean.valueOf(value.toString());
            context.setEmailVerified(verified);
        } else {
            List<String> values = toList(value);

            if (EMAIL.equalsIgnoreCase(attribute)) {
                setIfNotEmpty(context::setEmail, values);
            } else if (FIRST_NAME.equalsIgnoreCase(attribute)) {
                setIfNotEmpty(context::setFirstName, values);
            } else if (LAST_NAME.equalsIgnoreCase(attribute)) {
                setIfNotEmpty(context::setLastName, values);
            }  else {
                List<String> valuesToString = values.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .collect(Collectors.toList());

                context.setUserAttribute(attribute, valuesToString);
            }
        }
    }

    private void setIfNotEmpty(Consumer<String> consumer, List<String> values) {
        if (values != null && !values.isEmpty()) {
            consumer.accept(values.get(0));
        }
    }

    private List<String> toList(Object value) {
        List<Object> values = (value instanceof List)
                ? (List) value
                : Collections.singletonList(value);
        return values.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        if(StringUtil.isNullOrEmpty(attribute)){
            return;
        }
        Object value = getClaimValue(mapperModel, context);
        List<String> values = toList(value);
        if (EMAIL.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(user::setEmail, values);
        } else if (FIRST_NAME.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(user::setFirstName, values);
        } else if (LAST_NAME.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(user::setLastName, values);
        } else if (EMAIL_VERIFIED.equalsIgnoreCase(attribute)) {
            Object emailValue = getClaimValue(context, EMAIL);
            if (values != null && ! values.isEmpty() && emailValue != null && user.getEmail().equals((String)emailValue ))
                user.setEmailVerified(Boolean.valueOf(values.get(0)));
        } else {
            List<String> current = user.getAttributeStream(attribute).collect(Collectors.toList());
            if (!CollectionUtil.collectionEquals(values, current) ) {
                if (values.isEmpty()) {
                    Boolean passScope = Boolean.valueOf(context.getIdpConfig().getConfig().get(OAuth2IdentityProviderConfig.PASS_SCOPE));
                    String relatedScopesStr = mapperModel.getConfig().get(RELATED_SCOPES);
                    if (passScope && relatedScopesStr != null) {
                        List<String> relatedScopes = Arrays.asList(relatedScopesStr.split(Constants.CFG_DELIMITER));
                        Set<String> scopeSet =  new HashSet(context.getAuthenticationSession().getClient().getClientScopes(true).keySet());
                        String scopeParameter = context.getAuthenticationSession().getClientNote(OIDCLoginProtocol.SCOPE_PARAM);
                        if (scopeParameter != null && !scopeParameter.isEmpty())
                            scopeSet.addAll(Arrays.stream(scopeParameter.split(" ")).map(x -> x.split(":")[0]).collect(Collectors.toSet()));
                        //remove attribute only if at least one related scopes is either default client scope either exist in request parameter (taking into account dynamic scopes)
                       if (relatedScopes.stream().anyMatch(x -> scopeSet.contains(x)))
                           user.removeAttribute(attribute);
                    } else {
                        user.removeAttribute(attribute);
                    }
                } else {
                    user.setAttribute(attribute, values);
                }
            } else if (values.isEmpty() ) {
                user.removeAttribute(attribute);
            }
        }
    }

    @Override
    public String getHelpText() {
        return "Import declared claim if it exists in ID, access token or the claim set returned by the user profile endpoint into the specified user property or attribute.";
    }

}
