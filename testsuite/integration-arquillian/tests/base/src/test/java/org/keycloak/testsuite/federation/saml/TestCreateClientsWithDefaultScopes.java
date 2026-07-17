package org.keycloak.testsuite.federation.saml;

import java.io.IOException;

import org.keycloak.saml.common.exceptions.ParsingException;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test class for testing SAML Federation with default client scopes.
 */
public class TestCreateClientsWithDefaultScopes extends SAMLFederationTest {

    @Override
    @Ignore
    public void testCreateUpdateAndRemoveAll() throws IOException {
    }

    @Test
    public void testCreateClientsWithDefaultScopes() throws IOException {
        super.testCreateClientsWithDefaultScopes();
    }

    @Override
    @Ignore
    public void testCreateWithAllowListandRemove() throws IOException {
    }

    @Override
    @Ignore
    public void testCreateWithCategoryDenyListandRemove() throws IOException {

    }

    @Override
    @Ignore
    public void testCreateAndExportWithMappers() throws IOException, ParsingException {

    }

    @Override
    @Ignore
    public void testFederationMappers() throws IOException {

    }

    @Override
    @Ignore
    public void testFederationMappersActions() throws IOException {

    }
}
