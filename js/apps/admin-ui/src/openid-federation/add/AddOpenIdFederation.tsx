import { AlertVariant, PageSection } from "@patternfly/react-core";
import { convertFormValuesToObject } from "../../util";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useParams } from "../../utils/useParams";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAlerts } from "../../components/alert/Alerts";
import type { OpenIdFederationParams } from "../routes/OpenIdFederation";
import { adminClient } from "../../admin-client";
import { FormProvider, useForm } from "react-hook-form";
import { OpenIdFederationForm } from "./OpenIdFederationForm";
import OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/openIdFederationRepresentation";
import { toOpenIdFederationEdit } from "../routes/OpenIdFederationEdit";
import { useFetch } from "../../utils/useFetch";
import RealmRepresentation from "libs/keycloak-admin-client/lib/defs/realmRepresentation";
import { useState } from "react";

export default function AddOpenIdFederation() {
  const { t } = useTranslation("openid-federation");
  const navigate = useNavigate();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useParams<OpenIdFederationParams>();
  const [realm, setRealm] = useState<RealmRepresentation>();

  const form = useForm<OpenIdFederationRepresentation>();
  useFetch(
    () => adminClient.realms.findOne({ realm: realmName }),
    setRealm,
    [],
  );
  const save = async (r: OpenIdFederationRepresentation) => {
    r = convertFormValuesToObject(r);
    r.idpConfiguration?.defaultScope === "" &&
      delete r.idpConfiguration.defaultScope;
    try {
      const savedOpenIdFederation: OpenIdFederationRepresentation = { ...r };
      const createdOpenIdFederation =
        await adminClient.openIdFederations.create(savedOpenIdFederation);
      addAlert(t("saveSuccess"), AlertVariant.success);
      navigate(
        toOpenIdFederationEdit({
          realm: realmName,
          id: createdOpenIdFederation.id,
          tab: "settings",
        }),
      );
    } catch (error) {
      addError("realm-settings:saveError", error);
    }
  };
  return (
    <>
      <ViewHeader
        titleKey="openid-federation:addOpenIdFederation"
        subKey="openid-federation:addOpenIdFederationExplanation"
      />
      <PageSection variant="light">
        <FormProvider {...form}>
          <OpenIdFederationForm save={save} realm={realm} />
        </FormProvider>
      </PageSection>
    </>
  );
}
