import { lazy } from "react";
import type { Path } from "react-router-dom";

import type { AppRouteObject } from "../../routes";
import { generateEncodedPath } from "../../utils/generateEncodedPath";

export type TrustMarkCreateParams = {
  realm: string;
};

const AddTrustMark = lazy(() => import("../trust-mark/AddTrustMark"));

export const TrustMarkCreateRoute: AppRouteObject = {
  path: "/:realm/openid-federation/trust-marks/add",
  element: <AddTrustMark />,
  breadcrumb: (t) => t("addTrustMark"),
  handle: {
    access: "view-realm",
  },
};

export const toTrustMarkCreate = (
  params: TrustMarkCreateParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(TrustMarkCreateRoute.path, params),
});
