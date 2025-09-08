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
package org.keycloak.services.clientregistration.oidc;

import java.net.URI;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.clientregistration.AbstractClientRegistrationProvider;
import org.keycloak.services.clientregistration.ClientRegistrationException;
import org.keycloak.services.clientregistration.ErrorCodes;
import org.keycloak.services.cors.Cors;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientRegistrationProvider extends AbstractClientRegistrationProvider {

    public OIDCClientRegistrationProvider(KeycloakSession session) {
        super(session);
    }

    @OPTIONS
    public Response preflight() {
        return Cors.builder().auth().preflight().allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").add(Response.ok());
    }

    @OPTIONS
    @Path("{clientId}")
    public Response preflightClient() {
        return preflight();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOIDC(OIDCClientRepresentation clientOIDC) {
        Cors cors = cors();
        if (clientOIDC.getClientId() != null) {
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier included", Response.Status.BAD_REQUEST);
        }

        try {
            ClientRepresentation client = createOidcClient(clientOIDC, session, null);
            URI uri = getRegistrationClientUri(client.getClientId());
            clientOIDC = DescriptionConverter.toExternalResponse(session, client, uri, OIDCClientRepresentation.class, clientOIDC.getScope()!=null && clientOIDC.getScope().contains(OAuth2Constants.SCOPE_OPENID));
            event.detail(Details.GRANTED_CLIENT, clientOIDC.getScope());
            clientOIDC.setClientIdIssuedAt(Time.currentTime());
            return cors.add(Response.created(uri).entity(clientOIDC));
        } catch (ClientRegistrationException cre) {
            ServicesLogger.LOGGER.clientRegistrationException(cre.getMessage());
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client metadata invalid", Response.Status.BAD_REQUEST);
        }
    }

    @GET
    @Path("{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOIDC(@PathParam("clientId") String clientId) {
        Cors cors = cors();
        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);

        ClientRepresentation clientRepresentation = get(client);

        OIDCClientRepresentation clientOIDC = DescriptionConverter.toExternalResponse(session, clientRepresentation, getRegistrationClientUri(clientId), OIDCClientRepresentation.class, false);
        return cors().add(Response.ok(clientOIDC));
    }

    @PUT
    @Path("{clientId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOIDC(@PathParam("clientId") String clientId, OIDCClientRepresentation clientOIDC) {
        Cors cors = cors();
        try {

            ClientRepresentation client = updateOidcClient(clientId, clientOIDC, session, null);
            OIDCClientRepresentation updatedClient = DescriptionConverter.toExternalResponse(session, client, getRegistrationClientUri(clientId), OIDCClientRepresentation.class, clientOIDC.getScope()!=null && clientOIDC.getScope().contains(OAuth2Constants.SCOPE_OPENID));
            return cors.add(Response.ok(updatedClient));
        } catch (ClientRegistrationException cre) {
            ServicesLogger.LOGGER.clientRegistrationException(cre.getMessage());
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client metadata invalid", Response.Status.BAD_REQUEST);
        }
    }

    @DELETE
    @Path("{clientId}")
    public Response deleteOIDC(@PathParam("clientId") String clientId) {
        Cors cors = cors();
        delete(clientId);
        return cors.add(Response.noContent());
    }

    private Cors cors() {
        return Cors.builder().auth().checkAllowedOrigins(getAllowedOrigins());
    }

}
