/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.broker.saml;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.namespace.QName;

import org.keycloak.Config.Scope;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.saml.v2.util.SAMLMetadataUtil;
import org.keycloak.saml.validators.DestinationValidator;

import org.w3c.dom.Element;


/**
 * @author Pedro Igor
 */
public class SAMLIdentityProviderFactory extends AbstractIdentityProviderFactory<SAMLIdentityProvider> {

    public static final String PROVIDER_ID = "saml";

    private static final String MACEDIR_ENTITY_CATEGORY = "http://macedir.org/entity-category";
    private static final String REFEDS_HIDE_FROM_DISCOVERY = "http://refeds.org/category/hide-from-discovery";

    private DestinationValidator destinationValidator;

    @Override
    public String getName() {
        return "SAML v2.0";
    }

    @Override
    public SAMLIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new SAMLIdentityProvider(session, new SAMLIdentityProviderConfig(model), destinationValidator);
    }

    @Override
    public SAMLIdentityProviderConfig createConfig() {
        return new SAMLIdentityProviderConfig();
    }
    
    @Override
    public IdentityProviderModel parseConfig(KeycloakSession session, String config, IdentityProviderModel model) {
        SAMLIdentityProviderConfig samlIdentityProviderConfig = new SAMLIdentityProviderConfig(model);
        try {
            EntityDescriptorType entityType = SAMLMetadataUtil.parseEntityDescriptorType(config);
            Date validUntil = null;
            if (entityType.getValidUntil() != null) {
                validUntil = entityType.getValidUntil().toGregorianCalendar().getTime();
            }
            if (validUntil != null && validUntil.before(new Date())) {
                samlIdentityProviderConfig.setEnabled(false);
            } else {
                samlIdentityProviderConfig.setEnabled(true);
            }

            IDPSSODescriptorType idpDescriptor = SAMLMetadataUtil.locateIDPSSODescriptorType(entityType);

            if (idpDescriptor != null) {
                samlIdentityProviderConfig.setIdpEntityId(entityType.getEntityID());
                String singleSignOnServiceUrl = null;
                boolean postBindingRequest = false;
                boolean postBindingLogout = false;
                for (EndpointType endpoint : idpDescriptor.getSingleSignOnService()) {
                    if (endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get())) {
                        singleSignOnServiceUrl = endpoint.getLocation().toString();
                        postBindingRequest = true;
                    } else if (endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get())) {
                        singleSignOnServiceUrl = endpoint.getLocation().toString();
                        postBindingRequest = false;
                        break;
                    }
                }
                String singleLogoutServiceUrl = null;
                for (EndpointType endpoint : idpDescriptor.getSingleLogoutService()) {
                    if (endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get())) {
                        singleLogoutServiceUrl = endpoint.getLocation().toString();
                        postBindingLogout = true;
                    } else if (endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get())) {
                        singleLogoutServiceUrl = endpoint.getLocation().toString();
                        postBindingLogout = false;
                        break;
                    }

                }
                String artifactResolutionServiceUrl = null;
                boolean artifactBindingResponse = false;
                for (EndpointType endpoint : idpDescriptor.getArtifactResolutionService()) {
                    if (endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_SOAP_BINDING.get())) {
                        artifactResolutionServiceUrl = endpoint.getLocation().toString();
                        break;
                    }
                }
                samlIdentityProviderConfig.setIdpEntityId(entityType.getEntityID());
                samlIdentityProviderConfig.setSingleLogoutServiceUrl(singleLogoutServiceUrl);
                samlIdentityProviderConfig.setSingleSignOnServiceUrl(singleSignOnServiceUrl);
                samlIdentityProviderConfig.setArtifactResolutionServiceUrl(artifactResolutionServiceUrl);
                samlIdentityProviderConfig.setWantAuthnRequestsSigned(idpDescriptor.isWantAuthnRequestsSigned() != null ? idpDescriptor.isWantAuthnRequestsSigned() : false);
                samlIdentityProviderConfig.setAddExtensionsElementWithKeyInfo(false);
                samlIdentityProviderConfig.setValidateSignature(idpDescriptor.isWantAuthnRequestsSigned() != null ? idpDescriptor.isWantAuthnRequestsSigned() : false);
                samlIdentityProviderConfig.setArtifactBindingResponse(artifactBindingResponse);
                samlIdentityProviderConfig.setPostBindingAuthnRequest(postBindingRequest);
                samlIdentityProviderConfig.setPostBindingLogout(postBindingLogout);
                samlIdentityProviderConfig.setLoginHint(false);

                List<String> nameIdFormatList = idpDescriptor.getNameIDFormat();
                 //change NameIDPolicyFormat only for new IdP and if NameIDPolicyFormat exists and nameIdFormatList
                if (nameIdFormatList != null && !nameIdFormatList.isEmpty() && ( samlIdentityProviderConfig.getInternalId() == null || samlIdentityProviderConfig.getNameIDPolicyFormat() != null ))
                            samlIdentityProviderConfig.setNameIDPolicyFormat(nameIdFormatList.get(0));

                List<KeyDescriptorType> keyDescriptor = idpDescriptor.getKeyDescriptor();
                String defaultCertificate = null;

                //in case of autoupdate reconfigure keys
                    samlIdentityProviderConfig.getConfig().remove(SAMLIdentityProviderConfig.ENCRYPTION_PUBLIC_KEY);
                    samlIdentityProviderConfig.getConfig().remove(SAMLIdentityProviderConfig.SIGNING_CERTIFICATE_KEY);

                if (keyDescriptor != null) {
                    for (KeyDescriptorType keyDescriptorType : keyDescriptor) {
                        Element keyInfo = keyDescriptorType.getKeyInfo();
                        Element x509KeyInfo = DocumentUtil.getChildElement(keyInfo, new QName("dsig", "X509Certificate"));

                        if (KeyTypes.SIGNING.equals(keyDescriptorType.getUse())) {
                            samlIdentityProviderConfig.addSigningCertificate(x509KeyInfo.getTextContent());
                            samlIdentityProviderConfig.setValidateSignature(true);
                        } else if (KeyTypes.ENCRYPTION.equals(keyDescriptorType.getUse())) {
                            samlIdentityProviderConfig.setEncryptionPublicKey(x509KeyInfo.getTextContent());
                        } else if (keyDescriptorType.getUse() == null) {
                            defaultCertificate = x509KeyInfo.getTextContent();
                        }
                    }
                }

                if (defaultCertificate != null) {
                    if (samlIdentityProviderConfig.getSigningCertificates().length == 0) {
                        samlIdentityProviderConfig.addSigningCertificate(defaultCertificate);
                        samlIdentityProviderConfig.setValidateSignature(true);
                    }

                    if (samlIdentityProviderConfig.getEncryptionPublicKey() == null) {
                        samlIdentityProviderConfig.setEncryptionPublicKey(defaultCertificate);
                    }
                }

                    // check for hide on login attribute
                    boolean hideOnLogin = false;
                    List<SAMLIdentityProviderConfig.EntityAttributes> entityAttributes = new ArrayList<>();
                    if (entityType.getExtensions() != null && entityType.getExtensions().getEntityAttributes() != null) {
                        for (AttributeType attribute : entityType.getExtensions().getEntityAttributes().getAttribute()) {
                            SAMLIdentityProviderConfig.EntityAttributes entityAttr = new SAMLIdentityProviderConfig.EntityAttributes();
                            entityAttr.setName(attribute.getName());
                            entityAttr.setValues(attribute.getAttributeValue());
                            entityAttributes.add(entityAttr);
                            if (MACEDIR_ENTITY_CATEGORY.equals(attribute.getName())
                                    && attribute.getAttributeValue().contains(REFEDS_HIDE_FROM_DISCOVERY)) {
                                hideOnLogin = true;
                                break;
                            }
                        }
                        samlIdentityProviderConfig.setEntityAttributes(entityAttributes);
                    } else {
                        samlIdentityProviderConfig.setEntityAttributes(null);
                    }
                    samlIdentityProviderConfig.setHideOnLogin(hideOnLogin);
                    samlIdentityProviderConfig.getConfig().put(IdentityProviderModel.LEGACY_HIDE_ON_LOGIN_ATTR, String.valueOf(hideOnLogin));
                    return samlIdentityProviderConfig;
            }
        } catch (ParsingException pe) {
            throw new RuntimeException("Could not parse IdP SAML Metadata", pe);
        }

        return samlIdentityProviderConfig;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(Scope config) {
        super.init(config);

        this.destinationValidator = DestinationValidator.forProtocolMap(config.getArray("knownProtocols"));
    }
}
