package org.keycloak.protocol.oidc.federation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OpenIdFederationGeneralConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.models.enums.EntityTypeEnum;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.openid_federation.CommonMetadata;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.Metadata;
import org.keycloak.representations.openid_federation.OPMetadata;
import org.keycloak.representations.openid_federation.OpenIdFederationEntity;
import org.keycloak.representations.openid_federation.RPMetadata;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.OpenIdFederationTrustChainProcessorFactory;
import org.keycloak.utils.OpenIdFederationUtils;
import org.keycloak.utils.TrustChainProcessor;

public class OpenIdFederationWellKnownProvider extends OIDCWellKnownProvider {

    private final TrustChainProcessor trustChainProcessor;

    public OpenIdFederationWellKnownProvider(KeycloakSession session, Map<String, Object> openidConfigOverride) {
        super(session, openidConfigOverride, true);
        this.trustChainProcessor = session.getProvider(TrustChainProcessor.class, OpenIdFederationTrustChainProcessorFactory.PROVIDER_ID);
    }

    @Override
    public Object getConfig() {

        RealmModel realm = session.getContext().getRealm();
        OpenIdFederationGeneralConfig openIdFederationConfig = realm.getOpenIdFederationGeneralConfig();

        if (openIdFederationConfig ==  null || openIdFederationConfig.getOpenIdFederationList().isEmpty())
            throw new NotFoundException();

        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
        Metadata metadata = new Metadata();
        CommonMetadata common = OpenIdFederationUtils.commonMetadata(openIdFederationConfig);
        // Get registration types from general config
        Set<ClientRegistrationTypeEnum> opRegistrationTypes = openIdFederationConfig.getOpClientRegistrationTypesSupported() != null ? 
            new java.util.HashSet<>(openIdFederationConfig.getOpClientRegistrationTypesSupported()) : 
            Set.of();
        Set<ClientRegistrationTypeEnum> rpRegistrationTypes = openIdFederationConfig.getRpClientRegistrationTypesSupported() != null ? 
            new java.util.HashSet<>(openIdFederationConfig.getRpClientRegistrationTypesSupported()) : 
            Set.of();
        Set<ClientRegistrationTypeEnum> allRegistrationTypes = new java.util.HashSet<>();
        allRegistrationTypes.addAll(opRegistrationTypes);
        allRegistrationTypes.addAll(rpRegistrationTypes);

        if (openIdFederationConfig.getEntityTypes() != null && openIdFederationConfig.getEntityTypes().contains(EntityTypeEnum.OPENID_PROVIDER)) {
            OPMetadata opMetadata;
            try {
                opMetadata = from(((OIDCConfigurationRepresentation) super.getConfig()));
            } catch (IOException e) {
                throw new InternalServerErrorException("Could not form the configuration response");
            }

            if (opRegistrationTypes.contains(ClientRegistrationTypeEnum.EXPLICIT)) {
                opMetadata.setFederationRegistrationEndpoint(backendUriInfo.getBaseUriBuilder().clone().path(RealmsResource.class).path(RealmsResource.class, "getOpenIdFederationClientsService").build(realm.getName()).toString());
            }
            opMetadata.setClientRegistrationTypesSupported(opRegistrationTypes.stream().map(ClientRegistrationTypeEnum::getValue).collect(Collectors.toList()));
            metadata.setOpenIdProviderMetadata(opMetadata);
        }

        if (openIdFederationConfig.getEntityTypes() != null && openIdFederationConfig.getEntityTypes().contains(EntityTypeEnum.OPENID_RELYING_PARTY)) {
            RPMetadata rPMetadata = OpenIdFederationUtils.createRPMetadata(openIdFederationConfig, rpRegistrationTypes.stream(), null, RealmsResource.protocolUrl(backendUriInfo).clone().path(OIDCLoginProtocolService.class, "certs").build(realm.getName(),
                    OIDCLoginProtocol.LOGIN_PROTOCOL).toString(), frontendUriInfo, realm.getName());
            // For now, use default subject types since we removed the individual federation configs
            List<String> openIdFederationSubjectTypes = OIDCWellKnownProvider.DEFAULT_SUBJECT_TYPES_SUPPORTED;
            rPMetadata.setSubjectTypesSupported(openIdFederationSubjectTypes);
            metadata.setRelyingPartyMetadata(rPMetadata);
        }

        if (openIdFederationConfig.getFederationResolveEndpoint() != null || openIdFederationConfig.getFederationHistoricalKeysEndpoint() != null ||
                openIdFederationConfig.getOrganizationName() != null || openIdFederationConfig.getContacts() != null ||
                openIdFederationConfig.getOrganizationUri() != null || openIdFederationConfig.getPolicyUri() != null || openIdFederationConfig.getLogoUri() != null) {
            OpenIdFederationEntity federationEntity = getOpenIdFederationEntity(openIdFederationConfig, common);
            metadata.setFederationEntity(federationEntity);
        }

        EntityStatement entityStatement = new EntityStatement(Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName()), Long.valueOf(openIdFederationConfig.getLifespan()), new ArrayList<>(openIdFederationConfig.getAuthorityHints()), trustChainProcessor.getKeySet());
        entityStatement.setMetadata(metadata);

        return session.tokens().encodeForOpenIdFederation(entityStatement);
    }

    private static OpenIdFederationEntity getOpenIdFederationEntity(OpenIdFederationGeneralConfig openIdFederationConfig, CommonMetadata common) {
        OpenIdFederationEntity federationEntity = new OpenIdFederationEntity();
        federationEntity.setFederationResolveEndpoint(openIdFederationConfig.getFederationResolveEndpoint());
        federationEntity.setFederationHistoricalKeysEndpoint(openIdFederationConfig.getFederationHistoricalKeysEndpoint());
        federationEntity.setContacts(openIdFederationConfig.getContacts());
        federationEntity.setLogoUri(openIdFederationConfig.getLogoUri());
        federationEntity.setPolicyUri(openIdFederationConfig.getPolicyUri());
        federationEntity.setCommonMetadata(common);
        return federationEntity;
    }

    private static OPMetadata from(OIDCConfigurationRepresentation representation) throws IOException {
        return JsonSerialization.readValue(JsonSerialization.writeValueAsString(representation), OPMetadata.class);
    }

}
