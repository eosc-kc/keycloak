export default interface OpenIdFederationRepresentation {
  trustAnchor: string;
  clientRegistrationTypesSupported: ClientRegistrationTypesSupported[];
  entityTypes: EntityTypesSupported[];
  internalId?: string;
}

export type EntityTypesSupported = "OPENID_PROVIDER" | "OPENID_RELYING_PARTY";
export type ClientRegistrationTypesSupported = "EXPLICIT";
