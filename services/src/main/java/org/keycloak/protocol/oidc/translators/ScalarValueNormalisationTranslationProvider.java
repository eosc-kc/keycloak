package org.keycloak.protocol.oidc.translators;

import org.keycloak.component.ComponentModel;
import org.keycloak.protocol.oidc.ProxiedTokenIntrospectionTranslationsProvider;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

public class ScalarValueNormalisationTranslationProvider implements ProxiedTokenIntrospectionTranslationsProvider {

    private static final Logger logger = Logger.getLogger(ScalarValueNormalisationTranslationProvider.class);

    private final boolean isRegex;
    private final String claim;
    private final String oldValue;
    private final String newValue;

    public ScalarValueNormalisationTranslationProvider(ComponentModel model) {
        this.isRegex = Boolean.parseBoolean(model.getConfig().getFirst(ScalarValueNormalisationTranslationProviderFactory.IS_REGEX));
        this.claim = model.getConfig().getFirst(ScalarValueNormalisationTranslationProviderFactory.CLAIM);
        this.oldValue = model.getConfig().getFirst(ScalarValueNormalisationTranslationProviderFactory.OLD_VALUE);
        this.newValue = model.getConfig().getFirst(ScalarValueNormalisationTranslationProviderFactory.NEW_VALUE);
    }

    @Override
    public void translate(ObjectNode responseNode) {
        if (claim == null || claim.trim().isEmpty() || oldValue == null || oldValue.trim().isEmpty() || responseNode == null || responseNode.get(claim) == null || !responseNode.get(claim).asText().equals(oldValue)) {
            return;
        }

       if (newValue == null){
           responseNode.remove(claim);
       } else {
           responseNode.put(claim, newValue);
       }
    }

    @Override
    public void close() {}
}
