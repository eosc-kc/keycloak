import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  KeycloakSpinner,
  SelectVariant,
  TextControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  FormGroup,
  PageSection,
  SelectOption,
} from "@patternfly/react-core";
import { useMemo, useState } from "react";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { FormAccess } from "../../components/form/FormAccess";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { useParams } from "../../utils/useParams";
import { toIdentityProvider } from "../routes/IdentityProvider";
import {
  type EditTokenTranslationRuleParams,
  toEditTokenTranslationRule,
} from "../routes/TokenTranslationRule";
import {
  IDP_TOKEN_TRANSLATION_SUB_TYPE,
  TOKEN_TRANSLATION_PROVIDER_TYPE,
} from "./constants";

const normalizeConfig = (config: ComponentRepresentation["config"] = {}) =>
  Object.fromEntries(
    Object.entries(config).map(([key, value]) => [
      key,
      Array.isArray(value) ? value : [value],
    ]),
  );

export default function TokenTranslationRuleForm() {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { realm } = useRealm();
  const { alias, providerId, id } = useParams<EditTokenTranslationRuleParams>();
  const serverInfo = useServerInfo();
  const ruleTypes = useMemo(
    () =>
      [
        ...(serverInfo.componentTypes?.[TOKEN_TRANSLATION_PROVIDER_TYPE] ?? []),
      ].sort((left, right) => left.id.localeCompare(right.id)),
    [serverInfo],
  );
  const form = useForm<ComponentRepresentation>({
    mode: "onChange",
    defaultValues: {
      providerId: ruleTypes[0]?.id,
      config: {},
    },
  });
  const [loaded, setLoaded] = useState(!id);
  const [typeSelectOpen, setTypeSelectOpen] = useState(false);
  const selectedProviderId = useWatch({
    control: form.control,
    name: "providerId",
  });
  const selectedType = ruleTypes.find(
    ({ id: typeId }) => typeId === selectedProviderId,
  );

  const backToList = toIdentityProvider({
    realm,
    providerId,
    alias,
    tab: "token-translation-rules",
  });

  useFetch(
    async () => (id ? adminClient.components.findOne({ id }) : undefined),
    (rule) => {
      if (id && !rule) {
        throw new Error(t("notFound"));
      }
      if (rule) {
        form.reset({});
        convertToFormValues(rule, form.setValue);
      } else if (ruleTypes[0]) {
        form.setValue("providerId", ruleTypes[0].id);
      }
      setLoaded(true);
    },
    [id],
  );

  const save = async (formValues: ComponentRepresentation) => {
    const values =
      convertFormValuesToObject<ComponentRepresentation>(formValues);
    const component: ComponentRepresentation = {
      ...values,
      id,
      providerId: values.providerId,
      providerType: TOKEN_TRANSLATION_PROVIDER_TYPE,
      parentId: alias,
      subType: IDP_TOKEN_TRANSLATION_SUB_TYPE,
      config: normalizeConfig(values.config),
    };

    try {
      if (id) {
        await adminClient.components.update({ id }, component);
        addAlert(t("tokenTranslationRuleUpdateSuccess"), AlertVariant.success);
        form.reset(formValues);
      } else {
        const created = await adminClient.components.create(component);
        addAlert(t("tokenTranslationRuleCreateSuccess"), AlertVariant.success);
        navigate(
          toEditTokenTranslationRule({
            realm,
            providerId,
            alias,
            id: created.id,
          }),
        );
      }
    } catch (error) {
      addError(
        id
          ? "tokenTranslationRuleUpdateError"
          : "tokenTranslationRuleCreateError",
        error,
      );
    }
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "deleteTokenTranslationRule",
    messageKey: t("deleteTokenTranslationRuleConfirm", {
      rule: form.getValues("name"),
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.components.del({ id: id! });
        addAlert(t("tokenTranslationRuleDeleteSuccess"), AlertVariant.success);
        navigate(backToList);
      } catch (error) {
        addError("tokenTranslationRuleDeleteError", error);
      }
    },
  });

  if (!loaded) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant="light">
      <DeleteConfirm />
      <ViewHeader
        titleKey={
          id ? t("editTokenTranslationRule") : t("addTokenTranslationRule")
        }
        dropdownItems={
          id
            ? [
                <DropdownItem key="delete" onClick={toggleDeleteDialog}>
                  {t("delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <FormProvider {...form}>
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          className="pf-v5-u-mt-lg"
          onSubmit={form.handleSubmit(save)}
        >
          <TextControl
            name="name"
            label={t("name")}
            labelIcon={t("tokenTranslationRuleNameHelp")}
            rules={{ required: t("required") }}
          />
          {id ? (
            <TextControl
              name="providerId"
              label={t("tokenTranslationRuleType")}
              labelIcon={selectedType?.helpText}
              isDisabled
            />
          ) : (
            <FormGroup
              label={t("tokenTranslationRuleType")}
              labelIcon={
                <HelpItem
                  helpText={
                    selectedType?.helpText ?? t("tokenTranslationRuleTypeHelp")
                  }
                  fieldLabelId="tokenTranslationRuleType"
                />
              }
              fieldId="token-translation-rule-type"
              isRequired
            >
              <Controller
                name="providerId"
                control={form.control}
                rules={{ required: t("required") }}
                render={({ field }) => (
                  <KeycloakSelect
                    toggleId="token-translation-rule-type"
                    data-testid="token-translation-rule-type"
                    isOpen={typeSelectOpen}
                    onToggle={setTypeSelectOpen}
                    onSelect={(value) => {
                      form.reset({
                        name: form.getValues("name"),
                        providerId: value as string,
                        config: {},
                      });
                      setTypeSelectOpen(false);
                    }}
                    selections={field.value}
                    variant={SelectVariant.single}
                    aria-label={t("tokenTranslationRuleType")}
                  >
                    {ruleTypes.map((type: ComponentTypeRepresentation) => (
                      <SelectOption key={type.id} value={type.id}>
                        {type.id}
                      </SelectOption>
                    ))}
                  </KeycloakSelect>
                )}
              />
            </FormGroup>
          )}
          {selectedType && (
            <DynamicComponents
              key={selectedType.id}
              properties={selectedType.properties}
              isNew={!id}
            />
          )}
          <ActionGroup>
            <Button
              data-testid="save-token-translation-rule"
              variant="primary"
              type="submit"
            >
              {t("save")}
            </Button>
            <Button
              variant="link"
              component={(props) => <Link {...props} to={backToList} />}
            >
              {t("cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </FormProvider>
    </PageSection>
  );
}
