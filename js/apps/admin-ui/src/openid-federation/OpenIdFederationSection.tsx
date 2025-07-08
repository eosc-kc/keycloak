import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  AlertVariant,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { convertFormValuesToObject } from "../util";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useParams } from "../utils/useParams";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { OpenIdFederationGeneralTab } from "./GeneralTab";
import type { KeyValueType } from "../components/key-value-form/key-value-convert";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import {
  OpenIdFederationTab,
  toOpenIdFederation,
} from "./routes/OpenIdFederation";
import { useRealms } from "../context/RealmsContext";
import { useAlerts } from "../components/alert/Alerts";
import type { OpenIdFederationParams } from "./routes/OpenIdFederation";
import { adminClient } from "../admin-client";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { useFetch } from "../utils/useFetch";
import OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/OpenIdFederationRepresentation";

export default function OpenIdFederationSection() {
  const { t } = useTranslation("openid-federation");
  const navigate = useNavigate();
  const { refresh: refreshRealms } = useRealms();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useParams<OpenIdFederationParams>();
  const [realm, setRealm] = useState<RealmRepresentation>();
  const [openIdFederations, setOpenIdFederations] =
    useState<OpenIdFederationRepresentation[]>();
  const [key, setKey] = useState(0);
  const refresh = () => {
    setKey(key + 1);
    setRealm(undefined);
  };
  const useTab = (tab: OpenIdFederationTab) =>
    useRoutableTab(toOpenIdFederation({ realm: realmName, tab }));

  const generalTab = useTab("general");
  useFetch(() => adminClient.realms.findOne({ realm: realmName }), setRealm, [
    key,
  ]);
  useFetch(
    async () => {
      try {
        return await adminClient.openIdFederations.find({ realm: realmName });
      } catch (error) {
        console.log(error);
        setOpenIdFederations([]); // Optionally clear state immediately
        return [];
      }
    },
    setOpenIdFederations,
    [key],
  );

  const save = async (r: RealmRepresentation) => {
    r = convertFormValuesToObject(r);
    if (
      r.attributes?.["acr.loa.map"] &&
      typeof r.attributes["acr.loa.map"] !== "string"
    ) {
      r.attributes["acr.loa.map"] = JSON.stringify(
        Object.fromEntries(
          (r.attributes["acr.loa.map"] as KeyValueType[])
            .filter(({ key }) => key !== "")
            .map(({ key, value }) => [key, value]),
        ),
      );
    }

    try {
      const savedRealm: RealmRepresentation = {
        ...realm,
        ...r,
        id: r.realm,
      };

      // For the default value, null is expected instead of an empty string.
      if (savedRealm.smtpServer?.port === "") {
        savedRealm.smtpServer = { ...savedRealm.smtpServer, port: null };
      }
      await adminClient.realms.update({ realm: realmName }, savedRealm);
      addAlert(t("saveSuccess"), AlertVariant.success);
    } catch (error) {
      addError("realm-settings:saveError", error);
    }

    const isRealmRenamed = realmName !== (r.realm || realm?.realm);
    if (isRealmRenamed) {
      await refreshRealms();
      navigate(toOpenIdFederation({ realm: r.realm!, tab: "general" }));
    }
    refresh();
  };
  if (!realm) {
    return <KeycloakSpinner />;
  } else
    return (
      <>
        <ViewHeader
          titleKey="openid-federation:openIdFederation"
          subKey="openid-federation:openIdFederationExplanation"
        />
        <PageSection variant="light">
          <RoutableTabs
            isBox
            mountOnEnter
            aria-label="realm-settings-tabs"
            defaultLocation={toOpenIdFederation({
              realm: realmName,
              tab: "general",
            })}
          >
            <Tab
              title={<TabTitleText>{t("general")}</TabTitleText>}
              data-testid="rs-general-tab"
              {...generalTab}
            >
              <OpenIdFederationGeneralTab
                realm={realm}
                openIdFederations={openIdFederations}
                setOpenIdFederations={setOpenIdFederations}
                save={save}
              />
            </Tab>
          </RoutableTabs>
        </PageSection>
      </>
    );
}
