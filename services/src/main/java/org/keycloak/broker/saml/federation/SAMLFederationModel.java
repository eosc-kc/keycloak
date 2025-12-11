package org.keycloak.broker.saml.federation;

import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.*;

import com.fasterxml.jackson.core.type.TypeReference;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.models.FederationModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.LinkedList;

public class SAMLFederationModel extends FederationModel {
	
	public SAMLFederationModel(){
		super();
	}
	
	public SAMLFederationModel(FederationModel model) {
		super(model);
	}

    public String getNameIDPolicyFormat() {
        return getConfig().get(NAME_ID_POLICY_FORMAT);
    }

    public LinkedList<SAMLIdentityProviderConfig.Principal> getMultiplePrincipals() throws IOException {
        String principalsJson =getConfig().get(MULTIPLE_PRINCIPALS);
        if (principalsJson != null ) {
            return JsonSerialization.readValue(principalsJson, new TypeReference<LinkedList<SAMLIdentityProviderConfig.Principal>>() {
            });
        } else {
            return new LinkedList<>();
        }
    }

    public boolean isWantAuthnRequestsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_AUTHN_REQUESTS_SIGNED));
    }

    public boolean isWantLogoutRequestsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_LOGOUT_REQUESTS_SIGNED));
    }

    public boolean isWantAssertionsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_ASSERTIONS_SIGNED));
    }

    public String getWantAssertionsEncrypted() {
        return getConfig().get(WANT_ASSERTIONS_ENCRYPTED);
    }
    
    public boolean isPostBindingResponse() {
        return Boolean.valueOf(getConfig().get(POST_BINDING_RESPONSE));
    }

    public boolean isPostBindingLogoutReceivingRequest() {
        return Boolean.valueOf(getConfig().get(POST_BINDING_LOGOUT_RECEIVING_REQUEST));
    }

    public boolean isOmitAttributeConsumingServiceIndexAuthn() {
        return Boolean.valueOf(getConfig().get(OMIT_ATTRIBUTE_CONSUMING_SERVICE_INDEX_AUTHN));
    }

    public boolean isPassSetMfa() {
        return Boolean.valueOf(getConfig().get(IdentityProviderModel.PASS_SET_MFA));
    }
	
}
