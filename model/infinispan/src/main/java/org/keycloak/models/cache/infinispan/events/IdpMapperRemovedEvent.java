package org.keycloak.models.cache.infinispan.events;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.cache.infinispan.events.serialization.EventSerializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@SerializeWith(IdpMapperRemovedEvent.ExternalizerImpl.class)
public class IdpMapperRemovedEvent implements ClusterEvent {
    public static String EVENT_NAME = "IDP_MAPPER_REMOVED_EVENT";

    private String realmId;
    private String mapperId;

    public IdpMapperRemovedEvent(){ }

    public IdpMapperRemovedEvent(String realmId, String mapperId) {
        this.realmId = realmId;
        this.mapperId = mapperId;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getMapperId() {
        return mapperId;
    }

    public void setMapperId(String mapperId) {
        this.mapperId = mapperId;
    }

    public static class ExternalizerImpl implements Externalizer<IdpMapperRemovedEvent> {

        @Override
        public void writeObject(ObjectOutput output, IdpMapperRemovedEvent obj) throws IOException {
            MarshallUtil.marshallString(obj.getRealmId(), output);
            MarshallUtil.marshallString(obj.getMapperId(), output);
        }

        @Override
        public IdpMapperRemovedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            String realmId = MarshallUtil.unmarshallString(input);
            String mapperId = MarshallUtil.unmarshallString(input);
            return new IdpMapperRemovedEvent(realmId, mapperId);
        }
    }

}