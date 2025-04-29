# Changelog
All notable eosc-kc changes of Keycloak will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

For Keycloak upstream changelog please see https://www.keycloak.org/docs/latest/release_notes/index.html.
[Keycloak release note 22](https://www.keycloak.org/docs/latest/release_notes/index.html#keycloak-22-0-3)
Full Keycloak upstream jira issue can be shown if filtered by Fix version.

Our Keycloak version is working well with PostgreSQL database. For using other SQL databases, text field in database need to be evaluated.

## [Unreleased]

### Fixed
- Do not ask for mfa in first user login (with enabled mfa browser authentication)
- Send Identity Providers acr value to browser authentication flow
- Support value request parameter in the claims request

### Added
- Default acr value for Identity Providers login

### Changed
- Return error for any not configured acr

## [22.0.13-1.16] - 2025-04-11

### Fixed
- MFA signalled from SAML IdPs is handled

### Added
- Being possible to create unique user uid during Identity Broker Sign up

### Changed
- Process to SAML only LOAs equal or smaller than requested

## [22.0.13-1.15] - 2025-03-17

### Added
- Signalling of Multi-Factor Authentication (MFA) requests to OIDC and SAML (individual and federation) Identity Providers. Note: MFA signalled from SAML IdPs is not handled in this version.
- User Attribute Template Importer mapper

### Fixed
- Add authnAuthorities to login event when login is done via cookie

## [22.0.13-1.14] - 2025-02-13

### Changed
- Exclude service account users from renewing terms and conditions
- Add extra entity attribute on Hardcoded Claim Based on IdP Attribute mapper

### Fixed
- Fix bugs in hardcored protocol mappers
- Correct filtering multivalue user attribute in dynamic scope

## [22.0.13-1.13] - 2025-01-22

### Added
- Add Keycloak tag 22.0.13 [version 22.0.13](https://github.com/keycloak/keycloak/commits/22.0.13/)

### Fixed
-Fix bug when user without accepting terms and conditions, click sign up

### Changed
- Hide username configuration during first broker login

## [22.0.11-1.12] - 2025-01-10

### Fixed
- Handling of changed profile information during sign-up when review profile is set to "missing" or T&C is requested
- Add value to lastrefresh in saml federation only after SAML federation is finished adding all IdPs

## [22.0.11-1.11] - 2024-11-19

### Added
- Logo uri for IdPs
- Add cookie for chosen login IdPs

### Fixed
- Make IdPs selection more efficient

## [22.0.11-1.10] - 2024-10-21

### Changed
- SAML Federation process reexecute when no all IDPs are added

## [22.0.11-1.9] - 2024-10-10

### Fixed
- Small fixes and extra logs to saml federation parsing
- Link federated IdPs
- Messages for group events
- Add specific IdPs number during SAML Federation creation
- Fix allow/deny Entity Categories filtering for SAML federations
- Fix ui problems in SAML Federation

### Changed
- Remove indexes from federation and IdP related tables

## [22.0.11-1.8] - 2024-09-11

### Added
- Handling authenticating authority(ies) in user session
- Add user session name to ClaimToUserSessionNoteMapper
- Add Keycloak tag 22.0.11 [version 22.0.11](https://github.com/keycloak/keycloak/commits/22.0.11/)
- Refresh SAML federation and auto-updated IdP

### Changed
- Change condition for parsing NameIDPolicy in autoUpdated IdP
- Improve/ change update IdP REST API

### Fixed
- Fix bugs in AutoUpdated schedule task
- Correct query for autoUpdated IdP
- Do not change lastRefreshTime during IdP update
- Improvements and logs in SAML federation task execution

## [22.0.10-1.7] - 2024-07-10

### Changed
- Change user updater for federation IdPs

## [22.0.10-1.6] - 2024-07-03

### Fixed
- Check for offline refresh token grant based on refresh token request parameter
- Allow setting (Federation) IdP NameIDPolicy format to `null` 
- Hide SP Entity in Add Page

## [22.0.10-1.5] - 2024-06-13

### Added
- Refresh token flow may check oidc idp  refresh token valid

### Changed
- Hardcord Attribute Mapper supporting adding List of values
- Support for omitting attributeConsumingServiceIndex from authentication requests
- Linked Accounts Page changes

### Fixed
- Migrate 'spEntityId' to 'entityId' in SAML federation
- Correct SAML client signature parsing for SAML client
- Remove serviceProviderEntityId from metadata loader in SAML Identity Provider metadata loader

## [22.0.10-1.4] - 2024-05-31

### Added
- Add support for managing SSH keys from account console
- Add Token Introspection URL and Validate Refresh Tokens to OIDC IdP

### Fixed
- Correct Linkedin name and logo[RCIAM-1346](https://jira.argo.grnet.gr/browse/RCIAM-1346)
- Put uriInfo in error response
- Being possible to accept terms and conditions before User is saved in Keycloak during first broker login. Follow GDPR. [Keycloak-28714](https://github.com/keycloak/keycloak/issues/28714) 

### Changed
- Attribute Consuming Service: Update Input and Default Value
- Hide Identity Type in Linked Accounts Page

## [22.0.5-1.3] - 2024-04-18

### Added
- OIDC UserAttribute mapper strategy FORCE will not delete not existing values if pass scope is enabled and scope is not passed [Implement mapper strategy FORCE update only when claim is present](https://trello.com/c/ClFiAOgF/2543-implement-mapper-strategy-force-update-only-when-claim-is-present)
- Debug log for id token and user info of OIDC IdP [Investigate how to log OIDC login events incl user claims from OIDC IdP](https://trello.com/c/G7pWOVDa/2574-investigate-how-to-log-oidc-login-events-incl-user-claims-from-oidc-idp)

## [22.0.5-1.2] - 2024-04-03

For not throwing errors due to dublicate client_attribute entries you must execute the following delete statement before upgrading to this version :
_delete from client_attributes where name='client.offline.session.max.lifespan';_

### Added

- Allow forwarding OIDC scopes to upstream OIDC Identity Provider in Keycloak [Allow forwarding OIDC scopes to upstream OIDC Identity Provider in Keycloak](https://trello.com/c/9I5SeGN6/2470-allow-forwarding-oidc-scopes-to-upstream-oidc-identity-provider-in-keycloak)

### Fixed
- Fix configuration problems in generated Claim or attribute Mappers[Mapper for generating SAML attribute values or Claim values using other SAML attribute/Claim values as input](https://trello.com/c/8K46f2mo/1642-mapper-for-generating-saml-attribute-values-or-claim-values-using-other-saml-attribute-claim-values-as-input)
- Enchance SAML IdP Logout [Keycloak-19183](https://github.com/keycloak/keycloak/issues/19183)
- Change 'client.offline.session.max.timeout' to 'client.offline.session.max.lifespan' [Changes in federation registry and deployer for Keycloak staging version 22 and Clients migration](https://trello.com/c/Dk8ajZ2E/2214-changes-in-federation-registry-and-deployer-for-keycloak-staging-version-22-and-clients-migration)
- Group search case insensitive [RCIAM-1336](https://jira.argo.grnet.gr/browse/RCIAM-1336)

## [22.0.5-1.1] - 2024-02-27
- Use the target client when processing scopes for internal exchanges [Keycloak-23528](https://github.com/keycloak/keycloak/issues/23528)

### Added
- ID token lifetime option
- Eosc-kc version model with MigrationModel changes [RCIAM-945](https://jira.argo.grnet.gr/browse/RCIAM-945)
- Support for SAML Federation
- Identity Providers pager in Linked Accounts of Account Console. [EOSC-KC-50](https://github.com/eosc-kc/keycloak/issues/50)
- User reaccepting Terms and Conditions. [EOSC-KC-48](https://github.com/eosc-kc/keycloak/issues/48)
- Terms and Conditions - periodic reset for all realm users. [EOSC-KC-49](https://github.com/eosc-kc/keycloak/issues/49)
- Email notification for add/remove group. [EOSC-KC-75](https://github.com/eosc-kc/keycloak/issues/75)
- SAML/ OIDC Identity Provider AutoUpdate. [EOSC-KC-119](https://github.com/eosc-kc/keycloak/issues/119)
- External introspection endpoint [EOSC-KC-140](https://github.com/eosc-kc/keycloak/issues/140)
- New release created on tag
- The idpLoginFullUrl common attribute passed to the ftl templates for any theme except from the default
- Add scope parameter to token exchange [RCIAM-834](https://jira.argo.grnet.gr/browse/RCIAM-834)
- Hide scopes from scopes_supported in discovery endpoint [RCIAM-859](https://jira.argo.grnet.gr/browse/RCIAM-859)
- Id token lifespan [RCIAM-930](https://jira.argo.grnet.gr/browse/RCIAM-930)
- Add indexes to related to Federation and Identity Provider tables
- Resource request parameter and audience in access token [RCIAM-1061](https://jira.argo.grnet.gr/browse/RCIAM-1061)
- Autoupdated SAML Client [RCIAM-1181](https://jira.argo.grnet.gr/browse/RCIAM-1181)
- Client scope policy [RCIAM-1241](https://jira.argo.grnet.gr/browse/RCIAM-1241)
- Sort Identity Providers by guiOrder and displayname
- Add authnAuthority and voPersonID user attribute to event details
- Login events type for add, remove, suspend user from a group [RCIAM-1292](https://jira.argo.grnet.gr/browse/RCIAM-1292)
- Include claim in token introspection response only [RCIAM-742](https://jira.argo.grnet.gr/browse/RCIAM-742)
- Enhanced TokenIntrospection and UserInfo events and logs
- Configurable Claims for dynamic scopes, Filter dynamic scopes from access token scope [RCIAM-1190](https://jira.argo.grnet.gr/browse/RCIAM-1190)
- Database script for migration from version 18.0.1-2.17 to 22.0.5-1.1
- Mapper for generating SAML attribute values or Claim values using IdP alias or IdP entity attributes or User Attribute values [Mapper for generating SAML attribute values or Claim values using other SAML attribute/Claim values as input](https://trello.com/c/8K46f2mo/1642-mapper-for-generating-saml-attribute-values-or-claim-values-using-other-saml-attribute-claim-values-as-input)
- SAML IdP entity attributes [How to store IdP entity attributes in Keycloak](https://trello.com/c/wzF5s6Oi/2409-how-to-store-idp-entity-attributes-in-keycloak)
- List<String> fields in protocol mappers [Fix problem with List<String> fields in client scopes & attribute mapper configuration](https://trello.com/c/TrJyTo1B/2349-fix-problem-with-liststring-fields-in-client-scopes-attribute-mapper-configuration)

### Changed
- Increase User Attribute Value length to 4000 [EOSC-KC-132](https://github.com/eosc-kc/keycloak/issues/132)
- FreeMarkerLoginFormsProvider now has an additional common attribute passed to the ftl templates, the "uriInfo"
- Change emailVerified User field with UserAttributeMappers (conditional trust email). [EOSC-KC-70](https://github.com/eosc-kc/keycloak/issues/70)
- Offline_access scope return always refresh_token [RCIAM-744](https://jira.argo.grnet.gr/browse/RCIAM-744)
- Signing of SAML IdP logout requests separately [RCIAM-881](https://jira.argo.grnet.gr/browse/RCIAM-881)
- Allow omitting NameIDFormat [RCIAM-882](https://jira.argo.grnet.gr/browse/RCIAM-882)
- Record SAML login events based on SAML IdP entityID [EOSC-KC-134](https://github.com/eosc-kc/keycloak/issues/134)
- Add ePTID principal option [RCIAM-916](https://jira.argo.grnet.gr/browse/RCIAM-916)
- Add is required configuration option for UserAttributeMapper and AttributeToRoleMapper [RCIAM-861](https://jira.argo.grnet.gr/browse/RCIAM-861)
- Support for configuring claims supported in Keycloak OP metadata [RCIAM-899](https://jira.argo.grnet.gr/browse/RCIAM-899)
- Specific error page for no principals [RCIAM-766](https://jira.argo.grnet.gr/browse/RCIAM-766)
- Refresh token revoke per client and correct refresh flow [RCIAM-920](https://jira.argo.grnet.gr/browse/RCIAM-920)
- SAML entityID/OIDC issuer showing in user if IdP display name does not exist [RCIAM-887](https://jira.argo.grnet.gr/browse/RCIAM-887)
- User attribute value as text in database [RCIAM-1032](https://jira.argo.grnet.gr/browse/RCIAM-1032)
- Client description as text in database
- Client attribute value as text in database [RCIAM-1026)](https://jira.argo.grnet.gr/browse/RCIAM-1026)
- SAML IdP InResponseTo missing, warning instead of error
- Remove consent required from Token Exchange [RCIAM-1048](https://jira.argo.grnet.gr/browse/RCIAM-1048)
- Make optional the use of PKCE for Clients configured with PKCE only for Device Code Flow [RCIAM-1069](https://jira.argo.grnet.gr/browse/RCIAM-1069)
- No extra checks for confidential clients in token exchange [RCIAM-988](https://jira.argo.grnet.gr/browse/RCIAM-988)
- Consent extension [RCIAM-791](https://jira.argo.grnet.gr/browse/RCIAM-791)
- Search in User Attribute with like
- Protocol mapper that can combine multiple user attributes [RCIAM-1267](https://jira.argo.grnet.gr/browse/RCIAM-1267)
- Change SAML 2.0 Identity Provider Metadata [RCIAM-1288](https://jira.argo.grnet.gr/browse/RCIAM-1288)
- SAML federation alias use URL encoded of base64 entityId[Refactor SAML federation to use base64 instead of SHA256 for IdP aliases](https://trello.com/c/pDtqcm3L/2324-refactor-saml-federation-to-use-base64-instead-of-sha256-for-idp-aliases)

### Fixed
- Changes in account console and account rest service [RCIAM-860](https://jira.argo.grnet.gr/browse/RCIAM-860)
- Scope parameter in refresh flow [RCIAM-990](https://jira.argo.grnet.gr/browse/RCIAM-990)
- UserInfoEndpoint taking into account access token
- SAML element EncryptionMethod can consist any element [RCIAM-1014](https://jira.argo.grnet.gr/browse/RCIAM-1014)
- Continue client browser flow after User login from Identity Provider [RCIAM-1038](https://jira.argo.grnet.gr/browse/RCIAM-1038)
- Avoid duplicate entries for UseConsent with same userId - clientId [RCIAM-1141](https://jira.argo.grnet.gr/browse/RCIAM-1141)
- Client Signature Required true requires also AuthnRequestsSigned be true[Keycloak requires signed authN requests when WantAuthNSigned=false](https://trello.com/c/XpLOXiz2/2177-keycloak-requires-signed-authn-requests-when-wantauthnsignedfalse)
- Update client session based on client revocation policy [RCIAM-1178](https://jira.argo.grnet.gr/browse/RCIAM-1178)
- Refresh token scope same as old refresh token scope in refresh token flow [RCIAM-1158](https://jira.argo.grnet.gr/browse/RCIAM-1158)
- Update changes related to service account with Client registration 
- Dynamic scopes( default enabled): bug corrections, filtering and consent [RCIAM-848](https://jira.argo.grnet.gr/browse/RCIAM-848)
- Fix Token Exchange without scope parameter bug in scope 
- OIDC Protocol Mappers add value to existing claims instead of overwriting it[Keycloak-25774](https://github.com/keycloak/keycloak/issues/25774)
- Support federation metadata with nested EntitiesDescription[Support federation metadata with nested EntitiesDescription](https://trello.com/c/wRbquYl4/2395-support-federation-metadata-with-nested-entitiesdescription)

# Migration from eosc-kc version 18

In eosc-kc Keycloak version(version 22.0.5-x.x), SAML Federated Identity Providers Alias has been changed.
If you were in eosc version 18 (version 18.0.1-2.x) and you use SAML federation, after upgrating to this version you need to do the following steps :
1. run model/jpa/src/main/resources/sql/upgradeKeycloak22.sql
2. Restart Keycloak(for cache reloading)
