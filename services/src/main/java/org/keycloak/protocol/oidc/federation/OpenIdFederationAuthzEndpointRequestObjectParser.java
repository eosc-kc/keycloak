package org.keycloak.protocol.oidc.federation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthzEndpointRequestParser;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.urls.UrlType;

import com.fasterxml.jackson.databind.JsonNode;

public class OpenIdFederationAuthzEndpointRequestObjectParser extends AuthzEndpointRequestParser {

    private final JsonNode requestParams;

    public OpenIdFederationAuthzEndpointRequestObjectParser(KeycloakSession session, String requestObject, ClientModel client) {
        
        super(session);
        this.requestParams = session.tokens().decodeClientJWT(requestObject, client, createRequestObjectValidator(session), JsonNode.class);

        if (this.requestParams == null) {
            throw new RuntimeException("Failed to verify signature on 'request' object");
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

        session.setAttribute(AuthzEndpointRequestParser.AUTHZ_REQUEST_OBJECT, requestParams);
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
