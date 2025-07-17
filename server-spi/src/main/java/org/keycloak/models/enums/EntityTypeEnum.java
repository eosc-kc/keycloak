package org.keycloak.models.enums;

public enum EntityTypeEnum {
    OPENID_PROVIDER("openid_provider"), OPENID_RELAYING_PARTY("openid_relying_party");

    private final String value;

    EntityTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
