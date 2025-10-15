package org.keycloak.saml.processing.core.parsers.saml.metadata;

public class SAMLAssertionParser extends SAMLAttributeTypeParser {

    private static final SAMLAssertionParser INSTANCE = new SAMLAssertionParser();

    private SAMLAssertionParser() {
        super(SAMLMetadataQNames.ASSERTION);
    }

    public static SAMLAssertionParser getInstance() {
        return INSTANCE;
    }

}