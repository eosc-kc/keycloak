package org.keycloak.services.resources.admin;

import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientScopePolicyModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientScopePolicyRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ClientScopePolicyResource {

    protected static final Logger logger = Logger.getLogger(ClientScopePolicyResource.class);

    protected ClientScopeModel clientScope;
    protected AdminPermissionEvaluator.RequirePermissionCheck managePermission;
    protected AdminPermissionEvaluator.RequirePermissionCheck viewPermission;

    protected AdminEventBuilder adminEvent;
    protected KeycloakSession session;

    public ClientScopePolicyResource(KeycloakSession session, ClientScopeModel clientScope, AdminEventBuilder adminEvent,
                                     AdminPermissionEvaluator.RequirePermissionCheck managePermission,
                                     AdminPermissionEvaluator.RequirePermissionCheck viewPermission) {

        this.session = session;
        this.clientScope = clientScope;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT_SCOPE_POLICY);
        this.managePermission = managePermission;
        this.viewPermission = viewPermission;

    }

    /**
     * Create a mapper
     *
     * @param rep
     */
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createClientScopePolicy(ClientScopePolicyRepresentation rep) {
        managePermission.require();

        try {
            ClientScopePolicyModel model = RepresentationToModel.toModel(rep);
            model = clientScope.addClientScopePolicy(model);
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), model.getId()).representation(rep).success();
            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();

        } catch (Exception e) {
            throw ErrorResponse.exists("ClientScopePolicy exists for this user attribute");
        }
    }

    /**
     * Get mappers
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<ClientScopePolicyRepresentation> getClientScopePolicies() {
        viewPermission.require();

        return clientScope.getClientScopePoliciesStream().map(ModelToRepresentation::toRepresentation);
    }

    /**
     * Get mapper by id
     *
     * @param id Mapper id
     * @return
     */
    @GET
    @NoCache
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ClientScopePolicyRepresentation getClientScopePolicyById(@PathParam("id") String id) {
        viewPermission.require();

        ClientScopePolicyModel model = clientScope.getClientScopePolicy(id);
        if (model == null) throw new NotFoundException("This Client Scope Policy not found");
        return ModelToRepresentation.toRepresentation(model);
    }

    /**
     * Update the mapper
     *
     * @param id  Mapper id
     * @param rep
     */
    @PUT
    @NoCache
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@PathParam("id") String id, ClientScopePolicyRepresentation rep) {
        managePermission.require();

        ClientScopePolicyModel model = RepresentationToModel.toModel(rep);
        model = clientScope.updateClientScopePolicy(model);
        if (model == null)
            throw new NotFoundException("This Client Scope Policy not found");
        adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();
    }

    /**
     * Delete the mapper
     *
     * @param id Mapper id
     */
    @DELETE
    @NoCache
    @Path("{id}")
    public void delete(@PathParam("id") String id) {
        managePermission.require();

        clientScope.removeClientScopePolicy(id);
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();

    }


}
