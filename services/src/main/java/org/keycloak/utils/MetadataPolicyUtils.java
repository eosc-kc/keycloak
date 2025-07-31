package org.keycloak.utils;

import org.keycloak.exceptions.MetadataPolicyCombinationException;
import org.keycloak.exceptions.MetadataPolicyException;
import org.keycloak.representations.openid_federation.AbstractMetadataPolicy;
import org.keycloak.representations.openid_federation.CommonMetadata;
import org.keycloak.representations.openid_federation.CommonMetadataPolicy;
import org.keycloak.representations.openid_federation.OPMetadata;
import org.keycloak.representations.openid_federation.OPMetadataPolicy;
import org.keycloak.representations.openid_federation.RPMetadata;
import org.keycloak.representations.openid_federation.RPMetadataPolicy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class MetadataPolicyUtils {

    public static <T extends AbstractMetadataPolicy> T combinePolicies(T superior, T inferior) throws MetadataPolicyCombinationException {

        if (inferior == null) {
            return superior;
        }

        if (superior == null) {
            return inferior;
        }

        // Process all fields of T extends AbstractMetadataPolicy
        for (Field field : superior.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object superiorValue = field.get(superior);
                Object inferiorValue = field.get(inferior);

                if (!field.getType().equals(CommonMetadataPolicy.class)) {
                    // Handle Policy<T> or PolicyList<T> fields
                    combineField(superior, field, superiorValue, inferiorValue);
                }
            } catch (IllegalAccessException | NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                throw new MetadataPolicyCombinationException("Error combinePolicy for field: " + field.getName() + " . Cause: " + cause.getMessage() != null ? cause.getMessage() : "");
            }
        }
        combineCommonMetadataPolicy(superior.getCommonMetadataPolicy(), inferior.getCommonMetadataPolicy());

        return superior;
    }

    private static <T extends AbstractMetadataPolicy> void combineField(T superior, Field field, Object superiorValue, Object inferiorValue) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (superiorValue != null) {
            // Invoke combinePolicy dynamically
            Object combined = superiorValue.getClass()
                    .getMethod("combinePolicy", superiorValue.getClass())
                    .invoke(superiorValue, inferiorValue);
            field.set(superior, combined);
        } else {
            field.set(superior, inferiorValue);
        }
    }

    private static void combineCommonMetadataPolicy(CommonMetadataPolicy superior, CommonMetadataPolicy inferior) throws MetadataPolicyCombinationException {

        if (inferior == null)
            return;

        if (superior == null)
            superior = inferior;

        if (superior.getOrganizationName() != null) {
            superior.setOrganizationName(superior.getOrganizationName().combinePolicy(inferior.getOrganizationName()));
        } else {
            superior.setOrganizationName(inferior.getOrganizationName());
        }
        if (superior.getOrganizationUri() != null) {
            superior.setOrganizationUri(superior.getOrganizationUri().combinePolicy(inferior.getOrganizationUri()));
        } else {
            superior.setOrganizationUri(inferior.getOrganizationUri());
        }
        if (superior.getSignedJwksUri() != null) {
            superior.setSignedJwksUri(superior.getSignedJwksUri().combinePolicy(inferior.getSignedJwksUri()));
        } else {
            superior.setSignedJwksUri(inferior.getSignedJwksUri());
        }
    }

    public static RPMetadata applyPoliciesToRPStatement(RPMetadata rp, RPMetadataPolicy policy) throws MetadataPolicyException, MetadataPolicyCombinationException {

        if (policy == null) {
            return rp;
        }

        if (policy.getApplicationType() != null) {
            rp.setApplicationType(policy.getApplicationType().enforcePolicy(rp.getApplicationType(), "ApplicationType"));
        }

        if (policy.getClientIdIssuedAt() != null) {
            rp.setClientIdIssuedAt(policy.getClientIdIssuedAt().enforcePolicy(rp.getClientIdIssuedAt(), "ClientIdIssuedAt"));
        }

        if (policy.getClientName() != null) {
            rp.setClientName(policy.getClientName().enforcePolicy(rp.getClientName(), "ClientName"));
        }

        if (policy.getClientRegistrationTypes() != null) {
            rp.setClientRegistrationTypes(policy.getClientRegistrationTypes().enforcePolicy(rp.getClientRegistrationTypes(), "Client_registration_types"));
        }

        if (policy.getClientSecretExpiresAt() != null) {
            rp.setClientSecretExpiresAt(policy.getClientSecretExpiresAt().enforcePolicy(rp.getClientSecretExpiresAt(), "ClientSecretExpiresAt"));
        }

        if (policy.getClientUri() != null) {
            rp.setClientUri(policy.getClientUri().enforcePolicy(rp.getClientUri(), "ClientUri"));
        }

        if (policy.getContacts() != null) {
            rp.setContacts(policy.getContacts().enforcePolicy(rp.getContacts(), "Contacts"));
        }

        if (policy.getDefaultAcrValues() != null) {
            rp.setDefaultAcrValues(policy.getDefaultAcrValues().enforcePolicy(rp.getDefaultAcrValues(), "DefaultAcrValues"));
        }

        if (policy.getDefaultMaxAge() != null) {
            rp.setDefaultMaxAge(policy.getDefaultMaxAge().enforcePolicy(rp.getDefaultMaxAge(), "DefaultMaxAge"));
        }

        if (policy.getGrantTypes() != null) {
            rp.setGrantTypes(policy.getGrantTypes().enforcePolicy(rp.getGrantTypes(), "GrantTypes"));
        }

        if (policy.getIdTokenEncryptedResponseAlg() != null) {
            rp.setIdTokenEncryptedResponseAlg(policy.getIdTokenEncryptedResponseAlg().enforcePolicy(rp.getIdTokenEncryptedResponseAlg(), "IdTokenEncryptedResponseAlg"));
        }

        if (policy.getIdTokenEncryptedResponseEnc() != null) {
            rp.setIdTokenEncryptedResponseEnc(policy.getIdTokenEncryptedResponseEnc().enforcePolicy(rp.getIdTokenEncryptedResponseEnc(), "IdTokenEncryptedResponseEnc"));
        }

        if (policy.getIdTokenSignedResponseAlg() != null) {
            rp.setIdTokenSignedResponseAlg(policy.getIdTokenSignedResponseAlg().enforcePolicy(rp.getIdTokenSignedResponseAlg(), "IdTokenSignedResponseAlg"));
        }

        if (policy.getInitiateLoginUri() != null) {
            rp.setInitiateLoginUri(policy.getInitiateLoginUri().enforcePolicy(rp.getInitiateLoginUri(), "InitiateLoginUri"));
        }

        if (policy.getJwksUri() != null) {
            rp.setJwksUri(policy.getJwksUri().enforcePolicy(rp.getJwksUri(), "JwksUri"));
        }

        if (policy.getLogoUri() != null) {
            rp.setLogoUri(policy.getLogoUri().enforcePolicy(rp.getLogoUri(), "LogoUri"));
        }

        if (policy.getPolicyUri() != null) {
            rp.setPolicyUri(policy.getPolicyUri().enforcePolicy(rp.getPolicyUri(), "PolicyUri"));
        }

        if (policy.getPostLogoutRedirectUris() != null) {
            rp.setPostLogoutRedirectUris(policy.getPostLogoutRedirectUris().enforcePolicy(rp.getPostLogoutRedirectUris(), "PostLogoutRedirectUris"));
        }

        if (policy.getRedirectUris() != null) {
            rp.setRedirectUris(policy.getRedirectUris().enforcePolicy(rp.getRedirectUris(), "RedirectUris"));
        }

        if (policy.getRegistrationAccessToken() != null) {
            rp.setRegistrationAccessToken(policy.getRegistrationAccessToken().enforcePolicy(rp.getRegistrationAccessToken(), "RegistrationAccessToken"));
        }

        if (policy.getRegistrationClientUri() != null) {
            rp.setRegistrationClientUri(policy.getRegistrationClientUri().enforcePolicy(rp.getRegistrationClientUri(), "RegistrationClientUri"));
        }

        if (policy.getRequestObjectEncryptionAlg() != null) {
            rp.setRequestObjectEncryptionAlg(policy.getRequestObjectEncryptionAlg().enforcePolicy(rp.getRequestObjectEncryptionAlg(), "RequestObjectEncryptionAlg"));
        }

        if (policy.getRequestObjectEncryptionEnc() != null) {
            rp.setRequestObjectEncryptionEnc(policy.getRequestObjectEncryptionEnc().enforcePolicy(rp.getRequestObjectEncryptionEnc(), "RequestObjectEncryptionEnc"));
        }

        if (policy.getRequestObjectSigningAlg() != null) {
            rp.setRequestObjectSigningAlg(policy.getRequestObjectSigningAlg().enforcePolicy(rp.getRequestObjectSigningAlg(), "RequestObjectSigningAlg"));
        }

        if (policy.getRequestUris() != null) {
            rp.setRequestUris(policy.getRequestUris().enforcePolicy(rp.getRequestUris(), "RequestUris"));
        }

        if (policy.getRequireAuthTime() != null) {
            rp.setRequireAuthTime(policy.getRequireAuthTime().enforcePolicy(rp.getRequireAuthTime(), "RequireAuthTime"));
        }

        if (policy.getResponseTypes() != null) {
            rp.setResponseTypes(policy.getResponseTypes().enforcePolicy(rp.getResponseTypes(), "ResponseTypes"));
        }

        if (policy.getScope() != null) {
            rp.setScope(policy.getScope().enforcePolicy(rp.getScope(), "Scope"));
        }

        if (policy.getSectorIdentifierUri() != null) {
            rp.setSectorIdentifierUri(policy.getSectorIdentifierUri().enforcePolicy(rp.getSectorIdentifierUri(), "SectorIdentifierUri"));
        }

        if (policy.getSoftwareId() != null) {
            rp.setSoftwareId(policy.getSoftwareId().enforcePolicy(rp.getSoftwareId(), "SoftwareId"));
        }

        if (policy.getSoftwareVersion() != null) {
            rp.setSoftwareVersion(policy.getSoftwareVersion().enforcePolicy(rp.getSoftwareVersion(), "SoftwareVersion"));
        }

        if (policy.getSubjectType() != null) {
            rp.setSubjectType(policy.getSubjectType().enforcePolicy(rp.getSubjectType(), "SubjectType"));
        }

        if (policy.getTlsClientAuthSubjectDn() != null) {
            rp.setTlsClientAuthSubjectDn(policy.getTlsClientAuthSubjectDn().enforcePolicy(rp.getTlsClientAuthSubjectDn(), "TlsClientAuthSubjectDn"));
        }

        if (policy.getTlsClientCertificateBoundAccessTokens() != null) {
            rp.setTlsClientCertificateBoundAccessTokens(policy.getTlsClientCertificateBoundAccessTokens().enforcePolicy(rp.getTlsClientCertificateBoundAccessTokens(), "TlsClientCertificateBoundAccessTokens"));
        }

        if (policy.getTokenEndpointAuthMethod() != null) {
            rp.setTokenEndpointAuthMethod(policy.getTokenEndpointAuthMethod().enforcePolicy(rp.getTokenEndpointAuthMethod(), "TokenEndpointAuthMethod"));
        }

        if (policy.getTokenEndpointAuthSigningAlg() != null) {
            rp.setTokenEndpointAuthSigningAlg(policy.getTokenEndpointAuthSigningAlg().enforcePolicy(rp.getTokenEndpointAuthSigningAlg(), "TokenEndpointAuthSigningAlg"));
        }

        if (policy.getTosUri() != null) {
            rp.setTosUri(policy.getTosUri().enforcePolicy(rp.getTosUri(), "TosUri"));
        }

        if (policy.getUserinfoEncryptedResponseAlg() != null) {
            rp.setUserinfoEncryptedResponseAlg(policy.getUserinfoEncryptedResponseAlg().enforcePolicy(rp.getUserinfoEncryptedResponseAlg(), "UserinfoEncryptedResponseAlg"));
        }

        if (policy.getUserinfoEncryptedResponseEnc() != null) {
            rp.setUserinfoEncryptedResponseEnc(policy.getUserinfoEncryptedResponseEnc().enforcePolicy(rp.getUserinfoEncryptedResponseEnc(), "UserinfoEncryptedResponseEnc"));
        }

        if (policy.getUserinfoSignedResponseAlg() != null) {
            rp.setUserinfoSignedResponseAlg(policy.getUserinfoSignedResponseAlg().enforcePolicy(rp.getUserinfoSignedResponseAlg(), "UserinfoSignedResponseAlg"));
        }

        if (policy.getBackchannelTokenDeliveryMode() != null) {
            rp.setBackchannelTokenDeliveryMode(policy.getBackchannelTokenDeliveryMode().enforcePolicy(rp.getBackchannelTokenDeliveryMode(), "BackchannelTokenDeliveryMode"));
        }
        if (policy.getBackchannelClientNotificationEndpoint() != null) {
            rp.setBackchannelClientNotificationEndpoint(policy.getBackchannelClientNotificationEndpoint().enforcePolicy(rp.getBackchannelClientNotificationEndpoint(), "BackchannelClientNotificationEndpoint"));
        }
        if (policy.getBackchannelAuthenticationRequestSigningAlg() != null) {
            rp.setBackchannelAuthenticationRequestSigningAlg(policy.getBackchannelAuthenticationRequestSigningAlg().enforcePolicy(rp.getBackchannelAuthenticationRequestSigningAlg(), "BackchannelAuthenticationRequestSigningAlg"));
        }
        if (policy.getAuthorizationSignedResponseAlg() != null) {
            rp.setAuthorizationSignedResponseAlg(policy.getAuthorizationSignedResponseAlg().enforcePolicy(rp.getAuthorizationSignedResponseAlg(), "AuthorizationSignedResponseAlg"));
        }
        if (policy.getAuthorizationEncryptedResponseAlg() != null) {
            rp.setAuthorizationEncryptedResponseAlg(policy.getAuthorizationEncryptedResponseAlg().enforcePolicy(rp.getAuthorizationEncryptedResponseAlg(), "AuthorizationEncryptedResponseAlg"));
        }
        if (policy.getAuthorizationEncryptedResponseEnc() != null) {
            rp.setAuthorizationEncryptedResponseEnc(policy.getAuthorizationEncryptedResponseEnc().enforcePolicy(rp.getAuthorizationEncryptedResponseEnc(), "AuthorizationEncryptedResponseEnc"));
        }
        if (policy.getRequirePushedAuthorizationRequests() != null) {
            rp.setRequirePushedAuthorizationRequests(policy.getRequirePushedAuthorizationRequests().enforcePolicy(rp.getRequirePushedAuthorizationRequests(), "RequirePushedAuthorizationRequests"));
        }
        if (policy.getFrontChannelLogoutUri() != null) {
            rp.setFrontChannelLogoutUri(policy.getFrontChannelLogoutUri().enforcePolicy(rp.getFrontChannelLogoutUri(), "FrontChannelLogoutUri"));
        }
        if (policy.getFrontchannelLogoutSessionRequired() != null) {
            rp.setFrontchannelLogoutSessionRequired(policy.getFrontchannelLogoutSessionRequired().enforcePolicy(rp.getFrontchannelLogoutSessionRequired(), "FrontchannelLogoutSessionRequired"));
        }

        if (policy.getCommonMetadataPolicy() != null && rp.getCommonMetadata() != null) {
            applyPoliciesToCommonMetadata(rp.getCommonMetadata(), policy.getCommonMetadataPolicy());
        }

        return rp;
    }

    private static void applyPoliciesToCommonMetadata(CommonMetadata cm, CommonMetadataPolicy cmPolicy) throws MetadataPolicyException {
        if (cmPolicy.getSignedJwksUri() != null) {
            cm.setSignedJwksUri(cmPolicy.getSignedJwksUri().enforcePolicy(cm.getSignedJwksUri(), "SignedJwksUri"));
        }
        if (cmPolicy.getOrganizationName() != null) {
            cm.setOrganizationName(cmPolicy.getOrganizationName().enforcePolicy(cm.getOrganizationName(), "OrganizationName"));
        }
        if (cmPolicy.getOrganizationUri() != null) {
            cm.setOrganizationUri(cmPolicy.getOrganizationUri().enforcePolicy(cm.getOrganizationUri(), "OrganizationUri"));
        }
    }

    public static OPMetadata applyPoliciesToOPStatement(OPMetadata op, OPMetadataPolicy policy) throws MetadataPolicyException, MetadataPolicyCombinationException {
        if (policy == null) {
            return op;
        }
        if (policy.getFederationRegistrationEndpoint() != null) {
            op.setFederationRegistrationEndpoint(policy.getFederationRegistrationEndpoint().enforcePolicy(op.getFederationRegistrationEndpoint(), "FederationRegistrationEndpoint"));
        }
        if (policy.getClientRegistrationTypes() != null) {
            op.setClientRegistrationTypes(policy.getClientRegistrationTypes().enforcePolicy(op.getClientRegistrationTypes(), "ClientRegistrationTypes"));
        }
        if (policy.getContacts() != null) {
            op.setContacts(policy.getContacts().enforcePolicy(op.getContacts(), "Contacts"));
        }
        if (policy.getLogoUri() != null) {
            op.setLogoUri(policy.getLogoUri().enforcePolicy(op.getLogoUri(), "LogoUri"));
        }
        if (policy.getPolicyUri() != null) {
            op.setPolicyUri(policy.getPolicyUri().enforcePolicy(op.getPolicyUri(), "PolicyUri"));
        }
        if (policy.getCommonMetadataPolicy() != null && op.getCommonMetadata() != null) {
            applyPoliciesToCommonMetadata(op.getCommonMetadata(), policy.getCommonMetadataPolicy());
        }

        return op;
    }
}
