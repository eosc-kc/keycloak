// @vitest-environment node

import { describe, expect, it } from "vitest";
import { supportsTokenTranslationRules } from "./utils";

describe("supportsTokenTranslationRules", () => {
  it.each([
    { publicClient: true },
    { publicClient: false },
    { bearerOnly: true },
    { serviceAccountsEnabled: true },
    { authorizationServicesEnabled: true },
  ])("supports OpenID Connect clients with configuration %o", (config) => {
    expect(
      supportsTokenTranslationRules({
        protocol: "openid-connect",
        ...config,
      }),
    ).toBe(true);
  });

  it.each(["saml", "oid4vc", undefined])(
    "does not support clients using the %s protocol",
    (protocol) => {
      expect(supportsTokenTranslationRules({ protocol })).toBe(false);
    },
  );
});
