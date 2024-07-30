import {
  AlertVariant,
  ButtonVariant,
  Text,
  Dropdown,
  DropdownItem,
  DropdownToggle,
  PageSection,
  TextContent,
  TextVariants,
  ToolbarItem,
  Gallery,
  CardTitle,
  Split,
  SplitItem,
  Spinner,
} from "@patternfly/react-core";
import { useState } from "react";
import { useAlerts } from "../components/alert/Alerts";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useFetch } from "../utils/useFetch";
import { adminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { toIdentityFederationCreate } from "./routes/IdentityFederationCreate";
import { useRealm } from "../context/realm-context/RealmContext";
import {
  Action,
  KeycloakDataTable,
} from "../components/table-toolbar/KeycloakDataTable";
import { toIdentityFederation } from "./routes/IdentityFederation";
import type IdentityFederationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityFederationRepresentation";
import { ClickableCard } from "../components/keycloak-card/ClickableCard";
import { IconMapper } from "ui-shared";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";

const DetailLink = (identityFederation: any) => {
  const { realm } = useRealm();

  return (
    <Link
      key={identityFederation.providerId}
      to={toIdentityFederation({
        realm,
        providerId: identityFederation.providerId!,
        internalId: identityFederation.internalId,
        tab: "settings",
      })}
    >
      {identityFederation.alias}
    </Link>
  );
};

export default function IdentityFederationsSection() {
  const [identityFederations, setIdentityFederations] = useState<
    IdentityFederationRepresentation[]
  >([]);
  const [addIdentityFederationOpen, setAddIdentityFederationOpen] =
    useState(false);
  const [selectedFederation, setSelectedFederation] =
    useState<IdentityFederationRepresentation>();
  const { t } = useTranslation("identity-federations");
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  useFetch(
    async () => {
      setLoading(true);
      const federations = await adminClient.identityFederations.find();
      setLoading(false);
      return federations;
    },
    (federations) => {
      setIdentityFederations(federations);
    },
    [],
  );

  const federationOptions = [
    <DropdownItem
      key="link"
      component={
        <Link
          to={toIdentityFederationCreate({
            providerId: "saml",
            realm,
            tab: "settings",
          })}
        >
          {t("identity-federations:samlFederationV2")}
        </Link>
      }
    />,
  ];

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteFederation"),
    messageKey: t("deleteConfirm", {
      identityFederation: selectedFederation?.alias,
    }),
    continueButtonLabel: t("common:delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        setLoading(true);
        await adminClient.identityFederations.del({
          id: selectedFederation!.internalId!,
        });
        setIdentityFederations([
          ...identityFederations!.filter(
            (p) => p.internalId !== selectedFederation?.internalId,
          ),
        ]);
        setLoading(false);
        refresh();
        addAlert(
          t("identity-federations:deletedSuccessIdentityFederation"),
          AlertVariant.success,
        );
      } catch (error) {
        setLoading(false);
        addError(
          t("identity-federations:deletedErrorIdentityFederation"),
          error,
        );
      }
    },
  });

  const setRefreshing = (value: boolean) => {
    setIdentityFederations(
      identityFederations!.map((federation) =>
        federation.internalId === selectedFederation?.internalId
          ? { ...federation, refreshing: value }
          : federation,
      ),
    );
  };

  const [toggleRefreshDialog, RefreshConfirm] = useConfirmDialog({
    titleKey: t("refreshFederation"),
    messageKey: t("refreshConfirm", {
      identityFederation: selectedFederation?.alias,
    }),
    continueButtonLabel: t("refresh"),
    continueButtonVariant: ButtonVariant.primary,
    onConfirm: async () => {
      try {
        setRefreshing(true);
        await adminClient.identityFederations.refresh({
          id: selectedFederation!.internalId!,
        });
        setRefreshing(false);
        refresh();
        addAlert(
          t("identity-federations:refreshSuccessIdentityFederation"),
          AlertVariant.success,
        );
      } catch (error) {
        setRefreshing(false);
        addError(
          t("identity-federations:refreshErrorIdentityFederation"),
          error,
        );
      }
    },
  });

  const formatDate = (timestamp: any) => {
    // Create a new Date object
    const date = new Date(timestamp);

    // Get the individual components of the date
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0"); // Months are zero-based
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    const seconds = String(date.getSeconds()).padStart(2, "0");

    // Format the date as a string
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  };

  const formatTime = (minutes: any) => {
    return String(minutes);
  };

  return (
    <>
      <DeleteConfirm />
      <RefreshConfirm />
      <ViewHeader titleKey={t("identity-federations:identityFederations")} />
      <PageSection variant="light">
        {loading ? (
          <KeycloakSpinner />
        ) : identityFederations.length !== 0 ? (
          <KeycloakDataTable
            ariaLabelKey="identityFederations"
            loader={identityFederations}
            toolbarItem={
              <ToolbarItem>
                <Dropdown
                  toggle={
                    <DropdownToggle
                      onToggle={() =>
                        setAddIdentityFederationOpen(!addIdentityFederationOpen)
                      }
                      isPrimary
                    >
                      {t("identity-federations:addIdentityFederation")}
                    </DropdownToggle>
                  }
                  isOpen={addIdentityFederationOpen}
                  dropdownItems={federationOptions}
                />
              </ToolbarItem>
            }
            actions={[
              {
                title: t("common:delete"),
                onRowClick: (identityFederation) => {
                  setSelectedFederation(identityFederation);
                  toggleDeleteDialog();
                },
              } as Action<IdentityFederationRepresentation>,
              {
                title: t("refresh"),
                onRowClick: (identityFederation) => {
                  setSelectedFederation(identityFederation);
                  toggleRefreshDialog();
                },
              } as Action<IdentityFederationRepresentation>,
            ]}
            columns={[
              {
                name: "alias",
                displayKey: t("name"),
                cellRenderer: DetailLink,
              },
              {
                name: "providerId",
                displayKey: t("provider"),
              },
              {
                name: "category",
                displayKey: t("category"),
              },
              {
                name: "updateFrequencyInMins",
                displayKey: t("updateFrequency"),
                cellFormatters: [(value) => (value ? formatTime(value) : "")],
              },
              {
                name: "validUntilTimestamp",
                displayKey: t("validUntil"),
                cellFormatters: [(value) => (value ? formatDate(value) : "")],
              },
              {
                name: "lastMetadataRefreshTimestamp",
                displayKey: t("lastUpdatedTime"),
                cellRenderer: (identityFederation: any) => {
                  return identityFederation.refreshing ? (
                    <>
                      <Spinner size="sm" />
                      <span style={{ marginLeft: "8px" }}>Refreshing...</span>
                    </>
                  ) : (
                    <span>
                      {identityFederation?.lastMetadataRefreshTimestamp
                        ? formatDate(
                            identityFederation.lastMetadataRefreshTimestamp,
                          )
                        : ""}
                    </span>
                  );
                },
              },
            ]}
          />
        ) : (
          <>
            <TextContent>
              <Text component={TextVariants.p}>
                {t("identity-federations:getStarted")}
              </Text>
            </TextContent>
            <hr className="pf-u-mb-lg" />
            <Gallery hasGutter>
              <ClickableCard
                key={"saml"}
                onClick={() =>
                  navigate(
                    toIdentityFederationCreate({
                      providerId: "saml",
                      realm,
                      tab: "settings",
                    }),
                  )
                }
              >
                <CardTitle>
                  <Split hasGutter>
                    <SplitItem>
                      <IconMapper icon={"saml"} />
                    </SplitItem>
                    <SplitItem isFilled>{"Saml v2.0"}</SplitItem>
                  </Split>
                </CardTitle>
              </ClickableCard>
            </Gallery>
          </>
        )}
      </PageSection>
    </>
  );
}
