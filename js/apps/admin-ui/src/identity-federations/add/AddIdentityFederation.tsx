import type IdentityFederationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityFederationRepresentation";
import { AlertVariant, PageSection } from "@patternfly/react-core";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useNavigate } from "react-router-dom";
import { toIdentityFederations } from "../routes/IdentityFederations";
import { useAdminClient } from "../../admin-client";
import IdentityFederationForm from "./IdentityFederationForm";
import { cleanEmptyStrings } from "../../util";

export default function AddIdentityFederation() {
  const form = useForm<IdentityFederationRepresentation>();
  const { adminClient } = useAdminClient();
  const { t } = useTranslation("identity-federations");
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { realm } = useRealm();
  const id = "saml";

  const onSubmit = async (samlFederation: IdentityFederationRepresentation) => {
    try {
      if (samlFederation.config?.nameIDPolicyFormat === "isNull") {
        delete samlFederation.config.nameIDPolicyFormat;
      }
      await adminClient.identityFederations.create({
        ...cleanEmptyStrings(samlFederation),
        providerId: id,
      });
      addAlert(t("createFederationSuccess"), AlertVariant.success);
      navigate(
        toIdentityFederations({
          realm,
        }),
      );
    } catch (error: any) {
      addError("createFederationError", error);
    }
  };

  return (
    <PageSection variant="light">
      <FormProvider {...form}>
        <ViewHeader titleKey={t("addFederation")} divider={false} />
        <IdentityFederationForm
          type={"create"}
          providerId={id}
          onSubmit={onSubmit}
        />
      </FormProvider>
    </PageSection>
  );
}
