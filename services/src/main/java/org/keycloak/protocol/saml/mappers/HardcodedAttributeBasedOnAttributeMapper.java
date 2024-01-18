package org.keycloak.protocol.saml.mappers;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import java.util.*;

public class HardcodedAttributeBasedOnAttributeMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.ATTRIBUTE_VALUE);
        property.setLabel(ProtocolMapperUtils.ATTRIBUTE_CONDITIONAL_VALUES_LABEL);
        property.setHelpText(ProtocolMapperUtils.ATTRIBUTE_CONDITIONAL_VALUES_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

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
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.MULTIVALUED);
        property.setLabel(ProtocolMapperUtils.MULTIVALUED_LABEL);
        property.setHelpText(ProtocolMapperUtils.MULTIVALUED_HELP_TEXT);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperties.add(property);
        AttributeStatementHelper.setConfigProperties(configProperties);

    }

    public static final String PROVIDER_ID = "saml-hardcoded-claim-based-attribute-mapper";

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Hardcoded Attribute Based on User Attribute";
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Hardcode a claim into the token if user is login with configured Identity Provider.";
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        String userAttribute = mappingModel.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE);
        List<String> possibleAttributeValues = Arrays.asList(mappingModel.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE_VALUES).split(Constants.CFG_DELIMITER));
        Collection<String> userAttributeValues = KeycloakModelUtils.resolveAttribute(userSession.getUser(), userAttribute, false);

        if (userAttributeValues.stream().anyMatch(possibleAttributeValues::contains)) {
            List<String> attributeValues = Arrays.asList(mappingModel.getConfig().get(ProtocolMapperUtils.ATTRIBUTE_VALUE).split(Constants.CFG_DELIMITER));
            AttributeStatementHelper.addAttributes(attributeStatement, mappingModel, attributeValues);
        }
    }

    public static ProtocolMapperModel create(String name,
                                             String samlAttributeName, String nameFormat, String friendlyName, String attribute, String values, String attributeValues) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, samlAttributeName);
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, nameFormat);
        config.put(AttributeStatementHelper.FRIENDLY_NAME, friendlyName);
        config.put(ProtocolMapperUtils.ATTRIBUTE_VALUE, values);
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE, attribute);
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE_VALUES, attributeValues);
        mapper.setConfig(config);
        return mapper;
    }

}

