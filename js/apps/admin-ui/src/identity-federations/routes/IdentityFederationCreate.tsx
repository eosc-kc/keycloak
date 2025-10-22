import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityFederationCreateParams = {
  realm: string;
  tab?: string;
  providerId: string;
};

const AddIdentityFederation = lazy(
  () => import("../add/AddIdentityFederation"),
);

export const IdentityFederationCreateRoute: AppRouteObject = {
  path: "/:realm/identity-federations/:providerId/add/:tab",
  element: <AddIdentityFederation />,
  // element: <div>PLacyholder</div>,
  breadcrumb: (t) => t("addIdentityFederation"),
  handle: {
    access: "manage-identity-providers",
  },
};

export const toIdentityFederationCreate = (
  params: IdentityFederationCreateParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityFederationCreateRoute.path, params),
});
