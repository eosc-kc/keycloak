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
package org.keycloak.services.resources.admin;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.broker.oidc.federation.OpenIdFederationIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.Profile;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.models.enums.EntityTypeEnum;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.protocol.trustchain.TrustChainProcessor;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperTypeRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissionManagement;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.services.scheduled.AutoUpdateIdentityProviders;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.services.util.ResourcesUtil;
import org.keycloak.timer.TimerProvider;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.OpenIdFederationTrustChainProcessorFactory;
import org.keycloak.utils.ProfileHelper;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @resource Identity Providers
 * @author Pedro Igor
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class IdentityProviderResource {

    protected static final Logger logger = Logger.getLogger(IdentityProviderResource.class);

    private final AdminPermissionEvaluator auth;
    private final RealmModel realm;
    private final KeycloakSession session;
    private final IdentityProviderModel identityProviderModel;
    private final AdminEventBuilder adminEvent;

    public IdentityProviderResource(AdminPermissionEvaluator auth, RealmModel realm, KeycloakSession session, IdentityProviderModel identityProviderModel, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.identityProviderModel = identityProviderModel;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.IDENTITY_PROVIDER);
    }

    /**
     * Get the identity provider
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation(summary = "Get the identity provider")
    public IdentityProviderRepresentation getIdentityProvider() {
        this.auth.realm().requireViewIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        return StripSecretsUtils.stripSecrets(session, ModelToRepresentation.toRepresentation(session, realm, this.identityProviderModel));
    }

    /**
     * Delete the identity provider
     *
     * @return
     */
    @DELETE
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Delete the identity provider")
    public Response delete() {
        this.auth.realm().requireManageIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        String alias = this.identityProviderModel.getAlias();
        session.users().preRemove(realm, identityProviderModel);
        session.identityProviders().remove(alias);

        TimerProvider timer = session.getProvider(TimerProvider.class);
        timer.cancelTask(realm.getId()+"_AutoUpdateIdP_" + alias);

        realm.getIdentityProviderMappersByAliasStream(alias)
                .collect(Collectors.toList()).forEach(realm::removeIdentityProviderMapper);

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();

        return Response.noContent().build();
    }

    /**
     * Update the identity provider
     *
     * @param providerRep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Update the identity provider")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response update(IdentityProviderRepresentation providerRep) {
        this.auth.realm().requireManageIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        try {
            updateIdpFromRep(providerRep, realm, session);

            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(providerRep).success();

            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();

            if (message == null) {
                message = "Invalid request";
            }

            throw ErrorResponse.error(message, BAD_REQUEST);
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("Identity Provider " + providerRep.getAlias() + " already exists");
        }
    }

    @POST
    @Path("refresh")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation(summary = "Refresh autoupdated identity provider")
    public Response refreshIdP() {
        this.auth.realm().requireManageIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        if (!Boolean.valueOf(identityProviderModel.getConfig().get(IdentityProviderModel.AUTO_UPDATE))) {
            throw ErrorResponse.error("This is not auto updated IdP", BAD_REQUEST);
        }

        try {
            String file = session.getProvider(HttpClientProvider.class).getString(identityProviderModel.getConfig().get(IdentityProviderModel.METADATA_DESCRIPTOR_URL));
            IdentityProviderModel  idp = ResourcesUtil.getProviderFactoryById(session, identityProviderModel.getProviderId()).parseConfig(session, file, identityProviderModel);
            idp.getConfig().remove(IdentityProviderModel.LEGACY_HIDE_ON_LOGIN_ATTR);
            idp.getConfig().put(IdentityProviderModel.LAST_REFRESH_TIME, String.valueOf(Instant.now().toEpochMilli()));
            session.identityProviders().update(idp);
        } catch (IOException e) {
            logger.error("Problem during refreshing autoupdating Identity Provider with alias "+ identityProviderModel.getAlias()+" Error: ", e);
            throw ErrorResponse.error("Problem during refreshing autoupdating Identity Provider with alias "+ identityProviderModel.getAlias()+" Error: "+e.getMessage(), BAD_REQUEST);
        }
        return Response.noContent().build();
    }


    @POST
    @Path("refresh/oidfed")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Refresh OpenId Federation identity provider")
    public Response refreshOidFedIdP() {
        this.auth.realm().requireManageIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        if (!OpenIdFederationIdentityProviderFactory.PROVIDER_ID.equals(identityProviderModel.getProviderId()) || !realm.isOpenIdFederationTypeRegistrationSupported(EntityTypeEnum.OPENID_RELYING_PARTY, ClientRegistrationTypeEnum.EXPLICIT)) {
            throw ErrorResponse.error("This Identity Provider is not OpenId Federation type or this realm is not OpenId Federation as RP with explicit registration", BAD_REQUEST);
        }

        TrustChainProcessor trustChainProcessor = session.getProvider(TrustChainProcessor.class, OpenIdFederationTrustChainProcessorFactory.PROVIDER_ID);
        try {
            IdentityProviderModel model = trustChainProcessor.updateIdP(identityProviderModel, realm, session.getContext().getUri(UrlType.FRONTEND), session.getContext().getUri(UrlType.BACKEND));
            realm.updateIdentityProvider(model);
            return  Response.noContent().build();
        } catch (Exception e) {
            logger.warn("Error during updating OpenId Federation Identity Provider", e);
            identityProviderModel.setEnabled(false);
            realm.updateIdentityProvider(identityProviderModel);
            throw ErrorResponse.error("Error during updating OpenId Federation Identity Provider with alias "+ identityProviderModel.getAlias()+" Error: "+e.getMessage(), BAD_REQUEST);
        }
    }

    private void updateIdpFromRep(IdentityProviderRepresentation providerRep, RealmModel realm, KeycloakSession session) {

        if (!identityProviderModel.getInternalId().equals(providerRep.getInternalId())) {
            providerRep.setInternalId(identityProviderModel.getInternalId());
        }

        IdentityProviderModel updated = RepresentationToModel.toModel(realm, providerRep, session);

        if (updated.getConfig() != null && ComponentRepresentation.SECRET_VALUE.equals(updated.getConfig().get("clientSecret"))) {
            updated.getConfig().put("clientSecret", identityProviderModel.getConfig() != null ? identityProviderModel.getConfig().get("clientSecret") : null);
        }

        updated.getConfig().put(IdentityProviderModel.LAST_REFRESH_TIME, identityProviderModel.getConfig().get(IdentityProviderModel.LAST_REFRESH_TIME));

        session.identityProviders().update(updated);
        // update in case of legacy hide on login attr was used.
        providerRep.setHideOnLogin(updated.isHideOnLogin());

        if (Boolean.valueOf(updated.getConfig().get(IdentityProviderModel.AUTO_UPDATE)) && ! Boolean.valueOf(identityProviderModel.getConfig().get(IdentityProviderModel.AUTO_UPDATE))) {
            //change from simple to autoUpdated IdP
            TimerProvider timer = session.getProvider(TimerProvider.class);
            createScheduleTask(timer, updated.getAlias(), Long.parseLong(updated.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000, Long.parseLong(updated.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000);
        } else if (! Boolean.valueOf(updated.getConfig().get(IdentityProviderModel.AUTO_UPDATE)) && Boolean.valueOf(identityProviderModel.getConfig().get(IdentityProviderModel.AUTO_UPDATE))) {
            //change from autoUpdated to simple IdP
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTaskAndNotify(realm.getId()+"_AutoUpdateIdP_" + identityProviderModel.getAlias());
        } else if (Boolean.valueOf(updated.getConfig().get(IdentityProviderModel.AUTO_UPDATE)) && (!updated.getConfig().get(IdentityProviderModel.REFRESH_PERIOD).equals(identityProviderModel.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) || !identityProviderModel.getAlias().equals(updated.getAlias()))) {
            //change refreshPeriod or alias
            TimerProvider timer = session.getProvider(TimerProvider.class);
            long delay = updated.getConfig().get(IdentityProviderModel.LAST_REFRESH_TIME) == null ? Long.parseLong(updated.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000 : Long.parseLong(updated.getConfig().get(IdentityProviderModel.LAST_REFRESH_TIME) )+ Long.parseLong(updated.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000 - Instant.now().toEpochMilli();
            timer.cancelTaskAndNotify(realm.getId()+"_AutoUpdateIdP_" + identityProviderModel.getAlias());
            createScheduleTask(timer, updated.getAlias(), delay, Long.parseLong(updated.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000);
        }

        String newProviderAlias = providerRep.getAlias();
        String oldProviderAlias = identityProviderModel.getAlias();
        if (!oldProviderAlias.equals(newProviderAlias)) {

            // Admin changed the ID (alias) of identity provider. We must update all clients and users
            logger.debugf("Changing providerId in all clients and linked users. oldProviderId=%s, newProviderId=%s", oldProviderAlias, newProviderAlias);

            updateUsersAfterProviderAliasChange(session.users().searchForUserStream(realm, Collections.singletonMap(UserModel.INCLUDE_SERVICE_ACCOUNT, Boolean.FALSE.toString())),
                    oldProviderAlias, newProviderAlias, realm, session);
        }
    }

    private void createScheduleTask(TimerProvider timer, String alias, long delay, long interval) {
        AutoUpdateIdentityProviders autoUpdateProvider = new AutoUpdateIdentityProviders(alias, realm.getId());
        ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), autoUpdateProvider, interval);
        timer.schedule(taskRunner, delay < 1000 ? 1000 : delay, interval, realm.getId() + "_AutoUpdateIdP_" + alias);
    }

    private static void updateUsersAfterProviderAliasChange(Stream<UserModel> users, String oldProviderId, String newProviderId, RealmModel realm, KeycloakSession session) {
        users.forEach(user -> {
            FederatedIdentityModel federatedIdentity = session.users().getFederatedIdentity(realm, user, oldProviderId);
            if (federatedIdentity != null) {
                // Remove old link first
                session.users().removeFederatedIdentity(realm, user, oldProviderId);

                // And create new
                FederatedIdentityModel newFederatedIdentity = new FederatedIdentityModel(newProviderId, federatedIdentity.getUserId(), federatedIdentity.getUserName(),
                        federatedIdentity.getToken());
                session.users().addFederatedIdentity(realm, user, newFederatedIdentity);
            }
        });
    }


    private IdentityProviderFactory<?> getIdentityProviderFactory() {
        String providerId = identityProviderModel.getProviderId();
        return Stream.concat(session.getKeycloakSessionFactory().getProviderFactoriesStream(IdentityProvider.class),
                session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class))
                .filter(providerFactory -> Objects.equals(providerFactory.getId(), providerId))
                .map(IdentityProviderFactory.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("IDP not found by Provider ID: " + providerId));
    }

    /**
     * Export public broker configuration for identity provider
     *
     * @param format Format to use
     * @return
     */
    @GET
    @Path("export")
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Export public broker configuration for identity provider")
    public Response export(@Parameter(description = "Format to use") @QueryParam("format") String format) {
        this.auth.realm().requireViewIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        try {
            return createIdentityProviderInstance().export(session.getContext().getUri(), realm, format);
        } catch (Exception e) {
            throw ErrorResponse.error("Could not export public broker configuration for identity provider [" + identityProviderModel.getProviderId() + "].", Response.Status.NOT_FOUND);
        }
    }

    private IdentityProvider<?> createIdentityProviderInstance() {
        IdentityProviderFactory<?> factory = getIdentityProviderFactory();
        return factory.create(session, identityProviderModel);
    }

    /**
     * Get mapper types for identity provider
     */
    @GET
    @Path("mapper-types")
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Get mapper types for identity provider")
    public Map<String, IdentityProviderMapperTypeRepresentation> getMapperTypes() {
        this.auth.realm().requireViewIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        IdentityProvider<?> identityProviderInstance = createIdentityProviderInstance();
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        return sessionFactory.getProviderFactoriesStream(IdentityProviderMapper.class)
                .map(IdentityProviderMapper.class::cast)
                .filter(identityProviderInstance::isMapperSupported)
                .map(mapper -> {
                    IdentityProviderMapperTypeRepresentation rep = new IdentityProviderMapperTypeRepresentation();
                    rep.setId(mapper.getId());
                    rep.setCategory(mapper.getDisplayCategory());
                    rep.setName(mapper.getDisplayType());
                    rep.setHelpText(mapper.getHelpText());
                    rep.setProperties(mapper.getConfigProperties().stream()
                            .map(ModelToRepresentation::toRepresentation)
                            .collect(Collectors.toList()));
                    return rep;
                })
                .collect(Collectors.toMap(IdentityProviderMapperTypeRepresentation::getId, Function.identity()));
    }

    /**
     * Get mappers for identity provider
     */
    @GET
    @Path("mappers")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Get mappers for identity provider")
    public Stream<IdentityProviderMapperRepresentation> getMappers() {
        this.auth.realm().requireViewIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        return session.identityProviders().getMappersByAliasStream(identityProviderModel.getAlias())
                .map(ModelToRepresentation::toRepresentation);
    }

    /**
     * Add a mapper to identity provider
     *
     * @param mapper
     * @return
     */
    @POST
    @Path("mappers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation(summary = "Add a mapper to identity provider")
    public Response addMapper(IdentityProviderMapperRepresentation mapper) {
        this.auth.realm().requireManageIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        IdentityProviderMapperModel model = RepresentationToModel.toModel(mapper);

        try {
//            model = realm.addIdentityProviderMapper(model);
            model = session.identityProviders().createMapper(model);
        } catch (Exception e) {
            throw ErrorResponse.error("Failed to add mapper '" + model.getName() + "' to identity provider [" + identityProviderModel.getProviderId() + "].", Response.Status.BAD_REQUEST);
        }

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.IDENTITY_PROVIDER_MAPPER).resourcePath(session.getContext().getUri(), model.getId())
            .representation(mapper).success();

        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();

    }

    /**
     * Get mapper by id for the identity provider
     *
     * @param id
     * @return
     */
    @GET
    @NoCache
    @Path("mappers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Get mapper by id for the identity provider")
    public IdentityProviderMapperRepresentation getMapperById(@PathParam("id") String id) {
        this.auth.realm().requireViewIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        IdentityProviderMapperModel model = session.identityProviders().getMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        return ModelToRepresentation.toRepresentation(model);
    }

    /**
     * Update a mapper for the identity provider
     *
     * @param id Mapper id
     * @param rep
     */
    @PUT
    @NoCache
    @Path("mappers/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Update a mapper for the identity provider")
    public void update(@Parameter(description = "Mapper id") @PathParam("id") String id, IdentityProviderMapperRepresentation rep) {
        this.auth.realm().requireManageIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        IdentityProviderMapperModel model = session.identityProviders().getMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        model = RepresentationToModel.toModel(rep);

        session.identityProviders().updateMapper(model);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.IDENTITY_PROVIDER_MAPPER).resourcePath(session.getContext().getUri()).representation(rep).success();

    }

    /**
     * Delete a mapper for the identity provider
     *
     * @param id Mapper id
     */
    @DELETE
    @NoCache
    @Path("mappers/{id}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Delete a mapper for the identity provider")
    public void delete(@Parameter(description = "Mapper id") @PathParam("id") String id) {
        this.auth.realm().requireManageIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        IdentityProviderMapperModel model = session.identityProviders().getMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        session.identityProviders().removeMapper(model);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.IDENTITY_PROVIDER_MAPPER).resourcePath(session.getContext().getUri()).success();

    }

    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     * @return
     */
    @Path("management/permissions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Return object stating whether client Authorization permissions have been initialized or not and a reference")
    public ManagementPermissionReference getManagementPermissions() {
        ProfileHelper.requireFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
        this.auth.realm().requireViewIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        if (!permissions.idps().isPermissionsEnabled(identityProviderModel)) {
            return new ManagementPermissionReference();
        }
        return toMgmtRef(identityProviderModel, permissions);
    }

    private ManagementPermissionReference toMgmtRef(IdentityProviderModel model, AdminPermissionManagement permissions) {
        ManagementPermissionReference ref = new ManagementPermissionReference();
        ref.setEnabled(true);
        ref.setResource(permissions.idps().resource(model).getId());
        ref.setScopePermissions(permissions.idps().getPermissions(model));
        return ref;
    }


    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     *
     * @return initialized manage permissions reference
     */
    @Path("management/permissions")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Return object stating whether client Authorization permissions have been initialized or not and a reference")
    public ManagementPermissionReference setManagementPermissionsEnabled(ManagementPermissionReference ref) {
        ProfileHelper.requireFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
        this.auth.realm().requireManageIdentityProviders();
        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        permissions.idps().setPermissionsEnabled(identityProviderModel, ref.isEnabled());
        if (ref.isEnabled()) {
            return toMgmtRef(identityProviderModel, permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }

    @GET
    @Path("reload-keys")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation(summary = "Reaload keys for the identity provider if the provider supports it, \"true\" is returned if reload was performed, \"false\" if not.")
    public boolean reloadKeys() {
        this.auth.realm().requireManageIdentityProviders();

        if (identityProviderModel == null) {
            throw new jakarta.ws.rs.NotFoundException();
        }

        IdentityProvider<?> provider = IdentityBrokerService.getIdentityProvider(session, identityProviderModel.getAlias());
        return provider.reloadKeys();
    }

}
