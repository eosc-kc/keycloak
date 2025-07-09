package org.keycloak.protocol.oidc.federation;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.UriInfo;
import org.keycloak.TokenCategory;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OpenIdFederationGeneralConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.models.enums.EntityTypeEnum;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.openid_federation.CommonMetadata;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.OpenIdFederationEntity;
import org.keycloak.representations.openid_federation.Metadata;
import org.keycloak.representations.openid_federation.OPMetadata;
import org.keycloak.representations.openid_federation.RPMetadata;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OpenIdFederationWellKnownProvider extends OIDCWellKnownProvider {

    public OpenIdFederationWellKnownProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    public Object getConfig() {

        RealmModel realm = session.getContext().getRealm();
        OpenIdFederationGeneralConfig openIdFederationConfig = realm.getOpenIdFederationGeneralConfig();

        if (openIdFederationConfig ==  null || openIdFederationConfig.getOpenIdFederationList() == null || openIdFederationConfig.getOpenIdFederationList().isEmpty())
            throw new NotFoundException();

        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);
        Metadata metadata = new Metadata();
        CommonMetadata common = commonMetadata(openIdFederationConfig);
        Set<ClientRegistrationTypeEnum> registrationTypes = openIdFederationConfig.getOpenIdFederationList().stream().flatMap(x -> x.getClientRegistrationTypesSupported().stream()).collect(Collectors.toSet());

        if (openIdFederationConfig.getOpenIdFederationList().stream().flatMap(x -> x.getEntityTypes().stream()).collect(Collectors.toSet()).contains(EntityTypeEnum.OPENID_PROVIDER)) {
            OPMetadata opMetadata;
            try {
                opMetadata = from(((OIDCConfigurationRepresentation) super.getConfig()));
            } catch (IOException e) {
                throw new InternalServerErrorException("Could not form the configuration response");
            }

            if (registrationTypes.contains(ClientRegistrationTypeEnum.EXPLICIT)) {
                opMetadata.setFederationRegistrationEndpoint(backendUriInfo.getBaseUriBuilder().clone().path(RealmsResource.class).path(RealmsResource.class, "getOpenIdFederationClientsService").build(realm.getName()).toString());
            }
            opMetadata.setClientRegistrationTypes(registrationTypes.stream().map(ClientRegistrationTypeEnum::getValue).collect(Collectors.toList()));
            opMetadata.setContacts(openIdFederationConfig.getContacts());
            opMetadata.setLogoUri(openIdFederationConfig.getLogoUri());
            opMetadata.setPolicyUri(openIdFederationConfig.getPolicyUri());
            opMetadata.setCommonMetadata(common);
            metadata.setOpenIdProviderMetadata(opMetadata);
        }

        if (openIdFederationConfig.getOpenIdFederationList().stream().flatMap(x -> x.getEntityTypes().stream()).collect(Collectors.toSet()).contains(EntityTypeEnum.OPENID_RELAYING_PARTY)) {
            RPMetadata rPMetadata = new RPMetadata();
            rPMetadata.setClientRegistrationTypes(registrationTypes.stream().map(ClientRegistrationTypeEnum::getValue).collect(Collectors.toList()));
            rPMetadata.setContacts(openIdFederationConfig.getContacts());
            rPMetadata.setLogoUri(openIdFederationConfig.getLogoUri());
            rPMetadata.setPolicyUri(openIdFederationConfig.getPolicyUri());
            rPMetadata.setCommonMetadata(common);
            metadata.setRelyingPartyMetadata(rPMetadata);
        }

        OpenIdFederationEntity federationEntity = null;
        if (openIdFederationConfig.getFederationResolveEndpoint() != null || openIdFederationConfig.getFederationHistoricalKeysEndpoint() != null ||
                openIdFederationConfig.getOrganizationName() != null || ! openIdFederationConfig.getContacts().isEmpty() ||
                openIdFederationConfig.getHomepageUri() != null || openIdFederationConfig.getPolicyUri() != null || openIdFederationConfig.getLogoUri() != null) {
            federationEntity = new OpenIdFederationEntity();
            federationEntity.setFederationResolveEndpoint(openIdFederationConfig.getFederationResolveEndpoint());
            federationEntity.setFederationHistoricalKeysEndpoint(openIdFederationConfig.getFederationHistoricalKeysEndpoint());
            federationEntity.setContacts(openIdFederationConfig.getContacts());
            federationEntity.setLogoUri(openIdFederationConfig.getLogoUri());
            federationEntity.setPolicyUri(openIdFederationConfig.getPolicyUri());
            federationEntity.setCommonMetadata(common);
        }

        metadata.setFederationEntity(federationEntity);

        EntityStatement entityStatement = new EntityStatement();
        entityStatement.setMetadata(metadata);
        entityStatement.setAuthorityHints(new ArrayList<>(openIdFederationConfig.getAuthorityHints()));
        entityStatement.setJwks(getKeySet());
        entityStatement.issuer(Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName()));
        entityStatement.subject(Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName()));
        entityStatement.issuedNow();
        entityStatement.exp((long) Time.currentTime() + Long.valueOf(openIdFederationConfig.getLifespan()));
        entityStatement.type(TokenUtil.ENTITY_STATEMENT_JWT);

        String encodedToken = session.tokens().encodeForOpenIdFederation(entityStatement);

        return encodedToken;
    }

    private static OPMetadata from(OIDCConfigurationRepresentation representation) throws IOException {
        return JsonSerialization.readValue(JsonSerialization.writeValueAsString(representation), OPMetadata.class);
    }

    private CommonMetadata commonMetadata(OpenIdFederationGeneralConfig realmConfig){
        CommonMetadata common = new CommonMetadata();
        common.setHomepageUri(realmConfig.getHomepageUri());
        common.setOrganizationName(realmConfig.getOrganizationName());
        return common;
    }

    private JSONWebKeySet getKeySet() {
        List<JWK> keys = new LinkedList<>();
        session.keys().getKeysStream(session.getContext().getRealm())
                .filter(k -> k.getStatus().isEnabled() && k.getUse().equals(KeyUse.SIG) && k.getPublicKey() != null && k.getAlgorithm().equals(session.tokens().signatureAlgorithm(TokenCategory.ENTITY_STATEMENT)))
                .forEach(k -> {
                    JWKBuilder b = JWKBuilder.create().kid(k.getKid()).algorithm(k.getAlgorithm());
                    if (k.getType().equals(KeyType.RSA)) {
                        keys.add(b.rsa(k.getPublicKey(), k.getCertificate()));
                    } else if (k.getType().equals(KeyType.EC)) {
                        keys.add(b.ec(k.getPublicKey()));
                    }
                });

        JSONWebKeySet keySet = new JSONWebKeySet();

        JWK[] k = new JWK[keys.size()];
        k = keys.toArray(k);
        keySet.setKeys(k);
        return keySet;
    }

}
