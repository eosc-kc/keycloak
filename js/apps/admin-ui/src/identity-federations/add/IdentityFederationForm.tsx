import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";
import type IdentityFederationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityFederationRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Tab,
  TabTitleText,
  ToolbarItem,
  Spinner,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useState, useEffect } from "react";
import { useFormContext } from "react-hook-form";
import { FormAccess } from "../../components/form/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";
import { Link, useParams, useNavigate } from "react-router-dom";
import { toIdentityFederations } from "../routes/IdentityFederations";
import style from "../../components/form/fixed-buttons.module.css";
import IdentityProviderFederationConfig from "./IdentityFederationConfig";
import GeneralSettings from "./GeneralSettings";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import {
  IdentityFederationParams,
  IdentityFederationTab,
  toIdentityFederation,
} from "../routes/IdentityFederation";
import { toIdentityFederationAddMapper } from "../routes/AddMapper";
import { toIdentityFederationEditMapper } from "../routes/EditMapper";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import AllowDenyList from "./AllowDenyList";
import {
  useAlerts,
  ScrollForm,
  ListEmptyState,
  KeycloakDataTable,
  Action,
} from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";

type IdentityFederationFormProps = {
  onSubmit: any;
  type: string;
  internalId?: string;
  providerId: string;
  tab?: string;
};

type IdPWithMapperAttributes = IdentityProviderMapperRepresentation & {
  name: string;
  category?: string;
  helpText?: string;
  type: string;
  providerId: string;
  mapperId: string;
};

type MapperLinkProps = IdPWithMapperAttributes;

const MapperLink = ({ name, mapperId, providerId }: MapperLinkProps) => {
  const { realm } = useRealm();
  const { internalId } = useParams<IdentityFederationParams>();

  return (
    <Link
      to={toIdentityFederationEditMapper({
        realm,
        internalId: internalId!,
        providerId: providerId,
        id: mapperId,
      })}
    >
      {name}
    </Link>
  );
};

