package org.keycloak.partialimport;

import java.util.List;

import org.keycloak.broker.federation.FederationProvider;
import org.keycloak.broker.federation.SAMLFederationProviderFactory;
import org.keycloak.models.FederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.SAMLFederationRepresentation;

public class IdentityProvidersFederationPartialImport extends AbstractPartialImport<SAMLFederationRepresentation> {

    @Override
    public List<SAMLFederationRepresentation> getRepList(PartialImportRepresentation partialImportRep) {
        return partialImportRep.getSamlFederations();
    }

    @Override
    public String getName(SAMLFederationRepresentation rep) {
        return rep.getAlias();
    }

    @Override
    public String getModelId(RealmModel realm, KeycloakSession session, SAMLFederationRepresentation rep) {
        return rep.getInternalId();
    }

    @Override
    public boolean exists(RealmModel realm, KeycloakSession session, SAMLFederationRepresentation rep) {
        return realm.getSAMLFederationById(rep.getInternalId()) != null;
    }

    @Override
    public String existsMessage(RealmModel realm, SAMLFederationRepresentation metadataAggregate) {
        return "SAML Federation'" + getName(metadataAggregate) + "' already exists.";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.SAML_FEDERATION;
    }

    @Override
    public void remove(RealmModel realm, KeycloakSession session, SAMLFederationRepresentation rep) {
        FederationProvider federationProvider = SAMLFederationProviderFactory.getSAMLFederationProviderFactoryById(session, rep.getProviderId()).create(session,RepresentationToModel.toModel(rep),realm.getId());
        federationProvider.removeFederation();
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, SAMLFederationRepresentation rep) {
    	rep.setInternalId(KeycloakModelUtils.generateId());
        FederationModel model = RepresentationToModel.toModel(rep);
        realm.addSAMLFederation(model);
        rep.getFederationMappers().stream().forEach(mapper -> {
            mapper.setFederationId(rep.getInternalId());
            realm.addIdentityProvidersFederationMapper(RepresentationToModel.toModel(mapper));
        });
        FederationProvider federationProvider = SAMLFederationProviderFactory.getSAMLFederationProviderFactoryById(session, model.getProviderId()).create(session,model,realm.getId());
        federationProvider.enableUpdateTask();
    }

}
