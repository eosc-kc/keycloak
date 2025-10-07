package org.keycloak.services.clientpolicy.executor;


import java.util.List;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.keycloak.common.util.Base64Url;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.RPMetadata;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;
import org.keycloak.services.clientregistration.AbstractClientRegistrationProvider;
import org.keycloak.services.clientregistration.ClientRegistrationAuth;
import org.keycloak.util.JsonSerialization;

public class AutomaticClientRegistrationExecutor extends AbstractClientRegistrationProvider 
                                implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    private static final Logger logger = Logger.getLogger(AutomaticClientRegistrationExecutor.class);

    public AutomaticClientRegistrationExecutor(KeycloakSession session) {
        super(session);
        EventBuilder event = new EventBuilder(session.getContext().getRealm(), session);
        super.setEvent(event);
        super.setAuth(new ClientRegistrationAuth(session,this,event,"openid-connect"));
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {

        PreAuthorizationRequestContext paContext = null;
        switch (context.getEvent()) {
            case PRE_AUTHORIZATION_REQUEST:
                paContext = (PreAuthorizationRequestContext) context;
                String requestParam = paContext.getRequestParameters().getFirst(OIDCLoginProtocol.REQUEST_PARAM);

                String [] splits = requestParam.split("\\.");

                byte[] serializedRequestObject = Base64Url.decode(splits[1]);
                AuthorizationEndpointRequestObjectForAutomaticClientRegistration oidcRequest = null;
                try {
                    oidcRequest = JsonSerialization.readValue(serializedRequestObject, AuthorizationEndpointRequestObjectForAutomaticClientRegistration.class);
                } catch (Exception e) {
                    logger.debugv("exception on parsing request object");
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST_OBJECT, e.getMessage());
                }

                // getting  RP metadata with which client is created.
                RPMetadata rpMetadata = oidcRequest.getMetadata().getRelyingPartyMetadata();
                
                // some critical data for client creation is set from request object
                if (rpMetadata.getJwks() == null ) {
                    rpMetadata.setJwks(oidcRequest.getJwks());
                }
                if (rpMetadata.getClientId() == null ) {
                    rpMetadata.setClientId(oidcRequest.getClientId());
                }
                
                try{
                    createOidcClient(rpMetadata, session, oidcRequest.getExp());
                } catch (Exception e) {
                    logger.debugv("exception on creating client");
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST_OBJECT, e.getMessage());
                }
                break;
            default:
                break;
        }

    }

    @Override
    public String getProviderId() {
        return AutomaticClientRegistrationExecutorFactory.PROVIDER_ID;
    }


    public static class AuthorizationEndpointRequestObjectForAutomaticClientRegistration extends EntityStatement {

        @JsonProperty(OIDCLoginProtocol.CLIENT_ID_PARAM)
        String clientId;

        @JsonProperty(OIDCLoginProtocol.RESPONSE_TYPE_PARAM)
        String responseType;

        @JsonProperty(OIDCLoginProtocol.RESPONSE_MODE_PARAM)
        String responseMode;

        @JsonProperty(OIDCLoginProtocol.REDIRECT_URI_PARAM)
        String redirectUriParam;

        @JsonProperty(OIDCLoginProtocol.STATE_PARAM)
        String state;

        @JsonProperty(OIDCLoginProtocol.SCOPE_PARAM)
        String scope;

        @JsonProperty(OIDCLoginProtocol.LOGIN_HINT_PARAM)
        String loginHint;

        @JsonProperty(OIDCLoginProtocol.PROMPT_PARAM)
        String prompt;

        @JsonProperty(OIDCLoginProtocol.NONCE_PARAM)
        String nonce;

        Integer max_age;

        @JsonProperty(OIDCLoginProtocol.UI_LOCALES_PARAM)
        String uiLocales;

        @JsonProperty(OIDCLoginProtocol.ACR_PARAM)
        String acr;

        @JsonProperty(OAuth2Constants.DISPLAY)
        String display;

        @JsonProperty(OIDCLoginProtocol.CODE_CHALLENGE_PARAM)
        String codeChallenge;

        @JsonProperty(OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM)
        String codeChallengeMethod;

        @JsonProperty(OIDCLoginProtocol.DPOP_JKT)
        String dpopJkt;

        @JsonProperty(AdapterConstants.KC_IDP_HINT)
        String idpHint;

        @JsonProperty(Constants.KC_ACTION)
        String action;

        // OIDFED

        @JsonProperty("trust_chain")
        List<String> trustChain;

        public List<String> getTrustChain() {
            return trustChain;
        }

        public void setTrustChain(List<String> trustChain) {
            this.trustChain =  trustChain;
        }

        // CIBA
        
        @JsonProperty(CibaGrantType.CLIENT_NOTIFICATION_TOKEN)
        String clientNotificationToken;

        @JsonProperty(CibaGrantType.LOGIN_HINT_TOKEN)
        String loginHintToken;

        @JsonProperty(OIDCLoginProtocol.ID_TOKEN_HINT)
        String idTokenHint;

        @JsonProperty(CibaGrantType.USER_CODE)
        String userCode;

        @JsonProperty(CibaGrantType.BINDING_MESSAGE)
        String bindingMessage;

        Integer requested_expiry;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId =  clientId;
        }

        public String getResponseType() {
            return responseType;
        }

        public void setResponseType(String responseType) {
            this.responseType = responseType;
        }

        public String getResponseMode() {
            return responseMode;
        }

        public void setResponseMode(String responseMode) {
            this.responseMode = responseMode;
        }

        public String getRedirectUriParam() {
            return redirectUriParam;
        }

        public void setRedirectUriParam(String redirectUriParam) {
            this.redirectUriParam = redirectUriParam;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getLoginHint() {
            return loginHint;
        }

        public void setLoginHint(String loginHint) {
            this.loginHint = loginHint;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public String getNonce() {
            return nonce;
        }

        public void setNonce(String nonce) {
            this.nonce = nonce;
        }

        public Integer getMax_age() {
            return max_age;
        }

        public void setMax_age(Integer max_age) {
            this.max_age = max_age;
        }

        public String getUiLocales() {
            return uiLocales;
        }

        public void setUiLocales(String uiLocales) {
            this.uiLocales = uiLocales;
        }

        public String getAcr() {
            return acr;
        }

        public void setAcr(String acr) {
            this.acr = acr;
        }

        public String getCodeChallenge() {
            return codeChallenge;
        }

        public void setCodeChallenge(String codeChallenge) {
            this.codeChallenge = codeChallenge;
        }

        public String getCodeChallengeMethod() {
            return codeChallengeMethod;
        }

        public void setCodeChallengeMethod(String codeChallengeMethod) {
            this.codeChallengeMethod = codeChallengeMethod;
        }

        public String getDpopJkt() {
            return dpopJkt;
        }

        public void setDpopJkt(String dpopJkt) {
            this.dpopJkt = dpopJkt;
        }

        public String getDisplay() {
            return display;
        }

        public void setDisplay(String display) {
            this.display = display;
        }

        public String getIdpHint() {
            return idpHint;
        }

        public void setIdpHint(String idpHint) {
            this.idpHint = idpHint;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getClientNotificationToken() {
            return clientNotificationToken;
        }

        public void setClientNotificationToken(String clientNotificationToken) {
            this.clientNotificationToken = clientNotificationToken;
        }

        public String getLoginHintToken() {
            return loginHintToken;
        }

        public void setLoginHintToken(String loginHintToken) {
            this.loginHintToken = loginHintToken;
        }

        public String getIdTokenHint() {
            return idTokenHint;
        }

        public void setIdTokenHint(String idTokenHint) {
            this.idTokenHint = idTokenHint;
        }

        public String getBindingMessage() {
            return bindingMessage;
        }

        public void setBindingMessage(String bindingMessage) {
            this.bindingMessage = bindingMessage;
        }

        public String getUserCode() {
            return userCode;
        }

        public void setUserCode(String userCode) {
            this.userCode = userCode;
        }

        public Integer getRequested_expiry() {
            return requested_expiry;
        }

        public void setRequested_expiry(Integer requested_expiry) {
            this.requested_expiry = requested_expiry;
        }

    }
    
}
