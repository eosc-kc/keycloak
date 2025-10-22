import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityFederationEditMapperParams = {
  realm: string;
  providerId: string;
  internalId: string;
  id: string;
};

const AddMapper = lazy(() => import("../add/AddMapper"));

export const IdentityFederationEditMapperRoute: AppRouteObject = {
  path: "/:realm/identity-federations/:providerId/:internalId/mappers/:id",
  element: <AddMapper />,
  breadcrumb: (t) => t("editIdPMapper"),
  handle: {
    access: "manage-identity-providers",
  },
};

export const toIdentityFederationEditMapper = (
  params: IdentityFederationEditMapperParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityFederationEditMapperRoute.path, params),
});
