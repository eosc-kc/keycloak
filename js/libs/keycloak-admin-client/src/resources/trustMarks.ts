import type { KeycloakAdminClient } from "../client.js";
import type TrustMarkRepresentation from "../defs/trustMarkRepresentation.js";
import type { TrustMarkRequest } from "../defs/trustMarkRepresentation.js";
import Resource from "./resource.js";

export class TrustMarks extends Resource<{ realm?: string }> {
  public find = this.makeRequest<{}, TrustMarkRepresentation[]>({
    method: "GET",
  });

  public findOne = this.makeRequest<
    { id: string },
    TrustMarkRepresentation | undefined
  >({
    method: "GET",
    path: "/{id}",
    urlParamKeys: ["id"],
    catchNotFound: true,
  });

  public create = this.makeRequest<TrustMarkRequest, { id: string }>({
    method: "POST",
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public update = this.makeUpdateRequest<
    { id: string },
    TrustMarkRequest,
    void
  >({
    method: "PUT",
    path: "/{id}",
    urlParamKeys: ["id"],
  });

  public del = this.makeRequest<{ id: string }, void>({
    method: "DELETE",
    path: "/{id}",
    urlParamKeys: ["id"],
  });

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/trust-marks",
      getUrlParams: () => ({ realm: client.realmName }),
      getBaseUrl: () => client.baseUrl,
    });
  }
}
