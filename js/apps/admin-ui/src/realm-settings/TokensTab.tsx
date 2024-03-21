import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  NumberInput,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  Text,
  TextVariants,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../components/form/FormAccess";
import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import { FormPanel } from "../components/scroll-form/FormPanel";
import {
  TimeSelector,
  toHumanFormat,
} from "../components/time-selector/TimeSelector";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { useWhoAmI } from "../context/whoami/WhoAmI";
import { convertToFormValues, sortProviders } from "../util";

import "./realm-settings-section.css";

type RealmSettingsSessionsTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
  reset?: () => void;
};

export const RealmSettingsTokensTab = ({
  realm,
  reset,
  save,
}: RealmSettingsSessionsTabProps) => {
  const { t } = useTranslation("realm-settings");
  const serverInfo = useServerInfo();
  const { whoAmI } = useWhoAmI();

  const [defaultSigAlgDrpdwnIsOpen, setDefaultSigAlgDrpdwnOpen] =
    useState(false);

  const defaultSigAlgOptions = sortProviders(
    serverInfo.providers!["signature"].providers,
  );

  const form = useForm<RealmRepresentation>();
  const { setValue, control } = form;

  const offlineSessionMaxEnabled = useWatch({
    control,
    name: "offlineSessionMaxLifespanEnabled",
    defaultValue: realm.offlineSessionMaxLifespanEnabled,
  });

  const ssoSessionIdleTimeout = useWatch({
    control,
    name: "ssoSessionIdleTimeout",
    defaultValue: 36000,
  });

  const revokeRefreshToken = useWatch({
    control,
    name: "revokeRefreshToken",
    defaultValue: false,
  });

  useEffect(() => {
    convertToFormValues(realm, setValue);
  }, []);

  return (
    <PageSection variant="light">
      <FormPanel
        title={t("realm-settings:general")}
        className="kc-sso-session-template"
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          onSubmit={form.handleSubmit(save)}
        >
          <FormGroup
            label={t("defaultSigAlg")}
            fieldId="kc-default-signature-algorithm"
            labelIcon={
              <HelpItem
                helpText={t("realm-settings-help:defaultSigAlg")}
                fieldLabelId="realm-settings:algorithm"
              />
            }
          >
            <Controller
              name="defaultSignatureAlgorithm"
              defaultValue={"RS256"}
              control={form.control}
              render={({ field }) => (
                <Select
                  toggleId="kc-default-sig-alg"
                  onToggle={() =>
                    setDefaultSigAlgDrpdwnOpen(!defaultSigAlgDrpdwnIsOpen)
                  }
                  onSelect={(_, value) => {
                    field.onChange(value.toString());
                    setDefaultSigAlgDrpdwnOpen(false);
                  }}
                  selections={[field.value?.toString()]}
                  variant={SelectVariant.single}
                  aria-label={t("defaultSigAlg")}
                  isOpen={defaultSigAlgDrpdwnIsOpen}
                  data-testid="select-default-sig-alg"
                >
                  {defaultSigAlgOptions!.map((p, idx) => (
                    <SelectOption
                      selected={p === field.value}
                      key={`default-sig-alg-${idx}`}
                      value={p}
                    ></SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>

          <FormGroup
            label={t("oAuthDeviceCodeLifespan")}
            fieldId="oAuthDeviceCodeLifespan"
            labelIcon={
              <HelpItem
                helpText={t("realm-settings-help:oAuthDeviceCodeLifespan")}
                fieldLabelId="realm-settings:oAuthDeviceCodeLifespan"
              />
            }
          >
            <Controller
              name="oauth2DeviceCodeLifespan"
              defaultValue={0}
              control={form.control}
              render={({ field }) => (
                <TimeSelector
                  id="oAuthDeviceCodeLifespan"
                  data-testid="oAuthDeviceCodeLifespan"
                  value={field.value || 0}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("oAuthDevicePollingInterval")}
            fieldId="oAuthDevicePollingInterval"
            labelIcon={
              <HelpItem
                helpText={t("realm-settings-help:oAuthDevicePollingInterval")}
                fieldLabelId="realm-settings:oAuthDevicePollingInterval"
              />
            }
          >
            <Controller
              name="oauth2DevicePollingInterval"
              defaultValue={0}
              control={form.control}
              render={({ field }) => (
                <NumberInput
                  id="oAuthDevicePollingInterval"
                  value={field.value}
                  min={0}
                  onPlus={() => field.onChange(field.value || 0 + 1)}
                  onMinus={() => field.onChange(field.value || 0 - 1)}
                  onChange={(event) => {
                    const newValue = Number(event.currentTarget.value);
                    field.onChange(!isNaN(newValue) ? newValue : 0);
                  }}
                  placeholder={t("oAuthDevicePollingInterval")}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("shortVerificationUri")}
            fieldId="shortVerificationUri"
            labelIcon={
              <HelpItem
                helpText={t("realm-settings-help:shortVerificationUriTooltip")}
                fieldLabelId="realm-settings:shortVerificationUri"
              />
            }
          >
            <KeycloakTextInput
              id="shortVerificationUri"
              placeholder={t("shortVerificationUri")}
              {...form.register("attributes.shortVerificationUri")}
            />
          </FormGroup>
        </FormAccess>
        <FormGroup
          label={t("idTokenLifespan")}
          fieldId="idTokenLifespan"
          labelIcon={
            <HelpItem
              helpText={t("realm-settings-help:idTokenLifespan")}
              fieldLabelId="realm-settings:idTokenLifespan"
            />
          }
        >
          <Controller
            name="idTokenLifespan"
            control={form.control}
            render={({ field }) => (
              <TimeSelector
                className="kc-id-token-lifespan"
                data-testid="id-token-lifespan-input"
                aria-label="id-token-lifespan"
                value={field.value!}
                onChange={field.onChange}
                units={["minute", "hour", "day"]}
              />
            )}
          />
        </FormGroup>
      </FormPanel>
      <FormPanel
        title={t("realm-settings:refreshTokens")}
        className="kc-client-session-template"
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={form.handleSubmit(save)}
        >
          <FormGroup
            hasNoPaddingTop
            label={t("revokeRefreshToken")}
            fieldId="kc-revoke-refresh-token"
            labelIcon={
              <HelpItem
                helpText={t("realm-settings-help:revokeRefreshToken")}
                fieldLabelId="realm-settings:revokeRefreshToken"
              />
            }
          >
            <Controller
              name="revokeRefreshToken"
              control={form.control}
              defaultValue={false}
              render={({ field }) => (
                <Switch
                  id="kc-revoke-refresh-token"
                  data-testid="revoke-refresh-token-switch"
                  aria-label={t("revokeRefreshToken")}
                  label={t("common:enabled")}
                  labelOff={t("common:disabled")}
                  isChecked={field.value}
                  onChange={field.onChange}
                />
              )}
            />
          </FormGroup>
          {revokeRefreshToken && (
            <FormGroup
              label={t("refreshTokenMaxReuse")}
              labelIcon={
                <HelpItem
                  helpText={t("realm-settings-help:refreshTokenMaxReuse")}
                  fieldLabelId="realm-settings:refreshTokenMaxReuse"
                />
              }
              fieldId="refreshTokenMaxReuse"
            >
              <Controller
                name="refreshTokenMaxReuse"
                defaultValue={0}
                control={form.control}
                render={({ field }) => (
                  <NumberInput
                    type="text"
                    id="refreshTokenMaxReuseMs"
                    value={field.value}
                    onPlus={() => field.onChange(field.value! + 1)}
                    onMinus={() => field.onChange(field.value! - 1)}
                    onChange={(event) =>
                      field.onChange(
                        Number((event.target as HTMLInputElement).value),
                      )
                    }
                  />
                )}
              />
            </FormGroup>
          )}
        </FormAccess>
      </FormPanel>
      <FormPanel
        title={t("realm-settings:accessTokens")}
        className="kc-offline-session-template"
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={form.handleSubmit(save)}
        >
          <FormGroup
            label={t("accessTokenLifespan")}
            fieldId="accessTokenLifespan"
            helperText={t("recommendedSsoTimeout", {
              time: toHumanFormat(ssoSessionIdleTimeout!, whoAmI.getLocale()),
            })}
            labelIcon={
              <HelpItem
                helpText={t("realm-settings-help:accessTokenLifespan")}
                fieldLabelId="realm-settings:accessTokenLifespan"
              />
            }
          >
            <Controller
              name="accessTokenLifespan"
              control={form.control}
              render={({ field }) => (
                <TimeSelector
                  validated={
                    field.value! > ssoSessionIdleTimeout!
                      ? "warning"
                      : "default"
                  }
                  className="kc-access-token-lifespan"
                  data-testid="access-token-lifespan-input"
                  aria-label="access-token-lifespan"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>

          <FormGroup
            label={t("accessTokenLifespanImplicitFlow")}
            fieldId="accessTokenLifespanImplicitFlow"
            labelIcon={
              <HelpItem
                helpText={t(
                  "realm-settings-help:accessTokenLifespanImplicitFlow",
                )}
                fieldLabelId="realm-settings:accessTokenLifespanImplicitFlow"
              />
            }
          >
            <Controller
              name="accessTokenLifespanForImplicitFlow"
              control={form.control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-access-token-lifespan-implicit"
                  data-testid="access-token-lifespan-implicit-input"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("clientLoginTimeout")}
            fieldId="clientLoginTimeout"
            labelIcon={
              <HelpItem
                helpText={t("realm-settings-help:clientLoginTimeout")}
                fieldLabelId="realm-settings:clientLoginTimeout"
              />
            }
          >
            <Controller
              name="accessCodeLifespan"
              control={form.control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-client-login-timeout"
                  data-testid="client-login-timeout-input"
                  aria-label="client-login-timeout"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>

          {offlineSessionMaxEnabled && (
            <FormGroup
              label={t("offlineSessionMax")}
              fieldId="offlineSessionMax"
              id="offline-session-max-label"
              labelIcon={
                <HelpItem
                  helpText={t("realm-settings-help:offlineSessionMax")}
                  fieldLabelId="realm-settings:offlineSessionMax"
                />
              }
            >
              <Controller
                name="offlineSessionMaxLifespan"
                control={form.control}
                render={({ field }) => (
                  <TimeSelector
                    className="kc-offline-session-max"
                    data-testid="offline-session-max-input"
                    value={field.value!}
                    onChange={field.onChange}
                    units={["minute", "hour", "day"]}
                  />
                )}
              />
            </FormGroup>
          )}
          <FormGroup
            label={t("defaultAudValueAccessToken")}
            fieldId="defaultAudValueAccessToken"
            labelIcon={
              <HelpItem
                helpText={t("realm-settings-help:defaultAudValueAccessToken")}
                fieldLabelId="realm-settings:defaultAudValueAccessToken"
              />
            }
          >
            <KeycloakTextInput
              id="defaultAudValueAccessToken"
              {...form.register("attributes.defaultAudValueForAccessToken")}
            />
          </FormGroup>
        </FormAccess>
      </FormPanel>
      <FormPanel
        className="kc-login-settings-template"
        title={t("actionTokens")}
      >
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={form.handleSubmit(save)}
        >
          <FormGroup
            label={t("userInitiatedActionLifespan")}
            id="kc-user-initiated-action-lifespan"
            fieldId="userInitiatedActionLifespan"
            labelIcon={
              <HelpItem
                helpText={t("realm-settings-help:userInitiatedActionLifespan")}
                fieldLabelId="realm-settings:userInitiatedActionLifespan"
              />
            }
          >
            <Controller
              name="actionTokenGeneratedByUserLifespan"
              control={form.control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-user-initiated-action-lifespan"
                  data-testid="user-initiated-action-lifespan"
                  aria-label="user-initiated-action-lifespan"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("defaultAdminInitiated")}
            fieldId="defaultAdminInitiated"
            id="default-admin-initiated-label"
            labelIcon={
              <HelpItem
                helpText={t(
                  "realm-settings-help:defaultAdminInitiatedActionLifespan",
                )}
                fieldLabelId="realm-settings:defaultAdminInitiated"
              />
            }
          >
            <Controller
              name="actionTokenGeneratedByAdminLifespan"
              control={form.control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-default-admin-initiated"
                  data-testid="default-admin-initated-input"
                  aria-label="default-admin-initated-input"
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <Text
            className="kc-override-action-tokens-subtitle"
            component={TextVariants.h1}
          >
            {t("overrideActionTokens")}
          </Text>
          <FormGroup
            label={t("emailVerification")}
            fieldId="emailVerification"
            id="email-verification"
          >
            <Controller
              name="attributes.actionTokenGeneratedByUserLifespan-verify-email"
              defaultValue=""
              control={form.control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-email-verification"
                  data-testid="email-verification-input"
                  value={field.value}
                  onChange={(value) => field.onChange(value.toString())}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("idpAccountEmailVerification")}
            fieldId="idpAccountEmailVerification"
            id="idp-acct-label"
          >
            <Controller
              name="attributes.actionTokenGeneratedByUserLifespan-idp-verify-account-via-email"
              defaultValue={""}
              control={form.control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-idp-email-verification"
                  data-testid="idp-email-verification-input"
                  value={field.value}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("forgotPassword")}
            fieldId="forgotPassword"
            id="forgot-password-label"
          >
            <Controller
              name="attributes.actionTokenGeneratedByUserLifespan-reset-credentials"
              defaultValue={""}
              control={form.control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-forgot-pw"
                  data-testid="forgot-pw-input"
                  value={field.value}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("executeActions")}
            fieldId="executeActions"
            id="execute-actions"
          >
            <Controller
              name="attributes.actionTokenGeneratedByUserLifespan-execute-actions"
              defaultValue={""}
              control={form.control}
              render={({ field }) => (
                <TimeSelector
                  className="kc-execute-actions"
                  data-testid="execute-actions-input"
                  value={field.value}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                />
              )}
            />
          </FormGroup>
          <ActionGroup>
            <Button
              variant="primary"
              type="submit"
              data-testid="tokens-tab-save"
              isDisabled={!form.formState.isDirty}
            >
              {t("common:save")}
            </Button>
            <Button variant="link" onClick={reset}>
              {t("common:revert")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </FormPanel>
    </PageSection>
  );
};
