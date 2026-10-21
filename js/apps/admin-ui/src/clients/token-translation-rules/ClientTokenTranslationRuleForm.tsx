import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { KeycloakSpinner, useFetch } from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { Navigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";
import { TokenTranslationRuleForm } from "../../identity-providers/token-translation-rules/TokenTranslationRuleForm";
import { useParams } from "../../utils/useParams";
import { toClient } from "../routes/Client";
import {
  type EditClientTokenTranslationRuleParams,
  toEditClientTokenTranslationRule,
} from "../routes/TokenTranslationRule";
import { supportsTokenTranslationRules } from "../utils";

export default function ClientTokenTranslationRuleForm() {
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { clientId, id } = useParams<EditClientTokenTranslationRuleParams>();
  const [client, setClient] = useState<ClientRepresentation>();

  useFetch(() => adminClient.clients.findOne({ id: clientId }), setClient, [
    clientId,
  ]);

  if (!client?.clientId) {
    return <KeycloakSpinner />;
  }

  if (!supportsTokenTranslationRules(client)) {
    return (
      <Navigate replace to={toClient({ realm, clientId, tab: "settings" })} />
    );
  }

  return (
    <TokenTranslationRuleForm
      parentId={client.clientId}
      subType="client"
      backToList={toClient({
        realm,
        clientId,
        tab: "token-translation-rules",
      })}
      toEdit={(createdId) =>
        toEditClientTokenTranslationRule({
          realm,
          clientId,
          id: createdId,
        })
      }
      manageRole="manage-clients"
      fineGrainedAccess={client.access?.configure}
      id={id}
    />
  );
}
