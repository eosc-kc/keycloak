import type TrustMarkRepresentation from "@keycloak/keycloak-admin-client/lib/defs/trustMarkRepresentation";
import { AlertVariant, PageSection } from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

import {
  KeycloakSpinner,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";

import { useAdminClient } from "../../admin-client";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useParams } from "../../utils/useParams";
import { toOpenIdFederation } from "../routes/OpenIdFederation";
import { TrustMarkForm } from "./TrustMarkForm";
import {
  toTrustMarkFormValues,
  toTrustMarkRequest,
  type TrustMarkFormValues,
} from "./trust-mark-utils";

type Params = { id: string };

export default function EditTrustMark() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const { id } = useParams<Params>();
  const [trustMark, setTrustMark] = useState<TrustMarkRepresentation>();
  const form = useForm<TrustMarkFormValues>({ mode: "onChange" });

  useFetch(
    () => adminClient.trustMarks.findOne({ id }),
    (result) => {
      if (result) {
        setTrustMark(result);
        form.reset(toTrustMarkFormValues(result));
      }
    },
    [id],
  );

  const cancel = () => navigate(toOpenIdFederation({ realm }));
  const save = async (values: TrustMarkFormValues) => {
    const request = toTrustMarkRequest(values);
    try {
      await adminClient.trustMarks.update({ id }, request);
      form.reset(values);
      addAlert(t("trustMarkUpdateSuccess"), AlertVariant.success);
    } catch (error) {
      addError(t("trustMarkUpdateError"), error);
    }
  };

  if (!trustMark) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <ViewHeader titleKey="editTrustMark" subKey="editTrustMarkExplanation" />
      <PageSection variant="light">
        <FormProvider {...form}>
          <TrustMarkForm save={save} cancel={cancel} isExisting />
        </FormProvider>
      </PageSection>
    </>
  );
}
