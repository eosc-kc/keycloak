import type { KeycloakAdminClient } from "../client.js";
import Resource from "./resource.js";
import OpenIdFederationRepresentation from "../defs/OpenIdFederationRepresentation.js";

export class OpenIdFederations extends Resource<{ realm?: string }> {
  /**
   * Identity Federation
   * https://www.keycloak.org/docs-api/11.0/rest-api/#_identity_providers_resource
   */

  public find = this.makeRequest<any>({
    method: "GET",
  });

  public findOne = this.makeRequest<
    { internalId: string },
    OpenIdFederationRepresentation | undefined
  >({
    method: "GET",
    path: "/{internalId}",
    urlParamKeys: ["internalId"],
    catchNotFound: true,
  });

  public update = this.makeUpdateRequest<
    { internalId: string },
    OpenIdFederationRepresentation,
    void
  >({
    method: "PUT",
    path: "/{internalId}",
    urlParamKeys: ["internalId"],
  });

  public create = this.makeRequest<
    OpenIdFederationRepresentation,
    { id: string }
  >({
    method: "POST",
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public del = this.makeRequest<{ internalId: string }, void>({
    method: "DELETE",
    path: "/{internalId}",
    urlParamKeys: ["internalId"],
  });

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/openid-federations",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }
}
