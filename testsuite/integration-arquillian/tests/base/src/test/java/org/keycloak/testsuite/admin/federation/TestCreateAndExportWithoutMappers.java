package org.keycloak.testsuite.admin.federation;

import java.io.IOException;

import org.junit.Test;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.testsuite.admin.SAMLFederationTest;

/**
 * Test class for testing SAML Federation creation and export without mappers.
 */
public class TestCreateAndExportWithoutMappers extends SAMLFederationTest {

    @Test
    public void testCreateAndExportWithoutMappers() throws IOException, ParsingException {
        super.testCreateAndExportWithoutMappers();
    }
}
