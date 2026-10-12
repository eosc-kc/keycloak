package org.keycloak.services.resources.admin;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.TrustMarkModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.TrustMarkRepresentation;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.utils.OpenIdFederationTrustMarksUtils;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

public class TrustMarksResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    protected static final Logger logger = Logger.getLogger(TrustMarksResource.class);

    public TrustMarksResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.TRUST_MARK);
    }


    /**
     * Get a list with all trust marks of the realm
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get Trust Mark List")
    public List<TrustMarkRepresentation> list() {
        this.auth.realm().requireViewRealm();
        return realm.getTrustMarks().stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
    }

    /**
     * Create a new Trust Mark
     *
     * @param representation JSON body
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new trust mark")
    public Response create(TrustMarkRepresentation representation) {
        this.auth.realm().requireManageRealm();
        OpenIdFederationTrustMarksUtils.validateTrustMark(representation);
        TrustMarkModel model = RepresentationToModel.toModel(representation);
        realm.addTrustMark(model);
        representation.setId(model.getId());
        adminEvent.operation(OperationType.CREATE)
                .resourcePath(session.getContext().getUri(), representation.getId())
                .representation(representation).success();

        return Response.created(session.getContext().getUri().getAbsolutePathBuilder()
                .path(representation.getId()).build()).build();

    }

    /**
     * Base path for managing a trust mark`.
     *
     * @param id id of trust mark
     * @return
     */
    @Path("{id}")
    @NoCache
    public TrustMarkResource getTrustMark(final @PathParam("id") String id) {
        auth.realm().requireViewRealm();
        TrustMarkModel trustMarkModel = realm.getTrustMark(id);
        if (trustMarkModel == null) {
            throw new NotFoundException("Could not find trust mark");
        }
        return new TrustMarkResource(realm, auth, trustMarkModel, session, adminEvent);
    }

}
