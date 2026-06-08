package org.keycloak.protocol.oidc.resourceindicators;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.token.TokenInterceptorException;
import org.keycloak.protocol.oidc.token.TokenPostProcessor;
import org.keycloak.protocol.oidc.token.TokenPostProcessorContext;

public class ResourceIndicatorsPostProcessor implements TokenPostProcessor {

    private final KeycloakSession session;
    public static final String RESOURCE_CHECK_IN_TOKEN_AUDIENCE = "resourceCheckInTokenAudience";

    public ResourceIndicatorsPostProcessor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void process(TokenPostProcessorContext context) {
        List<String> requestedResources = (List<String>) context.clientSessionCtx().getAttribute(OAuth2Constants.RESOURCE, List.class);
        if (requestedResources != null && requestedResources.stream().anyMatch( requestedResource -> !ResourceIndicatorValidation.isValidResourceIndicator(requestedResource))) {
            throw new TokenInterceptorException(OAuthErrorException.INVALID_TARGET, ResourceIndicatorConstants.ERROR_INVALID_RESOURCE);
        }

        String grantType = context.clientSessionCtx().getAttribute(Constants.GRANT_TYPE, String.class);

        boolean originalResourceParamRequired = false;
        List<String> originalResourceParams = null;
        if (OAuth2Constants.AUTHORIZATION_CODE.equals(grantType)) {
            originalResourceParams = context.code().getResources();
            originalResourceParamRequired = true;
        } else if (OAuth2Constants.REFRESH_TOKEN.equals(grantType)) {
            originalResourceParams = (List<String>) context.requestRefreshToken().getOtherClaims().get(OAuth2Constants.RESOURCE);
            originalResourceParamRequired = true;
        }

        if ((originalResourceParams == null || originalResourceParams.isEmpty()) && ( requestedResources == null || requestedResources.isEmpty()) ) {
            return;
        }

        //Keycloak upstream code logic
//        if (originalResourceParamRequired) {
//            if (originalResourceParams == null || originalResourceParams.isEmpty() ) {
//                throw new TokenInterceptorException(OAuthErrorException.INVALID_TARGET, ResourceIndicatorConstants.ERROR_NOT_MATCHING);
//            }
//
//            if (requestedResources == null || requestedResources.isEmpty()) {
//                requestedResources = originalResourceParams;
//            } else if (!CollectionUtils.isEqualCollection(requestedResources, originalResourceParams)){
//                throw new TokenInterceptorException(OAuthErrorException.INVALID_TARGET, ResourceIndicatorConstants.ERROR_NOT_MATCHING);
//            }
//        }

        if (originalResourceParamRequired && (requestedResources == null || requestedResources.isEmpty())) {
            requestedResources = originalResourceParams;
        }

        if (session.getContext().getRealm().getAttribute(RESOURCE_CHECK_IN_TOKEN_AUDIENCE, false)) {
            List<String> audienceToSetList = context.accessToken().getAudience() == null ? new ArrayList<>() :
                    requestedResources.stream().map(requestedResource -> {
                        if (isClientUrn(requestedResource)) {
                            return findAudienceByClientUrn(requestedResource, context.accessToken().getAudience());
                        } else {
                            return findAudienceByClientAttribute(requestedResource, context.accessToken().getAudience());
                        }
                    }).filter(Objects::nonNull).collect(Collectors.toList());

            if (audienceToSetList.isEmpty()) {
                throw new TokenInterceptorException(OAuthErrorException.INVALID_TARGET, ResourceIndicatorConstants.ERROR_INVALID_RESOURCE);
            }
            context.accessToken().audience(audienceToSetList.toArray(String[]::new));
        } else {
            context.accessToken().audience(requestedResources.toArray(String[]::new));
        }

        if (context.refreshToken() != null) {
            context.refreshToken().getOtherClaims().put(OAuth2Constants.RESOURCE, requestedResources);
        }

    }

    private boolean isClientUrn(String resource) {
        return resource.startsWith(ResourceIndicatorConstants.URN_CLIENT_PREFIX);
    }

    private String findAudienceByClientUrn(String resource, String[] audience) {
        String requestedClientId = resource.substring(ResourceIndicatorConstants.URN_CLIENT_PREFIX.length());
        return find(requestedClientId, audience);
    }

    private String findAudienceByClientAttribute(String resource, String[] audience) {
        for (String a : audience) {
            ClientModel client = session.clients().getClientByClientId(session.getContext().getRealm(), a);
            if (client != null) {
                String clientResourceUrl = client.getAttribute(ResourceIndicatorConstants.CLIENT_RESOURCE_URL_ATTRIBUTE);
                if (resource.equals(clientResourceUrl)) {
                    return resource;
                }
            }
        }
        return null;
    }

    private String find(String search, String[] array) {
        for (String a : array) {
            if (a.equals(search)) {
                return a;
            }
        }
        return null;
    }

}
