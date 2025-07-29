package org.keycloak.services.scheduled;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.saml.EntityDescriptorDescriptionConverter;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;
import org.keycloak.validation.ValidationUtil;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

public class AutoUpdateSAMLClient implements ScheduledTask {

    protected static final Logger logger = Logger.getLogger(AutoUpdateSAMLClient.class);

    protected final String id;
    protected final String realmId;

    public AutoUpdateSAMLClient(String id, String realmId) {
        this.id = id;
        this.realmId = realmId;
    }

    @Override
    public void run(KeycloakSession session) {
        logger.info(" Updating SAML client with id= " + id + " in realm= " + realmId);
        RealmModel realm = session.realms().getRealm(realmId);
        if ( realm == null) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTaskAndNotify("AutoUpdateSAMLClient_" + id);
            return;
        }
        ClientModel client = session.clients().getClientById(realm, id);
        if (client == null || !"saml".equals(client.getProtocol()) || ! "true".equals(client.getAttribute(SamlConfigAttributes.SAML_AUTO_UPDATED)) ) {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.cancelTaskAndNotify("AutoUpdateSAMLClient_" + id);
            return;
        }
        InputStream inputStream = null;
        try {
            inputStream = session.getProvider(HttpClientProvider.class).get(client.getAttribute(SamlConfigAttributes.SAML_METADATA_URL));
            ClientRepresentation rep = ModelToRepresentation.toRepresentation(client, session);
            EntityDescriptorDescriptionConverter.loadEntityDescriptors(inputStream, rep);
            rep.getAttributes().put(SamlConfigAttributes.SAML_LAST_REFRESH_TIME, String.valueOf(Instant.now().toEpochMilli()));
            RepresentationToModel.updateClient(rep, client, session);
            RepresentationToModel.updateClientProtocolMappers(rep, client);

            ValidationUtil.validateClient(session, client, false, r -> {
                session.getTransactionManager().setRollbackOnly();
                throw new ErrorResponseException(
                        Errors.INVALID_INPUT,
                        r.getAllLocalizedErrorsAsString(AdminRoot.getMessages(session, realm, "en")),
                        Response.Status.BAD_REQUEST);
            });

            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    e.printStackTrace();
                }
            }
        }
    }
}

