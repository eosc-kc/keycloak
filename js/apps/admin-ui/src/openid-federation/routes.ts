import type { AppRouteObject } from "../routes";
import { OpenIdFederationRoute } from "./routes/OpenIdFederation";
import { OpenIdFederationEditRoute } from "./routes/OpenIdFederationEdit";
import { OpenIdFedeationCreateRoute } from "./routes/OpenIdFederationCreate";
const routes: AppRouteObject[] = [
  OpenIdFederationRoute,
  OpenIdFederationEditRoute,
  OpenIdFedeationCreateRoute,
];

export default routes;
