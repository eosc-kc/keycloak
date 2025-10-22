import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityFederationAddMapperParams = {
  realm: string;
  providerId: string;
  internalId: string;
  tab: string;
};

const AddMapper = lazy(() => import("../add/AddMapper"));

export const IdentityFederationAddMapperRoute: AppRouteObject = {
  path: "/:realm/identity-federations/:providerId/:internalId/mappers/create",
  element: <AddMapper />,
  breadcrumb: (t) => t("addIdPMapper"),
  handle: {
    access: "manage-identity-providers",
  },
};

export const toIdentityFederationAddMapper = (
  params: IdentityFederationAddMapperParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityFederationAddMapperRoute.path, params),
});
