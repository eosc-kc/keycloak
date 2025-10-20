package org.keycloak.dom.saml.v2.metadata;

public class ScopeType {

	protected String value;

    protected String regexp;

    public ScopeType(String regexp) {
        this.regexp = regexp;
    }

    /**
     * Gets the value of the value property.
     *
     * @return possible object is {@link String }
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value allowed object is {@link String }
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the regexp property.
     *
     * @return possible object is {@link String }
     */
    public String getRegxp() {
        return regexp;
    }
	
}
