package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.Constants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import java.util.*;

public class HardcodedClaimBasedOnAttributeMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.USER_ATTRIBUTE);
        property.setLabel(ProtocolMapperUtils.USER_MODEL_ATTRIBUTE_LABEL);
        property.setHelpText(ProtocolMapperUtils.USER_MODEL_CONDITIONAL_ATTRIBUTE_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.USER_ATTRIBUTE_VALUES);
        property.setLabel(ProtocolMapperUtils.USER_MODEL_CONDITIONAL_VALUES_LABEL);
        property.setHelpText(ProtocolMapperUtils.USER_MODEL_CONDITIONAL_VALUES_HELP_TEXT);
        property.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        property.setStringify(Boolean.TRUE);
        property.setDefaultValue("");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.MULTIVALUED);
        property.setLabel(ProtocolMapperUtils.MULTIVALUED_LABEL);
        property.setHelpText(ProtocolMapperUtils.MULTIVALUED_HELP_TEXT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.CLAIM_VALUE);
        property.setLabel(ProtocolMapperUtils.CLAIM_VALUE_LABEL);
        property.setHelpText(ProtocolMapperUtils.CONDITIONAL_CLAIM_VALUE_HELP_TEXT);
        property.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        property.setStringify(Boolean.TRUE);
        property.setDefaultValue("");
        configProperties.add(property);

        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, HardcodedClaimBasedOnAttributeMapper.class);

    }

    public static final String PROVIDER_ID = "oidc-hardcoded-claim-based-attribute-mapper";

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Hardcoded Claim Based on User Attribute";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Hardcode a claim into the token if user is login with configured Identity Provider.";
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
            String userAttribute = mappingModel.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE);
            List<String> possibleAttributeValues = Arrays.asList(mappingModel.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE_VALUES).split(Constants.CFG_DELIMITER));
            Collection<String> userAttributeValues = KeycloakModelUtils.resolveAttribute(userSession.getUser(), userAttribute , false);

            if (userAttributeValues.stream().anyMatch(possibleAttributeValues::contains)) {
                List<String> attributeValues = Arrays.asList(mappingModel.getConfig().get(ProtocolMapperUtils.CLAIM_VALUE).split(Constants.CFG_DELIMITER));
                OIDCAttributeMapperHelper.mapClaim(token, mappingModel, attributeValues.size() ==1 ? attributeValues.get(0) : attributeValues);
            }
    }

    public static ProtocolMapperModel create(String name,
                                             String hardcodedName,
                                             String hardcodedValue, String attribute,String attributeValues,
                                             boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, hardcodedName);
        config.put(ProtocolMapperUtils.CLAIM_VALUE, hardcodedValue);
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE, attribute);
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE_VALUES, attributeValues);
        config.put(ProtocolMapperUtils.MULTIVALUED, "true");
        if (accessToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        if (introspectionEndpoint) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "true");
        mapper.setConfig(config);
        return mapper;
    }

}


