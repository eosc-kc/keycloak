package org.keycloak.admin.client.resource;

import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.ClientScopePolicyRepresentation;

public interface ClientScopePolicyResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createClientScopePolicy(ClientScopePolicyRepresentation rep);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<ClientScopePolicyRepresentation> getClientScopePolicies();

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ClientScopePolicyRepresentation getClientScopePolicyById(@PathParam("id") String id);

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@PathParam("id") String id, ClientScopePolicyRepresentation rep);

    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") String id);
}
