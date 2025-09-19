import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { useAlerts } from "../components/alert/Alerts";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  FormGroup,
  PageSection,
  Switch,
  ValidatedOptions,
  ToolbarItem,
  Stack,
  StackItem,
  Select,
  SelectVariant,
  SelectOption,
} from "@patternfly/react-core";
import { adminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { Link, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";
import { MultiLineInput } from "../components/multi-line-input/MultiLineInput";
import { FormAccess } from "../components/form/FormAccess";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import { TimeSelector } from "../components/time-selector/TimeSelector";
import { addTrailingSlash, convertToFormValues } from "../util";
import { toOpenIdFederationCreate } from "./routes/OpenIdFederationCreate";
import {
  KeycloakDataTable,
  Action,
} from "../components/table-toolbar/KeycloakDataTable";
import { useRealm } from "../context/realm-context/RealmContext";
import { toOpenIdFederationEdit } from "./routes/OpenIdFederationEdit";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/openIdFederationRepresentation";
import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { FormattedLink } from "../components/external-link/FormattedLink";
import {
  ClientRegistrationTypesSupported,
  EntityTypesSupported,
} from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { FixedButtonsGroup } from "../components/form/FixedButtonGroup";

type OpenIdFederationGeneralTabProps = {
  realm: RealmRepresentation;
  openIdFederations?: OpenIdFederationRepresentation[];
  setOpenIdFederations: (
    openIdFederations: OpenIdFederationRepresentation[],
  ) => void;
  save: (realm: RealmRepresentation) => void;
};

const openIdFederationRPClientRegistrationTypesSupported: ClientRegistrationTypesSupported[] =
  ["EXPLICIT"];
const openIdFederationOPClientRegistrationTypesSupported: ClientRegistrationTypesSupported[] =
  ["EXPLICIT", "AUTOMATIC"];

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
}: OpenIdFederationGeneralTabProps) => {
  const { t } = useTranslation("openid-federation");
  const form = useForm<RealmRepresentation>();
  const {
    register,
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = form;
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();

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
    messageKey: t("deleteConfirm", {
      trustAnchor: selectedOpenIdFederation?.trustAnchor,
    }),
    continueButtonLabel: t("common:delete"),
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
        addAlert(
          t("openid-federations:deletedSuccessOpenIdFederation"),
          AlertVariant.success,
        );
      } catch (error) {
        addError(t("openid-federations:deletedErrorOpenIdFederation"), error);
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
  }) as boolean;
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
                        helpText={t(
                          "openid-federation-help:openIdFederationEnabled",
                        )}
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
                          label={t("common:on")}
                          labelOff={t("common:off")}
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
                        labelIcon={
                          <HelpItem
                            helpText={t(
                              "openid-federation-help:openIdFederationAuthorityHints",
                            )}
                            fieldLabelId="openid-federation:openIdFederationAuthorityHints"
                          />
                        }
                        validated={
                          errors["openIdFederationAuthorityHints"]?.message
                            ? ValidatedOptions.error
                            : ValidatedOptions.default
                        }
                        helperTextInvalid={
                          errors["openIdFederationAuthorityHints"]
                            ?.message as string
                        }
                      >
                        <MultiLineInput
                          id="kc-authority-hints"
                          name={"openIdFederationAuthorityHints"}
                          aria-label={t("openIdFederationAuthorityHints")}
                          addButtonLabel="openid-federation:addAuthorityHint"
                          validated={
                            errors["openIdFederationAuthorityHints"]?.message
                              ? ValidatedOptions.error
                              : ValidatedOptions.default
                          }
                          isRequired
                        />
                      </FormGroup>
                      <FormGroup
                        label={t("openIdFederationLifespan")}
                        fieldId="openIdFederationLifespan"
                        labelIcon={
                          <HelpItem
                            helpText={t(
                              "openid-federation-help:openIdFederationLifespan",
                            )}
                            fieldLabelId="openid-federation:openIdFederationLifespan"
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
                        labelIcon={
                          <HelpItem
                            helpText={t(
                              "openid-federation-help:openIdFederationContacts",
                            )}
                            fieldLabelId="openIdFederationContacts"
                          />
                        }
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
                      <FormGroup
                        label={t("openIdFederationLogoUri")}
                        fieldId="kc-logo-uri"
                      >
                        <KeycloakTextInput
                          id="kc-logo-uri"
                          {...register("openIdFederationLogoUri")}
                        />
                      </FormGroup>
                      <FormGroup
                        label={t("openIdFederationPolicyUri")}
                        fieldId="kc-poliicy-uri"
                      >
                        <KeycloakTextInput
                          id="kc-poliicy-uri"
                          {...register("openIdFederationPolicyUri")}
                        />
                      </FormGroup>
                      <FormGroup
                        label={t("openIdFederationOrganizationName")}
                        fieldId="kc-organization-name"
                      >
                        <KeycloakTextInput
                          id="kc-organization-name"
                          {...register("openIdFederationOrganizationName")}
                        />
                      </FormGroup>
                      <FormGroup
                        label={t("openIdFederationOrganizationUri")}
                        fieldId="kc-homepage-uri"
                      >
                        <KeycloakTextInput
                          id="kc-homepage-uri"
                          {...register("openIdFederationOrganizationUri")}
                        />
                      </FormGroup>
                      <FormGroup
                        label={t("openIdFederationResolveEndpoint")}
                        fieldId="kc-resolve-endpoint"
                      >
                        <KeycloakTextInput
                          id="kc-resolve-endpoint"
                          {...register("openIdFederationResolveEndpoint")}
                        />
                      </FormGroup>
                      <FormGroup
                        label={t("openIdFederationHistoricalKeysEndpoint")}
                        fieldId="kc-historical-keys-endpoint"
                      >
                        <KeycloakTextInput
                          id="kc-historical-keys-endpoint"
                          {...register(
                            "openIdFederationHistoricalKeysEndpoint",
                          )}
                        />
                      </FormGroup>
                      <FormGroup
                        label={t("endpoint")}
                        labelIcon={
                          <HelpItem
                            helpText={t("openid-federation-help:endpoint")}
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
                          label={t("common:on")}
                          labelOff={t("common:off")}
                          isChecked={openIdFederationEntityTypes.includes(
                            "OPENID_PROVIDER",
                          )}
                          onChange={(value) => {
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
                  {openIdFederationEntityTypes.includes("OPENID_PROVIDER") && (
                    <FormGroup
                      label={t(
                        "openIdFederationClientRegistrationTypesSupported",
                      )}
                      isRequired
                      labelIcon={
                        <HelpItem
                          helpText={t(
                            "openid-federation-help:openIdFederationOPClientRegistrationTypesSupported",
                          )}
                          fieldLabelId="resetopenIdFederationOPClientRegistrationTypesSupported"
                        />
                      }
                      validated={
                        errors.openIdFederationOPClientRegistrationTypesSupported
                          ? ValidatedOptions.error
                          : ValidatedOptions.default
                      }
                      helperTextInvalid={t("common:required")}
                      fieldId="types-supported"
                    >
                      <Controller
                        name={`openIdFederationOPClientRegistrationTypesSupported`}
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
                              "openIdFederationOPClientRegistrationTypesSupported"
                            }
                            variant={SelectVariant.typeaheadMulti}
                            chipGroupProps={{
                              numChips: 3,
                            }}
                            placeholderText={t(
                              "clientRegistrationTypesSupportedPlaceholder",
                            )}
                            menuAppendTo="parent"
                            onToggle={(open) =>
                              setOpenOPClientRegistrationTypesSupported(open)
                            }
                            isOpen={openOPClientRegistrationTypesSupported}
                            selections={field.value as string[]}
                            onSelect={(_, selectedValue) => {
                              const value:
                                | ClientRegistrationTypesSupported[]
                                | undefined = field.value;
                              field.onChange(
                                value?.find((item) => item === selectedValue)
                                  ? value.filter(
                                      (item) => item !== selectedValue,
                                    )
                                  : [...(value ? value : []), selectedValue],
                              );
                            }}
                            onClear={(event) => {
                              event.stopPropagation();
                              field.onChange([]);
                            }}
                            typeAheadAriaLabel={t("resetActions")}
                          >
                            {openIdFederationOPClientRegistrationTypesSupported.map(
                              (name) => (
                                <SelectOption
                                  key={name}
                                  value={name}
                                  data-testid={`${name}-option`}
                                >
                                  {name}
                                </SelectOption>
                              ),
                            )}
                          </Select>
                        )}
                      />
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
                          label={t("common:on")}
                          labelOff={t("common:off")}
                          isChecked={openIdFederationEntityTypes.includes(
                            "OPENID_RELYING_PARTY",
                          )}
                          onChange={(value) => {
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
                                  (item) => item !== "OPENID_RELYING_PARTY",
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
                            "openid-federation-help:openIdFederationRPClientRegistrationTypesSupported",
                          )}
                          fieldLabelId="resetOpenIdFederationRPClientRegistrationTypesSupported"
                        />
                      }
                      validated={
                        errors.openIdFederationRPClientRegistrationTypesSupported
                          ? ValidatedOptions.error
                          : ValidatedOptions.default
                      }
                      helperTextInvalid={t("common:required")}
                      fieldId="types-supported"
                    >
                      <Controller
                        name={`openIdFederationRPClientRegistrationTypesSupported`}
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
                              "openIdFederationList.openIdFederationOPClientRegistrationTypesSupported"
                            }
                            variant={SelectVariant.typeaheadMulti}
                            chipGroupProps={{
                              numChips: 3,
                            }}
                            placeholderText={t(
                              "clientRegistrationTypesSupportedPlaceholder",
                            )}
                            menuAppendTo="parent"
                            onToggle={(open) =>
                              setOpenRPClientRegistrationTypesSupported(open)
                            }
                            isOpen={openRPClientRegistrationTypesSupported}
                            selections={field.value as string[]}
                            onSelect={(_, selectedValue) => {
                              const value:
                                | ClientRegistrationTypesSupported[]
                                | undefined = field.value;
                              field.onChange(
                                value?.find((item) => item === selectedValue)
                                  ? value.filter(
                                      (item) => item !== selectedValue,
                                    )
                                  : [...(value ? value : []), selectedValue],
                              );
                            }}
                            onClear={(event) => {
                              event.stopPropagation();
                              field.onChange([]);
                            }}
                            typeAheadAriaLabel={t("resetActions")}
                          >
                            {openIdFederationRPClientRegistrationTypesSupported.map(
                              (name) => (
                                <SelectOption
                                  key={name}
                                  value={name}
                                  data-testid={`${name}-option`}
                                >
                                  {name}
                                </SelectOption>
                              ),
                            )}
                          </Select>
                        )}
                      />
                    </FormGroup>
                  )}
                </FormAccess>
              </FormProvider>
            ),
          },
          // Only include the Trust Anchor Settings section if enabled
          ...(isOpenIdFederationEnabled
            ? [
                {
                  title: t("openIdFederationSettings"),
                  panel: (
                    <KeycloakDataTable
                      ariaLabelKey="openIdFederationList"
                      loader={
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
                      actions={[
                        {
                          title: t("common:delete"),
                          onRowClick: (openIdFederation) => {
                            setSelectedOpenIdFederation(openIdFederation);
                            toggleDeleteDialog();
                          },
                        } as Action<OpenIdFederationRepresentation>,
                      ]}
                      columns={[
                        {
                          name: "trustAnchor",
                          cellRenderer: (row) => (
                            <OpenIdFederationLink {...row} />
                          ),
                          displayKey: t("trustAnchor"),
                        },
                      ]}
                      emptyState={
                        <ListEmptyState
                          message={t("addOpenIdFederationWarning")}
                          instructions={t("addOpenIdFederationEmptyState")}
                          primaryActionText={t("common:add")}
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
