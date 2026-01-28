import type OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/openIdFederationRepresentation";
import type RealmRepresentation from "libs/keycloak-admin-client/lib/defs/realmRepresentation";

import {
  FormGroup,
  NumberInput,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import {
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core/deprecated";
import { useEffect, useMemo, useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useLocation, useNavigate } from "react-router-dom";

import { HelpItem, ScrollForm, TextControl } from "@keycloak/keycloak-ui-shared";

import { FormAccess } from "../../components/form/FormAccess";
import { convertToFormValues } from "../../util";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import {
  type OpenIdFederationEditParams,
  type OpenIdFederationTab,
  toOpenIdFederationEdit,
} from "../routes/OpenIdFederationEdit";
import { useParams } from "../../utils/useParams";
import { useRealm } from "../../context/realm-context/RealmContext";
import { TextField } from "../../identity-providers/component/TextField";
import { SwitchField } from "../../identity-providers/component/SwitchField";
import { FormGroupField } from "../../identity-providers/component/FormGroupField";
import { AdvancedSettings } from "./AdvancedSettings";
import { FixedButtonsGroup } from "../../components/form/FixedButtonGroup";
import { toOpenIdFederationCreate } from "../routes/OpenIdFederationCreate";

const promptOptions: Record<string, string> = {
  unspecified: "",
  none: "none",
  consent: "consent",
  login: "login",
  select_account: "select_account",
};

interface OpenIdFederationFormProps {
  save: (data: OpenIdFederationRepresentation) => void;
  openIdFederation?: OpenIdFederationRepresentation;
  realm?: RealmRepresentation;
}

export const OpenIdFederationForm = ({
  save,
  openIdFederation = { trustAnchor: "" },
  realm,
}: OpenIdFederationFormProps) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();

  const { realm: realmName } = useRealm();
  const { id } = useParams<OpenIdFederationEditParams>();

  const { control, handleSubmit, setValue, reset, formState } =
    useFormContext<OpenIdFederationRepresentation>();

  const { isDirty } = formState;

  const [promptOpen, setPromptOpen] = useState(false);

  const passScope = useWatch({
    control,
    name: "idpConfiguration.passScope",
  });

  const setupForm = () => {
    reset(openIdFederation);
    convertToFormValues(openIdFederation, setValue);
  };

  const entityKey =
    (openIdFederation as any)?.internalId ??
    (openIdFederation as any)?.id ??
    openIdFederation?.trustAnchor ??
    "new";

  useEffect(() => {
    if (isDirty) return;
    setupForm();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entityKey, id]);

  const toTab = (tab: OpenIdFederationTab) =>
    id
      ? toOpenIdFederationEdit({
          realm: realmName,
          id: id || "",
          tab,
        })
      : toOpenIdFederationCreate({
          realm: realmName,
          tab,
        });

  const useTab = (tab: OpenIdFederationTab) => useRoutableTab(toTab(tab));
  const settingsTab = useTab("settings");
  const idpTab = useTab("idp");

  // Detect current tab (adjust if your routing differs)
  const isSettingsTab =
    location.pathname.includes("/settings") ||
    location.search.includes("tab=settings");

  const promptSelectOptions = useMemo(
    () =>
      Object.entries(promptOptions).map(([key, val]) => (
        <SelectOption key={key} value={val}>
          {t(`prompts.${key}`)}
        </SelectOption>
      )),
    [t],
  );

  // Single submit for the whole form:
  // if trustAnchor invalid, jump to Settings tab so user sees the error
  const onSubmit = handleSubmit(save, (invalid) => {
    if ((invalid as any)?.trustAnchor) {
      navigate(toTab("settings"));
    }
  });

  const sections = [
    {
      title: t("oidcSettings"),
      panel: (
        <div className="pf-v5-c-form pf-m-horizontal pf-v5-u-mt-lg">
          <SwitchField label="passLoginHint" field="idpConfiguration.loginHint" />
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
          <SwitchField field="idpConfiguration.disableNonce" label="disableNonce" />
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
              render={({ field }) => {
                // PF Select shows blank when selections === "" so show label for empty
                const displayedSelection = !field.value
                  ? t("prompts.unspecified")
                  : (field.value as string);

                return (
                  <Select
                    toggleId="prompt"
                    variant={SelectVariant.single}
                    aria-label={t("prompt")}
                    isOpen={promptOpen}
                    selections={displayedSelection}
                    placeholderText={t("prompts.unspecified")}
                    onToggle={(_event, isOpen) => setPromptOpen(isOpen)}
                    onSelect={(_event, selection) => {
                      field.onChange(selection as string);
                      setPromptOpen(false);
                    }}
                  >
                    {promptSelectOptions}
                  </Select>
                );
              }}
            />
          </FormGroupField>

          <SwitchField
            field="idpConfiguration.acceptsPromptNoneForwardFromClient"
            label="acceptsPromptNone"
          />

          <FormGroup
            label={t("allowedClockSkew")}
            labelIcon={
              <HelpItem
                helpText={t("allowedClockSkewHelp")}
                fieldLabelId="allowedClockSkew"
              />
            }
            fieldId="allowedClockSkew"
          >
            <Controller
              name="idpConfiguration.allowedClockSkew"
              defaultValue={0}
              control={control}
              render={({ field }) => {
                const v = Number(field.value ?? 0);
                return (
                  <NumberInput
                    data-testid="allowedClockSkew"
                    inputName="allowedClockSkew"
                    min={0}
                    max={2147483}
                    value={v}
                    onPlus={() => field.onChange(v + 1)}
                    onMinus={() => field.onChange(Math.max(0, v - 1))}
                    onChange={(event) => {
                      const value = Number(
                        (event.target as HTMLInputElement).value,
                      );
                      field.onChange(
                        Number.isNaN(value) ? 0 : Math.max(0, value),
                      );
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
        </div>
      ),
    },
    {
      title: t("advancedSettings"),
      panel: (
        <div className="pf-v5-c-form pf-m-horizontal">
          <AdvancedSettings />
        </div>
      ),
    },
  ];

  return (
    <PageSection variant="light" className="pf-v5-u-p-0">
      <FormAccess
        isHorizontal
        role="manage-realm"
        className="pf-v5-u-mt-lg"
        onSubmit={onSubmit}
      >
        {/* ✅ CRITICAL FIX:
            When Settings tab is NOT mounted, trustAnchor would be unregistered,
            so RHF would NOT validate it. Keep it registered with required rule. */}
        {!isSettingsTab && (
          <Controller
            name="trustAnchor"
            control={control}
            rules={{ required: t("required") }}
            render={({ field }) => (
              <input
                {...field}
                style={{ display: "none" }}
                tabIndex={-1}
                aria-hidden="true"
              />
            )}
          />
        )}

        <RoutableTabs isBox defaultLocation={toTab("settings")}>
          <Tab
            id="settings"
            title={<TabTitleText>{t("settings")}</TabTitleText>}
            {...settingsTab}
          >
            <PageSection variant="light">
              <TextControl
                name="trustAnchor"
                label={t("trustAnchor")}
                type="text"
                rules={{ required: t("required") }}
              />
            </PageSection>
          </Tab>

          {realm?.openIdFederationEntityTypes?.includes("OPENID_RELYING_PARTY") && (
            <Tab
              id="idp"
              title={<TabTitleText>{t("identityProviderSettings")}</TabTitleText>}
              {...idpTab}
            >
              <PageSection variant="light">
                <ScrollForm
                  className="pf-v5-u-px-lg"
                  sections={sections}
                  label=""
                />
              </PageSection>
            </Tab>
          )}
        </RoutableTabs>

        {/* Submits the SAME outer FormAccess form */}
        <FixedButtonsGroup name="idp-details" isSubmit reset={setupForm} />
      </FormAccess>
    </PageSection>
  );
};
