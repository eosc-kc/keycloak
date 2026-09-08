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

public class ClaimRenamingTranslationProvider implements ProxiedTokenIntrospectionTranslationsProvider {

    private static final Logger logger = Logger.getLogger(ClaimRenamingTranslationProvider.class);

    private final boolean isRegex;
    private final boolean overrideNewClaim;
    private final String oldClaim;
    private final String newClaim;

    public ClaimRenamingTranslationProvider(ComponentModel model) {
        this.isRegex = Boolean.parseBoolean(model.getConfig().getFirst(ClaimRenamingTranslationProviderFactory.IS_REGEX));
        this.overrideNewClaim = Boolean.parseBoolean(model.getConfig().getFirst(ClaimRenamingTranslationProviderFactory.OVERRIDE_NEW_CLAIM));
        this.oldClaim = model.getConfig().getFirst(ClaimRenamingTranslationProviderFactory.OLD_CLAIM);
        this.newClaim = model.getConfig().getFirst(ClaimRenamingTranslationProviderFactory.NEW_CLAIM);
    }

    @Override
    public void translate(ObjectNode responseNode) {
        if (oldClaim == null || oldClaim.trim().isEmpty() || newClaim == null || responseNode == null) {
            return;
        }

        if (isRegex) {
            translateRegex(responseNode);
        } else {
            translateDirect(responseNode);
        }
    }

    private void translateDirect(ObjectNode responseNode) {
        if (!responseNode.has(oldClaim) || oldClaim.equals(newClaim) || (responseNode.has(newClaim) && !overrideNewClaim)) {
            return;
        }

        // Move value to new claim and remove old claim
        responseNode.set(newClaim, responseNode.get(oldClaim));
        responseNode.remove(oldClaim);
    }

    private void translateRegex(ObjectNode responseNode) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(oldClaim);
        } catch (PatternSyntaxException e) {
            logger.errorv("Invalid regex pattern defined for oldClaim: {0}", oldClaim);
            return;
        }

        // Take a snapshot of field names to prevent ConcurrentModificationException
        List<String> fieldNames = new ArrayList<>();
        responseNode.fieldNames().forEachRemaining(fieldNames::add);

        for (String field : fieldNames) {
            Matcher matcher = pattern.matcher(field);
            if (matcher.find()) {
                String targetClaim = matcher.replaceAll(newClaim);

                if (field.equals(targetClaim) || (responseNode.has(targetClaim) && !overrideNewClaim)) {
                    continue;
                }

                responseNode.set(targetClaim, responseNode.get(field));
                responseNode.remove(field);
            }
        }
    }

    @Override
    public void close() {}
}
