package org.keycloak.protocol.saml.mappers;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.events.Details;
import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.*;

public class HardcodedAttributeBasedOnIdPMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.ATTRIBUTE_VALUE);
        property.setLabel(ProtocolMapperUtils.ATTRIBUTE_CONDITIONAL_VALUES_LABEL);
        property.setHelpText(ProtocolMapperUtils.ATTRIBUTE_CONDITIONAL_VALUES_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.IDP_ALIAS);
        property.setLabel(ProtocolMapperUtils.IDP_ALIAS_LABEL);
        property.setHelpText(ProtocolMapperUtils.IDP_ALIAS_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        AttributeStatementHelper.setConfigProperties(configProperties);

    }

    public static final String PROVIDER_ID = "saml-hardcoded-attribute-based-idp-mapper";

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Hardcoded Attribute Based on IdP";
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Hardcode an attribute into the SAML Assertion if user is login with configured Identity Provider.";
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        List<String> idpsAlias = Arrays.asList(mappingModel.getConfig().get(ProtocolMapperUtils.IDP_ALIAS).split(Constants.CFG_DELIMITER));
        String userIdP = userSession.getNote(Details.IDENTITY_PROVIDER);

        if (userIdP != null && idpsAlias.contains(userIdP)) {
            List<String> attributeValues = Arrays.asList(mappingModel.getConfig().get(ProtocolMapperUtils.ATTRIBUTE_VALUE).split(Constants.CFG_DELIMITER));
            AttributeStatementHelper.addAttributes(attributeStatement, mappingModel, attributeValues);
        }

    }

    public static ProtocolMapperModel create(String name,
                                             String samlAttributeName, String nameFormat, String friendlyName, String idpAlias, String attributeValues) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, samlAttributeName);
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, nameFormat);
        config.put(AttributeStatementHelper.FRIENDLY_NAME, friendlyName);
        config.put(ProtocolMapperUtils.IDP_ALIAS, idpAlias);
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE_VALUES, attributeValues);
        mapper.setConfig(config);
        return mapper;
    }

}
