export default interface OpenIdFederationRepresentation {
  trustAnchor: string;
  clientRegistrationTypesSupported: ClientRegistrationTypesSupported[];
  entityTypes: EntityTypesSupported[];
  internalId?: string;
}

export type EntityTypesSupported = "OPENID_PROVIDER";
export type ClientRegistrationTypesSupported = "EXPLICIT";
