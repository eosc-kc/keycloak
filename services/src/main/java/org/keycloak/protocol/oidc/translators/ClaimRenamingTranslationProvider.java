package org.keycloak.protocol.oidc.translators;

import org.keycloak.component.ComponentModel;
import org.keycloak.protocol.oidc.ProxiedTokenIntrospectionTranslationsProvider;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

public class ClaimRenamingTranslationProvider implements ProxiedTokenIntrospectionTranslationsProvider {

    private static final Logger logger = Logger.getLogger(ClaimRenamingTranslationProvider.class);

    private final boolean overrideNewClaim;
    private final String oldClaim;
    private final String newClaim;

    public ClaimRenamingTranslationProvider(ComponentModel model) {
        this.overrideNewClaim = Boolean.parseBoolean(model.getConfig().getFirst(ClaimRenamingTranslationProviderFactory.OVERRIDE_NEW_CLAIM));
        this.oldClaim = model.getConfig().getFirst(ClaimRenamingTranslationProviderFactory.OLD_CLAIM);
        this.newClaim = model.getConfig().getFirst(ClaimRenamingTranslationProviderFactory.NEW_CLAIM);
    }

    @Override
    public void translate(ObjectNode responseNode) {
        if (oldClaim == null || oldClaim.trim().isEmpty() || newClaim == null || responseNode == null) {
            return;
        }

        translateDirect(responseNode);
    }

    private void translateDirect(ObjectNode responseNode) {
        if (!responseNode.has(oldClaim) || oldClaim.equals(newClaim) || (responseNode.has(newClaim) && !overrideNewClaim)) {
            return;
        }

        // Move value to new claim and remove old claim
        responseNode.set(newClaim, responseNode.get(oldClaim));
        responseNode.remove(oldClaim);
    }

    @Override
    public void close() {}
}
