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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Parse the parameters from OIDC "request" object
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthzEndpointRequestObjectParser extends AuthzEndpointRequestParser {

    private final JsonNode requestParams;

    public AuthzEndpointRequestObjectParser(KeycloakSession session, String requestObject, ClientModel client) {
        super(session);
        this.requestParams = session.tokens().decodeClientJWT(requestObject, client, createRequestObjectValidator(session), JsonNode.class);

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
