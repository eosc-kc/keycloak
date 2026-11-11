export default interface TrustMarkRepresentation {
  id?: string;
  trustMarkType: string;
  selfSigned: boolean;
  sub?: string;
  iss?: string;
  iat?: number;
  exp?: number;
  logoUri?: string;
  ref?: string;
  delegation?: string;
  jwt?: string;
}

export type TrustMarkRequest =
  | {
      trustMarkType: string;
      selfSigned: false;
      jwt: string;
    }
  | {
      trustMarkType: string;
      selfSigned: true;
      iat: number;
      exp?: number;
      logoUri?: string;
      ref?: string;
      delegation?: string;
    };
