export default interface TrustMarkRepresentation {
  id?: string;
  trust_mark_type: string;
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
      trust_mark_type: string;
      selfSigned: false;
      jwt: string;
    }
  | {
      trust_mark_type: string;
      selfSigned: true;
      iat: number;
      exp?: number;
      logoUri?: string;
      ref?: string;
      delegation?: string;
    };
