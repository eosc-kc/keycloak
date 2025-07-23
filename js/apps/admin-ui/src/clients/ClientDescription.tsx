import { useTranslation } from "react-i18next";
import { TextControl, TextAreaControl, HelpItem } from "ui-shared";

import { FormAccess } from "../components/form/FormAccess";
import { DefaultSwitchControl } from "../components/SwitchControl";
import { FormGroup } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { convertAttributeNameToForm } from "../util";
import { Toggle } from "./add/SamlConfig";
import { FormFields } from "./ClientDetails";
import { Fragment } from "react";
import { TimeSelector } from "../components/time-selector/TimeSelector";
import { Countries } from "./add/Countries";

type ClientDescriptionProps = {
  protocol?: string;
  hasConfigureAccess?: boolean;
};

export const ClientDescription = ({
  hasConfigureAccess: configure,
}: ClientDescriptionProps) => {
  const { t } = useTranslation("clients");
  const {
    watch,
    control,
    formState: { errors },
  } = useFormContext<FormFields>();
  const autoUpdated = watch(
    convertAttributeNameToForm("attributes.saml.auto.updated"),
  ) as unknown as string;
  const expirationTime = watch(
    convertAttributeNameToForm("attributes.expiration.time"),
  ) as unknown as number;
  const lastRefreshed = watch(
    convertAttributeNameToForm("attributes.saml.last.refresh.time"),
  ) as unknown as string;
  const protocol = watch("protocol");
  const validateMetadataUrl = (uri: string | undefined, error: string) =>
    ((uri?.startsWith("https://") || uri?.startsWith("http://")) &&
      !uri.includes("*")) ||
    uri === "" ||
    error;

  return (
    <FormAccess role="manage-clients" fineGrainedAccess={configure} unWrap>
      <TextControl
        name="clientId"
        label={t("common:clientId")}
        labelIcon={t("clients-help:clientId")}
        rules={{ required: { value: true, message: t("common:required") } }}
      />
      {protocol === "saml" && (
        <Toggle
          name={convertAttributeNameToForm("attributes.saml.auto.updated")}
          label="autoUpdate"
        />
      )}
      {autoUpdated === "true" && (
        <Fragment>
          <TextControl
            name={convertAttributeNameToForm("attributes.saml.metadata.url")}
            label={t("clients:metadataUrl")}
            type="url"
            labelIcon={t("clients-help:metadataUrl")}
            rules={{
              required: { value: true, message: t("common:required") },
              validate: (uri) =>
                validateMetadataUrl(
                  uri,
                  t("clients:metadataUrlInvalid").toString(),
                ),
            }}
          />
          <FormGroup
            label={t("clients:refreshPeriod")}
            labelIcon={
              <HelpItem
                helpText={t("clients-help:refreshPeriod")}
                fieldLabelId="clients-help:refreshPeriod"
              />
            }
            helperTextInvalid={t("refreshPeriodInvalid")}
            validated={
              (
                convertAttributeNameToForm(
                  "attributes.saml.refresh.period",
                ) as string
              ).split(".")[1] in (errors.attributes ?? {})
                ? "error"
                : "default"
            }
            isRequired
          >
            <Controller
              name={convertAttributeNameToForm(
                "attributes.saml.refresh.period",
              )}
              control={control}
              rules={{
                required: { value: true, message: t("common:required") },
                min: 1,
              }}
              render={({ field }) => (
                <TimeSelector
                  value={field.value || 0}
                  onChange={field.onChange}
                  units={["minute", "hour", "day"]}
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
          </FormGroup>
          <Toggle
            name={convertAttributeNameToForm(
              "attributes.saml.skip.requested.attributes",
            )}
            label="skipRequestedAttrubues"
          />
          {!!lastRefreshed && (
            <FormGroup
              label={t("clients:lastRefresh")}
              labelIcon={
                <HelpItem
                  helpText={t("clients-help:lastRefresh")}
                  fieldLabelId="clients-help:lastRefresh"
                />
              }
            >
              {new Date(parseInt(lastRefreshed)).toLocaleString()}
            </FormGroup>
          )}
        </Fragment>
      )}

      <TextControl
        name="name"
        label={t("common:name")}
        labelIcon={t("clients-help:clientName")}
      />
      <TextAreaControl
        name="description"
        label={t("common:description")}
        labelIcon={t("clients-help:description")}
        rules={{
          maxLength: {
            value: 255,
            message: t("common:maxLength", { length: 255 }),
          },
        }}
      />
      <TextControl
        name="attributes.contacts"
        label={t("clients:contacts")}
        labelIcon={t("clients-help:contacts")}
        type="text"
      />
      <Countries />
      <DefaultSwitchControl
        name="alwaysDisplayInConsole"
        label={t("alwaysDisplayInUI")}
        labelIcon={t("clients-help:alwaysDisplayInUI")}
      />
      {expirationTime && (
        <FormGroup
          label={t("clients:expirationTime")}
          labelIcon={
            <HelpItem
              helpText={t("clients-help:expirationTime")}
              fieldLabelId="clients-help:expirationTime"
            />
          }
        >
          {expirationTime}
        </FormGroup>
      )}
    </FormAccess>
  );
};
