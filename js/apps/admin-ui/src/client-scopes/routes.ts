import type { AppRouteObject } from "../routes";
import { EditPolicyRoute } from "./routes/EditPolicy";
import { AddPolicyRoute } from "./routes/AddPolicy";
import { ClientScopeRoute } from "./routes/ClientScope";
import { ClientScopesRoute } from "./routes/ClientScopes";
import { MapperRoute } from "./routes/Mapper";
import { NewClientScopeRoute } from "./routes/NewClientScope";

const routes: AppRouteObject[] = [
  NewClientScopeRoute,
  MapperRoute,
  ClientScopeRoute,
  ClientScopesRoute,
  AddPolicyRoute,
  EditPolicyRoute,
];

export default routes;
