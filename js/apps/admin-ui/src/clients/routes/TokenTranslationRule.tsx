import { lazy } from "react";
import type { Path } from "react-router-dom";
import type { AppRouteObject } from "../../routes";
import { generateEncodedPath } from "../../utils/generateEncodedPath";

export type ClientTokenTranslationRuleParams = {
  realm: string;
  clientId: string;
};

export type EditClientTokenTranslationRuleParams =
  ClientTokenTranslationRuleParams & {
    id: string;
  };

const ClientTokenTranslationRuleForm = lazy(
  () => import("../token-translation-rules/ClientTokenTranslationRuleForm"),
);

export const AddClientTokenTranslationRuleRoute: AppRouteObject = {
  path: "/:realm/clients/:clientId/token-translation-rules/create",
  element: <ClientTokenTranslationRuleForm />,
  breadcrumb: (t) => t("addTokenTranslationRule"),
  handle: { access: "view-clients" },
};

export const EditClientTokenTranslationRuleRoute: AppRouteObject = {
  path: "/:realm/clients/:clientId/token-translation-rules/:id",
  element: <ClientTokenTranslationRuleForm />,
  breadcrumb: (t) => t("editTokenTranslationRule"),
  handle: { access: "view-clients" },
};

export const toAddClientTokenTranslationRule = (
  params: ClientTokenTranslationRuleParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(
    AddClientTokenTranslationRuleRoute.path,
    params,
  ),
});

export const toEditClientTokenTranslationRule = (
  params: EditClientTokenTranslationRuleParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(
    EditClientTokenTranslationRuleRoute.path,
    params,
  ),
});
