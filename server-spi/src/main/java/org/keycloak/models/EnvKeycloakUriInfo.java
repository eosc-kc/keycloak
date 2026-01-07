package org.keycloak.models;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

/**
 * this method only works for calling getBaseUri
 * Must not be used as changing KeycloakUriInfo
 */
public class EnvKeycloakUriInfo implements UriInfo {

    private URI uri;

    public EnvKeycloakUriInfo(URI uri) {
        this.uri = uri;
    }


    @Override
    public String getPath() {
        return uri.getPath();
    }

    @Override
    public String getPath(boolean decode) {
        return uri.getPath();
    }

    @Override
    public List<PathSegment> getPathSegments() {
        return null;
    }

    @Override
    public List<PathSegment> getPathSegments(boolean decode) {
        return null;
    }

    @Override
    public URI getRequestUri() {
        return uri;
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        return null;
    }

    @Override
    public URI getAbsolutePath() {
        return uri;
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return null;
    }

    @Override
    public URI getBaseUri() {
        return uri;
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return UriBuilder.fromUri(getBaseUri());
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        return null;
    }

    @Override
    public List<String> getMatchedURIs() {
        return null;
    }

    @Override
    public List<String> getMatchedURIs(boolean decode) {
        return null;
    }

    @Override
    public List<Object> getMatchedResources() {
        return null;
    }

    @Override
    public URI resolve(URI uri) {
        return getBaseUri().resolve(uri);
    }

    @Override
    public URI relativize(URI uri) {
        return uri;
    }
}
