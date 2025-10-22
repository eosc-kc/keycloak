import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityFederationTab = "settings" | "mappers";

export type IdentityFederationParams = {
  realm: string;
  providerId: string;
  internalId: string;
  tab: IdentityFederationTab;
};

const EditIdentityFederation = lazy(
  () => import("../add/EditIdentityFederation"),
);

export const IdentityFederationRoute: AppRouteObject = {
  path: "/:realm/identity-federations/:providerId/:internalId/:tab",
  element: <EditIdentityFederation />,
  breadcrumb: (t) => t("federationDetails"),
  handle: {
    access: "view-identity-providers",
  },
};

export const toIdentityFederation = (
  params: IdentityFederationParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityFederationRoute.path, params),
});
