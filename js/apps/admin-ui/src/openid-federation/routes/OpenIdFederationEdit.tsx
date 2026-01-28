import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type OpenIdFederationTab = "settings" | "idp";

export type OpenIdFederationEditParams = {
  realm: string;
  id: string;
  tab: OpenIdFederationTab;
};

const EditOpenIdFederation = lazy(() => import("../add/EditOpenIdFederation"));

export const OpenIdFederationEditRoute: AppRouteObject = {
  path: "/:realm/openid-federation/:id/:tab",
  element: <EditOpenIdFederation />,
  breadcrumb: (t) => t("editOpenIdFederation"),
  handle: {
    access: "view-realm",
  },
};

export const toOpenIdFederationEdit = (
  params: OpenIdFederationEditParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(OpenIdFederationEditRoute.path, params),
});
