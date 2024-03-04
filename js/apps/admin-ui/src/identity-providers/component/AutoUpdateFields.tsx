import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { FormGroup, ValidatedOptions } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  FormErrorText,
  HelpItem,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import "../add/discovery-settings.css";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { TimeSelector } from "../../components/time-selector/TimeSelector";

type AutoUpdateFieldsProps = {
  hideMetadata?: boolean;
  protocol?: string;
};

export const AutoUpdateFields = ({
  hideMetadata,
  protocol,
}: AutoUpdateFieldsProps) => {
  const { t } = useTranslation();

  const {
    watch,
    control,
    formState: { errors },
  } = useFormContext<IdentityProviderRepresentation>();

  const autoUpdated = watch("config.autoUpdate") as unknown as string;
  const lastRefreshed = watch("config.lastRefreshTime") as unknown as string;

  return (
    <>
      {!hideMetadata && (
        <TextControl
          name="config.metadataDescriptorUrl"
          label={
            protocol === "saml" ? t("metadataUrl") : t("discoveryEndpoint")
          }
          labelIcon={
            protocol === "saml"
              ? t("metadataUrlHelp")
              : t("discoveryEndpointHelp")
          }
          rules={
            autoUpdated === "true" ? { required: t("required") } : undefined
          }
        />
      )}
      <DefaultSwitchControl
        name="config.autoUpdate"
        label={t("autoUpdate")}
        labelIcon={t("autoUpdateHelp")}
        stringify
      />
      {autoUpdated === "true" && (
        <>
          <FormGroup
            label={t("refreshPeriod")}
            fieldId="refreshPeriod"
            className="pf-v5-u-my-md"
            labelIcon={
              <HelpItem
                helpText={t("refreshPeriodHelp")}
                fieldLabelId="refreshPeriodHelp"
              />
            }
            isRequired
          >
            <Controller
              name={"config.refreshPeriod"}
              defaultValue=""
              control={control}
              rules={{
                required: t("refreshPeriodNotValid"),
              }}
              render={({ field }) => (
                <TimeSelector
                  value={field.value!}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
                  min={1}
                  validated={
                    "refreshPeriod" in (errors.config ?? {})
                      ? ValidatedOptions.error
                      : ValidatedOptions.default
                  }
                />
              )}
            />
            {"refreshPeriod" in (errors.config ?? {}) && (
              <FormErrorText
                data-testid={`refreshPeriod-helper`}
                message={errors?.config?.refreshPeriod?.message as string}
              />
            )}
          </FormGroup>
          {!!lastRefreshed && (
            <FormGroup label={t("lastRefresh")}>
              {new Date(parseInt(lastRefreshed)).toLocaleString()}
            </FormGroup>
          )}
        </>
      )}
    </>
  );
};
