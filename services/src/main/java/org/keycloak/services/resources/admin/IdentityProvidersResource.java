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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.federation.OpenIdFederationIdentityProviderConfig;
import org.keycloak.broker.oidc.federation.OpenIdFederationIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.common.util.Time;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.http.FormPartValue;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.models.enums.EntityTypeEnum;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.trustchain.TrustChainProcessor;
import org.keycloak.utils.OpenIdFederationTrustChainProcessorFactory;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.scheduled.AutoUpdateIdentityProviders;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.services.scheduled.OpenIdFederationIdPExpirationTask;
import org.keycloak.services.util.ResourcesUtil;
import org.keycloak.timer.TimerProvider;
import org.keycloak.utils.ReservedCharValidator;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @resource Identity Providers
 * @author Pedro Igor
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class IdentityProvidersResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public IdentityProvidersResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.IDENTITY_PROVIDER);
    }

    /**
     * Get the identity provider factory for a provider id.
     *
     * @param providerId Provider id
     * @return
     */
    @Path("/providers/{provider_id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Get the identity provider factory for that provider id")
    public IdentityProviderFactory getIdentityProviderFactory(@Parameter(description = "The provider id to get the factory") @PathParam("provider_id") String providerId) {
        this.auth.realm().requireViewIdentityProviders();
        IdentityProviderFactory providerFactory = ResourcesUtil.getProviderFactoryById(session, providerId);
        if (providerFactory != null) {
            return providerFactory;
        }
        throw new BadRequestException();
    }

    /**
     * Import identity provider from uploaded JSON file
     */
    @POST
    @Path("import-config")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( description = "Import identity provider from uploaded JSON file")
    public Map<String, String> importFrom() throws IOException {
        this.auth.realm().requireManageIdentityProviders();
        MultivaluedMap<String, FormPartValue> formDataMap = session.getContext().getHttpRequest().getMultiPartFormParameters();
        if (!(formDataMap.containsKey("providerId") && formDataMap.containsKey("file"))) {
            throw new BadRequestException();
        }
        String providerId = formDataMap.getFirst("providerId").asString();
        String config = StreamUtil.readString(formDataMap.getFirst("file").asInputStream());
        IdentityProviderFactory<?> providerFactory = ResourcesUtil.getProviderFactoryById(session, providerId);
        final Map<String, String> parsedConfig = providerFactory.parseConfig(session, config, new IdentityProviderModel()).getConfig();
        String syncMode = parsedConfig.get(IdentityProviderModel.SYNC_MODE);
        if (syncMode == null) {
            parsedConfig.put(IdentityProviderModel.SYNC_MODE, "LEGACY");
        }
        return parsedConfig;
    }

    /**
     * Import identity provider from JSON body
     *
     * @param data JSON body
     * @return
     * @throws IOException
     */
    @POST
    @Path("import-config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Import identity provider from JSON body")
    public Map<String, String> importFrom(@Parameter(description = "JSON body") Map<String, Object> data) throws IOException {
        this.auth.realm().requireManageIdentityProviders();
        if (data == null || !(data.containsKey("providerId") && data.containsKey("fromUrl"))) {
            throw new BadRequestException();
        }

        ReservedCharValidator.validateNoSpace((String)data.get("alias"));

        String providerId = data.get("providerId").toString();
        String from = data.get("fromUrl").toString();
        String file = session.getProvider(HttpClientProvider.class).getString(from);
        IdentityProviderFactory providerFactory = ResourcesUtil.getProviderFactoryById(session, providerId);
        Map<String, String> config = providerFactory.parseConfig(session, file, new IdentityProviderModel()).getConfig();
        // add the URL just if needed by the identity provider
        config.put(IdentityProviderModel.METADATA_DESCRIPTOR_URL, from);
        return config;
    }

    /**
     * List identity providers.
     *
     * @param search Filter to search specific providers by name. Search can be prefixed (name*), contains (*name*) or exact (\"name\"). Default prefixed.
     * @param briefRepresentation Boolean which defines whether brief representations are returned (default: false)
     * @param firstResult Pagination offset
     * @param maxResults Maximum results size (defaults to 100)
     * @return The list of providers.
     */
    @GET
    @Path("instances")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation(summary = "List identity providers")
    public Stream<IdentityProviderRepresentation> getIdentityProviders(
            @Parameter(description = "Filter specific providers by name. Search can be prefix (name*), contains (*name*) or exact (\"name\"). Default prefixed.") @QueryParam("search") String search,
            @Parameter(description = "Boolean which defines whether brief representations are returned (default: false)") @QueryParam("briefRepresentation") Boolean briefRepresentation,
            @Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
            @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults,
            @Parameter(description = "Boolean which defines if only realm-level IDPs (not associated with orgs) should be returned (default: false)") @QueryParam("realmOnly") Boolean realmOnly) {
        this.auth.realm().requireViewIdentityProviders();

        if (maxResults == null) {
            maxResults = 100; // always set a maximum of 100 by default
        }

        Function<IdentityProviderModel, IdentityProviderRepresentation> toRepresentation = Optional.ofNullable(briefRepresentation).orElse(false)
                ? m -> ModelToRepresentation.toBriefRepresentation(realm, m)
                : m -> StripSecretsUtils.stripSecrets(session, ModelToRepresentation.toRepresentation(realm, m));

        boolean searchRealmOnlyIDPs = Optional.ofNullable(realmOnly).orElse(false);

        Map<String, String> searchOptions = new HashMap<>();
        if (StringUtil.isNotBlank(search)) {
            searchOptions.put(IdentityProviderModel.SEARCH, search);
        }
        if (searchRealmOnlyIDPs) {
            searchOptions.put(IdentityProviderModel.ORGANIZATION_ID, null);
        }
        return session.identityProviders().getAllStream(searchOptions, firstResult, maxResults).map(toRepresentation);
    }

    /**
     * Create a new identity provider
     *
     * @param representation JSON body
     * @return
     */
    @POST
    @Path("instances")
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Create a new identity provider")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response create(@Parameter(description = "JSON body") IdentityProviderRepresentation representation) {
        this.auth.realm().requireManageIdentityProviders();

        ReservedCharValidator.validateNoSpace(representation.getAlias());

        try {
            IdentityProviderModel identityProvider = OpenIdFederationIdentityProviderFactory.PROVIDER_ID.equals(representation.getProviderId()) ? createModelForOpenIdFederation(representation) : RepresentationToModel.toModel(realm, representation, session);
            session.identityProviders().create(identityProvider);

            representation.setInternalId(identityProvider.getInternalId());
            representation.setHideOnLogin(identityProvider.isHideOnLogin()); // update in case of legacy hide on login attr was used.
            //for autoupdated IdPs create schedule task
            if (Boolean.valueOf(identityProvider.getConfig().get(IdentityProviderModel.AUTO_UPDATE)))
                createScheduleTask(identityProvider.getAlias(), Long.parseLong(identityProvider.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000);
            //create expiration task for OpenIdFederation IdP
            if (identityProvider.getConfig().get(OIDCConfigAttributes.EXPIRATION_TIME) != null) {
                TimerProvider timer = session.getProvider(TimerProvider.class);
                OpenIdFederationIdPExpirationTask task = new OpenIdFederationIdPExpirationTask(identityProvider.getAlias(), realm.getId());
                long expiration = Long.valueOf(identityProvider.getConfig().get(OIDCConfigAttributes.EXPIRATION_TIME)) * 1000 - Time.currentTimeMillis();
                ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), task, expiration);
                timer.scheduleOnce(taskRunner, expiration, "OpenIdFederationIdPExpirationTask_" + identityProvider.getAlias());
            }
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), identityProvider.getAlias())
                    .representation(StripSecretsUtils.stripSecrets(session, representation)).success();

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(representation.getAlias()).build()).build();
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();

            if (message == null) {
                message = "Invalid request";
            }

            throw ErrorResponse.error(message, BAD_REQUEST);
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("Identity Provider " + representation.getAlias() + " already exists");
        }
    }

    private IdentityProviderModel createModelForOpenIdFederation(IdentityProviderRepresentation representation){
        if (realm.isOpenIdFederationTypeRegistrationSupported(EntityTypeEnum.OPENID_RELYING_PARTY, ClientRegistrationTypeEnum.EXPLICIT) && representation.getConfig().get(OIDCIdentityProviderConfig.ISSUER) != null) {
            try {
                  IdentityProviderModel model = new OpenIdFederationIdentityProviderConfig();
                  if (representation.getConfig().get("guiOrder") != null && !representation.getConfig().get("guiOrder").isEmpty()) {
                    model.getConfig().put("guiOrder", representation.getConfig().get("guiOrder"));
                  }
                  model.setAlias(representation.getAlias());
                  TrustChainProcessor trustChainProcessor = session.getProvider(TrustChainProcessor.class, OpenIdFederationTrustChainProcessorFactory.PROVIDER_ID);
                  return trustChainProcessor.rPexcplicitRegistration(representation.getConfig().get(OIDCIdentityProviderConfig.ISSUER), representation.getConfig().get(OpenIdFederationIdentityProviderConfig.TRUST_ANCHOR_ID), model, realm);
            } catch (Exception e) {
                throw ErrorResponse.error(e.getMessage(), BAD_REQUEST);
            }
        } else {
            throw ErrorResponse.error(realm.isOpenIdFederationTypeRegistrationSupported(EntityTypeEnum.OPENID_RELYING_PARTY, ClientRegistrationTypeEnum.EXPLICIT) ? "This realm does not support Openid Federation as RP with selected trust anchor" : "Trust anchor and issuer are required", BAD_REQUEST);
        }
    }

    private void createScheduleTask(String alias,long interval) {
        TimerProvider timer = session.getProvider(TimerProvider.class);
        AutoUpdateIdentityProviders autoUpdateProvider = new AutoUpdateIdentityProviders(alias, realm.getId());
        ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), autoUpdateProvider, interval);
        timer.schedule(taskRunner, interval, realm.getId()+"_AutoUpdateIdP_" + alias);
    }

    @Path("instances/{alias}")
    public IdentityProviderResource getIdentityProvider(@PathParam("alias") String alias) {
        this.auth.realm().requireViewIdentityProviders();
        IdentityProviderModel identityProviderModel = session.identityProviders().getByIdOrAlias(alias);

        return new IdentityProviderResource(this.auth, realm, session, identityProviderModel, adminEvent);
    }

}
