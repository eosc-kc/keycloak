package org.keycloak.broker.saml.mappers;

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;

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

public class UserAttributeTemplateMapper extends AbstractIdentityProviderMapper {

    private static final String[] COMPATIBLE_PROVIDERS = {SAMLIdentityProviderFactory.PROVIDER_ID};

    private static final String TEMPLATE = "template";
    private static final String USER_ATTRIBUTE = "user.attribute";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(TEMPLATE);
        property.setLabel("Template");
        property.setHelpText("""
        Template to use to format the user attribute to import/update. Substitutions are enclosed in ${}. 
        For example: '${ALIAS}.${NAMEID}'. ALIAS is the provider alias. NAMEID is that SAML name id assertion. 
        ATTRIBUTE.<NAME> references a SAML attribute where name is the attribute name or friendly name.
        
        The substitution can be converted to upper or lower case by appending |uppercase or |lowercase 
        to the substituted value, e.g. '${NAMEID | lowercase}'
        
        Local part of email can be extracted by appending |localpart to the substituted value, 
        e.g. ${CLAIM.email | localpart}. If "@" is not part of the string, this conversion 
        leaves the substitution untouched.
        """);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("${ALIAS}.${NAMEID}");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText("User attribute name to store claim.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

    }

    public static final String PROVIDER_ID = "saml-userattribute-idp-mapper";

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
        AssertionType assertion = (AssertionType)context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
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
            } else if (variable.equals("NAMEID")) {
                SubjectType subject = assertion.getSubject();
                SubjectType.STSubType subType = subject.getSubType();
                NameIDType subjectNameID = (NameIDType) subType.getBaseID();
                m.appendReplacement(sb, transformer.apply(subjectNameID.getValue()));
            } else if (variable.startsWith("ATTRIBUTE.")) {
                String name = variable.substring("ATTRIBUTE.".length());
                String value = "";
                for (AttributeStatementType statement : assertion.getAttributeStatements()) {
                    for (AttributeStatementType.ASTChoiceType choice : statement.getAttributes()) {
                        AttributeType attr = choice.getAttribute();
                        if (name.equals(attr.getName()) || name.equals(attr.getFriendlyName())) {
                            List<Object> attributeValue = attr.getAttributeValue();
                            if (attributeValue != null && !attributeValue.isEmpty()) {
                                value = attributeValue.get(0).toString();
                            }
                            break;
                        }
                    }
                }
                m.appendReplacement(sb, transformer.apply(value));
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

