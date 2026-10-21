export const TOKEN_TRANSLATION_PROVIDER_TYPE =
  "org.keycloak.protocol.oidc.ProxiedTokenIntrospectionTranslationsProvider";

export const IDP_TOKEN_TRANSLATION_SUB_TYPE = "idp";
export const CLIENT_TOKEN_TRANSLATION_SUB_TYPE = "client";
export const GLOBAL_TOKEN_TRANSLATION_SUB_TYPE = "global";
export const DEFAULT_TOKEN_TRANSLATION_SUB_TYPE = "default";

export type TokenTranslationRuleSubType =
  | typeof IDP_TOKEN_TRANSLATION_SUB_TYPE
  | typeof CLIENT_TOKEN_TRANSLATION_SUB_TYPE
  | typeof GLOBAL_TOKEN_TRANSLATION_SUB_TYPE
  | typeof DEFAULT_TOKEN_TRANSLATION_SUB_TYPE;

export const tokenTranslationRuleOrder = (order?: string | string[]) => {
  const value = Array.isArray(order) ? order[0] : order;
  const parsed = Number.parseInt(value ?? "", 10);
  return Number.isNaN(parsed) ? Number.MAX_SAFE_INTEGER : parsed;
};
