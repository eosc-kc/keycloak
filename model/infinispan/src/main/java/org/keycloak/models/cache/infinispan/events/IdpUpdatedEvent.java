package org.keycloak.models.cache.infinispan.events;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.cache.infinispan.events.serialization.EventSerializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@SerializeWith(IdpUpdatedEvent.ExternalizerImpl.class)
public class IdpUpdatedEvent implements ClusterEvent {
    public static String EVENT_NAME = "IDP_UPDATED_EVENT";

    private String realmId;
    private String idpId;

    public IdpUpdatedEvent(){ }

    public IdpUpdatedEvent(String realmId, String idpId) {
        this.realmId = realmId;
        this.idpId = idpId;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getIdpId() {
        return idpId;
    }

    public void setIdpId(String idpId) {
        this.idpId = idpId;
    }

    public static class ExternalizerImpl implements Externalizer<IdpUpdatedEvent> {

        @Override
        public void writeObject(ObjectOutput output, IdpUpdatedEvent obj) throws IOException {
            MarshallUtil.marshallString(obj.getRealmId(), output);
            MarshallUtil.marshallString(obj.getIdpId(), output);
        }

        @Override
        public IdpUpdatedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            String realmId = MarshallUtil.unmarshallString(input);
            String idpId = MarshallUtil.unmarshallString(input);
            return new IdpUpdatedEvent(realmId, idpId);
        }

    }

}
