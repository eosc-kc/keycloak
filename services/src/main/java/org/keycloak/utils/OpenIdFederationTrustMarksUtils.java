package org.keycloak.utils;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.TrustMarkRepresentation;
import org.keycloak.services.ErrorResponse;

public class OpenIdFederationTrustMarksUtils {

    public static void validateTrustMark(TrustMarkRepresentation rep) {
        // 1. trustMarkType required
        if (rep.getTrustMarkType() == null || rep.getTrustMarkType().trim().isEmpty()) {
            throw ErrorResponse.error("trustMarkType is required.", Response.Status.BAD_REQUEST);
        }

        if (rep.isSelfSigned()) {
            // 2. Self-signed specific checks
            if (rep.getIat() == null) {
                throw ErrorResponse.error("iat is required when selfSigned is true.", Response.Status.BAD_REQUEST);
            }
            if (rep.getExp() != null && rep.getExp() < Time.currentTime()) {
                throw ErrorResponse.error("exp must not be before current time.", Response.Status.BAD_REQUEST);
            }
        } else {
            // 3. Not self-signed specific checks
            if (rep.getJwt() == null || rep.getJwt().trim().isEmpty()) {
                throw ErrorResponse.error("jwt is required when selfSigned is false.", Response.Status.BAD_REQUEST);
            }

            if (rep.getLogoUri() != null || rep.getRef() != null ||
                    rep.getDelegation() != null || rep.getIat() != null || rep.getExp() != null) {
                throw ErrorResponse.error("Only jwt and trustMarkType are allowed when selfSigned is false.", Response.Status.BAD_REQUEST);
            }

            // Validate JWT claims based on OpenID Federation 1.0
            try {
                JWSInput jws = new JWSInput(rep.getJwt());
                JsonWebToken token = jws.readJsonContent(JsonWebToken.class);

                if (token.getIssuer() == null) {
                    throw ErrorResponse.error("JWT must contain the 'iss' claim.", Response.Status.BAD_REQUEST);
                }
                if (token.getSubject() == null) {
                    throw ErrorResponse.error("JWT must contain the 'sub' claim.", Response.Status.BAD_REQUEST);
                }
                if (token.getIat() == null) {
                    throw ErrorResponse.error("JWT must contain the 'iat' claim.", Response.Status.BAD_REQUEST);
                }
                if (token.getExp() != null && token.getExp() < Time.currentTime()) {
                    throw ErrorResponse.error("JWT 'exp' claim must not be before current time.", Response.Status.BAD_REQUEST);
                }
            } catch (JWSInputException e) {
                throw ErrorResponse.error("Invalid JWT format.", Response.Status.BAD_REQUEST);
            }
        }

        if (rep.getSubject() != null || rep.getIssuer() != null) {
            throw ErrorResponse.error("subject and issuer must not exist directly in the representation.", Response.Status.BAD_REQUEST);
        }
    }
}
