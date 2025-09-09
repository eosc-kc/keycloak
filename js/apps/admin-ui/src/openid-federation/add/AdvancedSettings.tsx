import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";
import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";
import type { FieldProps } from "../../identity-providers/component/FormGroupField";
import { FormGroupField } from "../../identity-providers/component/FormGroupField";
import { SwitchField } from "../../identity-providers/component/SwitchField";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/openIdFederationRepresentation";

const LoginFlow = ({
  field,
  label,
  defaultValue,
}: FieldProps & { defaultValue: string }) => {
  const { t } = useTranslation("identity-providers");
  const { control } = useFormContext();

  const [flows, setFlows] = useState<AuthenticationFlowRepresentation[]>();
  const [open, setOpen] = useState(false);

  useFetch(
    () => adminClient.authenticationManagement.getFlows(),
    (flows) =>
      setFlows(flows.filter((flow) => flow.providerId === "basic-flow")),
    [],
  );

  return (
    <FormGroup
      label={t(label)}
      labelIcon={
        <HelpItem
          helpText={t(`identity-providers-help:${label}`)}
          fieldLabelId={`identity-providers:${label}`}
        />
      }
      fieldId={label}
    >
      <Controller
        name={field}
        defaultValue={defaultValue}
        control={control}
        render={({ field }) => (
          <Select
            toggleId={label}
            required
            onToggle={() => setOpen(!open)}
            onSelect={(_, value) => {
              field.onChange(value as string);
              setOpen(false);
            }}
            selections={field.value || t("common:none")}
            variant={SelectVariant.single}
            aria-label={t(label)}
            isOpen={open}
          >
            {[
              ...(defaultValue === ""
                ? [
                    <SelectOption key="empty" value="">
                      {t("common:none")}
                    </SelectOption>,
                  ]
                : []),
              ...(flows?.map((option) => (
                <SelectOption
                  selected={option.alias === field.value}
                  key={option.id}
                  value={option.alias}
                >
                  {option.alias}
                </SelectOption>
              )) || []),
            ]}
          </Select>
        )}
      />
    </FormGroup>
  );
};

const syncModes = ["import", "legacy", "force"];

export const AdvancedSettings = () => {
  const { t } = useTranslation("identity-providers");
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext<OpenIdFederationRepresentation>();
  const [syncModeOpen, setSyncModeOpen] = useState(false);
  const filteredByClaim = useWatch({
    control,
    name: "idpConfiguration.filteredByClaim",
    defaultValue: "false",
  });
  const claimFilterRequired = filteredByClaim === "true";

  return (
    <>
      <SwitchField
        field="idpConfiguration.storeToken"
        label="storeTokens"
        fieldType="boolean"
      />
      <SwitchField
        field="idpConfiguration.addReadTokenRoleOnCreate"
        label="storedTokensReadable"
        fieldType="boolean"
      />

      <SwitchField
        field="idpConfiguration.isAccessTokenJWT"
        label="isAccessTokenJWT"
      />
      <SwitchField
        field="idpConfiguration.trustEmail"
        label="trustEmail"
        fieldType="boolean"
      />
      <SwitchField
        field="idpConfiguration.linkOnly"
        label="accountLinkingOnly"
        fieldType="boolean"
      />
      <SwitchField
        field="idpConfiguration.hideOnLoginPage"
        label="hideOnLoginPage"
      />
      <SwitchField
        field="idpConfiguration.promotedLoginbutton"
        label="promotedLoginbutton"
      />
      <FormGroupField label="filteredByClaim">
        <Controller
          name="idpConfiguration.filteredByClaim"
          defaultValue="false"
          control={control}
          render={({ field }) => (
            <Switch
              id="filteredByClaim"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={field.value === "true"}
              onChange={(value) => {
                field.onChange(value.toString());
              }}
            />
          )}
        />
      </FormGroupField>
      {claimFilterRequired && (
        <>
          <FormGroup
            label={t("identity-providers:claimFilterName")}
            labelIcon={
              <HelpItem
                helpText={t("identity-providers-help:claimFilterName")}
                fieldLabelId="identity-providers:claimFilterName"
              />
            }
            fieldId="kc-claim-filter-name"
            isRequired
            validated={
              errors.idpConfiguration?.claimFilterName
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            helperTextInvalid={t("common:required")}
          >
            <KeycloakTextInput
              isRequired
              id="kc-claim-filter-name"
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
          </FormGroup>
          <FormGroup
            label={t("identity-providers:claimFilterValue")}
            labelIcon={
              <HelpItem
                helpText={t("identity-providers-help:claimFilterValue")}
                fieldLabelId="identity-providers:claimFilterName"
              />
            }
            fieldId="kc-claim-filter-value"
            isRequired
            validated={
              errors.idpConfiguration?.claimFilterValue
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            helperTextInvalid={t("common:required")}
          >
            <KeycloakTextInput
              isRequired
              id="kc-claim-filter-value"
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
          </FormGroup>
        </>
      )}
      <SwitchField field="idpConfiguration.passSetMfa" label="passSetMfa" />
      <LoginFlow
        field="idpConfiguration.firstBrokerLoginFlowAlias"
        label="firstBrokerLoginFlowAlias"
        defaultValue="fist broker login"
      />
      <LoginFlow
        field="idpConfiguration.postBrokerLoginFlowAlias"
        label="postBrokerLoginFlowAlias"
        defaultValue=""
      />

      <FormGroup
        className="pf-u-pb-3xl"
        label={t("syncMode")}
        labelIcon={
          <HelpItem
            helpText={t("identity-providers-help:syncMode")}
            fieldLabelId="identity-providers:syncMode"
          />
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
              required
              direction="up"
              onToggle={() => setSyncModeOpen(!syncModeOpen)}
              onSelect={(_, value) => {
                field.onChange(value as string);
                setSyncModeOpen(false);
              }}
              selections={t(`syncModes.${field.value.toLowerCase()}`)}
              variant={SelectVariant.single}
              aria-label={t("syncMode")}
              isOpen={syncModeOpen}
            >
              {syncModes.map((option) => (
                <SelectOption
                  selected={option === field.value}
                  key={option}
                  value={option.toUpperCase()}
                >
                  {t(`syncModes.${option}`)}
                </SelectOption>
              ))}
            </Select>
          )}
        />
      </FormGroup>
    </>
  );
};
