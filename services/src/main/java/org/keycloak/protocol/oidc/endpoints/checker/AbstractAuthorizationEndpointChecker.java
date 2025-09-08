package org.keycloak.protocol.oidc.endpoints.checker;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.services.ServicesLogger;
import org.keycloak.util.TokenUtil;

public abstract class AbstractAuthorizationEndpointChecker<T extends AbstractAuthorizationEndpointChecker<T>> {

    protected EventBuilder event;
    protected AuthorizationEndpointRequest request;
    protected KeycloakSession session;
    protected RealmModel realm;

    protected String redirectUri;
    protected OIDCResponseType parsedResponseType;
    protected OIDCResponseMode parsedResponseMode;
    protected MultivaluedMap<String, String> params;

    public T event(EventBuilder event) {
        this.event = event;
        return (T) this;
    }

    public T request(AuthorizationEndpointRequest request) {
        this.request = request;
        return (T) this;
    }

    public T session(KeycloakSession session) {
        this.session = session;
        return (T) this;
    }

    public T realm(RealmModel realm) {
        this.realm = realm;
        return (T) this;
    }

    public T params(MultivaluedMap<String, String> params) {
        this.params = params;
        return (T) this;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public OIDCResponseType getParsedResponseType() {
        return parsedResponseType;
    }

    public OIDCResponseMode getParsedResponseMode() {
        return parsedResponseMode;
    }

    public EventBuilder getEvent() {
        return event;
    }

    public boolean isInvalidResponseType(AuthorizationCheckException ex) {
        return "Missing parameter: response_type".equals(ex.getErrorDescription()) || OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE.equals(ex.getError());
    }

    public void checkInvalidRequestMessage() throws AuthorizationCheckException {
        if (request.getInvalidRequestMessage() != null) {
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, Errors.INVALID_REQUEST, request.getInvalidRequestMessage());
        }
    }

    public void checkOIDCRequest() {
        if (!TokenUtil.isOIDCRequest(request.getScope())) {
            ServicesLogger.LOGGER.oidcScopeMissing();
        }
    }

    public void checkOIDCParams() throws AuthorizationCheckException {
        // If request is not OIDC request, but pure OAuth2 request and response_type is just 'token', then 'nonce' is not mandatory
        boolean isOIDCRequest = TokenUtil.isOIDCRequest(request.getScope());
        if (!isOIDCRequest && parsedResponseType.toString().equals(OIDCResponseType.TOKEN)) {
            return;
        }

        if (parsedResponseType.hasResponseType(OIDCResponseType.ID_TOKEN) && request.getNonce() == null) {
            ServicesLogger.LOGGER.missingParameter(OIDCLoginProtocol.NONCE_PARAM);
            event.error(Errors.INVALID_REQUEST);
            throw new AuthorizationCheckException(Response.Status.BAD_REQUEST, OAuthErrorException.INVALID_REQUEST, "Missing parameter: nonce");
        }
    }

}
