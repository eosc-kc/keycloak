import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";
export type OpenIdFederationTab = "settings" | "idp";

export type OpenIdFedertationCreateParams = {
  realm: string;
  tab: OpenIdFederationTab;
};

const AddOpenIdFederation = lazy(() => import("../add/AddOpenIdFederation"));

export const OpenIdFedeationCreateRoute: AppRouteObject = {
  path: "/:realm/openid-federation/add/:tab",
  element: <AddOpenIdFederation />,
  breadcrumb: (t) => t("addOpenIdFederation"),
  handle: {
    access: "view-realm",
  },
};

export const toOpenIdFederationCreate = (
  params: OpenIdFedertationCreateParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(OpenIdFedeationCreateRoute.path, params),
});
