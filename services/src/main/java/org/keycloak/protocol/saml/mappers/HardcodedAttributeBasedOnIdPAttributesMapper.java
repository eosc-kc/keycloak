package org.keycloak.protocol.saml.mappers;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HardcodedAttributeBasedOnIdPAttributesMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {
    private static final Logger logger = Logger.getLogger(HardcodedAttributeBasedOnIdPAttributesMapper.class);
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.IDP_ATTRIBUTE_NAME);
        property.setLabel(ProtocolMapperUtils.IDP_ATTRIBUTE_NAME_LABEL);
        property.setHelpText(ProtocolMapperUtils.IDP_ATTRIBUTE_NAME_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.IDP_ATTRIBUTE_VALUES);
        property.setLabel(ProtocolMapperUtils.IDP_ATTRIBUTE_VALUES_LABEL);
        property.setHelpText(ProtocolMapperUtils.IDP_ATTRIBUTE_VALUES_HELP_TEXT);
        property.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        property.setStringify(Boolean.TRUE);
        property.setDefaultValue("");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.ATTRIBUTE_VALUE);
        property.setLabel(ProtocolMapperUtils.ATTRIBUTE_CONDITIONAL_VALUES_LABEL);
        property.setHelpText(ProtocolMapperUtils.ATTRIBUTE_CONDITIONAL_VALUES_HELP_TEXT);
        property.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        property.setStringify(Boolean.TRUE);
        property.setDefaultValue("");
        configProperties.add(property);

        AttributeStatementHelper.setConfigProperties(configProperties);

    }

    public static final String PROVIDER_ID = "saml-hardcoded-attribute-based-idp-attribute-mapper";

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Hardcoded Attribute Based on IdP Attribute";
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Hardcode an attribute into the SAML Assertion if the user Identity Provider has all declared attributes.";
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        String idpAlias = userSession.getNote(Details.IDENTITY_PROVIDER);
        IdentityProviderModel idp = userSession.getRealm().getIdentityProviderByAlias(idpAlias);
        String entityAttributesJson = idp.getConfig().get(SAMLIdentityProviderConfig.ENTITY_ATTRIBUTES);

        if (entityAttributesJson != null) {
            try {
                List<SAMLIdentityProviderConfig.EntityAttributes> idpEntityAttributes = JsonSerialization.readValue(entityAttributesJson, new TypeReference<List<SAMLIdentityProviderConfig.EntityAttributes>>() {});

                List<String> possibleAttributeValues = Arrays.asList(mappingModel.getConfig().get(ProtocolMapperUtils.IDP_ATTRIBUTE_VALUES).split(Constants.CFG_DELIMITER));
                SAMLIdentityProviderConfig.EntityAttributes idpEntityAttribute = idpEntityAttributes == null ? null : idpEntityAttributes.stream().filter(x -> mappingModel.getConfig().get(ProtocolMapperUtils.IDP_ATTRIBUTE_NAME).equals(x.getName())).findFirst().orElse(null);

                if (idpEntityAttribute != null && possibleAttributeValues.stream().allMatch(x -> idpEntityAttribute.getValues().contains(x))) {
                    List<String> attributeValues = Arrays.asList(mappingModel.getConfig().get(ProtocolMapperUtils.ATTRIBUTE_VALUE).split(Constants.CFG_DELIMITER));
                    AttributeStatementHelper.addAttributes(attributeStatement, mappingModel, attributeValues);
                }
            } catch (IOException e) {
                logger.warn("problem executing HardcodedAttributeBasedOnIdPAttributesMapper");
                e.printStackTrace();
            }
        }
    }

    public static ProtocolMapperModel create(String name,
                                             String samlAttributeName, String nameFormat, String friendlyName, String idpAttributesName,String idpAttributesValues, String attributeValues) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, samlAttributeName);
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, nameFormat);
        config.put(AttributeStatementHelper.FRIENDLY_NAME, friendlyName);
        config.put(ProtocolMapperUtils.IDP_ATTRIBUTE_NAME, idpAttributesName);
        config.put(ProtocolMapperUtils.IDP_ATTRIBUTE_VALUES, idpAttributesValues);
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE_VALUES, attributeValues);
        mapper.setConfig(config);
        return mapper;
    }

}
