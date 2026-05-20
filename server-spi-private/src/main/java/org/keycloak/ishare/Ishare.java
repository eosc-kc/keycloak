package org.keycloak.ishare;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
class ISHAREAuthenticatorConfig implements Serializable {
    @JsonProperty(value="satellite-id", required=true)
    public String satelliteId;

    @JsonProperty(value="satellite-url", required=true)
    public String satelliteUrl;

    @JsonProperty(value="ishare-ca-file", required=true)
    public String ishareCaFile;
}

class ISHARESatellitePartiesResponse implements Serializable {
    @JsonProperty("party_token")
    public String party_token;
}

class ISHARESatelliteResponse implements Serializable {
    @JsonProperty("status")
    public String status;

    @JsonProperty("message")
    public String message;

    @JsonProperty("access_token")
    public String access_token;

    @JsonProperty("token_type")
    public String token_type;

    @JsonProperty("expires_in")
    public int expires_in;

    public ISHARESatelliteResponse() {}
}

class ISHAREPartyToken extends JsonWebToken {
    @JsonProperty("party_info")
    public ISHAREPartyInfo party_info;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Adherence implements Serializable
{
    @JsonProperty("status")
    public String status;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ISHAREPartyInfo implements Serializable {
    @JsonProperty("party_id")
    public String party_id;

    @JsonProperty("registrar_id")
    public String registrar_id;

    @JsonProperty("adherence")
    public Adherence adherence;

    @JsonProperty("certificates")
    public List<ISHARECertificateInfo> certificates;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ISHARECertificateInfo implements Serializable {
    @JsonProperty("subject_name")
    public String subject_name;

    @JsonProperty("certificate_type")
    public String certificate_type;

    @JsonProperty("x5c")
    public String x5c;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class JWEHeaderCerts implements Serializable {
    @JsonProperty("x5c")
    public String[] x5c;
}

public class Ishare {

    private static final Logger logger = Logger.getLogger(Ishare.class);
    public static final String CLIENT_ASSERTION_TYPE="urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String SUB_FOR_AUTHORIZATION="urn:TBD";
    private static final List<Algorithm> JWS_HEADER_APPROVED_ALGORITHMS = Stream.of(Algorithm.RS256, Algorithm.RS384, Algorithm.RS512).toList();

    String iSHARESatellitePartyId;
    String iSHARESatelliteBaseUrl;
    X509Certificate iSHARE_CA;
    KeycloakSession session;

    public Ishare(KeycloakSession session) throws RuntimeException {
        if (!init()) {
            throw new RuntimeException("Error initializing iSHARE");
        }
        this.session = session;
    }

    protected boolean init() {
        // TO-DO: Realm settings
        try {
            String keycloakHome = System.getenv("QUARKUS_HOME");

            String configFilePath = (keycloakHome != null ? keycloakHome : "/srv/keycloak") + "/conf/ishare.json";
            logger.infof("use ishare config %s", configFilePath);

            String configFileContent = getFileContent(new FileInputStream(configFilePath), "utf-8");

            ISHAREAuthenticatorConfig cfg = JsonSerialization.readValue(configFileContent, ISHAREAuthenticatorConfig.class);

            iSHARESatellitePartyId = cfg.satelliteId;
            iSHARESatelliteBaseUrl = cfg.satelliteUrl;

            FileInputStream inStream = new FileInputStream(cfg.ishareCaFile);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            iSHARE_CA = (X509Certificate) cf.generateCertificate(inStream);
        } catch (Exception e) {
            logger.errorf(e,"Exception during init");
            return false;
        }

        logger.info("ISHAREAuthenticator init done");
        return true;
    }

    private String getFileContent(FileInputStream fis, String encoding ) throws IOException
    {
        try (BufferedReader br = new BufferedReader( new InputStreamReader(fis, encoding )))
        {
            StringBuilder sb = new StringBuilder();
            String line;
            while(( line = br.readLine()) != null ) {
                sb.append( line );
                sb.append( '\n' );
            }
            return sb.toString();
        }
    }

    public boolean verifyClientToken(String idpEORI, String prIdpEORI, String incoming_token, String clientId)
    {
        try {
            JWSInput jws = new JWSInput(incoming_token);
            if (!validateJwtCert(jws)) {
                return false;
            }

            JsonWebToken token = jws.readJsonContent(JsonWebToken.class);
            logger.debugf("Trying to validate jws token of ishare client authenticator. Token is: %s", JsonSerialization.writeValueAsString(token));
            if (!validateJwtToken(token, idpEORI)) {
                return false;
            }
            if (token.getExp() -  token.getIat() != 30L) {
                logger.error("exp - iat must be exactly 30 seconds");
                return false;
            }

            if (token.getIssuer() == null || !token.getIssuer().equals(token.getSubject()) || !token.getIssuer().equals(clientId)){
                logger.error("Iss and sub must be equal to client_id");
                return false;
            }

            return verifyClientAtSatellite(clientId, jws.getHeader().getX5c().get(0), prIdpEORI != null ? prIdpEORI : idpEORI, createSatelliteClientAssertion(prIdpEORI != null ? prIdpEORI : idpEORI, this.session));
        } catch (Exception e) {
            logger.errorf(e,"Exception validating client_assertion");
            return false;
        }
    }

    public boolean isProbablyJwe(String incoming_token) {
        String[] parts = incoming_token.split("\\.");
        return parts.length == 5; // JWE requires 5 parts
    }

    JWE getDecryptedJWE(String incoming_token) throws JWEException
    {
        JWE jwe = new JWE();

        KeyWrapper key = this.session.keys().getActiveKey(session.getContext().getRealm(), KeyUse.SIG,"RS256");

        jwe.getKeyStorage().setDecryptionKey((PrivateKey)key.getPrivateKey());
        jwe.verifyAndDecodeJwe(incoming_token);
        return jwe;
    }

    public boolean decryptAndVerifyClientTokenAndParty(String idpEORI, String prIdpEORI, String clientId, String incoming_token)
    {
        try {
            JWE jwe = this.getDecryptedJWE(incoming_token);

            byte[] content = jwe.getContent();
            String client_assertion = new String(content, StandardCharsets.UTF_8);

            logger.infof("Got decrypted JWT token: %s", client_assertion);

            return this.verifyAuthorizationClientToken(idpEORI, prIdpEORI, clientId, client_assertion);
        } catch (Exception e) {
            logger.errorf(e,"Exception validating client_assertion");
        }
        return false;
    }

    public boolean verifyAuthorizationClientToken(String idpEORI, String prIdpEORI, String clientId, String incoming_token)
    {
        try {
            JWSInput jws = new JWSInput(incoming_token);
            if (!validateJwtCert(jws)) {
                return false;
            }

            JsonWebToken token = jws.readJsonContent(JsonWebToken.class);
            if (!validateJwtToken(token, idpEORI)) {
                return false;
            }
            if (token.getExp() -  token.getIat() != 30L) {
                logger.error("exp - iat must be exactly 30 seconds");
                return false;
            }
            if (!clientId.equals(token.getIssuer())){
                logger.error("Iss must be equal to client_id");
                return false;
            }

            if (!SUB_FOR_AUTHORIZATION.equals(token.getSubject())){
                logger.errorf("Sub must be equal to %s", SUB_FOR_AUTHORIZATION);
                return false;
            }

            return verifyClientAtSatellite(clientId, jws.getHeader().getX5c().get(0), prIdpEORI != null ? prIdpEORI : idpEORI, createSatelliteClientAssertion(prIdpEORI != null ? prIdpEORI : idpEORI, this.session));
        } catch (Exception e) {
            logger.errorf(e,"Exception validating client_assertion");
        }
        return false;
    }

    private String getParamsString(Map<String, String> params) throws java.io.UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }

    private String createSatelliteClientAssertion(String idpEORI, KeycloakSession session)
    {
        Instant now = Instant.now();

        JWSBuilder jwsBuilder = new JWSBuilder();

        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, "RS256");
        SignatureSignerContext signer = signatureProvider.signer();

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("iss", idpEORI);
        claims.put("sub", idpEORI);
        claims.put("aud", iSHARESatellitePartyId);
        claims.put("iat", now.getEpochSecond());
        claims.put("nbf", now.getEpochSecond());
        claims.put("exp", Date.from(now.plus(30L, ChronoUnit.SECONDS)).getTime() / 1000);


        List<X509Certificate> certs = signer.getCertificateChain();
        if (certs != null && certs.size() > 0) {
            jwsBuilder = jwsBuilder.x5c(certs);
        }

        String client_assertion = jwsBuilder
                .type("JWT")
                .x5c(certs)
                .jsonContent(claims)
                .sign(signer);

        return client_assertion;
    }

    private String readBody(HttpURLConnection connection) throws Exception
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = reader.readLine()) != null) {
            content.append(inputLine);
        }
        reader.close();
        return content.toString();
    }

    private String getAccessTokenFromSatellite(String idpEORI, String client_assertion) throws Exception
    {
        String tokenURL = iSHARESatelliteBaseUrl.concat("/connect/token");

        URL url = new URL(tokenURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Accept", "application/json");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("grant_type", "client_credentials");
        parameters.put("client_assertion_type", CLIENT_ASSERTION_TYPE);
        parameters.put("client_assertion", client_assertion);
        parameters.put("scope", Constants.ISHARE_SCOPE);
        parameters.put("client_id", idpEORI);

        connection.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.writeBytes(getParamsString(parameters));
        out.flush();
        out.close();

        int status = connection.getResponseCode();
        logger.tracef("Satellite response status: %d", status);

        if (status == 200) {
            String body = readBody(connection);

            ISHARESatelliteResponse resp = JsonSerialization.readValue(body, ISHARESatelliteResponse.class);
            if (resp.access_token == null || resp.access_token.isEmpty()) {
                // no access token means error
                logger.errorf("Couldn't obtain token from Satellite: %s", resp.message != null ? resp.message : "unknown error");
                return null;
            }
            logger.tracef("got access token: %s", resp.access_token);
            return resp.access_token;
        } else {
            logger.errorf("Satellite returned error. Statuscode: %d. Error message: %s", status, readBody(connection));
            return null;
        }
    }

    private boolean verifyClientAtSatellite(String clientId, String clientCert, String idpEORI, String client_assertion) throws Exception
    {
        String access_token = getAccessTokenFromSatellite(idpEORI, client_assertion);
        if (access_token == null) {
            return false;
        }

        //String tokenURL = iSHARESatelliteBaseUrl.concat(new String("/parties/").concat(callingPartyId));
        String tokenURL = iSHARESatelliteBaseUrl + "/parties/" + clientId;
        logger.tracef("call %s", tokenURL);

        URL url = new URL(tokenURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + access_token);
        connection.setRequestProperty("Accept", "application/json");
        connection.connect();

        int status = connection.getResponseCode();
        if (status == 200) {
            logger.errorf("error getting parties: %d. Error message: %s", status, readBody(connection));
            return false;
        }

        String body = readBody(connection);

        ISHARESatellitePartiesResponse resp = JsonSerialization.readValue(body, ISHARESatellitePartiesResponse.class);

        if (!validatePartiesToken(resp.party_token, clientId, clientCert, idpEORI)) {
            return false;
        }

        return true;
    }

    private boolean validatePartiesToken(String partiesToken, String clientId, String clientCert, String idpEORI) throws Exception
    {
        logger.tracef("validate parties token: %s", partiesToken);
        JWSInput jws = new JWSInput(partiesToken);
        if (!validateJwtCert(jws)) {
            logger.error("Error validating parties token. Invalid parties token cert");
            return false;
        }

        JsonWebToken token = jws.readJsonContent(JsonWebToken.class);
        if (!validateJwtToken(token, idpEORI)) {
            logger.error("Error validating parties token. invalid parties token");
            return false;
        }

        byte[] contentBytes = Base64Url.decode(jws.getEncodedContent());

        logger.tracef("token content: %s", new String(contentBytes));

        ISHAREPartyToken partyInfoToken = JsonSerialization.readValue(contentBytes, ISHAREPartyToken.class);

        if (!partyInfoToken.party_info.party_id.equals(clientId)) {
            logger.errorf("Error validating parties token. invalid party_id in party token: %s. Should be: %s", partyInfoToken.party_info.party_id, clientId);
            return false;
        }

        if (!partyInfoToken.party_info.adherence.status.equals("Active")) {
            logger.error("Error validating parties token. party not active");
            return false;
        }

        List<ISHARECertificateInfo> storedCerts = partyInfoToken.party_info.certificates;
        boolean atLeastOneCert = storedCerts.stream().anyMatch(cert -> cert.x5c.equals(clientCert));

        if (!atLeastOneCert) {
            logger.error("Error validating parties token. no matching certificate found in jwt");
        }

        return atLeastOneCert;
    }

    public boolean validateJwtToken(JsonWebToken token, String idpEORI)
    {
        if (!token.isActive()) {
            logger.error("token is not active anymore");
            return false;
        }
        if (token.getIat() == null || token.getIat() > Time.currentTime()) {
            logger.error("token iat must be declared and be before now");
            return false;
        }
        if (token.getId() == null) {
            logger.error("token iat must be declared");
            return false;
        }

        if (!token.hasAudience(idpEORI)) {
            logger.errorf("Invalid aud: %s. Should be: %s", Arrays.toString(token.getAudience()), idpEORI);
            return false;
          //  logger.warnf("Invalid aud: %s. Should be: %s", Arrays.toString(token.getAudience()), idpEORI);
        }

        return true;
    }

    public Map<String, Object> getClaimsFromClientAssertion(String assertion)
    {
        if (this.isProbablyJwe(assertion)) {
            try {
                JWE jwe = this.getDecryptedJWE(assertion);

                String nested_assertion = new String(jwe.getContent(), StandardCharsets.UTF_8);

                JWSInput jws = new JWSInput(nested_assertion);

                JsonWebToken webtoken = jws.readJsonContent(JsonWebToken.class);

                Map<String, Object> claims = webtoken.getOtherClaims();
                return claims;
            } catch (JWEException e) {
                logger.errorf("JWE Exception");
                return new HashMap<>();
            } catch (JWSInputException e) {
                logger.errorf("Invalid JWS Input");
                return new HashMap<>();
            }
        } else {
            try {
                JWSInput jws = new JWSInput(assertion);
                JsonWebToken webtoken = jws.readJsonContent(JsonWebToken.class);
                Map<String, Object> claims = webtoken.getOtherClaims();
                return claims;
            } catch (JWSInputException e) {
                logger.errorf(e,"Invalid JWS Input");
                return new HashMap<>();
            }
        }
    }

    public boolean validateJwtCert(JWSInput jws) throws Exception
    {
        JWSHeader header = jws.getHeader();
        if (header.getAlgorithm() == null || !JWS_HEADER_APPROVED_ALGORITHMS.contains(header.getAlgorithm())) {
            logger.errorf("Invalid JWT alg: %s. Must be RS256, RS384, or RS512", header.getAlgorithm());
            return false;
        }
        if (!"JWT".equals(header.getType())) {
            logger.errorf("Invalid JWT typ: %s. Must be JWT", header.getType());
            return false;
        }
        List<String> x5c = header.getX5c();
        if (x5c == null || x5c.isEmpty()) {
            logger.error("x5c header value empty");
            return false;
        }

        X509Certificate cert = PemUtils.decodeCertificate(x5c.get(0));

        // Note: This works only if iSHARE_CA has full chain to root.
        cert.verify(iSHARE_CA.getPublicKey());

        if (jws.getSignature() == null || !RSAProvider.verify(jws, cert.getPublicKey())) {
            logger.error("JWT signature key used does not correspond with public key from the x5c certificate");
            return false;
        }

        return true;
    }
}