export default function IdentityFederationForm({
  onSubmit,
  type,
  internalId,
  providerId,
}: IdentityFederationFormProps) {
  const form = useFormContext<IdentityFederationRepresentation>();
  const {
    handleSubmit,
    watch,
    formState: { isDirty },
  } = form;
  const { t } = useTranslation();
  const category = watch("category") as unknown as string;
  const { realm } = useRealm();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const [mapperAction, setmapperAction] = useState("");
  const { addAlert, addError } = useAlerts();
  const { adminClient } = useAdminClient();

  const toTab = (tab: IdentityFederationTab) =>
    toIdentityFederation({
      realm,
      providerId: providerId,
      internalId: internalId || "",
      tab,
    });

  const useTab = (tab: IdentityFederationTab) => useRoutableTab(toTab(tab));
  const settingsTab = useTab("settings");
  const mappersTab = useTab("mappers");
  const [selectedMapper, setSelectedMapper] =
    useState<IdPWithMapperAttributes>();
  const navigate = useNavigate();
  const [mapperLoading, setMapperLoading] = useState("");

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mapperLoading]);

  const [toggleConfirmMapperDialog, MapperConfirm] = useConfirmDialog({
    titleKey: `${mapperAction}Mapper`,
    messageKey: t(`${mapperAction}MapperConfirm`, {
      mapper: selectedMapper?.name,
    }),
    continueButtonLabel: mapperAction,
    continueButtonVariant:
      mapperAction === "update"
        ? ButtonVariant.warning
        : mapperAction === "add"
          ? ButtonVariant.primary
          : ButtonVariant.danger,
    onConfirm: async () => {
      try {
        setMapperLoading(selectedMapper?.mapperId!);
        await adminClient.identityFederations.updateFederationMapper({
          internalId: internalId!,
          action: mapperAction,
          mapperId: selectedMapper?.mapperId!,
        });
        setMapperLoading("");
        addAlert(t(`${mapperAction}MapperSuccess`), AlertVariant.success);
        navigate(
          toIdentityFederation({
            providerId,
            internalId: internalId!,
            tab: "mappers",
            realm,
          }),
        );
      } catch (error) {
        setMapperLoading("");
        addError("deleteErrorError", error);
      }
    },
  });

  const loader = async () => {
    const [loaderMappers, loaderMapperTypes] = await Promise.all([
      adminClient.identityFederations.findMappers({ internalId: internalId! }),
      adminClient.identityFederations.findMapperTypes(),
    ]);
    const components = loaderMappers.map((loaderMapper) => {
      const mapperType: any = Object.values(loaderMapperTypes).find(
        (loaderMapperType: any) =>
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
      title: "General Settings",
      panel: (
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(onSubmit)}
        >
          <GeneralSettings type={type} />
        </FormAccess>
      ),
    },
    {
      title: t("identityProvidersFederation"),
      isHidden: category === "Clients",
      panel: (
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(onSubmit)}
        >
          <IdentityProviderFederationConfig type={type} />
        </FormAccess>
      ),
    },
    {
      title: t("allowList"),
      panel: (
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(onSubmit)}
        >
          <AllowDenyList type="Allow" />
        </FormAccess>
      ),
    },
    {
      title: t("denyList"),
      panel: (
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(onSubmit)}
        >
          <AllowDenyList type="Deny" />
        </FormAccess>
      ),
    },
  ];

  return (
    <>
      <RoutableTabs isBox defaultLocation={toTab("settings")}>
        <Tab
          id="settings"
          title={<TabTitleText>{t("settings")}</TabTitleText>}
          {...settingsTab}
        >
          <ScrollForm
            label={t("jumpToSection")}
            className="pf-u-px-lg"
            sections={sections}
          />
        </Tab>
        {!!internalId && (
          <Tab
            id="mappers"
            data-testid="mappers-tab"
            title={<TabTitleText>{t("mappers")}</TabTitleText>}
            {...mappersTab}
          >
            <MapperConfirm />
            <KeycloakDataTable
              emptyState={
                <ListEmptyState
                  message={t("noMappers")}
                  instructions={t("noMappersInstructions")}
                  primaryActionText={t("addMapper")}
                  onPrimaryAction={() =>
                    navigate(
                      toIdentityFederationAddMapper({
                        realm,
                        internalId: internalId!,
                        providerId: providerId!,
                        tab: "mappers",
                      }),
                    )
                  }
                />
              }
              loader={loader}
              key={key}
              ariaLabelKey="mappersList"
              searchPlaceholderKey="searchForMapper"
              toolbarItem={
                <ToolbarItem>
                  <Button
                    id="add-mapper-button"
                    component={(props) => (
                      <Link
                        {...props}
                        to={toIdentityFederationAddMapper({
                          realm,
                          internalId: internalId!,
                          providerId: providerId!,
                          tab: "mappers",
                        })}
                      />
                    )}
                    data-testid="addMapper"
                  >
                    {t("createMapper")}
                  </Button>
                </ToolbarItem>
              }
              columns={[
                {
                  name: "name",
                  displayKey: "name",
                  cellRenderer: (row) => (
                    <MapperLink {...row} providerId={providerId} />
                  ),
                },
                {
                  name: "category",
                  displayKey: "category",
                },
                {
                  name: "type",
                  displayKey: "type",
                },
                {
                  name: "",
                  cellRenderer: (row) =>
                    mapperLoading === row.mapperId ? (
                      <Spinner isInline size="lg" />
                    ) : (
                      ""
                    ),
                },
              ]}
              actions={[
                ...(!mapperLoading
                  ? [
                      {
                        title: t("addMapperFederation"),
                        onRowClick: (mapper) => {
                          setSelectedMapper(mapper);
                          setmapperAction("add");
                          toggleConfirmMapperDialog();
                        },
                      } as Action<IdPWithMapperAttributes>,
                      {
                        title: t("updateMapper"),
                        onRowClick: (mapper) => {
                          setSelectedMapper(mapper);
                          setmapperAction("update");
                          toggleConfirmMapperDialog();
                        },
                      } as Action<IdPWithMapperAttributes>,
                      {
                        title: t("removeMapper"),
                        onRowClick: (mapper) => {
                          setSelectedMapper(mapper);
                          setmapperAction("remove");
                          toggleConfirmMapperDialog();
                        },
                      } as Action<IdPWithMapperAttributes>,
                    ]
                  : []),
              ]}
            />
          </Tab>
        )}
      </RoutableTabs>
      <FormAccess
        role="manage-identity-providers"
        isHorizontal
        onSubmit={handleSubmit(onSubmit)}
      >
        <ActionGroup className={style.buttonGroup}>
          <Button isDisabled={!isDirty} variant="primary" type="submit">
            {type === "edit" ? t("save") : t("add")}
          </Button>
          <Button
            variant="link"
            data-testid="cancel"
            component={(props) => (
              <Link {...props} to={toIdentityFederations({ realm })} />
            )}
          >
            {t("cancel")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </>
  );
}
