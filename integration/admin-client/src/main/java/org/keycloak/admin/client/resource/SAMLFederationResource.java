package org.keycloak.admin.client.resource;

import java.util.List;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.FederationMapperRepresentation;
import org.keycloak.representations.idm.SAMLFederationRepresentation;

public interface SAMLFederationResource {

    @GET
    @Path("instances/{alias}/export")
    public Response export( @PathParam("alias") String alias);

    @POST
    @Path("instances")
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(SAMLFederationRepresentation identityProviderFederationRepresentation);

    @GET
    @Path("instances/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public SAMLFederationRepresentation getSAMLFederation(@PathParam("id") String internalId);

    @DELETE
    @Path("instances/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") String internalId);

    @GET
    @Path("instances/{id}/mappers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FederationMapperRepresentation> getIdentityProviderFederationMappers(@PathParam("id") String internalId);

    @GET
    @Path("instances/{id}/mappers/{mapperId}")
    @Produces(MediaType.APPLICATION_JSON)
    public FederationMapperRepresentation getIdentityProviderFederationMapper(@PathParam("id") String id, @PathParam("mapperId") String mapperId);

    @POST
    @Path("instances/{id}/mappers")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createMapper(@PathParam("id") String id, FederationMapperRepresentation representation);

    @PUT
    @Path("instances/{id}/mappers/{mapperId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateMapper(@PathParam("id") String id, @PathParam("mapperId") String mapperId, FederationMapperRepresentation representation);

    @DELETE
    @Path("instances/{id}/mappers/{mapperId}")
    public void deleteMapper(@PathParam("id") String id, @PathParam("mapperId") String mapperId);

    @POST
    @Path("instances/{id}/mappers/{mapperId}/idp/{action}")
    public void massIdPMapperAction(@PathParam("id") String id, @PathParam("mapperId") String mapperId, @PathParam("action") String action);

}
