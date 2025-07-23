import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";
import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Divider,
  DropdownItem,
  Form,
  PageSection,
  Tab,
  TabTitleText,
  ToolbarItem,
} from "@patternfly/react-core";
import { useMemo, useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { FixedButtonsGroup } from "../../components/form/FixedButtonGroup";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { PermissionsTab } from "../../components/permission-tab/PermissionTab";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import { ScrollForm } from "../../components/scroll-form/ScrollForm";
import {
  Action,
  KeycloakDataTable,
} from "../../components/table-toolbar/KeycloakDataTable";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { toUpperCase } from "../../util";
import { useFetch } from "../../utils/useFetch";
import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";
import { useParams } from "../../utils/useParams";
import { toIdentityProviderAddMapper } from "../routes/AddMapper";
import { toIdentityProviderEditMapper } from "../routes/EditMapper";
import {
  IdentityProviderParams,
  IdentityProviderTab,
  toIdentityProvider,
} from "../routes/IdentityProvider";
import { toIdentityProviders } from "../routes/IdentityProviders";
import { AdvancedSettings } from "./AdvancedSettings";
import { DescriptorSettings } from "./DescriptorSettings";
import { DiscoverySettings } from "./DiscoverySettings";
import { ExtendedNonDiscoverySettings } from "./ExtendedNonDiscoverySettings";
import { GeneralSettings } from "./GeneralSettings";
import { OIDCAuthentication } from "./OIDCAuthentication";
import { OIDCGeneralSettings } from "./OIDCGeneralSettings";
import { ReqAuthnConstraints } from "./ReqAuthnConstraintsSettings";
import { SamlGeneralSettings } from "./SamlGeneralSettings";
import { OpenIdFederationSettings } from "./OpenIdFederationSettings";

type HeaderProps = {
  onChange: (value: boolean) => void;
  value: boolean;
  save: () => void;
  toggleDeleteDialog: () => void;
  toggleRefreshDialog: () => void;
  refreshEnabled: boolean;
};

type IdPWithMapperAttributes = IdentityProviderMapperRepresentation & {
  name: string;
  category?: string;
  helpText?: string;
  type: string;
  mapperId: string;
};

const Header = ({
  onChange,
  value,
  save,
  toggleDeleteDialog,
  toggleRefreshDialog,
  refreshEnabled,
}: HeaderProps) => {
  const { t } = useTranslation("identity-providers");
  const { alias: displayName } = useParams<{ alias: string }>();
  const [provider, setProvider] = useState<IdentityProviderRepresentation>();

  useFetch(
    () => adminClient.identityProviders.findOne({ alias: displayName }),
    (fetchedProvider) => {
      if (!fetchedProvider) {
        throw new Error(t("common:notFound"));
      }
      setProvider(fetchedProvider);
    },
    [],
  );

  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: "identity-providers:disableProvider",
    messageKey: t("disableConfirm", { provider: displayName }),
    continueButtonLabel: "common:disable",
    onConfirm: () => {
      onChange(!value);
      save();
    },
  });

  return (
    <>
      <DisableConfirm />
      <ViewHeader
        titleKey={toUpperCase(
          provider
            ? provider.displayName
              ? provider.displayName
              : provider.providerId!
            : "",
        )}
        divider={false}
        dropdownItems={[
          <DropdownItem key="delete" onClick={() => toggleDeleteDialog()}>
            {t("common:delete")}
          </DropdownItem>,
          ...(refreshEnabled
            ? [
                <DropdownItem
                  key="refresh"
                  onClick={() => toggleRefreshDialog()}
                >
                  {t("common:refresh")}
                </DropdownItem>,
              ]
            : []),
        ]}
        isEnabled={value}
        onToggle={(value) => {
          if (!value) {
            toggleDisableDialog();
          } else {
            onChange(value);
            save();
          }
        }}
      />
    </>
  );
};

type MapperLinkProps = IdPWithMapperAttributes & {
  provider?: IdentityProviderRepresentation;
};

const MapperLink = ({ name, mapperId, provider }: MapperLinkProps) => {
  const { realm } = useRealm();
  const { alias } = useParams<IdentityProviderParams>();

  return (
    <Link
      to={toIdentityProviderEditMapper({
        realm,
        alias,
        providerId: provider?.providerId!,
        id: mapperId,
      })}
    >
      {name}
    </Link>
  );
};

