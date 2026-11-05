import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import {
  FormGroup,
  Switch,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import {
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core/deprecated";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { useAdminClient } from "../../admin-client";
import {
  FormErrorText,
  HelpItem,
  SelectControl,
  useFetch,
} from "@keycloak/keycloak-ui-shared";

import {
  type FieldProps,
  FormGroupField,
} from "../../identity-providers/component/FormGroupField";
import { SwitchField } from "../../identity-providers/component/SwitchField";
import type { OpenIdFederationFormValues } from "./form-state";

const LoginFlow = ({
  field,
  label,
  defaultValue,
}: FieldProps & { defaultValue: string }) => {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { control } = useFormContext();

  const [flows, setFlows] = useState<AuthenticationFlowRepresentation[]>([]);
  const [open, setOpen] = useState(false);

  useFetch(
    async () => {
      const all = await adminClient.authenticationManagement.getFlows();
      return (all ?? []).filter((f) => f.providerId === "basic-flow");
    },
    setFlows,
    [adminClient],
  );

  return (
    <FormGroup
      label={t(label)}
      labelIcon={<HelpItem helpText={t(`${label}Help`)} fieldLabelId={label} />}
      fieldId={label}
    >
      <Controller
        name={field}
        defaultValue={defaultValue}
        control={control}
        render={({ field: rhfField }) => {
          const options = [
            ...(defaultValue === ""
              ? [
                  <SelectOption key="empty" value="">
                    {t("none")}
                  </SelectOption>,
                ]
              : []),
            ...flows.map((flow) => (
              <SelectOption key={flow.id} value={flow.alias}>
                {flow.alias}
              </SelectOption>
            )),
          ];

          const rawValue = (rhfField.value ?? "") as string;
          const displayedSelection = rawValue === "" ? t("none") : rawValue;

          return (
            <Select
              toggleId={label}
              variant={SelectVariant.single}
              aria-label={t(label)}
              isOpen={open}
              selections={displayedSelection}
              placeholderText={t("none")}
              onToggle={(_event, isOpen) => setOpen(isOpen)}
              onSelect={(_event, selection) => {
                // selection will be "" for the "none" option
                rhfField.onChange(selection as string);
                setOpen(false);
              }}
            >
              {options}
            </Select>
          );
        }}
      />
    </FormGroup>
  );
};

const syncModes = ["import", "legacy", "force"] as const;
const showInAccountConsoleValues = ["ALWAYS", "WHEN_LINKED", "NEVER"];

export const AdvancedSettings = () => {
  const { t } = useTranslation();
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext<OpenIdFederationFormValues>();

  const [syncModeOpen, setSyncModeOpen] = useState(false);

  const filteredByClaim = useWatch({
    control,
    name: "idpConfiguration.filteredByClaim",
    defaultValue: "false",
  });
  const claimFilterRequired = filteredByClaim === "true";

  return (
    <>
      <SwitchField field="idpConfiguration.storeToken" label="storeTokens" />
      <SwitchField
        field="idpConfiguration.addReadTokenRoleOnCreate"
        label="storedTokensReadable"
      />
      <SwitchField
        field="idpConfiguration.isAccessTokenJWT"
        label="isAccessTokenJWT"
      />
      <SwitchField field="idpConfiguration.trustEmail" label="trustEmail" />
      <SwitchField
        field="idpConfiguration.linkOnly"
        label="accountLinkingOnly"
      />
      <SwitchField
        field="idpConfiguration.hideOnLoginPage"
        label="hideOnLoginPage"
      />
      <SwitchField
        field="idpConfiguration.promotedLoginbutton"
        label="promotedLoginbutton"
      />
      <SelectControl
        name="idpConfiguration.showInAccountConsole"
        label={t("showInAccountConsole")}
        labelIcon={t("showInAccountConsoleHelp")}
        options={showInAccountConsoleValues.map((showInAccountConsole) => ({
          key: showInAccountConsole,
          value: t(
            `showInAccountConsole.${showInAccountConsole.toLowerCase()}`,
          ),
        }))}
        controller={{
          defaultValue: showInAccountConsoleValues[0],
          rules: { required: t("required") },
        }}
      />

      <FormGroupField label="filteredByClaim">
        <Controller
          name="idpConfiguration.filteredByClaim"
          defaultValue="false"
          control={control}
          render={({ field }) => (
            <Switch
              id="filteredByClaim"
              label={t("on")}
              labelOff={t("off")}
              isChecked={field.value === "true"}
              onChange={(_event, value) => field.onChange(value.toString())}
            />
          )}
        />
      </FormGroupField>
      {claimFilterRequired && (
        <>
          <FormGroup
            label={t("claimFilterName")}
            labelIcon={
              <HelpItem
                helpText={t("claimFilterNameHelp")}
                fieldLabelId="claimFilterName"
              />
            }
            fieldId="kc-oidfed-claim-filter-name"
            isRequired
          >
            <TextInput
              isRequired
              id="kc-oidfed-claim-filter-name"
              data-testid="claimFilterName"
              validated={
                errors.idpConfiguration?.claimFilterName
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              {...register("idpConfiguration.claimFilterName", {
                required: true,
              })}
            />
            {errors.idpConfiguration?.claimFilterName && (
              <FormErrorText message={t("required")} />
            )}
          </FormGroup>
          <FormGroup
            label={t("claimFilterValue")}
            labelIcon={
              <HelpItem
                helpText={t("claimFilterValueHelp")}
                fieldLabelId="claimFilterValue"
              />
            }
            fieldId="kc-oidfed-claim-filter-value"
            isRequired
          >
            <TextInput
              isRequired
              id="kc-oidfed-claim-filter-value"
              data-testid="claimFilterValue"
              validated={
                errors.idpConfiguration?.claimFilterValue
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              {...register("idpConfiguration.claimFilterValue", {
                required: true,
              })}
            />
            {errors.idpConfiguration?.claimFilterValue && (
              <FormErrorText message={t("required")} />
            )}
          </FormGroup>
        </>
      )}

      <SwitchField field="idpConfiguration.passSetMfa" label="passSetMfa" />

      <LoginFlow
        field="idpConfiguration.firstBrokerLoginFlowAlias"
        label="firstBrokerLoginFlowAlias"
        defaultValue=""
      />
      <LoginFlow
        field="idpConfiguration.postBrokerLoginFlowAlias"
        label="postBrokerLoginFlowAlias"
        defaultValue=""
      />

      <FormGroup
        className="pf-v5-u-pb-3xl"
        label={t("syncMode")}
        labelIcon={
          <HelpItem helpText={t("syncModeHelp")} fieldLabelId="syncMode" />
        }
        fieldId="syncMode"
      >
        <Controller
          name="idpConfiguration.syncMode"
          defaultValue={syncModes[0].toUpperCase()}
          control={control}
          render={({ field }) => (
            <Select
              toggleId="syncMode"
              variant={SelectVariant.single}
              aria-label={t("syncMode")}
              isOpen={syncModeOpen}
              selections={field.value}
              onToggle={(_event, isOpen) => setSyncModeOpen(isOpen)}
              onSelect={(_event, selection) => {
                field.onChange(selection as string);
                setSyncModeOpen(false);
              }}
            >
              {syncModes.map((option) => {
                const value = option.toUpperCase();

                return (
                  <SelectOption key={option} value={value}>
                    {t(`syncModes.${option}`)}
                  </SelectOption>
                );
              })}
            </Select>
          )}
        />
      </FormGroup>
      <SwitchField
        field="idpConfiguration.caseSensitiveOriginalUsername"
        label="caseSensitiveOriginalUsername"
      />
    </>
  );
};
