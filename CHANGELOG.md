# Changelog
All notable eosc-kc changes of Keycloak will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

This fork is based on Keycloak upstream version is 26.4.1 .
For Keycloak upstream changelog please see [Keycloak release note 26.4.0](https://www.keycloak.org/docs/latest/release_notes/index.html#keycloak-26-4-0) and latest minor and patch releases announcements.
Full Keycloak upstream jira issue can be shown if filtered by Fix version.

## [Unreleased]

### Added
- SAML Federation implementation
- Eosc-kc version model with MigrationModel changes [RCIAM-945](https://jira.argo.grnet.gr/browse/RCIAM-945)
- SAML/ OIDC IdP AutoUpdate
- External introspection endpoint [EOSC-KC-140](https://github.com/eosc-kc/keycloak/issues/140)
- Id token lifespan [RCIAM-930](https://jira.argo.grnet.gr/browse/RCIAM-930)
- Autoupdated SAML Client [RCIAM-1181](https://jira.argo.grnet.gr/browse/RCIAM-1181)
- Configurable Claims for dynamic scopes, Filter dynamic scopes from access token scope [RCIAM-1190](https://jira.argo.grnet.gr/browse/RCIAM-1190)
- Client scope policy [RCIAM-1241](https://jira.argo.grnet.gr/browse/RCIAM-1241)


### Changed
- Change emailVerified User field with UserAttributeMappers (conditional trust email). [EOSC-KC-70](https://github.com/eosc-kc/keycloak/issues/70)
- Support for configuring claims supported in Keycloak OP metadata [RCIAM-899](https://jira.argo.grnet.gr/browse/RCIAM-899)
- Add is required configuration option for UserAttributeMapper and AttributeToRoleMapper 
- Refresh token revoke per client and correct refresh flow [RCIAM-920](https://jira.argo.grnet.gr/browse/RCIAM-920)
- SAML entityID/OIDC issuer showing in user if IdP display name does not exist [RCIAM-887](https://jira.argo.grnet.gr/browse/RCIAM-887)
- Consent extension [RCIAM-791](https://jira.argo.grnet.gr/browse/RCIAM-791)
- Make optional the use of PKCE for Clients configured with PKCE only for Device Code Flow [RCIAM-1069](https://jira.argo.grnet.gr/browse/RCIAM-1069)


### Fixed
- Changes in account console and account rest service [RCIAM-860](https://jira.argo.grnet.gr/browse/RCIAM-860)
- Continue client browser flow after User login from Identity Provider [RCIAM-1038](https://jira.argo.grnet.gr/browse/RCIAM-1038)
- Client Signature Required true requires also AuthnRequestsSigned be true[Keycloak requires signed authN requests when WantAuthNSigned=false](https://trello.com/c/XpLOXiz2/2177-keycloak-requires-signed-authn-requests-when-wantauthnsignedfalse)
- Dynamic scopes( default enabled): bug corrections, filtering and consent [RCIAM-848](https://jira.argo.grnet.gr/browse/RCIAM-848)
