package org.keycloak.admin.client.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.TrustMarkRepresentation;

public interface TrustMarkResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    TrustMarkRepresentation getTrustMark() ;

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    Response update(TrustMarkRepresentation rep) ;

    @DELETE
    Response delete();
}
