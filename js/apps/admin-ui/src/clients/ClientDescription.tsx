import { useTranslation } from "react-i18next";
import {
  TextControl,
  TextAreaControl,
  HelpItem,
  FormErrorText,
} from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../components/form/FormAccess";
import { DefaultSwitchControl } from "../components/SwitchControl";
import { Countries } from "./add/Countries";
import { Controller, useFormContext } from "react-hook-form";
import { FormFields } from "./ClientDetails";
import { convertAttributeNameToForm } from "../util";
import { FormGroup } from "@patternfly/react-core";
import { TimeSelector } from "../components/time-selector/TimeSelector";
import useFormatDate, { FORMAT_DATE_AND_TIME } from "../utils/useFormatDate";

type ClientDescriptionProps = {
  protocol?: string;
  hasConfigureAccess?: boolean;
};

export const ClientDescription = ({
  hasConfigureAccess: configure,
}: ClientDescriptionProps) => {
  const { t } = useTranslation();
  const {
    watch,
    control,
    formState: { errors },
  } = useFormContext<FormFields>();
  const formatDate = useFormatDate();
  const autoUpdated = watch(
    convertAttributeNameToForm("attributes.saml.auto.updated"),
  ) as unknown as string;
  const protocol = watch("protocol");
  const expirationTime = watch(
    convertAttributeNameToForm("attributes.expiration.time"),
  ) as unknown as number;
  const lastRefreshed = watch(
    convertAttributeNameToForm("attributes.saml.last.refresh.time"),
  ) as unknown as string;
  const validateMetadataUrl = (uri: string | undefined, error: string) =>
    ((uri?.startsWith("https://") || uri?.startsWith("http://")) &&
      !uri.includes("*")) ||
    uri === "" ||
    error;

  return (
    <FormAccess role="manage-clients" fineGrainedAccess={configure} unWrap>
      <TextControl
        name="clientId"
        label={t("clientId")}
        labelIcon={t("clientIdHelp")}
        rules={{ required: t("required") }}
      />
      <TextControl
        name="name"
        label={t("name")}
        labelIcon={t("clientNameHelp")}
      />
      {protocol === "saml" && (
        <DefaultSwitchControl
          name={convertAttributeNameToForm("attributes.saml.auto.updated")}
          label={t("autoUpdate")}
          labelIcon={t("autoUpdateHelp")}
          stringify
        />
      )}
      {autoUpdated === "true" && (
        <>
          <TextControl
            name={convertAttributeNameToForm("attributes.saml.metadata.url")}
            label={t("metadataUrl")}
            type="url"
            labelIcon={t("metadataUrlHelp")}
            rules={{
              required: { value: true, message: t("required") },
              validate: (uri) =>
                validateMetadataUrl(uri, t("metadataUrlInvalid")),
            }}
          />
          <FormGroup
            label={t("refreshPeriod")}
            fieldId="refreshPeriod"
            className="pf-v5-u-my-md"
            labelIcon={
              <HelpItem
                helpText={t("refreshPeriodClientHelp")}
                fieldLabelId="refreshPeriodHelp"
              />
            }
            isRequired
          >
            <Controller
              name={convertAttributeNameToForm(
                "attributes.saml.refresh.period",
              )}
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
                    (
                      convertAttributeNameToForm(
                        "attributes.saml.refresh.period",
                      ) as string
                    ).split(".")[1] in (errors.attributes ?? {})
                      ? "error"
                      : "default"
                  }
                />
              )}
            />
            {(
              convertAttributeNameToForm(
                "attributes.saml.refresh.period",
              ) as string
            ).split(".")[1] in (errors.attributes ?? {}) && (
              <FormErrorText
                data-testid={`refreshPeriod-helper`}
                message={t("refreshPeriodInvalid")}
              />
            )}
          </FormGroup>
          <DefaultSwitchControl
            name={convertAttributeNameToForm(
              "attributes.saml.skip.requested.attributes",
            )}
            label={t("skipRequestedAttrubues")}
            labelIcon={t("skipRequestedAttrubuesHelp")}
            stringify
          />
          {!!lastRefreshed && (
            <FormGroup
              label={t("lastRefresh")}
              labelIcon={
                <HelpItem
                  helpText={t("lastClientRefreshHelp")}
                  fieldLabelId="lastClientRefreshHelp"
                />
              }
            >
              {formatDate(
                new Date(parseInt(lastRefreshed)),
                FORMAT_DATE_AND_TIME,
              )}
            </FormGroup>
          )}
        </>
      )}
      <TextAreaControl
        name="description"
        label={t("description")}
        labelIcon={t("clientDescriptionHelp")}
        rules={{
          maxLength: {
            value: 1000,
            message: t("maxLength", { length: 1000 }),
          },
        }}
      />
      <TextControl
        name="attributes.contacts"
        label={t("contacts")}
        labelIcon={t("contactsHelp")}
      />
      <Countries />
      <DefaultSwitchControl
        name="alwaysDisplayInConsole"
        label={t("alwaysDisplayInUI")}
        labelIcon={t("alwaysDisplayInUIHelp")}
      />
      {expirationTime && (
        <FormGroup
          label={t("expirationTime")}
          labelIcon={
            <HelpItem
              helpText={t("expirationTimeHelp")}
              fieldLabelId="expirationTimeHelp"
            />
          }
        >
          {formatDate(new Date(expirationTime * 1000), FORMAT_DATE_AND_TIME)}
        </FormGroup>
      )}
    </FormAccess>
  );
};
