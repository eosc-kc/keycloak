import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import {
  Action,
  KeycloakDataTable,
  ListEmptyState,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  ToolbarItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, type Path, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useParams } from "../../utils/useParams";
import type { IdentityProviderParams } from "../routes/IdentityProvider";
import {
  toAddTokenTranslationRule,
  toEditTokenTranslationRule,
} from "../routes/TokenTranslationRule";
import {
  IDP_TOKEN_TRANSLATION_SUB_TYPE,
  TOKEN_TRANSLATION_PROVIDER_TYPE,
  type TokenTranslationRuleSubType,
  tokenTranslationRuleOrder,
} from "./constants";

type TokenTranslationRuleRow = ComponentRepresentation & {
  type: string;
  order: string;
};

type ScopedTokenTranslationRulesListProps = {
  parentId: string;
  subType: TokenTranslationRuleSubType;
  toCreate: Partial<Path>;
  toDetail: (id: string) => Partial<Path>;
};

export const ScopedTokenTranslationRulesList = ({
  parentId,
  subType,
  toCreate,
  toDetail,
}: ScopedTokenTranslationRulesListProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const [key, setKey] = useState(0);
  const [selectedRule, setSelectedRule] = useState<ComponentRepresentation>();

  const loader = async (): Promise<TokenTranslationRuleRow[]> => {
    const rules = await adminClient.components.find({
      parent: parentId,
      type: TOKEN_TRANSLATION_PROVIDER_TYPE,
      subType,
    });

    return rules
      .filter((rule) => rule.subType === subType)
      .sort(
        (left, right) =>
          tokenTranslationRuleOrder(left.config?.order) -
          tokenTranslationRuleOrder(right.config?.order),
      )
      .map((rule) => ({
        ...rule,
        type: rule.providerId ?? "",
        order: Array.isArray(rule.config?.order)
          ? (rule.config.order[0] ?? "")
          : (rule.config?.order ?? ""),
      }));
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteTokenTranslationRule",
    messageKey: t("deleteTokenTranslationRuleConfirm", {
      rule: selectedRule?.name,
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({ id: selectedRule!.id! });
        addAlert(t("tokenTranslationRuleDeleteSuccess"), AlertVariant.success);
        setSelectedRule(undefined);
        setKey((value) => value + 1);
      } catch (error) {
        addError("tokenTranslationRuleDeleteError", error);
      }
    },
  });

  return (
    <>
      <DeleteConfirm />
      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="tokenTranslationRulesList"
        searchPlaceholderKey="searchForTokenTranslationRule"
        toolbarItem={
          <ToolbarItem>
            <Button
              data-testid="add-token-translation-rule"
              variant="primary"
              component={(props) => <Link {...props} to={toCreate} />}
            >
              {t("addTokenTranslationRule")}
            </Button>
          </ToolbarItem>
        }
        columns={[
          {
            name: "name",
            displayKey: "name",
            cellRenderer: (rule) => (
              <Link to={toDetail(rule.id!)}>{rule.name}</Link>
            ),
          },
          { name: "type", displayKey: "type" },
          { name: "order", displayKey: "order" },
        ]}
        actions={[
          {
            title: t("delete"),
            onRowClick: (rule) => {
              setSelectedRule(rule);
              toggleDeleteDialog();
            },
          } as Action<TokenTranslationRuleRow>,
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyTokenTranslationRules")}
            instructions={t("emptyTokenTranslationRulesInstructions")}
            primaryActionText={t("addTokenTranslationRule")}
            onPrimaryAction={() => navigate(toCreate)}
          />
        }
      />
    </>
  );
};

export const TokenTranslationRulesList = () => {
  const { realm } = useRealm();
  const { alias, providerId } = useParams<IdentityProviderParams>();

  return (
    <ScopedTokenTranslationRulesList
      parentId={alias}
      subType={IDP_TOKEN_TRANSLATION_SUB_TYPE}
      toCreate={toAddTokenTranslationRule({ realm, alias, providerId })}
      toDetail={(id) =>
        toEditTokenTranslationRule({ realm, alias, providerId, id })
      }
    />
  );
};
