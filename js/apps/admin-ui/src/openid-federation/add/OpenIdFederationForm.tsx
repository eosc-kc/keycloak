import OpenIdFederationRepresentation, {
  EntityTypesSupported,
  ClientRegistrationTypesSupported,
} from "@keycloak/keycloak-admin-client/lib/defs/openIdFederationRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  NumberInput,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  Tab,
  TabTitleText,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useState, useEffect } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { convertToFormValues } from "../../util";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import {
  OpenIdFederationEditParams,
  OpenIdFederationTab,
  toOpenIdFederationEdit,
} from "../routes/OpenIdFederationEdit";
import { useParams } from "react-router";
import { useRealm } from "../../context/realm-context/RealmContext";
import { TextField } from "../../identity-providers/component/TextField";
import { SwitchField } from "../../identity-providers/component/SwitchField";
import { FormGroupField } from "../../identity-providers/component/FormGroupField";
import { AdvancedSettings } from "./AdvancedSettings";
import { ScrollForm } from "../../components/scroll-form/ScrollForm";
import { FixedButtonsGroup } from "../../components/form/FixedButtonGroup";

type OpenIdFederationGeneralTabProps = {
  save: (openIdFederation: OpenIdFederationRepresentation) => void;
  openIdFederation?: OpenIdFederationRepresentation;
};
const entityTypesSupportedValues: EntityTypesSupported[] = [
  "OPENID_PROVIDER",
  "OPENID_RELYING_PARTY",
];
const clientRegistrationTypesSupportedValues: ClientRegistrationTypesSupported[] =
  ["EXPLICIT"];
const promptOptions = {
  unspecified: "",
  none: "none",
  consent: "consent",
  login: "login",
  select_account: "select_account",
};

