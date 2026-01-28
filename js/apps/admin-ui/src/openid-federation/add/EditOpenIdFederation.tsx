import type RealmRepresentation from "libs/keycloak-admin-client/lib/defs/realmRepresentation";
import type OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/openIdFederationRepresentation";

import { AlertVariant, PageSection } from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { useAdminClient } from "../../admin-client";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useParams } from "../../utils/useParams";
import { convertFormValuesToObject } from "../../util";

import {
  KeycloakSpinner,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";

import { OpenIdFederationForm } from "./OpenIdFederationForm";
import { FormAccess } from "../../components/form/FormAccess";

type Params = {
  id: string;
};

export default function EditIdentityFederation() {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const { realm: realmName, realmRepresentation } = useRealm();
  const { id } = useParams<Params>();

  const [key, setKey] = useState(0);
  const refresh = () => setKey((k) => k + 1);

  const [realm, setRealm] = useState<RealmRepresentation>();
  const [openIdFederation, setOpenIdFederation] =
    useState<OpenIdFederationRepresentation>();

  const form = useForm<OpenIdFederationRepresentation>();
  const { handleSubmit } = form;

  // Load OpenID federation
  useFetch(
    () =>
      adminClient.openIdFederations.findOne({
        realm: realmName,
        internalId: id,
      }),
    setOpenIdFederation,
    [adminClient, realmName, id, key],
  );

  // Load realm (optional, if OpenIdFederationForm needs it; otherwise use realmRepresentation)
  useFetch(
    () => adminClient.realms.findOne({ realm: realmName }),
    setRealm,
    [adminClient, realmName, key],
  );

  const effectiveRealm = realm ?? realmRepresentation;

  const save = async (r: OpenIdFederationRepresentation) => {
    const payload = convertFormValuesToObject(r);

    if (payload.idpConfiguration?.defaultScope === "") {
      delete payload.idpConfiguration.defaultScope;
    }

    try {
      await adminClient.openIdFederations.update(
        { realm: realmName, internalId: id },
        payload,
      );
      refresh();
      addAlert(t("saveSuccessOpenIdFederation"), AlertVariant.success);
    } catch (error) {
      addError(t("saveErrorOpenIdFederation"), error);
    }
  };

  if (!openIdFederation) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <ViewHeader
        titleKey="editOpenIdFederation"
        subKey="editOpenIdFederationExplanation"
      />
      <PageSection variant="light">
        <FormProvider {...form}>
            <OpenIdFederationForm
              realm={effectiveRealm}
              openIdFederation={openIdFederation}
              save={save}
            />
        </FormProvider>
      </PageSection>
    </>
  );
}
