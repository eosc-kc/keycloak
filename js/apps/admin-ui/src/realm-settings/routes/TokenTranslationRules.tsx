import { lazy } from "react";
import type { Path } from "react-router-dom";
import type { AppRouteObject } from "../../routes";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { TokenTranslationRuleSubType } from "../../identity-providers/token-translation-rules/constants";

export type RealmTokenTranslationRulesParams = {
  realm: string;
  subType: Extract<TokenTranslationRuleSubType, "global" | "default">;
};

export type RealmTokenTranslationRuleParams =
  RealmTokenTranslationRulesParams & {
    id: string;
  };

const RealmSettingsSection = lazy(() => import("../RealmSettingsSection"));
const RealmTokenTranslationRuleForm = lazy(
  () => import("../token-translation-rules/RealmTokenTranslationRuleForm"),
);

export const RealmTokenTranslationRulesRoute: AppRouteObject = {
  path: "/:realm/realm-settings/token-translation-rules/:subType",
  element: <RealmSettingsSection />,
  breadcrumb: (t) => t("tokenTranslationRules"),
  handle: { access: "view-realm" },
};

export const AddRealmTokenTranslationRuleRoute: AppRouteObject = {
  path: "/:realm/realm-settings/token-translation-rules/:subType/create",
  element: <RealmTokenTranslationRuleForm />,
  breadcrumb: (t) => t("addTokenTranslationRule"),
  handle: { access: "view-realm" },
};

export const EditRealmTokenTranslationRuleRoute: AppRouteObject = {
  path: "/:realm/realm-settings/token-translation-rules/:subType/:id",
  element: <RealmTokenTranslationRuleForm />,
  breadcrumb: (t) => t("editTokenTranslationRule"),
  handle: { access: "view-realm" },
};

export const toRealmTokenTranslationRules = (
  params: RealmTokenTranslationRulesParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(RealmTokenTranslationRulesRoute.path, params),
});

export const toAddRealmTokenTranslationRule = (
  params: RealmTokenTranslationRulesParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(AddRealmTokenTranslationRuleRoute.path, params),
});

export const toEditRealmTokenTranslationRule = (
  params: RealmTokenTranslationRuleParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(
    EditRealmTokenTranslationRuleRoute.path,
    params,
  ),
});
