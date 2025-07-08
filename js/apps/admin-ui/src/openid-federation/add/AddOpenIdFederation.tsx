import { AlertVariant, PageSection } from "@patternfly/react-core";
import { convertFormValuesToObject } from "../../util";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useParams } from "../../utils/useParams";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { toOpenIdFederation } from "../routes/OpenIdFederation";
import { useAlerts } from "../../components/alert/Alerts";
import type { OpenIdFederationParams } from "../routes/OpenIdFederation";
import { adminClient } from "../../admin-client";
import { FormProvider, useForm } from "react-hook-form";
import { OpenIdFederationForm } from "./OpenIdFederationForm";
import OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/OpenIdFederationRepresentation";

export default function AddOpenIdFederation() {
  const { t } = useTranslation("openid-federation");
  const navigate = useNavigate();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useParams<OpenIdFederationParams>();

  const form = useForm<OpenIdFederationRepresentation>();

  const save = async (r: OpenIdFederationRepresentation) => {
    r = convertFormValuesToObject(r);
    try {
      const savedIdentityFederation: OpenIdFederationRepresentation = { ...r };
      await adminClient.openIdFederations.create(savedIdentityFederation);
      addAlert(t("saveSuccess"), AlertVariant.success);
    } catch (error) {
      addError("realm-settings:saveError", error);
    }
    navigate(toOpenIdFederation({ realm: realmName! }));
  };
  return (
    <>
      <ViewHeader
        titleKey="openid-federation:addOpenIdFederation"
        subKey="openid-federation:addOpenIdFederationExplanation"
      />
      <PageSection variant="light">
        <FormProvider {...form}>
          <OpenIdFederationForm save={save} />
        </FormProvider>
      </PageSection>
    </>
  );
}
