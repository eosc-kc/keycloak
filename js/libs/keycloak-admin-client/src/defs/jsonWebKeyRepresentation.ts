export default interface JsonWebKeyRepresentation {
  kid?: string;
  kty?: string;
  use?: string;
  alg?: string;
  key_ops?: string[];
  n?: string;
  e?: string;
  crv?: string;
  x?: string;
  y?: string;
  [property: string]: unknown;
}
