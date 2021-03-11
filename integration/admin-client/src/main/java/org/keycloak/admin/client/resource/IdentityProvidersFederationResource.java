package org.keycloak.admin.client.resource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.keycloak.representations.idm.FederationMapperRepresentation;
import org.keycloak.representations.idm.IdentityProvidersFederationRepresentation;

public interface IdentityProvidersFederationResource {

    @POST
    @Path("instances")
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(IdentityProvidersFederationRepresentation identityProviderFederationRepresentation);

    @GET
    @Path("instances/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public IdentityProvidersFederationRepresentation getIdentityProviderFederation(@PathParam("id") String internalId);

    @DELETE
    @Path("instances/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") String internalId);

    @GET
    @Path("instances/{id}/mappers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FederationMapperRepresentation> getIdentityProviderFederationMappers(@PathParam("id") String internalId);
}
