package org.keycloak.broker.saml.aggregate.metadata;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

public class SAMLAggregateIdentityProviderEventListener implements EventListenerProvider {

  private final KeycloakSession session;
  
  
  public SAMLAggregateIdentityProviderEventListener(KeycloakSession session) {
    this.session = session;
    
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub

  }

  @Override
  public void onEvent(Event event) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onEvent(AdminEvent event, boolean includeRepresentation) {
    

  }

}
