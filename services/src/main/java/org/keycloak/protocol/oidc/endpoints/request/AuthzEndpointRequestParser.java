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

package org.keycloak.protocol.oidc.endpoints.request;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.jose.JOSE;
import org.keycloak.jose.JOSEHeader;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AuthzEndpointRequestParser {

    private static final Logger logger = Logger.getLogger(AuthzEndpointRequestParser.class);

    /**
     * Max number of additional req params copied into client session note to prevent DoS attacks
     *
     */
    public static final int ADDITIONAL_REQ_PARAMS_MAX_MUMBER = 5;

    /**
     * Max size of additional req param value copied into client session note to prevent DoS attacks - params with longer value are ignored
     *
     */
    public static final int ADDITIONAL_REQ_PARAMS_MAX_SIZE = 2000;

    public static final String AUTHZ_REQUEST_OBJECT = "ParsedRequestObject";
    public static final String AUTHZ_REQUEST_OBJECT_ENCRYPTED = "EncryptedRequestObject";

    /** Set of known protocol GET params not to be stored into additionalReqParams} */
    public static final Set<String> KNOWN_REQ_PARAMS = new HashSet<>();
    static {
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.CLIENT_ID_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.STATE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.SCOPE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.PROMPT_PARAM);
        KNOWN_REQ_PARAMS.add(AdapterConstants.KC_IDP_HINT);
        KNOWN_REQ_PARAMS.add(Constants.KC_ACTION);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.NONCE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.MAX_AGE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.UI_LOCALES_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.REQUEST_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.REQUEST_URI_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.CLAIMS_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.ACR_PARAM);

        // https://tools.ietf.org/html/rfc7636#section-6.1
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.CODE_CHALLENGE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM);

        // Those are not OAuth/OIDC parameters, but they should never be added to the additionalRequestParameters
        KNOWN_REQ_PARAMS.add(OAuth2Constants.CLIENT_ASSERTION_TYPE);
        KNOWN_REQ_PARAMS.add(OAuth2Constants.CLIENT_ASSERTION);
        KNOWN_REQ_PARAMS.add(OAuth2Constants.CLIENT_SECRET);
        KNOWN_REQ_PARAMS.add(OAuth2Constants.ISSUER);
    }

    public void parseRequest(AuthorizationEndpointRequest request) {
        String clientId = getParameter(OIDCLoginProtocol.CLIENT_ID_PARAM);
        if (clientId != null && request.clientId != null && !request.clientId.equals(clientId)) {
            throw new IllegalArgumentException("The client_id parameter doesn't match the one from OIDC 'request' or 'request_uri'");
        }
        if (clientId != null) {
            request.clientId = clientId;
        }

        String responseType = getParameter(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        validateResponseTypeParameter(responseType, request);
        if (responseType != null) {
            request.responseType = responseType;
        }

        request.responseMode = replaceIfNotNull(request.responseMode, getParameter(OIDCLoginProtocol.RESPONSE_MODE_PARAM));
        request.redirectUriParam = replaceIfNotNull(request.redirectUriParam, getParameter(OIDCLoginProtocol.REDIRECT_URI_PARAM));
        request.state = replaceIfNotNull(request.state, getParameter(OIDCLoginProtocol.STATE_PARAM));
        request.scope = replaceIfNotNull(request.scope, getParameter(OIDCLoginProtocol.SCOPE_PARAM));
        request.loginHint = replaceIfNotNull(request.loginHint, getParameter(OIDCLoginProtocol.LOGIN_HINT_PARAM));
        request.prompt = replaceIfNotNull(request.prompt, getParameter(OIDCLoginProtocol.PROMPT_PARAM));
        request.idpHint = replaceIfNotNull(request.idpHint, getParameter(AdapterConstants.KC_IDP_HINT));
        request.action = replaceIfNotNull(request.action, getParameter(Constants.KC_ACTION));
        request.nonce = replaceIfNotNull(request.nonce, getParameter(OIDCLoginProtocol.NONCE_PARAM));
        request.maxAge = replaceIfNotNull(request.maxAge, getIntParameter(OIDCLoginProtocol.MAX_AGE_PARAM));
        request.claims = replaceIfNotNull(request.claims, getParameter(OIDCLoginProtocol.CLAIMS_PARAM));
        request.acr = replaceIfNotNull(request.acr, getParameter(OIDCLoginProtocol.ACR_PARAM));
        request.display = replaceIfNotNull(request.display, getParameter(OAuth2Constants.DISPLAY));
        request.uiLocales = replaceIfNotNull(request.uiLocales, getParameter(OAuth2Constants.UI_LOCALES_PARAM));

        // https://tools.ietf.org/html/rfc7636#section-6.1
        request.codeChallenge = replaceIfNotNull(request.codeChallenge, getParameter(OIDCLoginProtocol.CODE_CHALLENGE_PARAM));
        request.codeChallengeMethod = replaceIfNotNull(request.codeChallengeMethod, getParameter(OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM));

        //OpenId Federation iss
        request.iss = replaceIfNotNull(request.iss, getParameter(OAuth2Constants.ISSUER));

        extractAdditionalReqParams(request.additionalReqParams);
    }

    protected void validateResponseTypeParameter(String responseTypeParameter, AuthorizationEndpointRequest request) {
        if (responseTypeParameter != null && request.responseType != null && !request.responseType.equals(responseTypeParameter)) {
            logger.warnf("The response_type parameter doesn't match the one from OIDC 'request' or 'request_uri'");
            request.setInvalidRequestMessage("Parameter response_type does not match");
        }
    }

    protected void extractAdditionalReqParams(Map<String, String> additionalReqParams) {
        for (String paramName : keySet()) {
            if (!KNOWN_REQ_PARAMS.contains(paramName)) {
                String value = getParameter(paramName);
                if (value != null && value.trim().isEmpty()) {
                    value = null;
                }
                if (value != null && value.length() <= ADDITIONAL_REQ_PARAMS_MAX_SIZE) {
                    if (additionalReqParams.size() >= ADDITIONAL_REQ_PARAMS_MAX_MUMBER) {
                        logger.debug("Maximal number of additional OIDC params (" + ADDITIONAL_REQ_PARAMS_MAX_MUMBER + ") exceeded, ignoring rest of them!");
                        break;
                    }
                    additionalReqParams.put(paramName, value);
                } else {
                    logger.debug("OIDC Additional param " + paramName + " ignored because value is empty or longer than " + ADDITIONAL_REQ_PARAMS_MAX_SIZE);
                }
            }

        }
    }

    protected BiConsumer<JOSE, ClientModel> createRequestObjectValidator(KeycloakSession session) {
        return (jwt, clientModel) -> {
            if (jwt instanceof JWSInput) {
                JOSEHeader header = jwt.getHeader();
                String headerAlgorithm = header.getRawAlgorithm();

                if (headerAlgorithm == null) {
                    throw new RuntimeException("Request object signed algorithm not specified");
                }

                String requestedSignatureAlgorithm = OIDCAdvancedConfigWrapper.fromClientModel(clientModel)
                        .getRequestObjectSignatureAlg();

                if (requestedSignatureAlgorithm != null && !requestedSignatureAlgorithm.equals(headerAlgorithm)) {
                    throw new RuntimeException(
                            "Request object signed with different algorithm than client requested algorithm");
                }
            } else {
                String encryptionAlg = OIDCAdvancedConfigWrapper.fromClientModel(clientModel).getRequestObjectEncryptionAlg();

                if (encryptionAlg != null) {
                    if (!encryptionAlg.equals(jwt.getHeader().getRawAlgorithm())) {
                        throw new RuntimeException("Request object encrypted with different algorithm than client requested algorithm");
                    }
                }

                String encryptionEncAlg = OIDCAdvancedConfigWrapper.fromClientModel(clientModel).getRequestObjectEncryptionEnc();

                if (encryptionEncAlg != null) {
                    JWE jwe = (JWE) jwt;
                    JWEHeader header = (JWEHeader) jwe.getHeader();

                    if (!encryptionEncAlg.equals(header.getEncryptionAlgorithm())) {
                        throw new RuntimeException("Request object content encrypted with different algorithm than client requested algorithm");
                    }
                }

                session.setAttribute(AuthzEndpointRequestParser.AUTHZ_REQUEST_OBJECT_ENCRYPTED, jwt);
            }
        };
    }

    protected <T> T replaceIfNotNull(T previousVal, T newVal) {
        return newVal==null ? previousVal : newVal;
    }

    protected abstract String getParameter(String paramName);

    protected abstract Integer getIntParameter(String paramName);

    protected abstract Set<String> keySet();

}
