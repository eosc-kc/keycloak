package org.keycloak.protocol.oidc.federation.common.exceptions;

public class MetadataPolicyException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public MetadataPolicyException(String message) {
        super(MetadataPolicyException.class.getSimpleName() + " : " + message);
    }
}