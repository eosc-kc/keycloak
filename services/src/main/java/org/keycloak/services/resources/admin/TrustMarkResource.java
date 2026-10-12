package org.keycloak.services.resources.admin;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
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

public class TrustMarkResource {
    protected static final Logger logger = Logger.getLogger(TrustMarkResource.class);

    protected RealmModel realm;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;
    protected TrustMarkModel trustMarkModel;
    protected KeycloakSession session;

    public TrustMarkResource(RealmModel realm, AdminPermissionEvaluator auth, TrustMarkModel trustMarkModel, KeycloakSession session, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.trustMarkModel = trustMarkModel;
        this.session = session;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT_SCOPE);
    }

    /**
     * Get representation of the trust mark
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get representation of the trust mark")
    public TrustMarkRepresentation getTrustMark() {
        this.auth.realm().requireViewRealm();
        return ModelToRepresentation.toRepresentation(trustMarkModel);
    }


    /**
     * Update the trust mark
     *
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update the trust mark")
    public Response update(final TrustMarkRepresentation rep) {
        auth.realm().requireManageRealm();
        OpenIdFederationTrustMarksUtils.validateTrustMark(rep);
        rep.setId(trustMarkModel.getId());
        trustMarkModel = RepresentationToModel.toModel(rep);
        realm.updateTrustMark(trustMarkModel);
        adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();
        return Response.noContent().build();

    }


    /**
     * Delete the trust mark
     */
    @DELETE
    @NoCache
    @Operation(summary = "Delete the trust mark")
    public Response delete() {
        auth.realm().requireManageRealm();
        realm.removeTrustMark(trustMarkModel.getId());

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
        return Response.noContent().build();
    }

}
