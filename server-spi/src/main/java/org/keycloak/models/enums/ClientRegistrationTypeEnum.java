package org.keycloak.models.enums;

public enum ClientRegistrationTypeEnum {
    EXPLICIT("explicit"), AUTOMATIC("automatic");

    private final String value;

    ClientRegistrationTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
