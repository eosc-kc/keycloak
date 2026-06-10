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
package org.keycloak.broker.federation;

import org.keycloak.models.FederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;


public interface SAMLFederationProviderFactory extends ProviderFactory<FederationProvider> {

	
    String getName();

    
    FederationProvider create(KeycloakSession session, FederationModel model, String realmId);


	static SAMLFederationProviderFactory getSAMLFederationProviderFactoryById(KeycloakSession session, String providerId) {
		return (SAMLFederationProviderFactory) session.getKeycloakSessionFactory()
				.getProviderFactoriesStream(FederationProvider.class).filter(pf -> pf.getId().equals(providerId))
				.findAny().orElse(null);
	};
    
}