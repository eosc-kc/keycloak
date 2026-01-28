export default interface OpenIdFederationRepresentation {
  trustAnchor: string;
  internalId?: string;
  idpConfiguration?: Record<string, any>;
}
