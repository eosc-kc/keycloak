import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { AlertVariant, PageSection } from "@patternfly/react-core";
import { OpenIdFederationGeneralSettings } from "./GeneralSettings";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useParams } from "../utils/useParams";
import { ViewHeader } from "../components/view-header/ViewHeader";
import type { KeyValueType } from "../components/key-value-form/key-value-convert";
import { toOpenIdFederation } from "./routes/OpenIdFederation";
import type { OpenIdFederationParams } from "./routes/OpenIdFederation";
import { useAdminClient } from "../admin-client";
import OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/openIdFederationRepresentation";
import {
  useAlerts,
  useFetch,
  KeycloakSpinner,
} from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { convertFormValuesToObject, isEmptyValue } from "../util";

export default function OpenIdFederationSection() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { refresh: refreshRealm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useParams<OpenIdFederationParams>();
  const [realm, setRealm] = useState<RealmRepresentation>();
  const { adminClient } = useAdminClient();
  const [openIdFederations, setOpenIdFederations] =
    useState<OpenIdFederationRepresentation[]>();
  const [key, setKey] = useState(0);
  const refresh = () => {
    setKey(key + 1);
    setRealm(undefined);
  };

  useFetch(() => adminClient.realms.findOne({ realm: realmName }), setRealm, [
    key,
  ]);
  useFetch(
    async () => {
      try {
        return await adminClient.openIdFederations.find({ realm: realmName });
      } catch {
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

    const emptyOpenIdFederationFields: (keyof RealmRepresentation)[] = [
      "openIdFederationContacts",
      "openIdFederationHistoricalKeysEndpoint",
      "openIdFederationLogoUri",
      "openIdFederationOrganizationName",
      "openIdFederationOrganizationUri",
      "openIdFederationPolicyUri",
      "openIdFederationResolveEndpoint",
    ];

    for (const field of emptyOpenIdFederationFields) {
      if (isEmptyValue(r[field])) {
        delete r[field];
      }
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
      addAlert(t("saveSuccessOpenIdFederation"), AlertVariant.success);
    } catch (error) {
      addError("saveErrorOpenIdFederation", error);
    }

    const isRealmRenamed = realmName !== (r.realm || realm?.realm);
    if (isRealmRenamed) {
      refreshRealm();
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
          titleKey="openIdFederation"
          subKey="openIdFederationExplanation"
        />
        <PageSection variant="light">
          <OpenIdFederationGeneralSettings
            realm={realm}
            openIdFederations={openIdFederations}
            setOpenIdFederations={setOpenIdFederations}
            save={save}
          />
        </PageSection>
      </>
    );
}
