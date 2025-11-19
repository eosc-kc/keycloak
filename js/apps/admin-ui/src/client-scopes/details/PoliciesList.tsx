import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";
import { AlertVariant, Button, ButtonVariant } from "@patternfly/react-core";
import type ProtocolMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/protocolMapperRepresentation";
import {
  Action,
  KeycloakDataTable,
  ListEmptyState,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { ClientScopeParams } from "../routes/ClientScope";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toAddPolicy } from "../routes/AddPolicy";
import { toEditPolicy } from "../routes/EditPolicy";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
type Row = ProtocolMapperRepresentation & {
  category: string;
  type: string;
  priority: number;
};

export const PoliciesList = () => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation("");
  const { id } = useParams<ClientScopeParams>();
  const { realm } = useRealm();
  const navigate = useNavigate();
  const [policies, setPolicies] = useState<any>([]);
  const { addAlert, addError } = useAlerts();
  const [selectedPolicy, setSelectedPolicy] = useState("");
  const [key, setKey] = useState(1);
  const refresh = () => {
    setKey(key + 1);
  };
  useFetch(
    async () => {
      const policies = await adminClient.clientScopes.listPolicies({ id: id! });
      return { policies };
    },
    ({ policies }) => {
      setPolicies(policies);
    },
    [key],
  );

  const [toggleDeletePolicyDialog, DeletePolicyConfirm] = useConfirmDialog({
    titleKey: "deleteScopePolicy",
    messageKey: "deleteScopePolicyConfirm",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.clientScopes.delPolicy({
          policyId: selectedPolicy!,
          id: id!,
        });
        addAlert(t("deleteScopePolicySuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError(t("deleteScopePolicyError"), error);
      }
    },
  });

  return (
    <>
      <DeletePolicyConfirm />
      <KeycloakDataTable
        loader={policies}
        ariaLabelKey="clientScopeList"
        isPaginated={false}
        toolbarItem={
          <Button
            component={(props) => (
              <Link
                {...props}
                to={toAddPolicy({
                  realm,
                  id: id!,
                })}
              />
            )}
          >
            {t("createScopePolicy")}
          </Button>
        }
        actions={[
          {
            title: t("delete"),
            onRowClick: (node) => {
              setSelectedPolicy(node.id!);
              toggleDeletePolicyDialog();
            },
          } as Action<Row>,
        ]}
        columns={[
          {
            name: t("userAttribute"),
            cellRenderer: (row: any) => {
              return (
                <Link
                  to={toEditPolicy({
                    realm,
                    id: id!,
                    policyId: row.id!,
                  })}
                >
                  {row.userAttribute}
                </Link>
              );
            },
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyScopePolicies")}
            instructions={t("emptyScopePoliciesInstructions")}
            secondaryActions={[
              {
                text: t("create"),
                onClick: () =>
                  navigate(
                    toAddPolicy({
                      realm,
                      id: id!,
                    }),
                  ),
              },
            ]}
          />
        }
      />
    </>
  );
};
