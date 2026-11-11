import type { JWTPayload, ProtectedHeaderParameters } from "jose";
import { decodeJwt, decodeProtectedHeader } from "jose";

import type TrustMarkRepresentation from "@keycloak/keycloak-admin-client/lib/defs/trustMarkRepresentation";
import type { TrustMarkRequest } from "@keycloak/keycloak-admin-client/lib/defs/trustMarkRepresentation";

export type TrustMarkFormValues = {
  trustMarkType: string;
  selfSigned: boolean;
  jwt: string;
  iat: string;
  exp: string;
  logoUri: string;
  ref: string;
  delegation: string;
};

export type DecodedTrustMark = {
  issuer: string;
  subject: string;
  trustMarkType: string;
  issuedAt: number;
  expiresAt?: number;
  logoUri?: string;
  ref?: string;
  delegation?: string;
};

export type TrustMarkValidationMessage =
  | "trustMarkInvalidJwt"
  | "trustMarkSignatureRequired"
  | "trustMarkInvalidTypeHeader"
  | "trustMarkInvalidAlgorithm"
  | "trustMarkKeyIdRequired"
  | "trustMarkRequiredClaims"
  | "trustMarkInvalidClaimTypes"
  | "trustMarkExpirationInPast"
  | "trustMarkTypeMismatch";

export type TrustMarkValidationResult = {
  decoded?: DecodedTrustMark;
  error?: TrustMarkValidationMessage;
};

export const normalizeTrustMarkJwt = (value: string) =>
  value.replaceAll(/\s/g, "");

const isNonEmptyString = (value: unknown): value is string =>
  typeof value === "string" && value.trim().length > 0;

const isNumericDate = (value: unknown): value is number =>
  typeof value === "number" && Number.isFinite(value);

const validateHeader = (
  header: ProtectedHeaderParameters,
): TrustMarkValidationMessage | undefined => {
  if (header.typ !== "trust-mark+jwt") {
    return "trustMarkInvalidTypeHeader";
  }
  if (!isNonEmptyString(header.alg) || header.alg.toLowerCase() === "none") {
    return "trustMarkInvalidAlgorithm";
  }
  if (!isNonEmptyString(header.kid)) {
    return "trustMarkKeyIdRequired";
  }
  return undefined;
};

const decodeClaims = (
  payload: JWTPayload,
  expectedType: string,
): TrustMarkValidationResult => {
  const trustMarkType = payload.trust_mark_type;
  if (
    !isNonEmptyString(payload.iss) ||
    !isNonEmptyString(payload.sub) ||
    !isNonEmptyString(trustMarkType) ||
    payload.iat === undefined
  ) {
    return { error: "trustMarkRequiredClaims" };
  }
  if (
    !isNumericDate(payload.iat) ||
    (payload.exp !== undefined && !isNumericDate(payload.exp)) ||
    (payload.logo_uri !== undefined && typeof payload.logo_uri !== "string") ||
    (payload.ref !== undefined && typeof payload.ref !== "string") ||
    (payload.delegation !== undefined && typeof payload.delegation !== "string")
  ) {
    return { error: "trustMarkInvalidClaimTypes" };
  }
  const decoded: DecodedTrustMark = {
    issuer: payload.iss,
    subject: payload.sub,
    trustMarkType,
    issuedAt: payload.iat,
    expiresAt: payload.exp,
    logoUri: payload.logo_uri as string | undefined,
    ref: payload.ref as string | undefined,
    delegation: payload.delegation as string | undefined,
  };

  if (
    decoded.expiresAt !== undefined &&
    decoded.expiresAt < Date.now() / 1000
  ) {
    return { decoded, error: "trustMarkExpirationInPast" };
  }

  return trustMarkType === expectedType.trim()
    ? { decoded }
    : { decoded, error: "trustMarkTypeMismatch" };
};

export const decodeAndValidateTrustMark = (
  trustMarkJwt: string,
  expectedType: string,
): TrustMarkValidationResult => {
  const normalizedJwt = normalizeTrustMarkJwt(trustMarkJwt);
  const parts = normalizedJwt.split(".");
  if (parts.length !== 3) {
    return { error: "trustMarkInvalidJwt" };
  }
  if (parts.some((part) => part.length === 0)) {
    return parts[2]
      ? { error: "trustMarkInvalidJwt" }
      : { error: "trustMarkSignatureRequired" };
  }
  if (parts.some((part) => !/^[A-Za-z0-9_-]+$/.test(part))) {
    return { error: "trustMarkInvalidJwt" };
  }

  try {
    const headerError = validateHeader(decodeProtectedHeader(normalizedJwt));
    const claimsResult = decodeClaims(decodeJwt(normalizedJwt), expectedType);
    return headerError
      ? { decoded: claimsResult.decoded, error: headerError }
      : claimsResult;
  } catch {
    return { error: "trustMarkInvalidJwt" };
  }
};

const toNumericDate = (value: string) =>
  Math.floor(new Date(value).getTime() / 1000);

const optionalString = (value: string) => {
  const trimmed = value.trim();
  return trimmed || undefined;
};

export const toTrustMarkRequest = (
  values: TrustMarkFormValues,
  { issuedAt }: { issuedAt?: number } = {},
): TrustMarkRequest => {
  const trustMarkType = values.trustMarkType.trim();
  if (!values.selfSigned) {
    return {
      trustMarkType,
      selfSigned: false,
      jwt: normalizeTrustMarkJwt(values.jwt),
    };
  }

  return {
    trustMarkType,
    selfSigned: true,
    iat: issuedAt ?? toNumericDate(values.iat),
    ...(values.exp && { exp: toNumericDate(values.exp) }),
    ...(optionalString(values.logoUri) && {
      logoUri: optionalString(values.logoUri),
    }),
    ...(optionalString(values.ref) && { ref: optionalString(values.ref) }),
    ...(optionalString(values.delegation) && {
      delegation: optionalString(values.delegation),
    }),
  };
};

export const formatNumericDateForInput = (value?: number) => {
  if (value === undefined) {
    return "";
  }
  const date = new Date(value * 1000);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  const pad = (part: number) => part.toString().padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
    date.getDate(),
  )}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(
    date.getSeconds(),
  )}`;
};

export const toTrustMarkFormValues = (
  trustMark?: TrustMarkRepresentation,
): TrustMarkFormValues => ({
  trustMarkType: trustMark?.trustMarkType ?? "",
  selfSigned: trustMark?.selfSigned ?? false,
  jwt: trustMark?.jwt ?? "",
  iat: formatNumericDateForInput(trustMark?.iat),
  exp: formatNumericDateForInput(trustMark?.exp),
  logoUri: trustMark?.logoUri ?? "",
  ref: trustMark?.ref ?? "",
  delegation: trustMark?.delegation ?? "",
});
