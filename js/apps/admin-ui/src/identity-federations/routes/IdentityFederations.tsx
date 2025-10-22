import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityFederationsParams = {
  realm: string;
};

const IdentityFederationsSection = lazy(
  () => import("../IdentityFederationsSection"),
);

export const IdentityFederationsRoute: AppRouteObject = {
  path: "/:realm/identity-federations",
  element: <IdentityFederationsSection />,
  breadcrumb: (t) => t("identityFederations"),
  handle: {
    access: "manage-identity-providers",
  },
};

export const toIdentityFederations = (
  params: IdentityFederationsParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityFederationsRoute.path, params),
});
