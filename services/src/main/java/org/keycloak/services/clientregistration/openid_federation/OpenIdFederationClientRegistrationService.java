package org.keycloak.services.clientregistration.openid_federation;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.models.enums.EntityTypeEnum;
import org.keycloak.protocol.trustchain.TrustChainProcessor;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.EntityStatementExplicitResponse;
import org.keycloak.representations.openid_federation.RPMetadata;
import org.keycloak.representations.openid_federation.TrustChainResolution;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.clientregistration.AbstractClientRegistrationProvider;
import org.keycloak.services.clientregistration.oidc.DescriptionConverter;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.OpenIdFederationTrustChainProcessorFactory;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenIdFederationClientRegistrationService extends AbstractClientRegistrationProvider {

    private static final Logger logger = Logger.getLogger(OpenIdFederationClientRegistrationService.class);
    private final TrustChainProcessor trustChainProcessor;

    public OpenIdFederationClientRegistrationService(KeycloakSession session) {
        super(session);
        this.trustChainProcessor = session.getProvider(TrustChainProcessor.class, OpenIdFederationTrustChainProcessorFactory.PROVIDER_ID);
    }

    @POST
    @Consumes({"application/entity-statement+jwt", "application/trust-chain+json"})
    public Response explicitClientRegistration(String body, @Context HttpHeaders headers) {
        RealmModel realm = session.getContext().getRealm();
        if (!realm.isOpenIdFederationTypeRegistrationSupported(EntityTypeEnum.OPENID_PROVIDER, ClientRegistrationTypeEnum.EXPLICIT) || realm.getOpenIdFederations().isEmpty()) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "Explicit OpenID Federation Client Registration is not supported in this realm", Response.Status.BAD_REQUEST);
        }
        checkSsl();

        if ("application/entity-statement+jwt".equals(headers.getMediaType().toString())) {
            EntityStatement statement = null;
            try {
                statement = trustChainProcessor.parseAndValidateSelfSigned(body);
            } catch (InvalidTrustChainException ex) {
                logger.error("Entity statement is not valid", ex);
                throw new ErrorResponseException(Errors.INVALID_REQUEST, "Entity statement is not valid", Response.Status.BAD_REQUEST);
            }

            trustChainProcessor.validationRules(statement, true);

            logger.info("starting validating trust chains");
            return commonExplicitRegistrationProcess(statement, realm);

        } else {
            EntityStatement statement = null;
            try {
                List<String> trustChains = JsonSerialization.readValue(body, new TypeReference<List<String>>() {});
                statement = trustChainProcessor.parseAndValidateSelfSigned(trustChains.get(0));
            } catch (Exception e) {
                throw new ErrorResponseException(Errors.INVALID_REQUEST, "Body must be Trust Chains", Response.Status.BAD_REQUEST);
            }
            trustChainProcessor.validationRules(statement, false);
            return commonExplicitRegistrationProcess(statement, realm);
        }
    }

    private Response commonExplicitRegistrationProcess(EntityStatement statement, RealmModel realm){
        logger.info("starting validating trust chains");
        TrustChainResolution validChain = trustChainProcessor.constructTrustChains(statement, realm.getOpenIdFederationsTrustAnchors(), true);
        if (validChain == null) {
            throw new ErrorResponseException(Errors.INVALID_TRUST_ANCHOR, "No trusted trust anchor could be found", Response.Status.NOT_FOUND);
        }
        RPMetadata rPMetadata = (RPMetadata) validChain.getMetadataAfterPolicies();

        ClientRepresentation client = createOrUpdateClient(statement, rPMetadata);
        URI uri = getRegistrationClientUri(client.getClientId());
        RPMetadata rPMetadataResponse = DescriptionConverter.toExternalResponse(session, client, uri, RPMetadata.class, rPMetadata.getScope()!=null && rPMetadata.getScope().contains(OAuth2Constants.SCOPE_OPENID));
        event.detail(Details.GRANTED_CLIENT, rPMetadataResponse.getScope());
        rPMetadataResponse.setClientIdIssuedAt(Time.currentTime());

        rPMetadataResponse.setClientRegistrationTypes(Stream.of(ClientRegistrationTypeEnum.EXPLICIT.getValue()).collect(Collectors.toList()));
        rPMetadataResponse.setCommonMetadata(rPMetadata.getCommonMetadata());
        EntityStatementExplicitResponse responseStatement = new EntityStatementExplicitResponse(statement, Urls.realmIssuer(session.getContext().getUri(UrlType.FRONTEND).getBaseUri(), session.getContext().getRealm().getName()), rPMetadataResponse, validChain.getTrustAnchorId(), validChain.getLeafId());
        responseStatement.type(TokenUtil.EXPLICIT_REGISTRATION_RESPONSE_JWT);
        String token = session.tokens().encodeForOpenIdFederation(responseStatement);
        return Response.ok(token).header("Content-Type", TokenUtil.APPLICATION_EXPLICIT_REGISTRATION_RESPONSE_JWT).build();
    }

    public ClientRepresentation createOrUpdateClient(EntityStatement statement, RPMetadata rPMetadata){
        if (rPMetadata.getJwks() == null && rPMetadata.getJwksUri() == null) {
            rPMetadata.setJwks(statement.getJwks());
        }
        rPMetadata.setClientId(statement.getSubject());

        ClientRepresentation client;
        try {
            if (session.getContext().getRealm().getClientByClientId(rPMetadata.getClientId()) == null) {
                client = createOidcClient(rPMetadata, session, statement.getExp());
            } else {
                client = updateOidcClient(rPMetadata.getClientId(), rPMetadata, session, statement.getExp());
            }
        } catch (Exception e) {
            logger.error("The following error was thrown during OpenId Federation Client explicit registration", e);
            throw new ErrorResponseException(Errors.INVALID_METADATA, "Client metadata invalid", Response.Status.BAD_REQUEST);
        }
        event.detail(Details.REQUESTED_SCOPES, client.getOptionalClientScopes());
        return client;
    }

    private void checkSsl() {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https") && session.getContext().getRealm().getSslRequired().isRequired(session.getContext().getConnection())) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

}
