import type { AppRouteObject } from "../routes";
import { IdentityFederationsRoute } from "./routes/IdentityFederations";
import { IdentityFederationCreateRoute } from "./routes/IdentityFederationCreate";
import { IdentityFederationRoute } from "./routes/IdentityFederation";
import { IdentityFederationAddMapperRoute } from "./routes/AddMapper";
import { IdentityFederationEditMapperRoute } from "./routes/EditMapper";

const routes: AppRouteObject[] = [
  IdentityFederationsRoute,
  IdentityFederationCreateRoute,
  IdentityFederationRoute,
  IdentityFederationAddMapperRoute,
  IdentityFederationEditMapperRoute,
];

export default routes;
