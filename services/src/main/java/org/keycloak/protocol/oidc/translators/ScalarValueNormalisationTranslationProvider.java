package org.keycloak.protocol.oidc.translators;

import org.keycloak.component.ComponentModel;
import org.keycloak.protocol.oidc.ProxiedTokenIntrospectionTranslationsProvider;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScalarValueNormalisationTranslationProvider implements ProxiedTokenIntrospectionTranslationsProvider {

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
        if (claim == null || claim.trim().isEmpty() || oldValue == null || oldValue.trim().isEmpty() || responseNode == null || responseNode.get(claim) == null || !responseNode.get(claim).isTextual()) {
            return;
        }

        String existingText = responseNode.get(claim).asText();

        if (isRegex) {
            Matcher matcher = Pattern.compile(oldValue).matcher(existingText);
            if (matcher.find() && newValue == null) {
                responseNode.remove(claim);
            } else if (matcher.find()) {
                responseNode.put(claim, matcher.replaceAll(newValue));
            }
        } else if (existingText.equals(oldValue)) {
            if (newValue == null) {
                responseNode.remove(claim);
            } else {
                responseNode.put(claim, newValue);
            }

        }
    }

    @Override
    public void close() {}
}
