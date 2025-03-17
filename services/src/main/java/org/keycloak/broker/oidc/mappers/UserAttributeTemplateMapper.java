package org.keycloak.broker.oidc.mappers;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.social.bitbucket.BitbucketIdentityProviderFactory;
import org.keycloak.social.facebook.FacebookIdentityProviderFactory;
import org.keycloak.social.github.GitHubIdentityProviderFactory;
import org.keycloak.social.gitlab.GitLabIdentityProviderFactory;
import org.keycloak.social.google.GoogleIdentityProviderFactory;
import org.keycloak.social.instagram.InstagramIdentityProviderFactory;
import org.keycloak.social.linkedin.LinkedInOIDCIdentityProviderFactory;
import org.keycloak.social.microsoft.MicrosoftIdentityProviderFactory;
import org.keycloak.social.openshift.OpenshiftV3IdentityProviderFactory;
import org.keycloak.social.openshift.OpenshiftV4IdentityProviderFactory;
import org.keycloak.social.paypal.PayPalIdentityProviderFactory;
import org.keycloak.social.stackoverflow.StackoverflowIdentityProviderFactory;
import org.keycloak.social.twitter.TwitterIdentityProviderFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.keycloak.broker.saml.mappers.UsernameTemplateMapper.TRANSFORMERS;

public class UserAttributeTemplateMapper extends AbstractClaimMapper {

    private static final String[] COMPATIBLE_PROVIDERS = {
            KeycloakOIDCIdentityProviderFactory.PROVIDER_ID,
            OIDCIdentityProviderFactory.PROVIDER_ID,
            BitbucketIdentityProviderFactory.PROVIDER_ID,
            FacebookIdentityProviderFactory.PROVIDER_ID,
            GitHubIdentityProviderFactory.PROVIDER_ID,
            GitLabIdentityProviderFactory.PROVIDER_ID,
            GoogleIdentityProviderFactory.PROVIDER_ID,
            InstagramIdentityProviderFactory.PROVIDER_ID,
            LinkedInOIDCIdentityProviderFactory.PROVIDER_ID,
            MicrosoftIdentityProviderFactory.PROVIDER_ID,
            OpenshiftV3IdentityProviderFactory.PROVIDER_ID,
            OpenshiftV4IdentityProviderFactory.PROVIDER_ID,
            PayPalIdentityProviderFactory.PROVIDER_ID,
            StackoverflowIdentityProviderFactory.PROVIDER_ID,
            TwitterIdentityProviderFactory.PROVIDER_ID
    };

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    public static final String TEMPLATE = "template";
    public static final String USER_ATTRIBUTE = "user.attribute";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(TEMPLATE);
        property.setLabel("Template");
        property.setHelpText("Template to use to format the user attribute to import/update.  Substitutions are enclosed in ${}.  For example: '${ALIAS}.${CLAIM.sub}'.  ALIAS is the provider alias.  CLAIM.<NAME> references an ID or Access token claim. \n"
                + "The substitution can be converted to upper or lower case by appending |uppercase or |lowercase to the substituted value, e.g. '${CLAIM.sub | lowercase}");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("${ALIAS}.${CLAIM.preferred_username}");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText("User attribute name to store claim.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "oidc-userattribute-idp-mapper";

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Preprocessor";
    }

    @Override
    public String getDisplayType() {
        return "User attribute Template Importer";
    }

    @Override
    public void updateBrokeredUserLegacy(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        user.setSingleAttribute(mapperModel.getConfig().get(USER_ATTRIBUTE), getUserAttributeValueFromTemplate(mapperModel, context));
    }

    private static final Pattern SUBSTITUTION = Pattern.compile("\\$\\{([^}]+?)(?:\\s*\\|\\s*(\\S+)\\s*)?\\}");

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        context.setUserAttribute(mapperModel.getConfig().get(USER_ATTRIBUTE), getUserAttributeValueFromTemplate(mapperModel, context));
    }

    private String getUserAttributeValueFromTemplate(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String template = mapperModel.getConfig().get(TEMPLATE);
        Matcher m = SUBSTITUTION.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String variable = m.group(1);
            UnaryOperator<String> transformer = Optional.ofNullable(m.group(2)).map(TRANSFORMERS::get).orElse(UnaryOperator.identity());

            if (variable.equals("ALIAS")) {
                m.appendReplacement(sb, transformer.apply(context.getIdpConfig().getAlias()));
            } else if (variable.equals("UUID")) {
                m.appendReplacement(sb, transformer.apply(KeycloakModelUtils.generateId()));
            } else if (variable.startsWith("CLAIM.")) {
                String name = variable.substring("CLAIM.".length());
                Object value = AbstractClaimMapper.getClaimValue(context, name);
                if (value == null) value = "";
                m.appendReplacement(sb, transformer.apply(value.toString()));
            } else {
                m.appendReplacement(sb, m.group(1));
            }

        }
        m.appendTail(sb);
        return sb.toString();
    }

    @Override
    public String getHelpText() {
        return "Set user attribute based on template.";
    }
}