export default function DetailSettings() {
  const { t } = useTranslation("identity-providers");
  const { alias, providerId } = useParams<IdentityProviderParams>();
  const isFeatureEnabled = useIsFeatureEnabled();
  const form = useForm<IdentityProviderRepresentation>();
  const { handleSubmit, getValues, reset } = form;
  const [provider, setProvider] = useState<IdentityProviderRepresentation>();
  const [refreshEnabled, setRefreshEnabled] = useState<boolean>(false);
  const [selectedMapper, setSelectedMapper] =
    useState<IdPWithMapperAttributes>();
  const serverInfo = useServerInfo();
  const providerInfo = useMemo(() => {
    const namespaces = [
      "org.keycloak.broker.social.SocialIdentityProvider",
      "org.keycloak.broker.provider.IdentityProvider",
    ];

    for (const namespace of namespaces) {
      const social = serverInfo.componentTypes?.[namespace]?.find(
        ({ id }) => id === providerId,
      );

      if (social) {
        return social;
      }
    }
  }, [serverInfo, providerId]);

  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { realm } = useRealm();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);

  useFetch(
    () => adminClient.identityProviders.findOne({ alias }),
    (fetchedProvider) => {
      if (!fetchedProvider) {
        throw new Error(t("common:notFound"));
      }
      reset(fetchedProvider);
      setProvider(fetchedProvider);
      setRefreshEnabled(fetchedProvider.config!.metadataUrl);

      if (fetchedProvider.config!.authnContextClassRefs) {
        form.setValue(
          "config.authnContextClassRefs",
          JSON.parse(fetchedProvider.config?.authnContextClassRefs),
        );
      }

      if (fetchedProvider.config!.authnContextDeclRefs) {
        form.setValue(
          "config.authnContextDeclRefs",
          JSON.parse(fetchedProvider.config?.authnContextDeclRefs),
        );
      }
    },
    [key],
  );

  const toTab = (tab: IdentityProviderTab) =>
    toIdentityProvider({
      realm,
      alias,
      providerId,
      tab,
    });

  const useTab = (tab: IdentityProviderTab) => useRoutableTab(toTab(tab));

  const settingsTab = useTab("settings");
  const mappersTab = useTab("mappers");
  const permissionsTab = useTab("permissions");

  const save = async (savedProvider?: IdentityProviderRepresentation) => {
    const p = savedProvider || getValues();
    // Remove nameIDPolicyFormat if it is null or undefined
    if (p.config?.nameIDPolicyFormat === "isNull") {
      delete p.config.nameIDPolicyFormat;
      delete provider?.config?.nameIDPolicyFormat;
    }
    if (p.config?.authnContextClassRefs)
      p.config.authnContextClassRefs = JSON.stringify(
        p.config.authnContextClassRefs,
      );
    if (p.config?.authnContextDeclRefs)
      p.config.authnContextDeclRefs = JSON.stringify(
        p.config.authnContextDeclRefs,
      );

    try {
      await adminClient.identityProviders.update(
        { alias },
        {
          ...p,
          config: { ...provider?.config, ...p.config },
          alias,
          providerId,
        },
      );
      addAlert(t("updateSuccess"), AlertVariant.success);
      refresh();
    } catch (error) {
      addError("identity-providers:updateError", error);
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("identity-providers:deleteProvider"),
    messageKey: t("identity-providers:deleteConfirm", { provider: alias }),
    continueButtonLabel: t("common:delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.identityProviders.del({ alias: alias });
        addAlert(t("deletedSuccess"), AlertVariant.success);
        navigate(toIdentityProviders({ realm }));
      } catch (error) {
        addError(t("identity-providers:deleteError"), error);
      }
    },
  });

  const [toggleRefreshDialog, RefreshConfirm] = useConfirmDialog({
    titleKey: t("identity-providers:refreshProvider"),
    messageKey: t("identity-providers:refreshConfirm", { provider: alias }),
    continueButtonLabel: t("common:refresh"),
    continueButtonVariant: ButtonVariant.primary,
    onConfirm: async () => {
      try {
        await adminClient.identityProviders.refresh({ alias: alias });
        addAlert(t("refreshSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError(t("identity-providers:refreshError"), error);
      }
    },
  });

  const [toggleDeleteMapperDialog, DeleteMapperConfirm] = useConfirmDialog({
    titleKey: t("identity-providers:deleteProviderMapper"),
    messageKey: t("identity-providers:deleteMapperConfirm", {
      mapper: selectedMapper?.name,
    }),
    continueButtonLabel: t("common:delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.identityProviders.delMapper({
          alias: alias,
          id: selectedMapper?.mapperId!,
        });
        addAlert(t("deleteMapperSuccess"), AlertVariant.success);
        refresh();
        navigate(
          toIdentityProvider({ providerId, alias, tab: "mappers", realm }),
        );
      } catch (error) {
        addError(t("identity-providers:deleteMapperError"), error);
      }
    },
  });

  if (!provider) {
    return <KeycloakSpinner />;
  }

  const isOIDC =
    provider.providerId!.includes("oidc") ||
    provider.providerId!.includes("openid-federation");
  const isSAML = provider.providerId!.includes("saml");

  const loader = async () => {
    const [loaderMappers, loaderMapperTypes] = await Promise.all([
      adminClient.identityProviders.findMappers({ alias }),
      adminClient.identityProviders.findMapperTypes({ alias }),
    ]);

    const components = loaderMappers.map((loaderMapper) => {
      const mapperType = Object.values(loaderMapperTypes).find(
        (loaderMapperType) =>
          loaderMapper.identityProviderMapper! === loaderMapperType.id!,
      );

      const result: IdPWithMapperAttributes = {
        ...mapperType,
        name: loaderMapper.name!,
        type: mapperType?.name!,
        mapperId: loaderMapper.id!,
      };

      return result;
    });

    return components;
  };

  const sections = [
    {
      title: t("generalSettings"),
      panel: (
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(save)}
        >
          {!isOIDC && !isSAML && (
            <>
              <GeneralSettings create={false} id={alias} />
              {providerInfo && (
                <DynamicComponents
                  stringify
                  properties={providerInfo.properties}
                />
              )}
            </>
          )}
          {isOIDC && <OIDCGeneralSettings id={alias} />}
          {isSAML && <SamlGeneralSettings id={alias} isAliasReadonly />}
        </FormAccess>
      ),
    },
    {
      title: t("openIdFederationSettings"),
      isHidden: !provider.providerId!.includes("openid-federation"),
      panel: <OpenIdFederationSettings readOnly={true} />,
    },
    {
      title: t("oidcSettings"),
      isHidden: !isOIDC,
      panel: (
        <>
          <DiscoverySettings readOnly={false} />
          <Form isHorizontal className="pf-u-py-lg">
            <Divider />
            <OIDCAuthentication create={false} />
          </Form>
          <ExtendedNonDiscoverySettings />
        </>
      ),
    },
    {
      title: t("samlSettings"),
      isHidden: !isSAML,
      panel: <DescriptorSettings readOnly={false} />,
    },
    {
      title: t("reqAuthnConstraints"),
      isHidden: !isSAML,
      panel: (
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(save)}
        >
          <ReqAuthnConstraints />
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
          <AdvancedSettings isOIDC={isOIDC!} isSAML={isSAML!} />

          <FixedButtonsGroup name="idp-details" isSubmit reset={reset} />
        </FormAccess>
      ),
    },
  ];

  return (
    <FormProvider {...form}>
      <DeleteConfirm />
      <RefreshConfirm />
      <DeleteMapperConfirm />
      <Controller
        name="enabled"
        control={form.control}
        defaultValue={true}
        render={({ field }) => (
          <Header
            value={field.value || false}
            onChange={field.onChange}
            save={save}
            toggleDeleteDialog={toggleDeleteDialog}
            toggleRefreshDialog={toggleRefreshDialog}
            refreshEnabled={refreshEnabled}
          />
        )}
      />

      <PageSection variant="light" className="pf-u-p-0">
        <RoutableTabs isBox defaultLocation={toTab("settings")}>
          <Tab
            id="settings"
            title={<TabTitleText>{t("common:settings")}</TabTitleText>}
            {...settingsTab}
          >
            <ScrollForm className="pf-u-px-lg" sections={sections} />
          </Tab>
          <Tab
            id="mappers"
            data-testid="mappers-tab"
            title={<TabTitleText>{t("common:mappers")}</TabTitleText>}
            {...mappersTab}
          >
            <KeycloakDataTable
              emptyState={
                <ListEmptyState
                  message={t("identity-providers:noMappers")}
                  instructions={t("identity-providers:noMappersInstructions")}
                  primaryActionText={t("identity-providers:addMapper")}
                  onPrimaryAction={() =>
                    navigate(
                      toIdentityProviderAddMapper({
                        realm,
                        alias: alias!,
                        providerId: provider.providerId!,
                        tab: "mappers",
                      }),
                    )
                  }
                />
              }
              loader={loader}
              key={key}
              ariaLabelKey="identity-providers:mappersList"
              searchPlaceholderKey="identity-providers:searchForMapper"
              toolbarItem={
                <ToolbarItem>
                  <Button
                    id="add-mapper-button"
                    component={(props) => (
                      <Link
                        {...props}
                        to={toIdentityProviderAddMapper({
                          realm,
                          alias: alias!,
                          providerId: provider.providerId!,
                          tab: "mappers",
                        })}
                      />
                    )}
                    data-testid="addMapper"
                  >
                    {t("addMapper")}
                  </Button>
                </ToolbarItem>
              }
              columns={[
                {
                  name: "name",
                  displayKey: "common:name",
                  cellRenderer: (row) => (
                    <MapperLink {...row} provider={provider} />
                  ),
                },
                {
                  name: "category",
                  displayKey: "common:category",
                },
                {
                  name: "type",
                  displayKey: "common:type",
                },
              ]}
              actions={[
                {
                  title: t("common:delete"),
                  onRowClick: (mapper) => {
                    setSelectedMapper(mapper);
                    toggleDeleteMapperDialog();
                  },
                } as Action<IdPWithMapperAttributes>,
              ]}
            />
          </Tab>
          {isFeatureEnabled(Feature.AdminFineGrainedAuthz) && (
            <Tab
              id="permissions"
              data-testid="permissionsTab"
              title={<TabTitleText>{t("common:permissions")}</TabTitleText>}
              {...permissionsTab}
            >
              <PermissionsTab id={alias} type="identityProviders" />
            </Tab>
          )}
        </RoutableTabs>
      </PageSection>
    </FormProvider>
  );
}
