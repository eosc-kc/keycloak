import { lazy } from "react";
import type { Path } from "react-router-dom";

import type { AppRouteObject } from "../../routes";
import { generateEncodedPath } from "../../utils/generateEncodedPath";

export type TrustMarkEditParams = {
  realm: string;
  id: string;
};

const EditTrustMark = lazy(() => import("../trust-mark/EditTrustMark"));

export const TrustMarkEditRoute: AppRouteObject = {
  path: "/:realm/openid-federation/trust-marks/:id",
  element: <EditTrustMark />,
  breadcrumb: (t) => t("editTrustMark"),
  handle: {
    access: "view-realm",
  },
};

export const toTrustMarkEdit = (
  params: TrustMarkEditParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(TrustMarkEditRoute.path, params),
});
