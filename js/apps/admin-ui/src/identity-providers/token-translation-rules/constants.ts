export const TOKEN_TRANSLATION_PROVIDER_TYPE =
  "org.keycloak.protocol.oidc.ProxiedTokenIntrospectionTranslationsProvider";

export const IDP_TOKEN_TRANSLATION_SUB_TYPE = "idp";

export const tokenTranslationRuleOrder = (order?: string | string[]) => {
  const value = Array.isArray(order) ? order[0] : order;
  const parsed = Number.parseInt(value ?? "", 10);
  return Number.isNaN(parsed) ? Number.MAX_SAFE_INTEGER : parsed;
};
