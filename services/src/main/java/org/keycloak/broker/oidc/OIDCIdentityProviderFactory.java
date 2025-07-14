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
package org.keycloak.broker.oidc;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Pedro Igor
 */
public class OIDCIdentityProviderFactory extends AbstractIdentityProviderFactory<OIDCIdentityProvider> {

    public static final String PROVIDER_ID = "oidc";

    @Override
    public String getName() {
        return "OpenID Connect v1.0";
    }

    @Override
    public OIDCIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new OIDCIdentityProvider(session, new OIDCIdentityProviderConfig(model));
    }

    @Override
    public OIDCIdentityProviderConfig createConfig() {
        return new OIDCIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public IdentityProviderModel parseConfig(KeycloakSession session, InputStream inputStream, IdentityProviderModel model) {
        return parseOIDCConfig(session, inputStream, model, OIDCIdentityProviderConfig.class);
    }

    protected static <T extends OIDCIdentityProviderConfig> T parseOIDCConfig(
            KeycloakSession session,
            InputStream inputStream,
            IdentityProviderModel model,
            Class<T> configClass
    ) {
        OIDCConfigurationRepresentation rep;
        try {
            rep = JsonSerialization.readValue(inputStream, OIDCConfigurationRepresentation.class);
        } catch (IOException e) {
            throw new RuntimeException("failed to load openid connect metadata", e);
        }
        try {
            T config = configClass.getConstructor(IdentityProviderModel.class).newInstance(model);
            config.setIssuer(rep.getIssuer());
            config.setLogoutUrl(rep.getLogoutEndpoint());
            config.setAuthorizationUrl(rep.getAuthorizationEndpoint());
            config.setTokenUrl(rep.getTokenEndpoint());
            config.setUserInfoUrl(rep.getUserinfoEndpoint());
            if (rep.getJwksUri() != null) {
                config.setValidateSignature(true);
                config.setUseJwksUrl(true);
                config.setJwksUrl(rep.getJwksUri());
            } else if (config.getJwksUrl() != null) {
                config.setUseJwksUrl(false);
                config.setJwksUrl(null);
            }
            config.setTokenIntrospectionUrl(rep.getIntrospectionEndpoint());
            config.setClaimsParameterSupported(rep.getClaimsParameterSupported() != null ? rep.getClaimsParameterSupported() : false);
            return config;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate config", e);
        }

    }

}
