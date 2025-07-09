import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type OpenIdFederationTab = "general";

export type OpenIdFederationParams = {
  realm: string;
  tab?: OpenIdFederationTab;
};

const OpenIdFederationSection = lazy(
  () => import("../OpenIdFederationSection"),
);

export const OpenIdFederationRoute: AppRouteObject = {
  path: "/:realm/openid-federation",
  element: <OpenIdFederationSection />,
  breadcrumb: (t) => t("openid-federation:title"),
  handle: {
    access: "view-realm",
  },
};

export const toOpenIdFederation = (
  params: OpenIdFederationParams,
): Partial<Path> => {
  return {
    pathname: generateEncodedPath(OpenIdFederationRoute.path, params),
  };
};
