package org.keycloak.protocol.oidc.translators;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.keycloak.component.ComponentModel;
import org.keycloak.protocol.oidc.ProxiedTokenIntrospectionTranslationsProvider;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

public class ClaimRemovalTranslationProvider implements ProxiedTokenIntrospectionTranslationsProvider {

    private static final Logger logger = Logger.getLogger(ClaimRemovalTranslationProvider.class);

    private final boolean isRegex;
    private final String claim;

    public ClaimRemovalTranslationProvider(ComponentModel model) {
        this.isRegex = Boolean.parseBoolean(model.getConfig().getFirst(ClaimRemovalTranslationProviderFactory.IS_REGEX));
        this.claim = model.getConfig().getFirst(ClaimRemovalTranslationProviderFactory.CLAIM);
    }

    @Override
    public void translate(ObjectNode responseNode) {
        if (claim == null || claim.trim().isEmpty() || responseNode == null) {
            return;
        }

        if (isRegex) {
            removeRegex(responseNode);
        } else {
            removeDirect(responseNode);
        }
    }

    private void removeDirect(ObjectNode responseNode) {
        if (!responseNode.has(claim)) {
            return;
        }
        responseNode.remove(claim);
    }

    private void removeRegex(ObjectNode responseNode) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(claim);
        } catch (PatternSyntaxException e) {
            logger.errorv("Invalid regex pattern defined for oldClaim: {0}", claim);
            return;
        }

        // Take a snapshot of field names to prevent ConcurrentModificationException
        List<String> fieldNames = new ArrayList<>();
        responseNode.fieldNames().forEachRemaining(fieldNames::add);

        for (String field : fieldNames) {
            Matcher matcher = pattern.matcher(field);
            if (matcher.find()) {
                responseNode.remove(field);
            }
        }
    }

    @Override
    public void close() {}
}
