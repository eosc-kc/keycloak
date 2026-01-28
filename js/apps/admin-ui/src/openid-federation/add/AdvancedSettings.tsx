import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import { FormGroup } from "@patternfly/react-core";
import {
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core/deprecated";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { useAdminClient } from "../../admin-client";
import { useFetch, HelpItem } from "@keycloak/keycloak-ui-shared";

import type { FieldProps } from "../../identity-providers/component/FormGroupField";
import { SwitchField } from "../../identity-providers/component/SwitchField";

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
          const displayedSelection =
            rawValue === "" ? t("none") : rawValue;

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

export const AdvancedSettings = () => {
  const { t } = useTranslation();
  const { control } = useFormContext();

  const [syncModeOpen, setSyncModeOpen] = useState(false);

  // if you need the old "filteredByClaim" behavior again, you can re-enable it with useWatch
  // const filteredByClaim = useWatch({
  //   control,
  //   name: "idpConfiguration.filteredByClaim",
  //   defaultValue: "false",
  // });
  // const claimFilterRequired = filteredByClaim === "true";

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
    </>
  );
};
