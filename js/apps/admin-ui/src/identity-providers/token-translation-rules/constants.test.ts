// @vitest-environment node

import { describe, expect, test } from "vitest";
import { tokenTranslationRuleOrder } from "./constants";

describe("tokenTranslationRuleOrder", () => {
  test("uses the first configured order value", () => {
    expect(tokenTranslationRuleOrder(["2", "9"])).toBe(2);
  });

  test("puts missing and invalid orders last", () => {
    expect(tokenTranslationRuleOrder()).toBe(Number.MAX_SAFE_INTEGER);
    expect(tokenTranslationRuleOrder("invalid")).toBe(Number.MAX_SAFE_INTEGER);
  });
});
