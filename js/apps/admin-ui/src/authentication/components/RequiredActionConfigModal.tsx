import type RequiredActionConfigInfoRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionConfigInfoRepresentation";
import type RequiredActionConfigRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionConfigRepresentation";
import type RequiredActionProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import {
  isUserProfileError,
  setUserProfileServerError,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { TrashIcon } from "@patternfly/react-icons";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { convertFormValuesToObject, convertToFormValues } from "../../util";

type RequiredActionConfigModalForm = {
  config: { [index: string]: any };
};

type RequiredActionConfigModalProps = {
  requiredAction: RequiredActionProviderRepresentation;
  onClose: () => void;
};

export const RequiredActionConfigModal = ({
  requiredAction,
  onClose,
}: RequiredActionConfigModalProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const isTermsAndConditions = requiredAction.alias === "TERMS_AND_CONDITIONS";

  const [configDescription, setConfigDescription] =
    useState<RequiredActionConfigInfoRepresentation>();

  const form = useForm<RequiredActionConfigModalForm>({
    defaultValues: { config: {} },
    shouldUnregister: false,
    mode: "onSubmit",
  });

  const { setValue, handleSubmit, reset, getValues } = form;

  const setupForm = (config?: RequiredActionConfigRepresentation) => {
    convertToFormValues(config || {}, setValue);
  };

  useFetch(
    async () => {
      const cd =
        await adminClient.authenticationManagement.getRequiredActionConfigDescription(
          { alias: requiredAction.alias! },
        );

      const cfg =
        await adminClient.authenticationManagement.getRequiredActionConfig({
          alias: requiredAction.alias!,
        });

      return { configDescription: cd, config: cfg };
    },
    async ({ configDescription, config }) => {
      setConfigDescription(configDescription);
      setupForm(config);

      // ✅ Force a value so min validation can actually run on load.
      // If it's missing, set to 0 -> immediately invalid vs min: 1
    },
    [],
  );

  const save = async (saved: RequiredActionConfigModalForm) => {
    const newConfig = convertFormValuesToObject(saved);
    try {
      await adminClient.authenticationManagement.updateRequiredActionConfig(
        { alias: requiredAction.alias! },
        newConfig,
      );
      setupForm(newConfig);
      addAlert(t("configSaveSuccess"), AlertVariant.success);
      onClose();
    } catch (error) {
      if (isUserProfileError(error)) {
        setUserProfileServerError(
          error,
          (_name: string | number, err: unknown) => {
            addError("configSaveError", (err as any).message);
          },
          t,
        );
      } else {
        addError("configSaveError", error);
      }
    }
  };

  const doResetTermsAndConditions = async () => {
    try {
      await adminClient.authenticationManagement.resetRequiredActionConfig(
        { alias: requiredAction.alias! },
      );
      addAlert(t("requiredActionResetSuccess"), AlertVariant.success);
    } catch (error) {
      addError("requiredActionResetError", error);
    }
  };

  const [toggleResetDialog, ResetConfirm] = useConfirmDialog({
    titleKey: t("resetTermsConfirmTitle"),
    messageKey: t("resetTermsConfirmMessage"),
    continueButtonLabel: t("resetTermsConfirmButton"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: doResetTermsAndConditions,
  });

  return (
    <Modal
      variant={ModalVariant.small}
      isOpen
      title={t("requiredActionConfig", { name: requiredAction.name })}
      onClose={onClose}
    >
      <ResetConfirm />
      <Form id="required-action-config-form" onSubmit={handleSubmit(save)}>
        <FormProvider {...form}>
          <DynamicComponents
            stringify
            properties={configDescription?.properties || []}
          />
        </FormProvider>
        <ActionGroup>
          <Button data-testid="save" variant="primary" type="submit">
            {t("save")}
          </Button>

          <Button
            data-testid="cancel"
            variant={ButtonVariant.link}
            type="button"
            onClick={onClose}
          >
            {t("cancel")}
          </Button>
          <div className="pf-v5-u-ml-3xl pf-v5-u-display-flex pf-v5-u-gap-md">
            <Button
              data-testid="clear"
              variant={ButtonVariant.link}
              type="button"
              onClick={async () => {
                await adminClient.authenticationManagement.removeRequiredActionConfig(
                  { alias: requiredAction.alias! },
                );
                reset({ config: {} });
                onClose();
              }}
            >
              {t("clear")} <TrashIcon />
            </Button>

            {isTermsAndConditions && (
              <Button
                data-testid="reset-terms"
                variant={ButtonVariant.link}
                type="button"
                onClick={() => toggleResetDialog()}
              >
                {t("reset")}
              </Button>
            )}
          </div>
        </ActionGroup>
      </Form>
    </Modal>
  );
};
