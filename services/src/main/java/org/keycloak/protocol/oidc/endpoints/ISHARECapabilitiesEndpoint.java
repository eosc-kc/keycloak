package org.keycloak.protocol.oidc.endpoints;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.TokenCategory;
import org.keycloak.common.ClientConnection;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.cors.Cors;
import org.keycloak.utils.MediaType;
import org.keycloak.utils.OAuth2Error;
import org.keycloak.utils.StringUtil;
import org.keycloak.wellknown.WellKnownProvider;

import org.jboss.resteasy.reactive.NoCache;

public class ISHARECapabilitiesEndpoint {
    private final HttpRequest request;

    private final KeycloakSession session;

    private final ClientConnection clientConnection;

    private final RealmModel realm;
    private final OAuth2Error error;
    private Cors cors;

    public ISHARECapabilitiesEndpoint(KeycloakSession session) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        this.realm = session.getContext().getRealm();
        this.error = new OAuth2Error().json(false).realm(realm);
        this.request = session.getContext().getHttpRequest();
    }

    @Path("/")
    @OPTIONS
    public Response issueUserInfoPreflight() {
        return Cors.builder().auth().preflight().add(Response.ok());
    }

    @Path("/")
    @GET
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public Response issueUserInfoGet() {
        setupCors();
        return issueCapabilities();
    }

    private Response issueCapabilities() {
        cors.allowAllOrigins();

        EventBuilder event = new EventBuilder(realm, session, clientConnection)
                .event(EventType.ISHARE_CAPABILITIES_REQUEST)
                .detail(Details.AUTH_METHOD, Details.VALIDATE_ACCESS_TOKEN);

        WellKnownProvider oidcProvider = session.getProvider(WellKnownProvider.class, OIDCWellKnownProviderFactory.PROVIDER_ID);
        OIDCConfigurationRepresentation oidcConfig = OIDCConfigurationRepresentation.class.cast(oidcProvider.getConfig());

        String issuer = StringUtil.isNotBlank(realm.getIssuer()) ? realm.getIssuer() : Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", issuer);
        claims.put("sub", issuer);
        claims.put("aud", issuer);
        //Todo put public in aud
//        claims.put("aud", "public");
        claims.put("jti", UUID.randomUUID().toString());

        Instant now = Instant.now();
        claims.put("iat", now.getEpochSecond());
        claims.put("nbf", now.getEpochSecond());
        claims.put("exp", Date.from(now.plus(30L, ChronoUnit.SECONDS)).getTime() / 1000);

        // create own token category?
        String signatureAlgorithm = session.tokens().signatureAlgorithm(TokenCategory.USERINFO);

        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);
        SignatureSignerContext signer = signatureProvider.signer();

        JWSBuilder jwsBuilder = new JWSBuilder().x5c(signer.getCertificateChain());

        Map<String, Object> capInfo = new HashMap<>();
        capInfo.put("party_id", issuer);

        List<Object> roles = new LinkedList<>();
        Map<String, Object> our_role = new HashMap<>();
        our_role.put("role", "IdentityProvider");
        roles.add(our_role);
        capInfo.put("ishare_roles", roles);

        List<Object> supportedVersions = new LinkedList<>();

        Map<String, Object> version = new HashMap<>();
        version.put("version", "1.7");

        Map<String, Object> features = new HashMap<>();
        List<Object> publicFeatures = new LinkedList<>();


        String userinfoEP = oidcConfig.getUserinfoEndpoint();
        String tokenEP = oidcConfig.getTokenEndpoint();
        String authEP = oidcConfig.getAuthorizationEndpoint();
        String capEP = userinfoEP.replace("userinfo", "capabilities"); // a wicked hacky hack

        publicFeatures.add(createFeature("oidc token", "OIDC iSHARE Access Token", "Call to get access token for code", tokenEP, null));
        publicFeatures.add(createFeature("oidc authorize", "OIDC iSHARE Authorization", "Initiates iSHARE OIDC Flow", authEP, null));
        publicFeatures.add(createFeature("oidc user info", "OIDC iSHARE User Info", "Obtains user info", userinfoEP, tokenEP));
        publicFeatures.add(createFeature("capabilities", "capabilities", "Retrieves iSHARE capabilities", capEP, null));

        features.put("public", publicFeatures);

        List<Object> featureList = new LinkedList<>();
        featureList.add(features);

        version.put("supported_features", featureList);

        supportedVersions.add(version);

        capInfo.put("supported_versions", supportedVersions);

        claims.put("capabilities_info", capInfo);

        String signedCapabilitiesInfo = jwsBuilder.type("JWT").jsonContent(claims).sign(signer);
        Map<String, Object> capResponse = new HashMap<>();
        capResponse.put("capabilities_token", signedCapabilitiesInfo);
        event.success();

        return cors.add(Response.ok(capResponse).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON));
    }

    private Map<String, Object> createFeature(String id, String name, String desc, String url, String token_endpoint)
    {
        Map<String, Object> feature = new HashMap<>();
        feature.put("id", id);
        feature.put("feature", name);
        feature.put("description", desc);
        feature.put("url", url);
        if (token_endpoint != null) {
            feature.put("token_endpoint", token_endpoint);
        }
        return feature;
    }

    private void setupCors() {
        cors = Cors.builder().auth().allowedMethods(request.getHttpMethod()).auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);
        error.cors(cors);
    }
}
