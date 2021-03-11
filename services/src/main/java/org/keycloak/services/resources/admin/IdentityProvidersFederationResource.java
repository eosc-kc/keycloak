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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.broker.federation.IdpFederationProvider;
import org.keycloak.broker.federation.IdpFederationProviderFactory;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.FederationMapperModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.FederationMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperTypeRepresentation;
import org.keycloak.representations.idm.IdentityProvidersFederationRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;


public class IdentityProvidersFederationResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    public IdentityProvidersFederationResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.IDENTITY_PROVIDERS_FEDERATION);
    }

    /**
     * Export public broker configuration for identity provider
     *
     * @param format Format to use
     * @return
     */
    @GET
    @Path("instances/{alias}/export")
    @NoCache
    public Response export(
    		@PathParam("alias") String alias
    		) {
        this.auth.realm().requireViewIdentityProviders();

        IdentityProvidersFederationModel model = realm.getIdentityProvidersFederationByAlias(alias);

        if (model == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        try {
        	IdpFederationProvider idpFederationProvider = IdpFederationProviderFactory.getIdpFederationProviderFactoryById(session, model.getProviderId()).create(session, model, this.realm.getId());
        	return idpFederationProvider.export(session.getContext().getUri(), realm);
        } catch (Exception e) {
            return ErrorResponse.error("Could not export public broker configuration for IdP aggregation [" + model.getProviderId() + "].", Response.Status.NOT_FOUND);
        }

    }


    /**
     * Get a list with all identity provider federations of the realm
     *
     * @return
     */
    @GET
    @Path("instances")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<IdentityProvidersFederationRepresentation> list() {
        this.auth.realm().requireViewIdentityProviders();
        return realm.getIdentityProviderFederations().stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
    }

    /**
     * Create a new identity provider federation
     *
     * @param representation JSON body
     * @return
     */
    @POST
    @Path("instances")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(IdentityProvidersFederationRepresentation representation) {
        this.auth.realm().requireManageIdentityProviders();

        try {
        	IdentityProvidersFederationModel model = RepresentationToModel.toModel(representation);
        	IdpFederationProvider idpFederationProvider = IdpFederationProviderFactory.getIdpFederationProviderFactoryById(session, model.getProviderId()).create(session,model,this.realm.getId());
        	String id = idpFederationProvider.updateFederation();

			adminEvent.operation(OperationType.CREATE)
					.resourcePath(session.getContext().getUri(), representation.getAlias())
					.representation(representation).success();
			idpFederationProvider.enableUpdateTask();

			return Response.created(session.getContext().getUri().getAbsolutePathBuilder()
					.path(id).build()).build();

        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Identity Provider federation" + representation.getAlias() + " already exists");
        }
    }

    @GET
    @Path("instances/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public IdentityProvidersFederationRepresentation getIdentityProviderFederation(@PathParam("id") String internalId) {
        this.auth.realm().requireViewIdentityProviders();
        //create alias representation
        IdentityProvidersFederationModel model = realm.getIdentityProvidersFederationById(internalId);
        if (model == null) {
            throw new NotFoundException();
        }
        IdentityProvidersFederationRepresentation representation = ModelToRepresentation.toRepresentation(model);
        return representation;
    }

    /**
     * Delete the identity provider federation, along with all its IdPs
     *
     * @return
     */
    @DELETE
    @Path("instances/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public Response delete(@PathParam("id") String internalId) {
        this.auth.realm().requireManageIdentityProviders();

        IdentityProvidersFederationModel model = realm.getIdentityProvidersFederationById(internalId);
        if (model == null) {
            throw new NotFoundException();
        }
    	IdpFederationProvider idpFederationProvider = IdpFederationProviderFactory.getIdpFederationProviderFactoryById(session, model.getProviderId()).create(session,model,this.realm.getId());

    	idpFederationProvider.removeFederation();

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
        return Response.noContent().build();
    }
    
    @GET
    @Path("mapper-types")
    @NoCache
    public Map<String, IdentityProviderMapperTypeRepresentation> getMapperTypes() {
        this.auth.realm().requireViewIdentityProviders();

        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        Map<String, IdentityProviderMapperTypeRepresentation> types = new HashMap<>();
        List<ProviderFactory> factories = sessionFactory.getProviderFactories(IdentityProviderMapper.class);
        for (ProviderFactory factory : factories) {
            IdentityProviderMapper mapper = (IdentityProviderMapper)factory;
            for (String type : mapper.getCompatibleProviders()) {
                if (IdentityProviderMapper.ANY_PROVIDER.equals(type) || type.equals("saml")) {
                    IdentityProviderMapperTypeRepresentation rep = new IdentityProviderMapperTypeRepresentation();
                    rep.setId(mapper.getId());
                    rep.setCategory(mapper.getDisplayCategory());
                    rep.setName(mapper.getDisplayType());
                    rep.setHelpText(mapper.getHelpText());
                    List<ProviderConfigProperty> configProperties = mapper.getConfigProperties();
                    for (ProviderConfigProperty prop : configProperties) {
                        ConfigPropertyRepresentation propRep = ModelToRepresentation.toRepresentation(prop);
                        rep.getProperties().add(propRep);
                    }
                    types.put(rep.getId(), rep);
                    break;
                }
            }
        }
        return types;
    }
    
    
    @GET
    @Path("instances/{id}/mappers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FederationMapperRepresentation> getIdentityProviderFederationMappers(@PathParam("id") String internalId) {
        this.auth.realm().requireViewIdentityProviders();
        return realm.getIdentityProviderFederationMappers(internalId).stream().map( ModelToRepresentation::toRepresentation).collect(Collectors.toList());
    }
    
    @POST
    @Path("instances/{id}/mappers")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@PathParam("id") String internalId,FederationMapperRepresentation representation) {
        this.auth.realm().requireManageIdentityProviders();
        
        FederationMapperModel model = RepresentationToModel.toModel(representation);
        realm.addIdentityProvidersFederationMapper(model);
        
        adminEvent.operation(OperationType.CREATE)
        .resourcePath(session.getContext().getUri(), representation.getId())
        .representation(representation).success();
        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();
    }
    
    @GET
    @Path("instances/{id}/mappers/{mapperId}")
    @Produces(MediaType.APPLICATION_JSON)
    public FederationMapperRepresentation getIdentityProviderFederationMapper(@PathParam("id") String internalId,@PathParam("mapperId") String mapperId) {
        this.auth.realm().requireViewIdentityProviders();
        return ModelToRepresentation.toRepresentation(realm.getIdentityProviderFederationMapper(internalId,mapperId));
    }
    
    /**
     * Update a mapper for the identity provider federation
     *
     * @param id Mapper id
     * @param rep
     */
    @PUT
    @NoCache
    @Path("instances/{id}/mappers/{mapperId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateMapper(@PathParam("id") String id,@PathParam("mapperId") String mapperId, FederationMapperRepresentation representation) {
        this.auth.realm().requireManageIdentityProviders();


        FederationMapperModel model = realm.getIdentityProviderFederationMapper(id ,mapperId);
        if (model == null) throw new NotFoundException("Model not found");
        model = RepresentationToModel.toModel(representation);
        realm.updateIdentityProvidersFederationMapper(model);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.IDENTITY_PROVIDERS_FEDERATION).resourcePath(session.getContext().getUri()).representation(representation).success();

    }

    /**
     * Delete a mapper for the identity provider federation
     *
     * @param id Mapper id
     */
    @DELETE
    @NoCache
    @Path("instances/{id}/mappers/{mapperId}")
    public void deleteMapper(@PathParam("id") String id,@PathParam("mapperId") String mapperId) {
        this.auth.realm().requireManageIdentityProviders();

        realm.removeIdentityProvidersFederationMapper(mapperId);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.IDENTITY_PROVIDERS_FEDERATION).resourcePath(session.getContext().getUri()).success();

    }



}