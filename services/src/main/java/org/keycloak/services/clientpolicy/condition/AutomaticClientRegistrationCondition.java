package org.keycloak.services.clientpolicy.condition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import org.keycloak.exceptions.InvalidTrustChainException;
import org.keycloak.jose.JOSE;
import org.keycloak.jose.JOSEParser;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OpenIdFederationConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.models.enums.EntityTypeEnum;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.TrustChainResolution;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;
import org.keycloak.services.trustchain.OpenIdFederationTrustChainProcessorFactory;
import org.keycloak.services.trustchain.TrustChainProcessor;
import org.keycloak.utils.OpenIdFederationUtils;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.util.JWKSUtils;
import org.keycloak.OAuthErrorException;
import org.keycloak.crypto.PublicKeysWrapper;


public class AutomaticClientRegistrationCondition extends AbstractClientPolicyConditionProvider<ClientPolicyConditionConfigurationRepresentation> {

    private static final Logger logger = Logger.getLogger(AutomaticClientRegistrationCondition.class);
    private final TrustChainProcessor trustChainProcessor;
    
    public AutomaticClientRegistrationCondition(KeycloakSession session) {
        super(session);
        this.trustChainProcessor = session.getProvider(TrustChainProcessor.class, OpenIdFederationTrustChainProcessorFactory.PROVIDER_ID);
    }

    @Override
    public Class<ClientPolicyConditionConfigurationRepresentation> getConditionConfigurationClass() {
        return ClientPolicyConditionConfigurationRepresentation.class;
    }

    @Override
    public String getProviderId() {
        return AutomaticClientRegistrationConditionFactory.PROVIDER_ID;
    }

    @Override
    public ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {      

        switch (context.getEvent()) {
            case PRE_AUTHORIZATION_REQUEST:
                
                RealmModel realm = session.getContext().getRealm();
                PreAuthorizationRequestContext paContext = (PreAuthorizationRequestContext) context;
                ClientModel client = session.getContext().getRealm().getClientByClientId(paContext.getClientId());
                String requestObject = paContext.getRequestParameters().getFirst(OIDCLoginProtocol.REQUEST_PARAM);

                if (client == null) {
                    if (realm.isOpenIdFederationTypeRegistrationSupported(EntityTypeEnum.OPENID_PROVIDER, ClientRegistrationTypeEnum.AUTOMATIC) && !realm.getOpenIdFederations().isEmpty()) {

                        try {  
                            // get Entity Configuration from RP's well-known endpoint
                            String entityConfigurationOfRP = OpenIdFederationUtils.getSelfSignedToken(paContext.getClientId(), session);
                            
                            // validate the Configuration and get Entity Statement object which is used to get JWKS of the RP
                            EntityStatement entityStatementFromRPConfiguraionEndpoint = trustChainProcessor.parseAndValidateSelfSigned(entityConfigurationOfRP);
      
                            // validate the request object with the JWKS of RP which is got from the well-known endpoint
                            if (requestObject == null) {
                                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST_OBJECT, "request object is null.");
                            }
                            JOSE joseToken = JOSEParser.parse(requestObject);
                            EntityStatement entityStatementInRequestObject = verifyJWS((JWSInput)joseToken, entityStatementFromRPConfiguraionEndpoint.getJwks());
                            
                            entityStatementInRequestObject.setSubject(paContext.getClientId());

                            logger.info("starting validating trust chains");
                            
                            TrustChainResolution validTrustChain = trustChainProcessor.constructTrustChains(entityStatementInRequestObject, realm.getOpenIdFederations().stream().map(OpenIdFederationConfig::getTrustAnchor).collect(Collectors.toSet()));
                            if (validTrustChain == null) {
                                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST_OBJECT, "There is no valid trust chain.");
                            }
                            
                            return ClientPolicyVote.YES;
                            
                        } catch (InvalidTrustChainException ite) {
                            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST_OBJECT, ite.getMessage());
                        } catch (IOException ioe) {
                            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT, ioe.getMessage());
                        }
                    }
                    return ClientPolicyVote.NO;
                }
                return ClientPolicyVote.NO;
            default:
                return ClientPolicyVote.NO;
        }
    }

    private EntityStatement verifyJWS(JWSInput jws, JSONWebKeySet jwks) {
        try {
            String signatureAlgorithm = jws.getHeader().getAlgorithm().name();
            SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);

            if (signatureProvider == null) {
                if (jws.getHeader().getAlgorithm().equals(org.keycloak.jose.jws.Algorithm.none)) {
                    return jws.readJsonContent(EntityStatement.class);
                }
                return null;
            }

            PublicKeysWrapper pkw = JWKSUtils.getKeyWrappersForUse(jwks, JWK.Use.SIG);
            boolean valid = session.getProvider(SignatureProvider.class, signatureAlgorithm).verifier(pkw.getKeys().get(0)).
                verify(jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8), jws.getSignature());
            return valid ? jws.readJsonContent(EntityStatement.class) : null;
        } catch (Exception e) {
            logger.debug("Failed to decode token", e);
            return null;
        }
    }

}
