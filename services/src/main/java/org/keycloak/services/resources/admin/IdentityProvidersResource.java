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

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.UriInfo;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.federation.OpenIdFederationIdentityProviderConfig;
import org.keycloak.broker.oidc.federation.OpenIdFederationIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.http.FormPartValue;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OpenIdFederationConfig;
import org.keycloak.models.OpenIdFederationGeneralConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.EntityStatementExplicitResponse;
import org.keycloak.representations.openid_federation.Metadata;
import org.keycloak.representations.openid_federation.OPMetadata;
import org.keycloak.representations.openid_federation.RPMetadata;
import org.keycloak.representations.openid_federation.TrustChainForExplicit;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.scheduled.OpenIdFederationIdPExpirationTask;
import org.keycloak.services.util.ResourcesUtil;
import org.keycloak.urls.UrlType;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.OpenIdFederationTrustChainProcessor;
import org.keycloak.utils.OpenIdFederationUtils;
import org.keycloak.utils.ReservedCharValidator;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

import org.keycloak.services.scheduled.AutoUpdateIdentityProviders;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

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
    @Operation( summary = "Get identity providers")
    public Response getIdentityProviders(@Parameter(description = "Provider id") @PathParam("provider_id") String providerId) {
        this.auth.realm().requireViewIdentityProviders();
        IdentityProviderFactory providerFactory = ResourcesUtil.getProviderFactoryById(session, providerId);
        if (providerFactory != null) {
            return Response.ok(providerFactory).build();
        }
        return Response.status(BAD_REQUEST).build();
    }

    /**
     * Import identity provider from uploaded JSON file
     *
     * @return
     * @throws IOException
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
        InputStream inputStream = formDataMap.getFirst("file").asInputStream();
        IdentityProviderFactory providerFactory = ResourcesUtil.getProviderFactoryById(session, providerId);
        Map<String, String> config = providerFactory.parseConfig(session, inputStream, new IdentityProviderModel()).getConfig();
        return config;
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
        
        ReservedCharValidator.validate((String)data.get("alias"));
        
        String providerId = data.get("providerId").toString();
        String from = data.get("fromUrl").toString();
        InputStream inputStream = session.getProvider(HttpClientProvider.class).get(from);
        try {
            IdentityProviderFactory providerFactory = ResourcesUtil.getProviderFactoryById(session, providerId);
            Map<String, String> config = providerFactory.parseConfig(session, inputStream, new IdentityProviderModel()).getConfig();
            return config;
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
    }

   /**
     * Get used (initiated) identity provider providerId(s).  i.e. ['saml', 'oidc', 'github']
     *
     * @return
     */
    @GET
    @Path("types-used")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getUsedIdentityProviderIdTypes() {
        this.auth.realm().requireViewIdentityProviders();
        return realm.getIdentityProvidersStream().map(IdentityProviderModel::getProviderId).collect(Collectors.toCollection(HashSet::new));
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
            @Parameter(description = "Boolean which defines whether brief representations are returned (default: false)") @DefaultValue ("true") @QueryParam("briefRepresentation") Boolean briefRepresentation,
            @Parameter(description = "Pagination offset") @DefaultValue ("0") @QueryParam("first") Integer firstResult,
            @Parameter(description = "Maximum results size (defaults to 100)") @DefaultValue ("100") @QueryParam("max") Integer maxResults) {
        this.auth.realm().requireViewIdentityProviders();

        Function<IdentityProviderModel, IdentityProviderRepresentation> toRepresentation = briefRepresentation != null && briefRepresentation
                ? m -> ModelToRepresentation.toBriefRepresentation(realm, m)
                : m -> StripSecretsUtils.strip(ModelToRepresentation.toRepresentation(realm, m));

        Stream<IdentityProviderModel> stream = realm.searchIdentityProviders(search, firstResult, maxResults);

        return stream.map(toRepresentation);
    }

     /**
     * get IdPs alias per federation
     * @param federationId
     * @return
     */
    @Path("/federation/{federationId}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getIdPsPerFederation(@PathParam("federationId") String federationId) {
        this.auth.realm().requireViewIdentityProviders();
       return realm.getIdentityProvidersByFederation(federationId);
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
    public Response create(@Parameter(description = "JSON body") IdentityProviderRepresentation representation) {
        this.auth.realm().requireManageIdentityProviders();

        ReservedCharValidator.validate(representation.getAlias());

        try {
            IdentityProviderModel identityProvider = OpenIdFederationIdentityProviderFactory.PROVIDER_ID.equals(representation.getProviderId()) ? createModelForOpenIdFederation(representation) : RepresentationToModel.toModel(realm, representation, session);
            this.realm.addIdentityProvider(identityProvider);

            representation.setInternalId(identityProvider.getInternalId());
            //for autoupdated IdPs create schedule task
            if ("true".equals(identityProvider.getConfig().get(IdentityProviderModel.AUTO_UPDATE)))
                createScheduleTask(identityProvider.getAlias(), Long.parseLong(identityProvider.getConfig().get(IdentityProviderModel.REFRESH_PERIOD)) * 1000);
            //create expiration task for OpenIdFederation IdP
            if (identityProvider.getConfig().get(OIDCConfigAttributes.EXPIRATION_TIME) != null) {
                TimerProvider timer = session.getProvider(TimerProvider.class);
                OpenIdFederationIdPExpirationTask task = new OpenIdFederationIdPExpirationTask(identityProvider.getAlias(), realm.getId());
                long expiration = (Long.valueOf(identityProvider.getConfig().get(OIDCConfigAttributes.EXPIRATION_TIME)) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)) * 1000;
                ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), task, expiration);
                timer.schedule(taskRunner, expiration, "OpenIdFederationIdPExpirationTask_" + identityProvider.getAlias());
            }
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), identityProvider.getAlias())
                    .representation(StripSecretsUtils.strip(representation)).success();

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
        if (realm.isOpenIdFederationEnabled() && representation.getConfig().get(OpenIdFederationIdentityProviderConfig.TRUST_ANCHOR_ID) != null && representation.getConfig().get(OIDCIdentityProviderConfig.ISSUER) != null) {
            try {
                OpenIdFederationGeneralConfig federationGeneralConfig = realm.getOpenIdFederationGeneralConfig();
                OpenIdFederationConfig federationConfig = realm.getOpenIdFederations().stream().filter(x -> representation.getConfig().get(OpenIdFederationIdentityProviderConfig.TRUST_ANCHOR_ID).equals(x.getTrustAnchor())).findAny().orElseThrow(() -> new NotFoundException("Trust anchor does not exist"));
                OpenIdFederationTrustChainProcessor trustChainProcessor = new OpenIdFederationTrustChainProcessor(session);
                String opIssuer = representation.getConfig().get(OIDCIdentityProviderConfig.ISSUER);
                EntityStatement opStatement = trustChainProcessor.parseAndValidateSelfSigned(OpenIdFederationUtils.getSelfSignedToken(opIssuer, session));
                if (!trustChainProcessor.validateEntityStatementFields(opStatement, opIssuer, opIssuer) || opStatement.getMetadata().getOpenIdProviderMetadata() == null || !opStatement.getMetadata().getOpenIdProviderMetadata().getClientRegistrationTypes().contains("explicit") || opStatement.getMetadata().getOpenIdProviderMetadata().getFederationRegistrationEndpoint() == null) {
                    throw new BadRequestException("No valid OP Entity Statement");
                }
                List<TrustChainForExplicit> trustChainForExplicits = trustChainProcessor.constructTrustChains(opStatement, Stream.of(federationConfig.getTrustAnchor()).collect(Collectors.toSet()), false);
                if (trustChainForExplicits.isEmpty()) {
                    throw new BadRequestException("No common trust chain found");
                }
                IdentityProviderFactory providerFactory = (IdentityProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(
                        IdentityProvider.class, OpenIdFederationIdentityProviderFactory.PROVIDER_ID);
                if (providerFactory == null) {
                    throw new IllegalArgumentException("Problem creating Identity Provider factory");
                }
                IdentityProviderModel model = providerFactory.parseConfig(session, session.getProvider(HttpClientProvider.class).get(opIssuer+OpenIdFederationUtils.OIDC_WELL_KNOWN_SUBPATH), new OpenIdFederationIdentityProviderConfig());

                UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
                UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
                JSONWebKeySet jwks = trustChainProcessor.getKeySet();
                EntityStatement entityStatement = new EntityStatement(Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName()), Long.valueOf(federationGeneralConfig.getLifespan()), new ArrayList<>(federationGeneralConfig.getAuthorityHints()), jwks);
                entityStatement.addAudience(opIssuer);
                Metadata metadata = new Metadata();
                RPMetadata rPMetadata = OpenIdFederationUtils.createRPMetadata(federationGeneralConfig, federationConfig.getClientRegistrationTypesSupported().stream(), OpenIdFederationUtils.commonMetadata(federationGeneralConfig), RealmsResource.protocolUrl(backendUriInfo).clone().path(OIDCLoginProtocolService.class, "certs").build(realm.getName(),
                        OIDCLoginProtocol.LOGIN_PROTOCOL).toString());
                rPMetadata.setClientName(representation.getDisplayName());
                rPMetadata.setRedirectUris(Stream.of(Urls.identityProviderAuthnResponse(frontendUriInfo.getBaseUri(), representation.getAlias(), realm.getName()).toString()).collect(Collectors.toList()));
                metadataFromOP(rPMetadata, federationConfig.getIdpConfiguration(), opStatement.getMetadata().getOpenIdProviderMetadata());
                metadataFromFederation(rPMetadata, federationConfig.getIdpConfiguration());
                metadata.setRelyingPartyMetadata(rPMetadata);
                entityStatement.setMetadata(metadata);
                StringEntity entity = new StringEntity(session.tokens().encodeForOpenIdFederation(entityStatement), ContentType.create(TokenUtil.APPLICATION_ENTITY_STATEMENT_JWT));
                SimpleHttp.Response response = SimpleHttp.doPost(opStatement.getMetadata().getOpenIdProviderMetadata().getFederationRegistrationEndpoint(), session).entity(entity).asResponse();
                if (response.getStatus() < 200 || response.getStatus() >= 400) {
                    throw new BadRequestException("Error during explicit client registration with body : "+ response.asString());
                }
                EntityStatementExplicitResponse statementResponse = trustChainProcessor.parseAndValidateSelfSigned(response.asString(), EntityStatementExplicitResponse.class, opStatement.getJwks());
                if (!trustChainProcessor.validateEntityStatementFields(statementResponse, opIssuer, opIssuer) || statementResponse.getTrustAnchor() == null || LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) > statementResponse.getExp() ) {
                    throw new BadRequestException("No valid OP Entity Statement");
                }
                OpenIdFederationUtils.convertEntityStatementToIdp(model, realm, representation.getAlias(), statementResponse, federationConfig.getIdpConfiguration());
                return model;
            } catch (Exception e) {
                throw ErrorResponse.error(e.getMessage(), BAD_REQUEST);
            }
        } else {
            throw ErrorResponse.error(realm.isOpenIdFederationEnabled() ? "This realm does not support openid Federation" : "Trust anchor and issuer are required", BAD_REQUEST);
        }
    }

    private void createScheduleTask(String alias,long interval) {
        TimerProvider timer = session.getProvider(TimerProvider.class);
        AutoUpdateIdentityProviders autoUpdateProvider = new AutoUpdateIdentityProviders(alias, realm.getId());
        ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), autoUpdateProvider, interval);
        timer.schedule(taskRunner, interval, realm.getId()+"_AutoUpdateIdP_" + alias);
    }

    private void metadataFromFederation(RPMetadata rPMetadata, Map<String, String> federationConfig){
        rPMetadata.setScope(federationConfig.get(OAuth2IdentityProviderConfig.DEFAULT_SCOPE));
        String clientAuthMethod = federationConfig.get(OAuth2IdentityProviderConfig.CLIENT_AUTH_METHOD);
        rPMetadata.setGrantTypes(Stream.of(clientAuthMethod != null ? clientAuthMethod : OIDCLoginProtocol.CLIENT_SECRET_POST).collect(Collectors.toList()));
    }

    private void metadataFromOP(RPMetadata rPMetadata, Map<String, String> federationConfig, OPMetadata opMetadata) {
        List<String> subjectTypesSupported = federationConfig.get(OpenIdFederationUtils.SUBJECT_TYPES_SUPPORTED) == null
                ? OIDCWellKnownProvider.DEFAULT_SUBJECT_TYPES_SUPPORTED
                : Arrays.asList(federationConfig.get(OpenIdFederationUtils.SUBJECT_TYPES_SUPPORTED).split("##"));

        rPMetadata.setSubjectType(subjectTypesSupported.stream()
                .filter(x -> opMetadata.getSubjectTypesSupported().contains(x))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No subject type common exists")));
    }

    @Path("instances/{alias}")
    public IdentityProviderResource getIdentityProvider(@PathParam("alias") String alias) {
        this.auth.realm().requireViewIdentityProviders();
        IdentityProviderModel identityProviderModel =  this.realm.getIdentityProvidersStream()
                .filter(p -> Objects.equals(p.getAlias(), alias) || Objects.equals(p.getInternalId(), alias))
                .findFirst().orElse(null);

        return new IdentityProviderResource(this.auth, realm, session, identityProviderModel, adminEvent);
    }

}
