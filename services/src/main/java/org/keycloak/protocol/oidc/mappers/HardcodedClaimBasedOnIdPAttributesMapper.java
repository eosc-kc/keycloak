package org.keycloak.protocol.oidc.mappers;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.events.Details;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.DefaultTokenExchangeProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HardcodedClaimBasedOnIdPAttributesMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper {

    private static final Logger logger = Logger.getLogger(HardcodedClaimBasedOnIdPAttributesMapper.class);
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
        property.setName(ProtocolMapperUtils.IDP_ATTRIBUTE_NAME2);
        property.setLabel(ProtocolMapperUtils.IDP_ATTRIBUTE_NAME2_LABEL);
        property.setHelpText(ProtocolMapperUtils.IDP_ATTRIBUTE_NAME2_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.IDP_ATTRIBUTE_VALUES2);
        property.setLabel(ProtocolMapperUtils.IDP_ATTRIBUTE_VALUES2_LABEL);
        property.setHelpText(ProtocolMapperUtils.IDP_ATTRIBUTE_VALUES2_HELP_TEXT);
        property.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        property.setStringify(Boolean.TRUE);
        property.setDefaultValue("");
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

    public static final String PROVIDER_ID = "oidc-hardcoded-attribute-based-idp-attribute-mapper";

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Hardcoded Claim Based on IdP Attribute";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Hardcode a claim into the token if the user Identity Provider has all declared attributes.";
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        String idpAlias = userSession.getNote(Details.IDENTITY_PROVIDER);
        IdentityProviderModel idp = userSession.getRealm().getIdentityProviderByAlias(idpAlias);
        String entityAttributesJson = idp.getConfig().get(SAMLIdentityProviderConfig.ENTITY_ATTRIBUTES);

        if (entityAttributesJson != null) {
            try {
                List<SAMLIdentityProviderConfig.EntityAttributes> idpEntityAttributes = JsonSerialization.readValue(entityAttributesJson, new TypeReference<List<SAMLIdentityProviderConfig.EntityAttributes>>() {});

                List<String> possibleAttributeValues = Arrays.asList(mappingModel.getConfig().get(ProtocolMapperUtils.IDP_ATTRIBUTE_VALUES).split(Constants.CFG_DELIMITER));
                SAMLIdentityProviderConfig.EntityAttributes idpEntityAttribute = idpEntityAttributes == null ? null : idpEntityAttributes.stream().filter(x -> mappingModel.getConfig().get(ProtocolMapperUtils.IDP_ATTRIBUTE_NAME).equals(x.getName())).findFirst().orElse(null);

                boolean secondAttribute = false;
                List<String> possibleAttributeValues2 = new ArrayList<>();
                SAMLIdentityProviderConfig.EntityAttributes idpEntityAttribute2 = null;
                if (idpEntityAttributes != null && mappingModel.getConfig().get(ProtocolMapperUtils.IDP_ATTRIBUTE_NAME2) != null){
                    secondAttribute = true;
                    possibleAttributeValues2 = Arrays.asList(mappingModel.getConfig().get(ProtocolMapperUtils.IDP_ATTRIBUTE_VALUES).split(Constants.CFG_DELIMITER));
                    idpEntityAttribute2 = idpEntityAttributes.stream().filter(x -> mappingModel.getConfig().get(ProtocolMapperUtils.IDP_ATTRIBUTE_NAME).equals(x.getName())).findFirst().orElse(null);
                }

                if (idpEntityAttribute != null && possibleAttributeValues.stream().allMatch(x -> idpEntityAttribute.getValues().contains(x)) && (!secondAttribute || (idpEntityAttribute2 != null && possibleAttributeValues2.stream().allMatch(x -> idpEntityAttribute.getValues().contains(x))))) {
                    String attributeValue = mappingModel.getConfig().get(ProtocolMapperUtils.CLAIM_VALUE);
                    if (attributeValue == null) return;
                    OIDCAttributeMapperHelper.mapClaim(token, mappingModel, attributeValue);
                }
            } catch (IOException e) {
                logger.warn("problem executing HardcodedAttributeBasedOnIdPAttributesMapper");
                e.printStackTrace();
            }
        }
    }

    public static ProtocolMapperModel create(String name,
                                             String hardcodedName,
                                             String hardcodedValue, String idpAttributesName,String idpAttributesValues,
                                             boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, hardcodedName);
        config.put(ProtocolMapperUtils.CLAIM_VALUE, hardcodedValue);
        config.put(ProtocolMapperUtils.IDP_ATTRIBUTE_NAME, idpAttributesName);
        config.put(ProtocolMapperUtils.IDP_ATTRIBUTE_VALUES, idpAttributesValues);
        config.put(ProtocolMapperUtils.MULTIVALUED, "true");
        if (accessToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        if (introspectionEndpoint) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "true");
        mapper.setConfig(config);
        return mapper;
    }

}


