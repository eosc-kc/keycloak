import { PageSection, AlertVariant } from "@patternfly/react-core";
import { convertFormValuesToObject } from "../../util";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useParams } from "../../utils/useParams";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { toOpenIdFederation } from "../routes/OpenIdFederation";
import { useAlerts } from "../../components/alert/Alerts";
import { adminClient } from "../../admin-client";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { useFetch } from "../../utils/useFetch";
import { FormProvider, useForm } from "react-hook-form";
import { OpenIdFederationForm } from "./OpenIdFederationForm";
import { OpenIdFederationEditParams } from "../routes/OpenIdFederationEdit";
import OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/OpenIdFederationRepresentation";

export default function EditIdentityFederation() {
  const { t } = useTranslation("openid-federation");
  const navigate = useNavigate();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName, id: id } = useParams<OpenIdFederationEditParams>();
  const [openIdFederation, setOpenIdFederation] =
    useState<OpenIdFederationRepresentation>();

  const form = useForm<OpenIdFederationRepresentation>();
  useFetch(
    () =>
      adminClient.openIdFederations.findOne({
        realm: realmName,
        internalId: id,
      }),
    setOpenIdFederation,
    [id],
  );

  const save = async (r: OpenIdFederationRepresentation) => {
    r = convertFormValuesToObject(r);
    try {
      const savedIdentityFederation: OpenIdFederationRepresentation = { ...r };
      await adminClient.openIdFederations.update(
        { internalId: id },
        savedIdentityFederation,
      );
      addAlert(t("saveSuccess"), AlertVariant.success);
    } catch (error) {
      addError("realm-settings:saveError", error);
    }
    navigate(toOpenIdFederation({ realm: realmName! }));
  };
  if (!openIdFederation) {
    return <KeycloakSpinner />;
  } else
    return (
      <>
        <ViewHeader
          titleKey="openid-federation:editOpenIdFederation"
          subKey="openid-federation:editOpenIdFederationExplanation"
        />
        <PageSection variant="light">
          <FormProvider {...form}>
            <OpenIdFederationForm
              openIdFederation={openIdFederation}
              save={save}
            />
          </FormProvider>
        </PageSection>
      </>
    );
}