export const OpenIdFederationForm = ({
  save,
  openIdFederation = {
    trustAnchor: "",
    entityTypes: [],
    clientRegistrationTypesSupported: [],
  },
}: OpenIdFederationGeneralTabProps) => {
  const { t } = useTranslation("openid-federation");

  const [
    openClientRegistrationTypesSupported,
    setOpenClientRegistrationTypesSupported,
  ] = useState(false);

  const [promptOpen, setPromptOpen] = useState(false);
  const [openEntityTypes, setOpenEntityTypes] = useState(false);
  const { realm } = useRealm();
  const { id } = useParams<OpenIdFederationEditParams>();

  const {
    register,
    control,
    handleSubmit,
    setValue,
    reset,
    formState: { isDirty, errors },
  } = useFormContext<OpenIdFederationRepresentation>();

  const passScope = useWatch({
    control,
    name: "idpConfiguration.passScope",
  });
  const setupForm = () => {
    reset(openIdFederation);
    convertToFormValues(openIdFederation, setValue);
  };

  useEffect(setupForm, []);

  const toTab = (tab: OpenIdFederationTab) =>
    toOpenIdFederationEdit({
      realm,
      id: id || "",
      tab,
    });
  const useTab = (tab: OpenIdFederationTab) => useRoutableTab(toTab(tab));

  const settingsTab = useTab("settings");
  const idpTab = useTab("idp");

  const sections = [
    {
      title: t("oidcSettings"),
      panel: (
        <FormAccess
          isHorizontal
          role="manage-realm"
          className="pf-u-mt-lg"
          onSubmit={handleSubmit(save)}
        >
          <SwitchField
            label="passLoginHint"
            field="idpConfiguration.loginHint"
          />
          <SwitchField label="passMaxAge" field="idpConfiguration.passMaxAge" />
          <SwitchField
            label="passCurrentLocale"
            field="idpConfiguration.uiLocales"
          />
          <SwitchField
            field="idpConfiguration.backchannelSupported"
            label="backchannelLogout"
          />
          <SwitchField
            field="idpConfiguration.disableUserInfo"
            label="disableUserInfo"
          />
          <SwitchField
            field="idpConfiguration.disableNonce"
            label="disableNonce"
          />
          <SwitchField
            field="idpConfiguration.validateRefreshToken"
            label="validateRefreshToken"
          />
          <TextField field="idpConfiguration.defaultScope" label="scopes" />
          <SwitchField label="passScope" field="idpConfiguration.passScope" />
          {passScope === "true" && (
            <TextField
              field="idpConfiguration.optionalScope"
              label="optionalScopes"
            />
          )}
          <FormGroupField label="prompt">
            <Controller
              name="idpConfiguration.prompt"
              defaultValue=""
              control={control}
              render={({ field }) => (
                <Select
                  toggleId="prompt"
                  required
                  onToggle={() => setPromptOpen(!promptOpen)}
                  onSelect={(_, value) => {
                    field.onChange(value as string);
                    setPromptOpen(false);
                  }}
                  selections={
                    field.value || t(`identity-providers:prompts.unspecified`)
                  }
                  variant={SelectVariant.single}
                  aria-label={t("prompt")}
                  isOpen={promptOpen}
                >
                  {Object.entries(promptOptions).map(([key, val]) => (
                    <SelectOption
                      selected={val === field.value}
                      key={key}
                      value={val}
                    >
                      {t(`identity-providers:prompts.${key}`)}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroupField>
          <SwitchField
            field="idpConfiguration.acceptsPromptNoneForwardFromClient"
            label="acceptsPromptNone"
          />
          <FormGroup
            label={t("identity-providers:allowedClockSkew")}
            labelIcon={
              <HelpItem
                helpText={"identity-providers-help:allowedClockSkew"}
                fieldLabelId="identity-providers:allowedClockSkew"
              />
            }
            fieldId="allowedClockSkew"
          >
            <Controller
              name="idpConfiguration.allowedClockSkew"
              defaultValue={0}
              control={control}
              render={({ field }) => {
                const v = Number(field.value);
                return (
                  <NumberInput
                    data-testid="allowedClockSkew"
                    inputName="allowedClockSkew"
                    min={0}
                    max={2147483}
                    value={v}
                    readOnly
                    onPlus={() => field.onChange(v + 1)}
                    onMinus={() => field.onChange(v - 1)}
                    onChange={(event) => {
                      const value = Number(
                        (event.target as HTMLInputElement).value,
                      );
                      field.onChange(value < 0 ? 0 : value);
                    }}
                  />
                );
              }}
            />
          </FormGroup>
          <TextField
            field="idpConfiguration.forwardParameters"
            label="forwardParameters"
          />
        </FormAccess>
      ),
    },
    {
      title: t("advancedSettings"),
      panel: (
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(save)}
        >
          <AdvancedSettings />
          <FixedButtonsGroup name="idp-details" isSubmit reset={setupForm} />
        </FormAccess>
      ),
    },
  ];

  return (
    <PageSection variant="light" className="pf-u-p-0">
      <RoutableTabs isBox defaultLocation={toTab("settings")}>
        <Tab
          id="settings"
          title={<TabTitleText>{t("common:settings")}</TabTitleText>}
          {...settingsTab}
        >
          <PageSection variant="light">
            <FormAccess
              isHorizontal
              role="manage-realm"
              className="pf-u-mt-lg"
              onSubmit={handleSubmit(save)}
            >
              <FormGroup
                label={t("trustAnchor")}
                isRequired
                labelIcon={
                  <HelpItem
                    helpText={t("openid-federation-help:trustAnchor")}
                    fieldLabelId="openid-federation:trustAnchor"
                  />
                }
                fieldId="kc-trustAnchor"
                validated={
                  errors.trustAnchor
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                helperTextInvalid={t("common:required")}
              >
                <KeycloakTextInput
                  id="kc-logo-uri"
                  validated={
                    errors.trustAnchor
                      ? ValidatedOptions.error
                      : ValidatedOptions.default
                  }
                  {...register(`trustAnchor`, {
                    required: true,
                  })}
                />
              </FormGroup>
              <FormGroup
                label={t("entityTypes")}
                isRequired
                labelIcon={
                  <HelpItem
                    helpText={t("openid-federation-help:entityTypes")}
                    fieldLabelId="resetEntityTypes"
                  />
                }
                validated={
                  errors.entityTypes
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                helperTextInvalid={t("common:required")}
                fieldId="entity-types"
              >
                <Controller
                  name={`entityTypes`}
                  defaultValue={[] as EntityTypesSupported[]}
                  control={control}
                  rules={{
                    required: {
                      value: true,
                      message: t("common:required"),
                    },
                  }}
                  render={({ field }) => (
                    <Select
                      maxHeight={375}
                      toggleId={"entityTypes"}
                      variant={SelectVariant.typeaheadMulti}
                      chipGroupProps={{
                        numChips: 3,
                      }}
                      placeholderText={t("entityTypesPlaceholder")}
                      menuAppendTo="parent"
                      validated={errors.entityTypes ? "error" : "default"}
                      onToggle={(open) => setOpenEntityTypes(open)}
                      isOpen={openEntityTypes}
                      selections={field.value as string[]}
                      onSelect={(_, selectedValue) => {
                        const value: EntityTypesSupported[] | undefined =
                          field.value;
                        field.onChange(
                          value.find((item) => item === selectedValue)
                            ? value.filter((item) => item !== selectedValue)
                            : [...(value ? value : []), selectedValue],
                        );
                      }}
                      onClear={(event) => {
                        event.stopPropagation();
                        field.onChange([]);
                      }}
                      typeAheadAriaLabel={t("resetActions")}
                    >
                      {entityTypesSupportedValues.map((name) => (
                        <SelectOption
                          key={name}
                          value={name}
                          data-testid={`${name}-option`}
                        >
                          {name}
                        </SelectOption>
                      ))}
                    </Select>
                  )}
                />
              </FormGroup>
              <FormGroup
                label={t("clientRegistrationTypesSupported")}
                isRequired
                labelIcon={
                  <HelpItem
                    helpText={t(
                      "openid-federation-help:clientRegistrationTypesSupported",
                    )}
                    fieldLabelId="resetTypesSupported"
                  />
                }
                validated={
                  errors.clientRegistrationTypesSupported
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                helperTextInvalid={t("common:required")}
                fieldId="types-supported"
              >
                <Controller
                  name={`clientRegistrationTypesSupported`}
                  defaultValue={[] as ClientRegistrationTypesSupported[]}
                  control={control}
                  rules={{
                    required: {
                      value: true,
                      message: t("common:required"),
                    },
                  }}
                  render={({ field }) => (
                    <Select
                      maxHeight={375}
                      toggleId={
                        "openIdFederationList.clientRegistrationTypesSupported"
                      }
                      variant={SelectVariant.typeaheadMulti}
                      chipGroupProps={{
                        numChips: 3,
                      }}
                      placeholderText={t(
                        "clientRegistrationTypesSupportedPlaceholder",
                      )}
                      validated={
                        errors.clientRegistrationTypesSupported
                          ? "error"
                          : "default"
                      }
                      menuAppendTo="parent"
                      onToggle={(open) =>
                        setOpenClientRegistrationTypesSupported(open)
                      }
                      isOpen={openClientRegistrationTypesSupported}
                      selections={field.value as string[]}
                      onSelect={(_, selectedValue) => {
                        const value:
                          | ClientRegistrationTypesSupported[]
                          | undefined = field.value;
                        field.onChange(
                          value.find((item) => item === selectedValue)
                            ? value.filter((item) => item !== selectedValue)
                            : [...(value ? value : []), selectedValue],
                        );
                      }}
                      onClear={(event) => {
                        event.stopPropagation();
                        field.onChange([]);
                      }}
                      typeAheadAriaLabel={t("resetActions")}
                    >
                      {clientRegistrationTypesSupportedValues.map((name) => (
                        <SelectOption
                          key={name}
                          value={name}
                          data-testid={`${name}-option`}
                        >
                          {name}
                        </SelectOption>
                      ))}
                    </Select>
                  )}
                />
              </FormGroup>
              <ActionGroup>
                <Button
                  variant="primary"
                  type="submit"
                  data-testid="general-tab-save"
                  isDisabled={!isDirty}
                >
                  {t("common:save")}
                </Button>
                <Button
                  data-testid="general-tab-revert"
                  variant="link"
                  onClick={setupForm}
                >
                  {t("common:revert")}
                </Button>
              </ActionGroup>
            </FormAccess>
          </PageSection>
        </Tab>
        {openIdFederation.entityTypes.includes("OPENID_RELYING_PARTY") && (
          <Tab
            id="idp"
            title={<TabTitleText>{t("identityProviderSettings")}</TabTitleText>}
            {...idpTab}
          >
            <PageSection variant="light">
              <ScrollForm className="pf-u-px-lg" sections={sections} />
            </PageSection>
          </Tab>
        )}
      </RoutableTabs>
    </PageSection>
  );
};
