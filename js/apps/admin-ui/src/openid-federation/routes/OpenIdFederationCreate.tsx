import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type OpenIdFedertationCreateParams = {
  realm: string;
};

const AddOpenIdFederation = lazy(() => import("../add/AddOpenIdFederation"));

export const OpenIdFedeationCreateRoute: AppRouteObject = {
  path: "/:realm/openid-federation/add",
  element: <AddOpenIdFederation />,
  breadcrumb: (t) => t("openid-federation:addOpenIdFederation"),
  handle: {
    access: "view-realm",
  },
};

export const toOpenIdFederationCreate = (
  params: OpenIdFedertationCreateParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(OpenIdFedeationCreateRoute.path, params),
});
