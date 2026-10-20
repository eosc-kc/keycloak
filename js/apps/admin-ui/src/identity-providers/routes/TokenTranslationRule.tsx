import { lazy } from "react";
import type { Path } from "react-router-dom";
import type { AppRouteObject } from "../../routes";
import { generateEncodedPath } from "../../utils/generateEncodedPath";

export type TokenTranslationRuleParams = {
  realm: string;
  providerId: string;
  alias: string;
};

export type EditTokenTranslationRuleParams = TokenTranslationRuleParams & {
  id: string;
};

const TokenTranslationRuleForm = lazy(
  () => import("../token-translation-rules/TokenTranslationRuleForm"),
);

export const AddTokenTranslationRuleRoute: AppRouteObject = {
  path: "/:realm/identity-providers/:providerId/:alias/token-translation-rules/create",
  element: <TokenTranslationRuleForm />,
  breadcrumb: (t) => t("addTokenTranslationRule"),
  handle: {
    access: "manage-identity-providers",
  },
};

export const EditTokenTranslationRuleRoute: AppRouteObject = {
  path: "/:realm/identity-providers/:providerId/:alias/token-translation-rules/:id",
  element: <TokenTranslationRuleForm />,
  breadcrumb: (t) => t("editTokenTranslationRule"),
  handle: {
    access: "view-identity-providers",
  },
};

export const toAddTokenTranslationRule = (
  params: TokenTranslationRuleParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(AddTokenTranslationRuleRoute.path, params),
});

export const toEditTokenTranslationRule = (
  params: EditTokenTranslationRuleParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(EditTokenTranslationRuleRoute.path, params),
});
