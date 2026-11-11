import type TrustMarkRepresentation from "@keycloak/keycloak-admin-client/lib/defs/trustMarkRepresentation";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  ToolbarItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";

import {
  KeycloakDataTable,
  ListEmptyState,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";

import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { toTrustMarkCreate } from "../routes/TrustMarkCreate";
import { toTrustMarkEdit } from "../routes/TrustMarkEdit";

type TrustMarkTableProps = {
  realm: string;
  trustMarks: TrustMarkRepresentation[];
  setTrustMarks: (trustMarks: TrustMarkRepresentation[]) => void;
};

export const TrustMarkTable = ({
  realm,
  trustMarks,
  setTrustMarks,
}: TrustMarkTableProps) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const [selected, setSelected] = useState<TrustMarkRepresentation>();
  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteTrustMark"),
    messageKey: t("deleteConfirmTrustMark", {
      trustMarkType: selected?.trustMarkType,
    }),
    continueButtonLabel: t("delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.trustMarks.del({ id: selected!.id! });
        setTrustMarks(trustMarks.filter(({ id }) => id !== selected?.id));
        addAlert(t("trustMarkDeleteSuccess"), AlertVariant.success);
      } catch (error) {
        addError(t("trustMarkDeleteError"), error);
      }
    },
  });

  const create = () => navigate(toTrustMarkCreate({ realm }));

  return (
    <>
      <DeleteConfirm />
      <KeycloakDataTable
        key={trustMarks.length}
        ariaLabelKey="trustMarkList"
        loader={async () => trustMarks}
        toolbarItem={
          <ToolbarItem>
            <Button variant="primary" onClick={create}>
              {t("addTrustMark")}
            </Button>
          </ToolbarItem>
        }
        actionResolver={({ data }) => [
          {
            title: t("edit"),
            onClick: () =>
              navigate(toTrustMarkEdit({ realm, id: data.id ?? "" })),
          },
          {
            title: t("delete"),
            onClick: () => {
              setSelected(data);
              toggleDeleteDialog();
            },
          },
        ]}
        columns={[
          {
            name: "trustMarkType",
            displayKey: t("trustMarkType"),
            cellRenderer: ({ id, trustMarkType }) => (
              <Link to={toTrustMarkEdit({ realm, id: id ?? "" })}>
                {trustMarkType}
              </Link>
            ),
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("trustMarksTableEmpty")}
            instructions={t("trustMarksTableEmptyInstructions")}
            primaryActionText={t("addTrustMark")}
            onPrimaryAction={create}
          />
        }
      />
    </>
  );
};
