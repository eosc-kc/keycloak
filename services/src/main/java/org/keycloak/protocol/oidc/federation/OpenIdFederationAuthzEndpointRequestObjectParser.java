package org.keycloak.protocol.oidc.federation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.common.util.Time;
import org.keycloak.jose.JOSE;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthzEndpointRequestParser;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.databind.JsonNode;

public class OpenIdFederationAuthzEndpointRequestObjectParser extends AuthzEndpointRequestParser {

    private static final String REQUEST_OBJECT_JTI_KEY_PREFIX = "oidfed.jti.";

    private final JsonNode requestParams;

    public OpenIdFederationAuthzEndpointRequestObjectParser(KeycloakSession session, String requestObject, ClientModel client) {
        
        super(session);
        session.removeAttribute(AUTHZ_REQUEST_OBJECT_ENCRYPTED);
        session.removeAttribute(AUTHZ_REQUEST_OBJECT_INNER_JWS);

        this.requestParams = session.tokens().decodeClientJWT(requestObject, client, createRequestObjectValidator(session), JsonNode.class);

        if (this.requestParams == null) {
            throw new RuntimeException("Failed to verify signature on 'request' object");
        }

        // OpenID Federation §12.1.1.1: "The JWT MUST be signed and MAY be encrypted."
        // Encryption alone (JWE wrapping raw JSON) is not enough; a nested signed JWS is required.
        if (session.getAttribute(AUTHZ_REQUEST_OBJECT_ENCRYPTED) != null
                && session.getAttribute(AUTHZ_REQUEST_OBJECT_INNER_JWS) == null) {
            throw new RuntimeException("Encrypted request object for OpenID Federation automatic authorisation must contain a nested signed JWT");
        }

        if (requestParams.has(OIDCLoginProtocol.REQUEST_URI_PARAM)) {
            throw new RuntimeException("The request_uri claim should not be set in the request object");
        }

        if (requestParams.has("sub")  || ! requestParams.has("jti") || !requestParams.has("exp")
                || !requestParams.has("client_id") || !requestParams.has("iss") || !requestParams.has("aud")
                || !client.getClientId().equals(requestParams.get("client_id").asText()) || !client.getClientId().equals(requestParams.get("iss").asText())
                || !Urls.realmIssuer(session.getContext().getUri(UrlType.FRONTEND).getBaseUri(), session.getContext().getRealm().getName()).equals(requestParams.get("aud").asText())) {
            throw new RuntimeException(Messages.OPENID_FEDERATION_AUTOMATIC_FALSE_REQUEST_OBJECT);
        }

        // OpenID Federation §12.1.1.1: OP MUST verify exp and treat jti as single-use
        long exp = requestParams.get("exp").asLong();
        long now = Time.currentTime();
        String jti = requestParams.get("jti").asText();
        if (now >= exp || StringUtil.isBlank(jti)) {
            throw new RuntimeException(Messages.OPENID_FEDERATION_AUTOMATIC_FALSE_REQUEST_OBJECT);
        }

        //jti unique check
        if (!session.singleUseObjects().putIfAbsent(REQUEST_OBJECT_JTI_KEY_PREFIX + client.getClientId() + "." + jti, exp - now)) {
            throw new RuntimeException(Messages.OPENID_FEDERATION_AUTOMATIC_FALSE_REQUEST_OBJECT);
        }

        session.setAttribute(AuthzEndpointRequestParser.AUTHZ_REQUEST_OBJECT, requestParams);
    }

    /**
     * OpenID Federation §12.1.1.1 requires a signed Request Object. Reject {@code alg=none}
     * regardless of whether the client configured {@code request_object_signing_alg}.
     * Also records that a nested/inner JWS was seen so encrypted-only (unsigned) payloads can be rejected.
     */
    @Override
    protected BiConsumer<JOSE, ClientModel> createRequestObjectValidator(KeycloakSession session) {
        BiConsumer<JOSE, ClientModel> parentValidator = super.createRequestObjectValidator(session);
        return (jwt, clientModel) -> {
            if (jwt instanceof JWSInput) {
                String headerAlgorithm = jwt.getHeader().getRawAlgorithm();
                if (headerAlgorithm == null || Algorithm.none.name().equals(headerAlgorithm)) {
                    throw new RuntimeException("Request object must be signed for OpenID Federation automatic registration");
                }
                session.setAttribute(AUTHZ_REQUEST_OBJECT_INNER_JWS, Boolean.TRUE);
            }
            parentValidator.accept(jwt, clientModel);
        };
    }

    @Override
    protected String getParameter(String paramName) {
        JsonNode val = this.requestParams.get(paramName);
        if (val == null) {
            return null;
        } else if (val.isValueNode()) {
            return val.asText();
        } else {
            return val.toString();
        }
    }

    @Override
    protected List<String> getParameterAsList(String paramName) {
        JsonNode val = this.requestParams.get(paramName);
        if (val == null || val.isEmpty()) {
            return null;
        } else if (val.isArray()) {
            List<String> parameters = StreamSupport.stream(val.spliterator(), false)
                    .map(JsonNode::asText)
                    .collect(Collectors.toList());
            return parameters.isEmpty() ? null : parameters;
        } else if (val.isValueNode()) {
            List<String> parameters = new ArrayList<>();
            parameters.add(val.asText());
            return parameters;
        } else {
            List<String> parameters = new ArrayList<>();
            parameters.add(val.toString());
            return parameters;
        }
    }

    @Override
    protected Integer getIntParameter(String paramName) {
        Object val = this.requestParams.get(paramName);
        return val==null ? null : Integer.valueOf(getParameter(paramName));
    }

    @Override
    protected Set<String> keySet() {
        HashSet<String> keys = new HashSet<>();
        requestParams.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

}
