package org.keycloak.protocol.oidc.federation;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthzEndpointRequestParser;

import java.util.HashSet;
import java.util.Set;

public class OpenIdFederationAuthzEndpointRequestObjectParser extends AuthzEndpointRequestParser {

    private final JsonNode requestParams;

    public OpenIdFederationAuthzEndpointRequestObjectParser(KeycloakSession session, String requestObject) {
        super(session);
        this.requestParams = session.tokens().decode(requestObject, JsonNode.class);

        if (this.requestParams == null) {
            throw new RuntimeException("Failed to verify signature on 'request' object");
        }

        if (requestParams.has(OIDCLoginProtocol.REQUEST_URI_PARAM)) {
            throw new RuntimeException("The request_uri claim should not be set in the request object");
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

