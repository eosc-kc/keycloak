# Changelog
All notable eosc-kc changes of Keycloak will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

This fork is based on Keycloak upstream version is 26.4.1 .
For Keycloak upstream changelog please see [Keycloak release note 26.5.0](https://www.keycloak.org/2026/01/keycloak-2650-released) and latest minor and patch releases announcements.
Full Keycloak upstream jira issue can be shown if filtered by Fix version.

Our Keycloak version is working well with PostgreSQL database. For using other SQL databases, text field in database need to be evaluated.

## [UNRELEASED]

### Added
- SCIM : Support type "Group" in "members" field for child groups

### Fixed
- Fix location of SCIM resources so IDs don't appear twice in the URL
- Fix introspection error for invalid client authentication
- IdP backchannel logout: read sid claim from ID token to support backchannel logout [#12142](https://github.com/keycloak/keycloak/issues/12142)
- Handle missing UserInfo token algorithm [#49969](https://github.com/keycloak/keycloak/issues/49969)

## [26.6.3-1.0] -2026-07-22

### Added
- SAML Federation implementation
- Eosc-kc version model with MigrationModel changes [RCIAM-945](https://jira.argo.grnet.gr/browse/RCIAM-945)
- SAML/ OIDC IdP AutoUpdate
- External introspection endpoint [EOSC-KC-140](https://github.com/eosc-kc/keycloak/issues/140)
- Id token lifespan [RCIAM-930](https://jira.argo.grnet.gr/browse/RCIAM-930)
- Autoupdated SAML Client [RCIAM-1181](https://jira.argo.grnet.gr/browse/RCIAM-1181)
- Configurable Claims for dynamic scopes, Filter dynamic scopes from access token scope [RCIAM-1190](https://jira.argo.grnet.gr/browse/RCIAM-1190)
- Client scope policy [RCIAM-1241](https://jira.argo.grnet.gr/browse/RCIAM-1241)
- Add authnAuthority and voPersonID user attribute to event details
- Login events type for add, remove, suspend user from a group [RCIAM-1292](https://jira.argo.grnet.gr/browse/RCIAM-1292)
- Enhanced TokenIntrospection and UserInfo events and logs
- Mapper for generating SAML attribute values or Claim values using IdP alias or IdP entity attributes or User Attribute values [Mapper for generating SAML attribute values or Claim values using other SAML attribute/Claim values as input](https://trello.com/c/8K46f2mo/1642-mapper-for-generating-saml-attribute-values-or-claim-values-using-other-saml-attribute-claim-values-as-input)
- SAML IdP entity attributes [How to store IdP entity attributes in Keycloak](https://trello.com/c/wzF5s6Oi/2409-how-to-store-idp-entity-attributes-in-keycloak)
- List<String> fields in protocol mappers [Fix problem with List<String> fields in client scopes & attribute mapper configuration](https://trello.com/c/TrJyTo1B/2349-fix-problem-with-liststring-fields-in-client-scopes-attribute-mapper-configuration)
- Allow forwarding OIDC scopes to upstream OIDC Identity Provider in Keycloak [Allow forwarding OIDC scopes to upstream OIDC Identity Provider in Keycloak](https://trello.com/c/9I5SeGN6/2470-allow-forwarding-oidc-scopes-to-upstream-oidc-identity-provider-in-keycloak)
- Add optional scopes for OIDC IdP and related changes
- OIDC UserAttribute mapper strategy FORCE will not delete not existing values if pass scope is enabled and scope is not passed [Implement mapper strategy FORCE update only when claim is present](https://trello.com/c/ClFiAOgF/2543-implement-mapper-strategy-force-update-only-when-claim-is-present)
- Handling authenticating authority(ies) in user session
- Add user session name to ClaimToUserSessionNoteMapper
- Refresh token flow may check oidc idp  refresh token valid
- Refresh SAML federation and auto-updated IdP
- Logo uri for IdPs
- Add cookie for chosen login IdPs
- Search without accents in Identity Providers
- Add authnAuthorities to login event when login is done via cookie
- User attribute unique validation [RCIAM-429](https://tts.grnet.gr/jira/browse/RCIAM-429)
- Add no editable Attribute Validator [RCIAM-429](https://tts.grnet.gr/jira/browse/RCIAM-429)
- Signalling Multi-Factor Authentication (MFA) requests to OIDC and SAML Identity Providers 
- Extend Proxied Token Introspection to support fallback endpoints
- Default acr value for Identity Providers login
- OpenID Federation OP with explicit registration
- OpenID Federation RP with explicit registration
- OpenID Federation OP with automatic registration 
- Metadata Policies (experimental feature)
- Add option Grant Valid Scope Subset to downscope-assertion-grant-enforcer [RCIAM-349](https://tts.grnet.gr/jira/browse/RCIAM-349)
- User reaccepting Terms and Conditions. [EOSC-KC-48](https://github.com/eosc-kc/keycloak/issues/48)
- Terms and Conditions - periodic reset for all realm users. [EOSC-KC-49](https://github.com/eosc-kc/keycloak/issues/49)
- Debug logs for OTP request and success
- iShare implementation
- Support for custom user scim extension

### Changed
- Change emailVerified User field with UserAttributeMappers (conditional trust email). [EOSC-KC-70](https://github.com/eosc-kc/keycloak/issues/70)
- Support for configuring claims supported in Keycloak[services](services%2Fsrc%2Fmain%2Fresources%2FMETA-INF%2Fservices) OP metadata [RCIAM-899](https://jira.argo.grnet.gr/browse/RCIAM-899)
- Add is required configuration option for UserAttributeMapper and AttributeToRoleMapper 
- Refresh token revoke per client and correct refresh flow [RCIAM-920](https://jira.argo.grnet.gr/browse/RCIAM-920)
- SAML entityID/OIDC issuer showing in user if IdP display name does not exist [RCIAM-887](https://jira.argo.grnet.gr/browse/RCIAM-887)
- Consent extension [RCIAM-791](https://jira.argo.grnet.gr/browse/RCIAM-791)
- Make optional the use of PKCE for Clients configured with PKCE only for Device Code Flow [RCIAM-1069](https://jira.argo.grnet.gr/browse/RCIAM-1069)
- Protocol mapper that can combine multiple user attributes [RCIAM-1267](https://jira.argo.grnet.gr/browse/RCIAM-1267)
- LinkedAccountRepresentation consists linkedUserID
- Support for omitting attributeConsumingServiceIndex from authentication requests
- Every user can update his profile in account console based on User profile user managed attributes
- User attribute value as text in database [RCIAM-1032](https://jira.argo.grnet.gr/browse/RCIAM-1032)
- Client description as text in database
- Client attribute value as text in database [RCIAM-1026)](https://jira.argo.grnet.gr/browse/RCIAM-1026)
- Group attribute value as text in database
- Support MFA with Identity Providers
- Return refresh token when configured or offline_access scope exists
- Do not check for consent in Token Exchange
- Add audience request parameters in audience token of Token Exchange
- Do not reject if the requester-client is not in the audience of the subject token
- New federated user set emailVerified based on 'Trust Email' IdP field and User Attribute Mapper
- Support for multiple resource indicators
- Add option for not checking resource indicator based on token audience
- Being possible to overide resource in authorization code and refresh flow

### Fixed
- Changes in account console and account rest service [RCIAM-860](https://jira.argo.grnet.gr/browse/RCIAM-860)
- Continue client browser flow after User login from Identity Provider [RCIAM-1038](https://jira.argo.grnet.gr/browse/RCIAM-1038)
- Client Signature Required true requires also AuthnRequestsSigned be true[Keycloak requires signed authN requests when WantAuthNSigned=false](https://trello.com/c/XpLOXiz2/2177-keycloak-requires-signed-authn-requests-when-wantauthnsignedfalse)
- Dynamic scopes( default enabled): bug corrections, filtering and consent [RCIAM-848](https://jira.argo.grnet.gr/browse/RCIAM-848)
- Token Exchange work with dynamic scopes enabled
- Update changes related to service account with Client registration 
- Being possible to accept terms and conditions before User is saved in Keycloak during first broker login. Follow GDPR.
- Being possible to add realm default scopes during Dynamic Client Registration/ OpenID Federation when scopes are including in client representation

## Migration notes

- **Only when upgrading from `22.0.13-1.25`:**

```sql
UPDATE DATABASECHANGELOG
SET MD5SUM = NULL
WHERE ID IN ('federation-indexes', 'federation-indexes-2', 'text-values');
