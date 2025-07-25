import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderOpenIdFederationParams = { realm: string };

const AddOpenIdFederation = lazy(() => import("../add/AddOpenIdFederation"));

export const IdentityProviderOpendIdFederationRoute: AppRouteObject = {
  path: "/:realm/identity-providers/openid-federation/add",
  element: <AddOpenIdFederation />,
  breadcrumb: (t) => t("identity-providers:addOpenIdFederationProvider"),
  handle: {
    access: "manage-identity-providers",
  },
};

export const toIdentityProviderOpenIdFederation = (
  params: IdentityProviderOpenIdFederationParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(
    IdentityProviderOpendIdFederationRoute.path,
    params,
  ),
});
