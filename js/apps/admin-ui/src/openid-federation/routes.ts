import type { AppRouteObject } from "../routes";
import { OpenIdFederationRoute } from "./routes/OpenIdFederation";
import { OpenIdFederationEditRoute } from "./routes/OpenIdFederationEdit";
import { OpenIdFedeationCreateRoute } from "./routes/OpenIdFederationCreate";
import { TrustMarkCreateRoute } from "./routes/TrustMarkCreate";
import { TrustMarkEditRoute } from "./routes/TrustMarkEdit";
const routes: AppRouteObject[] = [
  OpenIdFederationRoute,
  OpenIdFederationEditRoute,
  OpenIdFedeationCreateRoute,
  TrustMarkCreateRoute,
  TrustMarkEditRoute,
];

export default routes;
