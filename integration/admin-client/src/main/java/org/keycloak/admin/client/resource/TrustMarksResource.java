package org.keycloak.admin.client.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.TrustMarkRepresentation;

public interface TrustMarksResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<TrustMarkRepresentation> list() ;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(TrustMarkRepresentation representation) ;

    @Path("{id}")
    TrustMarkResource getTrustMark(@PathParam("id") String id);
}
