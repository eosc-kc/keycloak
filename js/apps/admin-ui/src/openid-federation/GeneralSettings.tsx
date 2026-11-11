import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
// import { useAlerts } from "../components/alert/Alerts";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  FormGroup,
  PageSection,
  Switch,
  TextInput,
  ValidatedOptions,
  ToolbarItem,
  Stack,
  StackItem,
} from "@patternfly/react-core";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { Link, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import {
  Controller,
  FormProvider,
  useForm,
  useFormContext,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { MultiLineInput } from "../components/multi-line-input/MultiLineInput";
import { FormAccess } from "../components/form/FormAccess";
import { TimeSelector } from "../components/time-selector/TimeSelector";
import { addTrailingSlash, convertToFormValues } from "../util";
import {
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core/deprecated";
import { toOpenIdFederationCreate } from "./routes/OpenIdFederationCreate";
import { useRealm } from "../context/realm-context/RealmContext";
import { toOpenIdFederationEdit } from "./routes/OpenIdFederationEdit";
import { FormattedLink } from "../components/external-link/FormattedLink";
import { FixedButtonsGroup } from "../components/form/FixedButtonGroup";
import OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/openIdFederationRepresentation";
import {
  ClientRegistrationTypesSupported,
  EntityTypesSupported,
} from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  useAlerts,
  HelpItem,
  ScrollForm,
  FormErrorText,
  TextControl,
  KeycloakDataTable,
  ListEmptyState,
} from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import type TrustMarkRepresentation from "@keycloak/keycloak-admin-client/lib/defs/trustMarkRepresentation";
import { TrustMarkTable } from "./trust-mark/TrustMarkTable";

type OpenIdFederationGeneralTabProps = {
  realm: RealmRepresentation;
  openIdFederations?: OpenIdFederationRepresentation[];
  setOpenIdFederations: (
    openIdFederations: OpenIdFederationRepresentation[],
  ) => void;
  save: (realm: RealmRepresentation) => void;
  trustMarks?: TrustMarkRepresentation[];
  setTrustMarks?: (trustMarks: TrustMarkRepresentation[]) => void;
};

const openIdFederationRPClientRegistrationTypesSupported: ClientRegistrationTypesSupported[] =
  ["EXPLICIT"];
const openIdFederationOPClientRegistrationTypesSupported: ClientRegistrationTypesSupported[] =
  ["EXPLICIT", "AUTOMATIC"];

type AdvancedSettingName =
  | "openIdFederationConnectionTimeout"
  | "openIdFederationSocketTimeout"
  | "openIdFederationConnectionRequestTimeout"
  | "openIdFederationMaxAuthorityHints"
  | "openIdFederationMaxHops"
  | "openIdFederationTotalTimeout";

type AdvancedNumberControlProps = {
  name: AdvancedSettingName;
  label: string;
  helpText: string;
  minimum: number;
};

const AdvancedNumberControl = ({
  name,
  label,
  helpText,
  minimum,
}: AdvancedNumberControlProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext<RealmRepresentation>();

  return (
    <FormGroup
      label={label}
      fieldId={name}
      isRequired
      labelIcon={<HelpItem helpText={helpText} fieldLabelId={name} />}
    >
      <Controller
        name={name}
        control={control}
        rules={{
          required: t("required"),
          min: {
            value: minimum,
            message: t("openIdFederationValueAtLeast", { value: minimum }),
          },
          validate: (value) =>
            Number.isInteger(value) || t("openIdFederationIntegerValue"),
        }}
        render={({ field, fieldState }) => (
          <>
            <TextInput
              id={name}
              data-testid={name}
              name={field.name}
              type="number"
              min={minimum}
              step={1}
              value={field.value ?? ""}
              onBlur={field.onBlur}
              ref={field.ref}
              onChange={(_event, value) =>
                field.onChange(value === "" ? "" : Number(value))
              }
              validated={
                fieldState.error
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              aria-label={label}
              isRequired
            />
            {fieldState.error?.message && (
              <FormErrorText message={fieldState.error.message} />
            )}
          </>
        )}
      />
    </FormGroup>
  );
};

const OpenIdFederationAdvancedSettings = () => {
  const { t } = useTranslation();

  return (
    <>
      <AdvancedNumberControl
        name="openIdFederationConnectionTimeout"
        label={t("connectionTimeout")}
        helpText={t("openIdFederationConnectionTimeoutHelp")}
        minimum={-1}
      />
      <AdvancedNumberControl
        name="openIdFederationSocketTimeout"
        label={t("readTimeout")}
        helpText={t("openIdFederationSocketTimeoutHelp")}
        minimum={0}
      />
      <AdvancedNumberControl
        name="openIdFederationConnectionRequestTimeout"
        label={t("openIdFederationConnectionRequestTimeout")}
        helpText={t("openIdFederationConnectionRequestTimeoutHelp")}
        minimum={0}
      />
      <AdvancedNumberControl
        name="openIdFederationMaxAuthorityHints"
        label={t("openIdFederationMaxAuthorityHints")}
        helpText={t("openIdFederationMaxAuthorityHintsHelp")}
        minimum={1}
      />
      <AdvancedNumberControl
        name="openIdFederationMaxHops"
        label={t("openIdFederationMaxHops")}
        helpText={t("openIdFederationMaxHopsHelp")}
        minimum={1}
      />
      <AdvancedNumberControl
        name="openIdFederationTotalTimeout"
        label={t("openIdFederationTotalTimeout")}
        helpText={t("openIdFederationTotalTimeoutHelp")}
        minimum={1}
      />
    </>
  );
};

const OpenIdFederationLink = (
  openIdFederation: OpenIdFederationRepresentation,
) => {
  const { realm } = useRealm();
  return (
    <Link
      to={toOpenIdFederationEdit({
        realm,
        id: openIdFederation.internalId || "",
        tab: "settings",
      })}
    >
      {openIdFederation.trustAnchor}
    </Link>
  );
};

export const OpenIdFederationGeneralSettings = ({
  realm,
  openIdFederations = [],
  setOpenIdFederations,
  save,
  trustMarks = [],
  setTrustMarks = () => undefined,
}: OpenIdFederationGeneralTabProps) => {
  const { t } = useTranslation();
  const form = useForm<RealmRepresentation>();
  const {
    control,
    handleSubmit,
    clearErrors,
    unregister,
    setValue,
    formState: { errors },
  } = form;
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { adminClient } = useAdminClient();
  const [selectedOpenIdFederation, setSelectedOpenIdFederation] =
    useState<OpenIdFederationRepresentation>();
  const [isOpenIdFederationEnabled, setIsOpenIdFederationEnabled] = useState(
    !!realm.openIdFederationEnabled,
  );
  const [
    openOPClientRegistrationTypesSupported,
    setOpenOPClientRegistrationTypesSupported,
  ] = useState(false);
  const [
    openRPClientRegistrationTypesSupported,
    setOpenRPClientRegistrationTypesSupported,
  ] = useState(false);

  const { realm: realmName } = useRealm();

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteOpenIdFederation"),
    messageKey: t("deleteConfirmTrustAnchor", {
      trustAnchor: selectedOpenIdFederation?.trustAnchor,
    }),
    continueButtonLabel: t("delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.openIdFederations.del({
          internalId: selectedOpenIdFederation!.internalId!,
        });
        setOpenIdFederations([
          ...openIdFederations!.filter(
            (p) => p.internalId !== selectedOpenIdFederation?.internalId,
          ),
        ]);
        addAlert(t("deletedSuccessTrustAnchor"), AlertVariant.success);
      } catch (error) {
        addError(t("deletedErrorTrustAnchor"), error);
      }
    },
  });

  const openIdFederationEntityTypes = useWatch({
    control,
    name: "openIdFederationEntityTypes",
    defaultValue: [],
  }) as EntityTypesSupported[];
  const openIdFederationEnabled = useWatch({
    control,
    name: "openIdFederationEnabled",
    defaultValue: realm.openIdFederationEnabled ?? false,
  }) as boolean;

  useEffect(() => {
    if (!openIdFederationEnabled) {
      clearErrors("openIdFederationAuthorityHints");
      unregister("openIdFederationAuthorityHints");
    }
  }, [openIdFederationEnabled, clearErrors, unregister]);
  const setupForm = () => {
    const defaultValues: RealmRepresentation = {
      ...realm,
      openIdFederationContacts: realm.openIdFederationContacts ?? [],
      openIdFederationAuthorityHints:
        realm.openIdFederationAuthorityHints ?? [],
      openIdFederationLogoUri: realm.openIdFederationLogoUri ?? "",
      openIdFederationPolicyUri: realm.openIdFederationPolicyUri ?? "",
      openIdFederationOrganizationName:
        realm.openIdFederationOrganizationName ?? "",
      openIdFederationOrganizationUri:
        realm.openIdFederationOrganizationUri ?? "",
      openIdFederationResolveEndpoint:
        realm.openIdFederationResolveEndpoint ?? "",
      openIdFederationHistoricalKeysEndpoint:
        realm.openIdFederationHistoricalKeysEndpoint ?? "",
      openIdFederationLifespan: realm.openIdFederationLifespan ?? 86400,
      openIdFederationConnectionTimeout:
        realm.openIdFederationConnectionTimeout ?? 3,
      openIdFederationSocketTimeout: realm.openIdFederationSocketTimeout ?? 5,
      openIdFederationConnectionRequestTimeout:
        realm.openIdFederationConnectionRequestTimeout ?? 5,
      openIdFederationMaxAuthorityHints:
        realm.openIdFederationMaxAuthorityHints ?? 5,
      openIdFederationMaxHops: realm.openIdFederationMaxHops ?? 10,
      openIdFederationTotalTimeout: realm.openIdFederationTotalTimeout ?? 30,
      openIdFederationEnabled: realm.openIdFederationEnabled ?? false,
      // ...add other fields as needed
    };
    convertToFormValues(defaultValues, setValue);
  };

  useEffect(() => {
    setIsOpenIdFederationEnabled(!!realm.openIdFederationEnabled);
  }, [realm]);

  useEffect(setupForm, []);

  return (
    <PageSection variant="light">
      <DeleteConfirm />
      <ScrollForm
        className="pf-u-px-lg pf-u-pb-lg"
        label={""}
        sections={[
          {
            title: t("generalSettings"),
            panel: (
              <FormProvider {...form}>
                <FormAccess
                  isHorizontal
                  role="manage-realm"
                  className="pf-u-mt-lg"
                  onSubmit={handleSubmit(save)}
                >
                  <FormGroup
                    hasNoPaddingTop
                    label={t("openIdFederationEnabled")}
                    labelIcon={
                      <HelpItem
                        helpText={t("openIdFederationEnabledHelp")}
                        fieldLabelId="openid-federation:openIdFederationEnabled"
                      />
                    }
                    fieldId="kc-user-profile-enabled"
                  >
                    <Controller
                      name="openIdFederationEnabled"
                      defaultValue={false}
                      control={control}
                      render={({ field }) => (
                        <Switch
                          id="openidFederationEnabled"
                          label={t("on")}
                          labelOff={t("off")}
                          isChecked={field.value}
                          onChange={field.onChange}
                        />
                      )}
                    />
                  </FormGroup>
                  {openIdFederationEnabled && (
                    <>
                      <FormGroup
                        label={t("openIdFederationAuthorityHints")}
                        fieldId="kc-redirect"
                        isRequired
                      >
                        <MultiLineInput
                          id="kc-authority-hints"
                          name={"openIdFederationAuthorityHints"}
                          aria-label={t("openIdFederationAuthorityHints")}
                          addButtonLabel="addAuthorityHint"
                          validated={
                            errors["openIdFederationAuthorityHints"]?.message
                              ? ValidatedOptions.error
                              : ValidatedOptions.default
                          }
                          isRequired
                        />
                        {errors["openIdFederationAuthorityHints"]?.message && (
                          <FormErrorText
                            message={
                              errors["openIdFederationAuthorityHints"]?.message
                            }
                          />
                        )}
                      </FormGroup>
                      <FormGroup
                        label={t("openIdFederationLifespan")}
                        fieldId="openIdFederationLifespan"
                        labelIcon={
                          <HelpItem
                            helpText={t("openIdFederationLifespanHelp")}
                            fieldLabelId="openIdFederationLifespan"
                          />
                        }
                      >
                        <Controller
                          name="openIdFederationLifespan"
                          defaultValue={realm.openIdFederationLifespan || 86400}
                          control={control}
                          render={({ field }) => (
                            <TimeSelector
                              className="kc-lifespan"
                              data-testid="lifespan-input"
                              value={field.value!}
                              onChange={field.onChange}
                              units={["minute", "hour", "day"]}
                            />
                          )}
                        />
                      </FormGroup>
                      <FormGroup
                        label={t("openIdFederationContacts")}
                        fieldId="kc-openIdFederationContacts"
                      >
                        <MultiLineInput
                          name="openIdFederationContacts"
                          aria-label={t(
                            "openid-federation:openIdFederationContacts",
                          )}
                          addButtonLabel={t("addContacts")}
                          data-testid="declref-field"
                        />
                      </FormGroup>
                      <TextControl
                        name="openIdFederationLogoUri"
                        label={t("openIdFederationLogoUri")}
                        type="text"
                      />
                      <TextControl
                        name="openIdFederationPolicyUri"
                        label={t("openIdFederationPolicyUri")}
                        type="text"
                      />
                      <TextControl
                        name="openIdFederationOrganizationName"
                        label={t("openIdFederationOrganizationName")}
                        type="text"
                      />
                      <TextControl
                        name="openIdFederationOrganizationUri"
                        label={t("openIdFederationOrganizationUri")}
                        type="url"
                      />
                      <TextControl
                        name="openIdFederationResolveEndpoint"
                        label={t("openIdFederationResolveEndpoint")}
                        type="text"
                      />
                      <TextControl
                        name="openIdFederationHistoricalKeysEndpoint"
                        label={t("openIdFederationHistoricalKeysEndpoint")}
                        type="text"
                      />
                      <FormGroup
                        label={t("endpoint")}
                        labelIcon={
                          <HelpItem
                            helpText={t("endpointHelp")}
                            fieldLabelId="realm-settings:endpoints"
                          />
                        }
                        fieldId="kc-endpoints"
                      >
                        <Stack>
                          <StackItem>
                            <FormattedLink
                              href={`${addTrailingSlash(
                                adminClient.baseUrl,
                              )}realms/${realmName}/.well-known/openid-federation`}
                              title={t("openIDFederationEndpointConfiguration")}
                            />
                          </StackItem>
                        </Stack>
                      </FormGroup>
                    </>
                  )}
                </FormAccess>
              </FormProvider>
            ),
          },
          ...(openIdFederationEnabled
            ? [
                {
                  title: t("openIdProviderSettings"),
                  panel: (
                    <FormProvider {...form}>
                      <FormAccess
                        isHorizontal
                        role="manage-realm"
                        className="pf-u-mt-lg"
                        onSubmit={handleSubmit(save)}
                      >
                        <FormGroup
                          hasNoPaddingTop
                          label={t("enableOpenIdProvider")}
                          fieldId="kc-enableOpenIdProvider"
                        >
                          <Controller
                            name="openIdFederationEntityTypes"
                            defaultValue={[]}
                            control={control}
                            render={({ field }) => (
                              <Switch
                                label={t("on")}
                                labelOff={t("off")}
                                isChecked={openIdFederationEntityTypes.includes(
                                  "OPENID_PROVIDER",
                                )}
                                onChange={(_event, value) => {
                                  field.onChange(!value);
                                  if (value) {
                                    setValue("openIdFederationEntityTypes", [
                                      ...openIdFederationEntityTypes,
                                      "OPENID_PROVIDER",
                                    ]);
                                  } else {
                                    setValue(
                                      "openIdFederationEntityTypes",
                                      openIdFederationEntityTypes.filter(
                                        (item) => item !== "OPENID_PROVIDER",
                                      ),
                                    );
                                  }
                                }}
                                aria-label={t("clientAuthentication")}
                              />
                            )}
                          />
                        </FormGroup>
                        {openIdFederationEntityTypes.includes(
                          "OPENID_PROVIDER",
                        ) && (
                          <FormGroup
                            label={t(
                              "openIdFederationClientRegistrationTypesSupported",
                            )}
                            isRequired
                            labelIcon={
                              <HelpItem
                                helpText={t(
                                  "openIdFederationOPClientRegistrationTypesSupportedHelp",
                                )}
                                fieldLabelId="resetopenIdFederationOPClientRegistrationTypesSupported"
                              />
                            }
                            // helperTextInvalid={t("required")}
                            fieldId="types-supported"
                          >
                            <Controller
                              name="openIdFederationOPClientRegistrationTypesSupported"
                              defaultValue={
                                [] as ClientRegistrationTypesSupported[]
                              }
                              control={control}
                              rules={{
                                required: {
                                  value: true,
                                  message: t("required"),
                                },
                              }}
                              render={({ field }) => (
                                <Select
                                  variant={SelectVariant.typeaheadMulti}
                                  maxHeight={375}
                                  toggleId="openIdFederationOPClientRegistrationTypesSupported"
                                  chipGroupProps={{ numChips: 3 }}
                                  placeholderText={t(
                                    "clientRegistrationTypesSupportedPlaceholder",
                                  )}
                                  validated={
                                    errors.openIdFederationOPClientRegistrationTypesSupported
                                      ? ValidatedOptions.error
                                      : ValidatedOptions.default
                                  }
                                  menuAppendTo="parent"
                                  isOpen={
                                    openOPClientRegistrationTypesSupported
                                  }
                                  selections={field.value as string[]}
                                  typeAheadAriaLabel={t("resetActions")}
                                  onToggle={(_event, isOpen) =>
                                    setOpenOPClientRegistrationTypesSupported(
                                      isOpen,
                                    )
                                  }
                                  onSelect={(_event, selection) => {
                                    const selected =
                                      selection as ClientRegistrationTypesSupported;
                                    const current = (field.value ??
                                      []) as ClientRegistrationTypesSupported[];

                                    field.onChange(
                                      current.includes(selected)
                                        ? current.filter((v) => v !== selected)
                                        : [...current, selected],
                                    );
                                  }}
                                  onClear={() => {
                                    field.onChange([]);
                                    setOpenOPClientRegistrationTypesSupported(
                                      false,
                                    );
                                  }}
                                >
                                  {openIdFederationOPClientRegistrationTypesSupported.map(
                                    (name) => (
                                      <SelectOption
                                        key={name}
                                        value={name}
                                        data-testid={`${name}-option`}
                                      />
                                    ),
                                  )}
                                </Select>
                              )}
                            />
                            {errors
                              .openIdFederationOPClientRegistrationTypesSupported
                              ?.message && (
                              <FormErrorText
                                message={
                                  errors
                                    .openIdFederationOPClientRegistrationTypesSupported
                                    ?.message
                                }
                              />
                            )}
                          </FormGroup>
                        )}
                      </FormAccess>
                    </FormProvider>
                  ),
                },
                {
                  title: t("openIdRelyingPartySettings"),
                  panel: (
                    <FormProvider {...form}>
                      <FormAccess
                        isHorizontal
                        role="manage-realm"
                        className="pf-u-mt-lg"
                        onSubmit={handleSubmit(save)}
                      >
                        <FormGroup
                          hasNoPaddingTop
                          label={t("enableOpenIdRelyingParty")}
                          fieldId="kc-enableOpenIdRelyingParty"
                        >
                          <Controller
                            name="openIdFederationEntityTypes"
                            defaultValue={[]}
                            control={control}
                            render={({ field }) => (
                              <Switch
                                label={t("on")}
                                labelOff={t("off")}
                                isChecked={openIdFederationEntityTypes.includes(
                                  "OPENID_RELYING_PARTY",
                                )}
                                onChange={(_event, value) => {
                                  field.onChange(!value);
                                  if (value) {
                                    setValue("openIdFederationEntityTypes", [
                                      ...openIdFederationEntityTypes,
                                      "OPENID_RELYING_PARTY",
                                    ]);
                                  } else {
                                    setValue(
                                      "openIdFederationEntityTypes",
                                      openIdFederationEntityTypes.filter(
                                        (item) =>
                                          item !== "OPENID_RELYING_PARTY",
                                      ),
                                    );
                                  }
                                }}
                                aria-label={t("clientAuthentication")}
                              />
                            )}
                          />
                        </FormGroup>
                        {openIdFederationEntityTypes.includes(
                          "OPENID_RELYING_PARTY",
                        ) && (
                          <FormGroup
                            label={t(
                              "openIdFederationClientRegistrationTypesSupported",
                            )}
                            isRequired
                            labelIcon={
                              <HelpItem
                                helpText={t(
                                  "openIdFederationRPClientRegistrationTypesSupportedHelp",
                                )}
                                fieldLabelId="resetOpenIdFederationRPClientRegistrationTypesSupported"
                              />
                            }
                            fieldId="openIdFederationRPClientRegistrationTypesSupported"
                          >
                            <Controller
                              name="openIdFederationRPClientRegistrationTypesSupported"
                              defaultValue={
                                [] as ClientRegistrationTypesSupported[]
                              }
                              control={control}
                              rules={{
                                required: {
                                  value: true,
                                  message: t("required"),
                                },
                              }}
                              render={({ field }) => (
                                <Select
                                  variant={SelectVariant.typeaheadMulti}
                                  maxHeight={375}
                                  toggleId="openIdFederationRPClientRegistrationTypesSupported"
                                  chipGroupProps={{ numChips: 3 }}
                                  placeholderText={t(
                                    "clientRegistrationTypesSupportedPlaceholder",
                                  )}
                                  validated={
                                    errors.openIdFederationRPClientRegistrationTypesSupported
                                      ? ValidatedOptions.error
                                      : ValidatedOptions.default
                                  }
                                  menuAppendTo="parent"
                                  isOpen={
                                    openRPClientRegistrationTypesSupported
                                  }
                                  selections={field.value as string[]}
                                  typeAheadAriaLabel={t("resetActions")}
                                  onToggle={(_event, isOpen) =>
                                    setOpenRPClientRegistrationTypesSupported(
                                      isOpen,
                                    )
                                  }
                                  onSelect={(_event, selection) => {
                                    const selected =
                                      selection as ClientRegistrationTypesSupported;
                                    const current = (field.value ??
                                      []) as ClientRegistrationTypesSupported[];

                                    field.onChange(
                                      current.includes(selected)
                                        ? current.filter((v) => v !== selected)
                                        : [...current, selected],
                                    );
                                  }}
                                  onClear={() => {
                                    field.onChange([]);
                                    setOpenRPClientRegistrationTypesSupported(
                                      false,
                                    );
                                  }}
                                >
                                  {openIdFederationRPClientRegistrationTypesSupported.map(
                                    (name) => (
                                      <SelectOption
                                        key={name}
                                        value={name}
                                        data-testid={`${name}-option`}
                                      />
                                    ),
                                  )}
                                </Select>
                              )}
                            />

                            {errors
                              .openIdFederationRPClientRegistrationTypesSupported
                              ?.message && (
                              <FormErrorText
                                message={
                                  errors
                                    .openIdFederationRPClientRegistrationTypesSupported
                                    .message
                                }
                              />
                            )}
                          </FormGroup>
                        )}
                      </FormAccess>
                    </FormProvider>
                  ),
                },
              ]
            : []),
          ...(isOpenIdFederationEnabled
            ? [
                {
                  title: t("trustMarkSettings"),
                  panel: (
                    <TrustMarkTable
                      realm={realm.realm as string}
                      trustMarks={trustMarks}
                      setTrustMarks={setTrustMarks}
                    />
                  ),
                },
                {
                  title: t("openIdFederationSettings"),
                  panel: (
                    <KeycloakDataTable
                      key={openIdFederations.length} // simple re-render trigger; optional
                      ariaLabelKey="openIdFederationList"
                      loader={async () =>
                        openIdFederations as OpenIdFederationRepresentation[]
                      }
                      toolbarItem={
                        <ToolbarItem>
                          <Button
                            variant="primary"
                            data-testid="add-list-item"
                            onClick={() =>
                              navigate(
                                toOpenIdFederationCreate({
                                  realm: realm.realm as string,
                                  tab: "settings",
                                }),
                              )
                            }
                          >
                            {t("addTrustAnchor")}
                          </Button>
                        </ToolbarItem>
                      }
                      actionResolver={({ data }) => [
                        {
                          title: t("delete"),
                          onClick: () => {
                            setSelectedOpenIdFederation(data);
                            toggleDeleteDialog();
                          },
                        },
                      ]}
                      columns={[
                        {
                          name: "trustAnchor",
                          displayKey: t("trustAnchor"),
                          cellRenderer: (row) => (
                            <OpenIdFederationLink {...row} />
                          ),
                        },
                      ]}
                      emptyState={
                        <ListEmptyState
                          message={t("addOpenIdFederationWarning")}
                          instructions={t("addOpenIdFederationEmptyState")}
                          primaryActionText={t("add")}
                          onPrimaryAction={() =>
                            navigate(
                              toOpenIdFederationCreate({
                                realm: realm.realm as string,
                                tab: "settings",
                              }),
                            )
                          }
                        />
                      }
                    />
                  ),
                },
                {
                  title: t("advancedSettings"),
                  panel: (
                    <FormProvider {...form}>
                      <FormAccess
                        isHorizontal
                        role="manage-realm"
                        className="pf-u-mt-lg"
                        onSubmit={handleSubmit(save)}
                      >
                        <OpenIdFederationAdvancedSettings />
                      </FormAccess>
                    </FormProvider>
                  ),
                },
              ]
            : []),
        ]}
      />
      <FormAccess
        isHorizontal
        role="manage-realm"
        className="pf-u-mt-lg"
        onSubmit={handleSubmit(save)}
      >
        <FixedButtonsGroup name="idp-details" isSubmit reset={setupForm} />
      </FormAccess>
    </PageSection>
  );
};
