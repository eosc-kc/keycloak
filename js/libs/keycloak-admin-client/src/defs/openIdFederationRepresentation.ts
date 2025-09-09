export default interface OpenIdFederationRepresentation {
  trustAnchor: string;
  clientRegistrationTypesSupported: ClientRegistrationTypesSupported[];
  entityTypes: EntityTypesSupported[];
  internalId?: string;
  idpConfiguration?: Record<string, any>;
}

export type EntityTypesSupported = "OPENID_PROVIDER" | "OPENID_RELYING_PARTY";
export type ClientRegistrationTypesSupported = "EXPLICIT";
