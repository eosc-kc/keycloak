import type JsonWebKeySetRepresentation from "libs/keycloak-admin-client/lib/defs/jsonWebKeySetRepresentation";
import type OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/openIdFederationRepresentation";
import { importJWK, type JWK } from "jose";

import { prettyPrintJSON } from "../../util";

export type OpenIdFederationFormValues = Omit<
  OpenIdFederationRepresentation,
  "jwks"
> & {
  jwks: string;
};

export type JwksValidationMessage =
  | "jwksInvalidJson"
  | "jwksInvalidFormat"
  | "jwksKeysRequired"
  | "jwksKeyIdRequired"
  | "jwksDuplicateKeyId"
  | "jwksInvalidKey";

const isObject = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null && !Array.isArray(value);

export const validateJwks = async (
  value: string,
): Promise<JwksValidationMessage | undefined> => {
  if (!value.trim()) {
    return undefined;
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(value);
  } catch {
    return "jwksInvalidJson";
  }

  if (!isObject(parsed) || !Array.isArray(parsed.keys)) {
    return "jwksInvalidFormat";
  }

  if (parsed.keys.length === 0) {
    return "jwksKeysRequired";
  }

  const keyIds = new Set<string>();
  for (const value of parsed.keys) {
    if (!isObject(value)) {
      return "jwksInvalidFormat";
    }

    const keyId = value.kid;
    if (typeof keyId !== "string" || !keyId.trim()) {
      return "jwksKeyIdRequired";
    }

    if (keyIds.has(keyId)) {
      return "jwksDuplicateKeyId";
    }
    keyIds.add(keyId);

    try {
      await importJWK(value as JWK);
    } catch {
      return "jwksInvalidKey";
    }
  }

  return undefined;
};

export const decodeJwksFromForm = (
  value: string,
): JsonWebKeySetRepresentation | undefined => {
  if (!value.trim()) {
    return undefined;
  }

  return JSON.parse(value) as JsonWebKeySetRepresentation;
};

export const toOpenIdFederationFormValues = (
  representation: OpenIdFederationRepresentation,
): OpenIdFederationFormValues => ({
  ...representation,
  jwks: representation.jwks ? prettyPrintJSON(representation.jwks) : "",
});

export const toOpenIdFederationRepresentation = (
  formValues: OpenIdFederationFormValues,
): OpenIdFederationRepresentation => {
  const { jwks, ...representation } = formValues;

  return {
    ...representation,
    jwks: decodeJwksFromForm(jwks),
  };
};
