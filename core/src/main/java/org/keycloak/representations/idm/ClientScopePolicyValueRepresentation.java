package org.keycloak.representations.idm;

public class ClientScopePolicyValueRepresentation {

    private String id;
    private String value;
    private Boolean negateOutput;
    private Boolean regex;

    public ClientScopePolicyValueRepresentation(){
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Boolean getNegateOutput() {
        return negateOutput;
    }

    public void setNegateOutput(Boolean negateOutput) {
        this.negateOutput = negateOutput;
    }

    public Boolean getRegex() {
        return regex;
    }

    public void setRegex(Boolean regex) {
        this.regex = regex;
    }
}
