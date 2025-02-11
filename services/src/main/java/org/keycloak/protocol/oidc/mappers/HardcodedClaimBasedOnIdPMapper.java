package org.keycloak.protocol.oidc.mappers;

import org.keycloak.events.Details;
import org.keycloak.models.Constants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import java.util.*;

public class HardcodedClaimBasedOnIdPMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;

        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.IDP_ALIAS);
        property.setLabel(ProtocolMapperUtils.IDP_ALIAS_LABEL);
        property.setHelpText(ProtocolMapperUtils.IDP_ALIAS_HELP_TEXT);
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

        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, HardcodedClaimBasedOnIdPMapper.class);

    }

    public static final String PROVIDER_ID = "oidc-hardcoded-claim-based-idp-mapper";

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Hardcoded Claim Based on IdP";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Hardcode an attribute into the SAML Assertion if at least one value of user attribute exists.";
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
            List<String> idpsAlias = Arrays.asList(mappingModel.getConfig().get(ProtocolMapperUtils.IDP_ALIAS).split(Constants.CFG_DELIMITER));
            String userIdP = userSession.getNote(Details.IDENTITY_PROVIDER);

            if (userIdP != null && idpsAlias.contains(userIdP)) {
                String attributeValue = mappingModel.getConfig().get(ProtocolMapperUtils.CLAIM_VALUE);
                if (attributeValue == null) return;
                OIDCAttributeMapperHelper.mapClaim(token, mappingModel, attributeValue);
            }

    }

    public static ProtocolMapperModel create(String name,
                                             String hardcodedName,
                                             String hardcodedValue, String idpAlias,
                                             boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, hardcodedName);
        config.put(ProtocolMapperUtils.CLAIM_VALUE, hardcodedValue);
        config.put(ProtocolMapperUtils.IDP_ALIAS, idpAlias);
        config.put(ProtocolMapperUtils.MULTIVALUED, "true");
        if (accessToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        if (introspectionEndpoint) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "true");
        mapper.setConfig(config);
        return mapper;
    }

}

