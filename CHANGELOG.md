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

### Changed
- Change emailVerified User field with UserAttributeMappers (conditional trust email). [EOSC-KC-70](https://github.com/eosc-kc/keycloak/issues/70)
