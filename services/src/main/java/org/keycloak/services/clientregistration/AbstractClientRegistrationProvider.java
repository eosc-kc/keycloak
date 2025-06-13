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

package org.keycloak.services.clientregistration;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.mappers.AbstractPairwiseSubMapper;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper;
import org.keycloak.protocol.oidc.utils.SubjectType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisteredContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdatedContext;
import org.keycloak.services.clientregistration.oidc.DescriptionConverter;
import org.keycloak.services.clientregistration.oidc.OIDCClientRegistrationContext;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.scheduled.AutoUpdateSAMLClient;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;
import java.time.Instant;
import org.keycloak.validation.ValidationUtil;

import jakarta.ws.rs.core.Response;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractClientRegistrationProvider implements ClientRegistrationProvider {

    protected KeycloakSession session;
    protected EventBuilder event;
    protected ClientRegistrationAuth auth;

    public AbstractClientRegistrationProvider(KeycloakSession session) {
        this.session = session;
    }

    protected OIDCClientRepresentation createOidcClient(OIDCClientRepresentation clientOIDC, KeycloakSession session, Long exp){
        ClientRepresentation client = DescriptionConverter.toInternal(session, clientOIDC);
        List<String> grantTypes = clientOIDC.getGrantTypes();

        if (grantTypes != null && grantTypes.contains(OAuth2Constants.UMA_GRANT_TYPE)) {
            client.setAuthorizationServicesEnabled(true);
        }

        if (!(grantTypes == null || grantTypes.contains(OAuth2Constants.REFRESH_TOKEN))) {
            OIDCAdvancedConfigWrapper.fromClientRepresentation(client).setUseRefreshToken(false);
        }

        if (exp != null)
            client.getAttributes().put(OIDCConfigAttributes.EXPIRATION_TIME, exp.toString() );

        OIDCClientRegistrationContext oidcContext = new OIDCClientRegistrationContext(session, client, this, clientOIDC);
        client = create(oidcContext, exp == null ? EventType.CLIENT_REGISTER : EventType.FEDERATION_CLIENT_REGISTER);

        ClientModel clientModel = session.getContext().getRealm().getClientByClientId(client.getClientId());
        updatePairwiseSubMappers(clientModel, SubjectType.parse(clientOIDC.getSubjectType()), clientOIDC.getSectorIdentifierUri());
        updateClientRepWithProtocolMappers(clientModel, client);

        validateClient(clientModel, clientOIDC, true);

        URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(client.getClientId()).build();
        clientOIDC = DescriptionConverter.toExternalResponse(session, client, uri);
        clientOIDC.setClientIdIssuedAt(Time.currentTime());
        return clientOIDC;
    }

    protected void updatePairwiseSubMappers(ClientModel clientModel, SubjectType subjectType, String sectorIdentifierUri) {
        if (subjectType == SubjectType.PAIRWISE) {

            // See if we have existing pairwise mapper and update it. Otherwise create new
            AtomicBoolean foundPairwise = new AtomicBoolean(false);

            clientModel.getProtocolMappersStream().filter((ProtocolMapperModel mapping) -> {
                if (mapping.getProtocolMapper().endsWith(AbstractPairwiseSubMapper.PROVIDER_ID_SUFFIX)) {
                    foundPairwise.set(true);
                    return true;
                } else {
                    return false;
                }
            }).collect(Collectors.toList()).forEach((ProtocolMapperModel mapping) -> {
                PairwiseSubMapperHelper.setSectorIdentifierUri(mapping, sectorIdentifierUri);
                clientModel.updateProtocolMapper(mapping);
            });

            // We don't have existing pairwise mapper. So create new
            if (!foundPairwise.get()) {
                ProtocolMapperRepresentation newPairwise = SHA256PairwiseSubMapper.createPairwiseMapper(sectorIdentifierUri, null);
                clientModel.addProtocolMapper(RepresentationToModel.toModel(newPairwise));
            }

        } else {
            // Rather find and remove all pairwise mappers
            clientModel.getProtocolMappersStream()
                    .filter(mapperRep -> mapperRep.getProtocolMapper().endsWith(AbstractPairwiseSubMapper.PROVIDER_ID_SUFFIX))
                    .collect(Collectors.toList())
                    .forEach(clientModel::removeProtocolMapper);
        }
    }

    protected void updateClientRepWithProtocolMappers(ClientModel clientModel, ClientRepresentation rep) {
        List<ProtocolMapperRepresentation> mappings =
                clientModel.getProtocolMappersStream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
        rep.setProtocolMappers(mappings);
    }

    public ClientRepresentation create(ClientRegistrationContext context, EventType eventType) {
        ClientRepresentation client = context.getClient();

        event.event(eventType);

        RegistrationAuth registrationAuth = auth.requireCreate(context);

        try {
            RealmModel realm = session.getContext().getRealm();
            ClientModel clientModel = ClientManager.createClient(session, realm, client);

            if (client.getDefaultRoles() != null) {
                for (String name : client.getDefaultRoles()) {
                    addDefaultRole(clientModel, name);
                }
            }

            if (clientModel.isServiceAccountsEnabled()) {
                new ClientManager(new RealmManager(session)).enableServiceAccount(clientModel);
            }

            if (Boolean.TRUE.equals(client.getAuthorizationServicesEnabled())) {
                RepresentationToModel.createResourceServer(clientModel, session, true);
            }

            session.getContext().setClient(clientModel);
            session.clientPolicy().triggerOnEvent(new DynamicClientRegisteredContext(context, clientModel, auth.getJwt(), realm));
            ClientRegistrationPolicyManager.triggerAfterRegister(context, registrationAuth, clientModel);

            client = ModelToRepresentation.toRepresentation(clientModel, session);

            client.setSecret(clientModel.getSecret());

            String registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, clientModel, registrationAuth);
            client.setRegistrationAccessToken(registrationAccessToken);

            if (auth.isInitialAccessToken()) {
                ClientInitialAccessModel initialAccessModel = auth.getInitialAccessModel();
                session.realms().decreaseRemainingCount(realm, initialAccessModel);
            }

            client.setDirectAccessGrantsEnabled(false);

            Stream<String> defaultRolesNames = getDefaultRolesStream(clientModel);
            if (defaultRolesNames != null) {
                client.setDefaultRoles(defaultRolesNames.toArray(String[]::new));
            }

            //saml autoupdated schedule task
            if ("saml".equals(clientModel.getProtocol()) && clientModel.getAttributes() != null && Boolean.valueOf(clientModel.getAttributes().get(SamlConfigAttributes.SAML_AUTO_UPDATED))) {
                AutoUpdateSAMLClient autoUpdateProvider = new AutoUpdateSAMLClient(clientModel.getId(), realm.getId());
                Long interval = Long.parseLong(clientModel.getAttributes().get(SamlConfigAttributes.SAML_REFRESH_PERIOD))* 1000;
                ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), autoUpdateProvider, interval);
                session.getProvider(TimerProvider.class).schedule(taskRunner, interval, "AutoUpdateSAMLClient_" + clientModel.getId());
            }

            event.client(client.getClientId()).success();
            return client;
        } catch (ModelDuplicateException e) {
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier in use", Response.Status.BAD_REQUEST);
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }
    }

    public ClientRepresentation get(ClientModel client) {
        event.event(EventType.CLIENT_INFO);

        auth.requireView(client);

        ClientRepresentation rep = ModelToRepresentation.toRepresentation(client, session);
        if (!(Boolean.TRUE.equals(rep.isBearerOnly()) || Boolean.TRUE.equals(rep.isPublicClient()))) {
            rep.setSecret(client.getSecret());
        }

        if (auth.isRegistrationAccessToken()) {
            String registrationAccessToken = ClientRegistrationTokenUtils.updateTokenSignature(session, auth);
            rep.setRegistrationAccessToken(registrationAccessToken);
        }

        Stream<String> defaultRolesNames = getDefaultRolesStream(client);
        if (defaultRolesNames != null) {
            rep.setDefaultRoles(defaultRolesNames.toArray(String[]::new));
        }

        event.client(client.getClientId()).success();
        return rep;
    }

    public ClientRepresentation update(String clientId, ClientRegistrationContext context) {
        ClientRepresentation rep = context.getClient();

        event.event(EventType.CLIENT_UPDATE).client(clientId);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        session.setAttribute(ClientRegistrationAccessTokenConstants.ROTATION_ENABLED, true);
        RegistrationAuth registrationAuth = auth.requireUpdate(context, client);

        if (!client.getClientId().equals(rep.getClientId())) {
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier modified", Response.Status.BAD_REQUEST);
        }

        UserModel serviceAccount = this.session.users().getServiceAccount(client);
        if (TRUE.equals(rep.isServiceAccountsEnabled()) && serviceAccount == null) {
            new ClientManager(new RealmManager(session)).enableServiceAccount(client);
        } else if (serviceAccount != null && FALSE.equals(rep.isServiceAccountsEnabled())) {
                new UserManager(session).removeUser(session.getContext().getRealm(), serviceAccount);
        }

        RepresentationToModel.updateClient(rep, client, session);
        RepresentationToModel.updateClientProtocolMappers(rep, client);

        if (rep.getDefaultRoles() != null) {
            updateDefaultRoles(client, rep.getDefaultRoles());
        }

        rep = ModelToRepresentation.toRepresentation(client, session);

        rep.setSecret(client.getSecret());

        Stream<String> defaultRolesNames = getDefaultRolesStream(client);
        if (defaultRolesNames != null) {
            rep.setDefaultRoles(defaultRolesNames.toArray(String[]::new));
        }

        if (auth.isRegistrationAccessToken()) {
            String registrationAccessToken;
            if ((boolean) session.getAttribute(ClientRegistrationAccessTokenConstants.ROTATION_ENABLED)) {
                registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, client, auth.getRegistrationAuth());
            } else {
                registrationAccessToken = ClientRegistrationTokenUtils.updateTokenSignature(session, auth);
            }
            rep.setRegistrationAccessToken(registrationAccessToken);
        }
        session.removeAttribute(ClientRegistrationAccessTokenConstants.ROTATION_ENABLED);

        try {
            session.getContext().setClient(client);
            session.clientPolicy().triggerOnEvent(new DynamicClientUpdatedContext(session, client, auth.getJwt(), client.getRealm()));
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }
        ClientRegistrationPolicyManager.triggerAfterUpdate(context, registrationAuth, client);

        if ("saml".equals(rep.getProtocol()) && rep.getAttributes() != null && Boolean.valueOf(rep.getAttributes().get(SamlConfigAttributes.SAML_AUTO_UPDATED)) && !rep.getAttributes().get(SamlConfigAttributes.SAML_REFRESH_PERIOD).equals(client.getAttributes().get(SamlConfigAttributes.SAML_REFRESH_PERIOD))) {
            //saml autoupdated schedule task ( autoupdate with different refresh period)
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTask("AutoUpdateSAMLClient_" + client.getId());
            AutoUpdateSAMLClient autoUpdateProvider = new AutoUpdateSAMLClient(client.getId(), session.getContext().getRealm().getId());
            Long interval = Long.parseLong(rep.getAttributes().get(SamlConfigAttributes.SAML_REFRESH_PERIOD))* 1000;
            Long delay = client.getAttributes().get(SamlConfigAttributes.SAML_LAST_REFRESH_TIME) == null ? 1 : Long.parseLong(client.getAttributes().get(SamlConfigAttributes.SAML_LAST_REFRESH_TIME) )+ Long.parseLong(rep.getAttributes().get(SamlConfigAttributes.SAML_REFRESH_PERIOD)) * 1000 - Instant.now().toEpochMilli();
            ClusterAwareScheduledTaskRunner taskRunner = new ClusterAwareScheduledTaskRunner(session.getKeycloakSessionFactory(), autoUpdateProvider, interval);
            timer.schedule(taskRunner, delay > 1000 ? delay : 1000, interval, "AutoUpdateSAMLClient_" + client.getId());
        } else  if ("saml".equals(rep.getProtocol()) && rep.getAttributes() != null && ! Boolean.valueOf(rep.getAttributes().get(SamlConfigAttributes.SAML_AUTO_UPDATED)) && Boolean.valueOf(client.getAttributes().get(SamlConfigAttributes.SAML_AUTO_UPDATED))) {
            //saml remove autoupdate
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTask("AutoUpdateSAMLClient_" + client.getId());
        }

        event.client(client.getClientId()).success();
        return rep;
    }


    public void delete(String clientId) {
        event.event(EventType.CLIENT_DELETE).client(clientId);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        auth.requireDelete(client);

        if (new ClientManager(new RealmManager(session)).removeClient(session.getContext().getRealm(), client)) {
            event.client(client.getClientId()).success();
        } else {
            throw new ForbiddenException();
        }
    }

    public void validateClient(ClientModel clientModel, OIDCClientRepresentation oidcClient, boolean create) {
        ValidationUtil.validateClient(session, clientModel, oidcClient, create, r -> {
            session.getTransactionManager().setRollbackOnly();
            String errorCode = r.fieldHasError("redirectUris") ? ErrorCodes.INVALID_REDIRECT_URI : ErrorCodes.INVALID_CLIENT_METADATA;
            throw new ErrorResponseException(errorCode, r.getAllErrorsAsString(), Response.Status.BAD_REQUEST);
        });
    }

    public void validateClient(ClientRepresentation clientRep, boolean create) {
        validateClient(session.getContext().getRealm().getClientByClientId(clientRep.getClientId()), null, create);
    }

    @Override
    public void setAuth(ClientRegistrationAuth auth) {
        this.auth = auth;
    }

    @Override
    public ClientRegistrationAuth getAuth() {
        return this.auth;
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

    @Override
    public EventBuilder getEvent() {
        return event;
    }

    @Override
    public void close() {
    }

    /* ===========  default roles =========== */

    private void addDefaultRole(ClientModel client, String name) {
        client.getRealm().getDefaultRole().addCompositeRole(getOrAddRoleId(client, name));
    }

    private RoleModel getOrAddRoleId(ClientModel client, String name) {
        RoleModel role = client.getRole(name);
        if (role == null) {
            role = client.addRole(name);
        }
        return role;
    }

    private Stream<String> getDefaultRolesStream(ClientModel client) {
        return client.getRealm().getDefaultRole().getCompositesStream()
                .filter(role -> role.isClientRole() && Objects.equals(role.getContainerId(), client.getId()))
                .map(RoleModel::getName);
    }

    private void updateDefaultRoles(ClientModel client, String... defaultRoles) {
        List<String> defaultRolesArray = Arrays.asList(String.valueOf(defaultRoles));
        Collection<String> entities = getDefaultRolesStream(client).collect(Collectors.toList());
        Set<String> already = new HashSet<>();
        ArrayList<String> remove = new ArrayList<>();
        for (String rel : entities) {
            if (! defaultRolesArray.contains(rel)) {
                remove.add(rel);
            } else {
                already.add(rel);
            }
        }
        removeDefaultRoles(client, remove.toArray(new String[] {}));

        for (String roleName : defaultRoles) {
            if (!already.contains(roleName)) {
                addDefaultRole(client, roleName);
            }
        }
    }

    private void removeDefaultRoles(ClientModel client, String... defaultRoles) {
        for (String defaultRole : defaultRoles) {
            client.getRealm().getDefaultRole().removeCompositeRole(client.getRole(defaultRole));
        }
    }
}
