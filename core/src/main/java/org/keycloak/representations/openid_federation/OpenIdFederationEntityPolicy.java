package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.keycloak.jose.jwk.JSONWebKeySet;

public class OpenIdFederationEntityPolicy extends AbstractMetadataPolicy {

    @JsonProperty("federation_api_endpoint")
    private Policy<String> federationApiEndpoint;

    @JsonProperty("federation_list_endpoint")
    private Policy<String> federationListEndpoint;

    @JsonProperty("federation_resolve_endpoint")
    private Policy<String> federationResolveEndpoint;

    @JsonProperty("federation_trust_mark_status_endpoint")
    private Policy<String> federationTrustMarkStatusEndpoint;

    @JsonProperty("federation_trust_mark_list_endpoint")
    private Policy<String> federationTrustMarkListEndpoint;

    @JsonProperty("federation_trust_mark_endpoint")
    private Policy<String> federationTrustMarkEndpoint;

    @JsonProperty("federation_historical_keys_endpoint")
    private Policy<String> federationHistoricalKeysEndpoint;

    @JsonProperty("endpoint_auth_signing_alg_values_supported")
    private PolicyList<String> endpointAuthSigningAlgValuesSupported;

    private  PolicyList<String> contacts;

    @JsonProperty("jwks_uri")
    private  Policy<String> jwksUri;

    protected  Policy<JSONWebKeySet> jwks;

    @JsonProperty("logo_uri")
    private  Policy<String> logoUri;

    @JsonProperty("policy_uri")
    private  Policy<String> policyUri;

    public Policy<String> getFederationApiEndpoint() {
        return federationApiEndpoint;
    }

    public void setFederationApiEndpoint(Policy<String> federationApiEndpoint) {
        this.federationApiEndpoint = federationApiEndpoint;
    }

    public Policy<String> getFederationListEndpoint() {
        return federationListEndpoint;
    }

    public void setFederationListEndpoint(Policy<String> federationListEndpoint) {
        this.federationListEndpoint = federationListEndpoint;
    }

    public Policy<String> getFederationResolveEndpoint() {
        return federationResolveEndpoint;
    }

    public void setFederationResolveEndpoint(Policy<String> federationResolveEndpoint) {
        this.federationResolveEndpoint = federationResolveEndpoint;
    }

    public Policy<String> getFederationTrustMarkStatusEndpoint() {
        return federationTrustMarkStatusEndpoint;
    }

    public void setFederationTrustMarkStatusEndpoint(Policy<String> federationTrustMarkStatusEndpoint) {
        this.federationTrustMarkStatusEndpoint = federationTrustMarkStatusEndpoint;
    }

    public Policy<String> getFederationTrustMarkListEndpoint() {
        return federationTrustMarkListEndpoint;
    }

    public void setFederationTrustMarkListEndpoint(Policy<String> federationTrustMarkListEndpoint) {
        this.federationTrustMarkListEndpoint = federationTrustMarkListEndpoint;
    }

    public Policy<String> getFederationTrustMarkEndpoint() {
        return federationTrustMarkEndpoint;
    }

    public void setFederationTrustMarkEndpoint(Policy<String> federationTrustMarkEndpoint) {
        this.federationTrustMarkEndpoint = federationTrustMarkEndpoint;
    }

    public Policy<String> getFederationHistoricalKeysEndpoint() {
        return federationHistoricalKeysEndpoint;
    }

    public void setFederationHistoricalKeysEndpoint(Policy<String> federationHistoricalKeysEndpoint) {
        this.federationHistoricalKeysEndpoint = federationHistoricalKeysEndpoint;
    }

    public PolicyList<String> getEndpointAuthSigningAlgValuesSupported() {
        return endpointAuthSigningAlgValuesSupported;
    }

    public void setEndpointAuthSigningAlgValuesSupported(PolicyList<String> endpointAuthSigningAlgValuesSupported) {
        this.endpointAuthSigningAlgValuesSupported = endpointAuthSigningAlgValuesSupported;
    }

    public PolicyList<String> getContacts() {
        return contacts;
    }

    public void setContacts(PolicyList<String> contacts) {
        this.contacts = contacts;
    }

    public Policy<String> getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(Policy<String> jwksUri) {
        this.jwksUri = jwksUri;
    }

    public Policy<JSONWebKeySet> getJwks() {
        return jwks;
    }

    public void setJwks(Policy<JSONWebKeySet> jwks) {
        this.jwks = jwks;
    }

    public Policy<String> getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(Policy<String> logoUri) {
        this.logoUri = logoUri;
    }

    public Policy<String> getPolicyUri() {
        return policyUri;
    }

    public void setPolicyUri(Policy<String> policyUri) {
        this.policyUri = policyUri;
    }
}
