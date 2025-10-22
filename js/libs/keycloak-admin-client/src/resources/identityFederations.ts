import type { KeycloakAdminClient } from "../client.js";
import type IdentityProviderMapperRepresentation from "../defs/identityProviderMapperRepresentation.js";
import type IdentityFederationRepresentation from "../defs/identityFederationRepresentation.js";
import type { IdentityProviderMapperTypeRepresentation } from "../defs/identityProviderMapperTypeRepresentation.js";
import Resource from "./resource.js";

export interface PaginatedQuery {
  first?: number;
  max?: number;
}

export interface IdentityFederationsQuery extends PaginatedQuery {
  search?: string;
}

export class IdentityFederations extends Resource<{ realm?: string }> {
  /**
   * Identity Federation
   * https://www.keycloak.org/docs-api/11.0/rest-api/#_identity_providers_resource
   */

  public find = this.makeRequest<any>({
    method: "GET",
    path: "/instances",
  });

  public findOne = this.makeRequest<
    { internalId: string },
    IdentityFederationRepresentation | undefined
  >({
    method: "GET",
    path: "/instances/{internalId}",
    urlParamKeys: ["internalId"],
    catchNotFound: true,
  });

  public update = this.makeUpdateRequest<
    { internalId: string },
    IdentityFederationRepresentation,
    void
  >({
    method: "PUT",
    path: "/instances/{internalId}",
    urlParamKeys: ["internalId"],
  });

  public create = this.makeRequest<
    IdentityFederationRepresentation,
    { id: string }
  >({
    method: "POST",
    path: "/instances",
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public del = this.makeRequest<{ id: string }, void>({
    method: "DELETE",
    path: "/instances/{id}",
    urlParamKeys: ["id"],
  });

  public refresh = this.makeRequest<{ id: string }, void>({
    method: "POST",
    path: "/instances/{id}/refresh",
    urlParamKeys: ["id"],
  });

  constructor(client: KeycloakAdminClient) {
    super(client, {
      path: "/admin/realms/{realm}/saml-federations",
      getUrlParams: () => ({
        realm: client.realmName,
      }),
      getBaseUrl: () => client.baseUrl,
    });
  }

  public findMapperTypes = this.makeRequest<
    Record<string, IdentityProviderMapperTypeRepresentation>
  >({
    method: "GET",
    path: "/mapper-types",
  });

  public findOneMapper = this.makeRequest<
    { internalId: string; id: string },
    IdentityProviderMapperRepresentation | undefined
  >({
    method: "GET",
    path: "/instances/{internalId}/mappers/{id}",
    urlParamKeys: ["internalId", "id"],
    catchNotFound: true,
  });

  public findMappers = this.makeRequest<
    { internalId: string },
    IdentityProviderMapperRepresentation[]
  >({
    method: "GET",
    path: "/instances/{internalId}/mappers",
    urlParamKeys: ["internalId"],
  });

  public createMapper = this.makeRequest<
    {
      federationId: string;
      identityProviderMapper: IdentityProviderMapperRepresentation;
    },
    { id: string }
  >({
    method: "POST",
    path: "/instances/{federationId}/mappers",
    urlParamKeys: ["federationId"],
    payloadKey: "identityProviderMapper",
    returnResourceIdInLocationHeader: { field: "id" },
  });

  public updateMapper = this.makeUpdateRequest<
    { internalId: string; id: string },
    IdentityProviderMapperRepresentation,
    void
  >({
    method: "PUT",
    path: "/instances/{internalId}/mappers/{id}",
    urlParamKeys: ["internalId", "id"],
  });

  public delMapper = this.makeRequest<{ internalId: string; id: string }, void>(
    {
      method: "DELETE",
      path: "/instances/{internalId}/mappers/{id}",
      urlParamKeys: ["internalId", "id"],
    },
  );

  public updateFederationMapper = this.makeRequest<{
    internalId: string;
    mapperId: string;
    action: string;
  }>({
    method: "POST",
    path: "/instances/{internalId}/mappers/{mapperId}/idp/{action}",
    urlParamKeys: ["internalId", "mapperId", "action"],
  });
}
