/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.crypto;

/**
 * Category of a realm key, used to select protocol-specific keys with fallback to {@link #GENERAL}.
 */
public enum KeyCategory {

    GENERAL("general"),
    OIDC("oidc"),
    SAML("saml"),
    OPENID_FEDERATION("openid-federation");

    private final String specName;

    KeyCategory(String specName) {
        this.specName = specName;
    }

    public String getSpecName() {
        return specName;
    }

    public static KeyCategory fromSpecName(String specName) {
        if (specName == null || specName.isEmpty()) {
            return GENERAL;
        }
        for (KeyCategory category : values()) {
            if (category.specName.equalsIgnoreCase(specName) || category.name().equalsIgnoreCase(specName)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown key category: " + specName);
    }
}
