package org.keycloak.protocol.oidc.endpoints.checker;

import jakarta.ws.rs.core.Response;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.cors.Cors;
import org.keycloak.sessions.AuthenticationSessionModel;

// Exception propagated to the caller, which will allow caller to send proper error response based on the context (Browser OIDC Authorization Endpoint, PAR etc)
public class AuthorizationCheckException extends Exception {

    private final Response.Status status;
    private final String error;
    private final String errorDescription;

    public AuthorizationCheckException(Response.Status status, String error, String errorDescription) {
        this.status = status;
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public void throwAsErrorPageException(AuthenticationSessionModel authenticationSession, KeycloakSession session) {
        throw new ErrorPageException(session, authenticationSession, status, error, errorDescription);
    }

    public void throwAsCorsErrorResponseException(Cors cors, EventBuilder event) {
        event.detail("detail", errorDescription).error(error);
        throw new CorsErrorResponseException(cors, error, errorDescription, status);
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}
