package org.keycloak.protocol.oidc.mappers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ResourceIndicatorMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, TokenIntrospectionTokenMapper, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "oidc-resource-indicator-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    private static final Logger logger = Logger.getLogger(ResourceIndicatorMapper.class);

    static {
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, ResourceIndicatorMapper.class);

        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.DEFAULT_AUD_VALUE);
        property.setLabel(ProtocolMapperUtils.DEFAULT_AUD_VALUE_LABEL);
        property.setHelpText(ProtocolMapperUtils.DEFAULT_AUD_VALUE_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Resource Indicators";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Adds requested OAuth2 Resource Indicators to audience claim.";
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.RESOURCE_INDICATOR);
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        if (clientSessionCtx == null || clientSessionCtx.getClientSession() == null) {
            return;
        }
        String resourceInAuthorizationRequest = clientSessionCtx.getClientSession().getNote(OAuth2Constants.RESOURCE);
        String defaultAud = mappingModel.getConfig().get(ProtocolMapperUtils.DEFAULT_AUD_VALUE);
        if (resourceInAuthorizationRequest != null && !resourceInAuthorizationRequest.isBlank()) {
            try {
                List<String> resources = JsonSerialization.readValue(resourceInAuthorizationRequest, new TypeReference<List<String>>() {});
                resources.forEach(resource -> {
                    logger.debugv(" mapper: resource in authorization request = {0}", resource);
                    token.addAudience(resource);
                });
            } catch (IOException e) {
                logger.warnf("problem decoding resource parameter {0} during mapper use",resourceInAuthorizationRequest);
            }
        } else if (defaultAud != null && !defaultAud.isEmpty()) {
            token.addAudience(defaultAud);
        }
    }

    public static ProtocolMapperModel create(String name, boolean accessToken, boolean introspectionEndpoint) {
        return create(name, null, accessToken, introspectionEndpoint);
    }

    public static ProtocolMapperModel create(String name, String defaultAud, boolean accessToken, boolean introspectionEndpoint) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(ProtocolMapperUtils.DEFAULT_AUD_VALUE, defaultAud);
        if (accessToken) {
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        }
        if (introspectionEndpoint) {
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "true");
        }
        mapper.setConfig(config);
        return mapper;
    }
}
