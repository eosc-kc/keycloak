import { AlertVariant, PageSection } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

import { useAlerts } from "@keycloak/keycloak-ui-shared";

import { useAdminClient } from "../../admin-client";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toOpenIdFederation } from "../routes/OpenIdFederation";
import { toTrustMarkEdit } from "../routes/TrustMarkEdit";
import { TrustMarkForm } from "./TrustMarkForm";
import {
  toTrustMarkFormValues,
  toTrustMarkRequest,
  type TrustMarkFormValues,
} from "./trust-mark-utils";

export default function AddTrustMark() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const form = useForm<TrustMarkFormValues>({
    mode: "onChange",
    defaultValues: toTrustMarkFormValues(),
  });

  const cancel = () => navigate(toOpenIdFederation({ realm }));
  const save = async (values: TrustMarkFormValues) => {
    try {
      const created = await adminClient.trustMarks.create(
        toTrustMarkRequest(values, {
          issuedAt: Math.floor(Date.now() / 1000),
        }),
      );
      addAlert(t("trustMarkCreateSuccess"), AlertVariant.success);
      navigate(toTrustMarkEdit({ realm, id: created.id }));
    } catch (error) {
      addError(t("trustMarkCreateError"), error);
    }
  };

  return (
    <>
      <ViewHeader titleKey="addTrustMark" subKey="addTrustMarkExplanation" />
      <PageSection variant="light">
        <FormProvider {...form}>
          <TrustMarkForm save={save} cancel={cancel} />
        </FormProvider>
      </PageSection>
    </>
  );
}
