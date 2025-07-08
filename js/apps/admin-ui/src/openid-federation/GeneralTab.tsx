import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { useAlerts } from "../components/alert/Alerts";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  FormGroup,
  PageSection,
  Switch,
  ValidatedOptions,
  ToolbarItem,
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
import { convertToFormValues } from "../util";
import { toOpenIdFederationCreate } from "./routes/OpenIdFederationCreate";
import {
  KeycloakDataTable,
  Action,
} from "../components/table-toolbar/KeycloakDataTable";
import { useRealm } from "../context/realm-context/RealmContext";
import { toOpenIdFederationEdit } from "./routes/OpenIdFederationEdit";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import OpenIdFederationRepresentation, {
  ClientRegistrationTypesSupported,
  EntityTypesSupported,
} from "libs/keycloak-admin-client/lib/defs/OpenIdFederationRepresentation";
import { ScrollForm } from "../components/scroll-form/ScrollForm";

type OpenIdFederationGeneralTabProps = {
  realm: RealmRepresentation;
  openIdFederations?: OpenIdFederationRepresentation[];
  setOpenIdFederations: (
    openIdFederations: OpenIdFederationRepresentation[],
  ) => void;
  save: (realm: RealmRepresentation) => void;
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
      })}
    >
      {openIdFederation.trustAnchor}
    </Link>
  );
};

export const OpenIdFederationGeneralTab = ({
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
    formState: { isDirty, errors },
  } = form;
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();

  const [selectedOpenIdFederation, setSelectedOpenIdFederation] =
    useState<OpenIdFederationRepresentation>();
  const [isOpenIdFederationEnabled, setIsOpenIdFederationEnabled] = useState(
    !!realm.openIdFederationEnabled,
  );

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

  const openIdFederationEnabled = useWatch({
    control,
    name: "openIdFederationEnabled",
  }) as boolean;
  const setupForm = () => {
    convertToFormValues(realm, setValue);
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
                        label={t("openIdFederationHomepageUri")}
                        fieldId="kc-homepage-uri"
                      >
                        <KeycloakTextInput
                          id="kc-homepage-uri"
                          {...register("openIdFederationHomepageUri")}
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
                    </>
                  )}
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
                        {
                          name: "entityTypes",
                          displayKey: t("entityTypes"),
                          cellRenderer: (row) => {
                            const value: EntityTypesSupported[] | undefined =
                              row.entityTypes;
                            return value ? value.map((v) => v).join(", ") : "";
                          },
                        },
                        {
                          name: "clientRegistrationTypesSupported",
                          displayKey: t("clientRegistrationTypesSupported"),
                          cellRenderer: (row) => {
                            const value:
                              | ClientRegistrationTypesSupported[]
                              | undefined =
                              row.clientRegistrationTypesSupported;
                            return value ? value.map((v) => v).join(", ") : "";
                          },
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
    </PageSection>
  );
};
