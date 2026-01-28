import type RealmRepresentation from "libs/keycloak-admin-client/lib/defs/realmRepresentation";
import type OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/openIdFederationRepresentation";

import { AlertVariant, PageSection } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useState } from "react";

import { useAdminClient } from "../../admin-client";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { OpenIdFederationForm } from "./OpenIdFederationForm";
import { toOpenIdFederationEdit } from "../routes/OpenIdFederationEdit";
import { convertFormValuesToObject } from "../../util";

import {
  useAlerts,
  useFetch,
  KeycloakSpinner,
} from "@keycloak/keycloak-ui-shared";

export default function AddOpenIdFederation() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const { realm: realmName, realmRepresentation } = useRealm();

  const form = useForm<OpenIdFederationRepresentation>({
    shouldUnregister: false,
  });

  const [realm, setRealm] = useState<RealmRepresentation>();

  useFetch(() => adminClient.realms.findOne({ realm: realmName }), setRealm, []);

  const effectiveRealm = realm ?? realmRepresentation;

  const save = async (r: OpenIdFederationRepresentation) => {
    const payload = convertFormValuesToObject(r);

    if (payload.idpConfiguration?.defaultScope === "") {
      delete payload.idpConfiguration.defaultScope;
    }

    try {
      const created = await adminClient.openIdFederations.create(payload);
      addAlert(t("saveSuccessOpenIdFederation"), AlertVariant.success);
      navigate(
        toOpenIdFederationEdit({
          realm: realmName,
          id: created.id,
          tab: "settings",
        }),
      );
    } catch (error) {
      addError(t("saveErrorOpenIdFederation"), error);
    }
  };

  if (!effectiveRealm) return <KeycloakSpinner />;

  return (
    <>
      <ViewHeader
        titleKey="addOpenIdFederation"
        subKey="addOpenIdFederationExplanation"
      />
      <PageSection variant="light">
        <FormProvider {...form}>
          <OpenIdFederationForm save={save} realm={effectiveRealm} />
        </FormProvider>
      </PageSection>
    </>
  );
}
