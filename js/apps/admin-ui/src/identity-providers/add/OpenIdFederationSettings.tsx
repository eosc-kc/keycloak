import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";

import { FormGroup, Title, ValidatedOptions } from "@patternfly/react-core";
import {
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core/deprecated";
import { useState } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem, FormErrorText, TextControl } from "@keycloak/keycloak-ui-shared";

import { convertAttributeNameToForm } from "../../util";
import useFormatDate from "../../utils/useFormatDate";

type OpenIdFederationSettingsProps = {
  readOnly: boolean;
  create?: boolean;
  trustAnchors?: string[];
};

export const OpenIdFederationSettings = ({
  readOnly,
  create,
  trustAnchors = [],
}: OpenIdFederationSettingsProps) => {
  const { t } = useTranslation();
  const formatDate = useFormatDate();

  const [open, setOpen] = useState(false);

  const {
    control,
    formState: { errors },
  } = useFormContext<IdentityProviderRepresentation>();

  const config = useWatch({
    control,
    name: convertAttributeNameToForm("config") as any,
  }) as Record<string, any> | undefined;

  const expirationSeconds = Number(config?.["expiration.time"] ?? 0);

  return (
    <div className="pf-v5-c-form pf-m-horizontal">
      {!create ? (
        <>
          <TextControl
            name="config.authorityHints"
            label={t("authorityHints")}
            type="url"
            // isReadOnly={readOnly}
          />

          <TextControl
            name="config.trustAnchorId"
            label={t("trustAnchorId")}
            type="text"
            // isReadOnly={readOnly}
          />

          
            <TextControl
              name="__expirationTimeDisplay"
              label={t("expirationTime")}
              type="text"
              isDisabled
              value={
                expirationSeconds
                  ? formatDate(new Date(expirationSeconds * 1000))
                  : ""
              }
            />
        </>
      ) : (
        <>
          <Title headingLevel="h2" size="xl" className="kc-form-panel__title">
            {t("openIdFederationSettings")}
          </Title>

          <TextControl
            name="config.issuer"
            label={t("issuer")}
            type="url"
            // isReadOnly={readOnly}
            rules={{ required: t("required") }}
          />
          {errors.config?.issuer?.message && (
            <FormErrorText message={String(errors.config.issuer.message)} />
          )}

          <FormGroup
            label={t("trustAnchorId")}
            isRequired
            labelIcon={
              <HelpItem
                helpText={t("trustAnchorIdHelp")}
                fieldLabelId="trustAnchorId"
              />
            }
            fieldId="kc-trust-anchor-id"
            // validated={
            //   errors.config?.trustAnchorId
            //     ? ValidatedOptions.error
            //     : ValidatedOptions.default
            // }
          >
            <Controller
              name="config.trustAnchorId"
              defaultValue=""
              control={control}
              rules={{ required: t("required") as any }}
              render={({ field }) => (
                <Select
                  toggleId="config.trustAnchorId"
                  variant={SelectVariant.typeahead}
                  isOpen={open}
                  selections={field.value ?? ""}
                  onToggle={(_event, isOpen) => setOpen(isOpen)}
                  onFilter={(_event, value) => {
                    const v = (value ?? "").toLowerCase();
                    const filtered =
                      v.length === 0
                        ? trustAnchors
                        : trustAnchors.filter((a) =>
                            a.toLowerCase().includes(v),
                          );

                    // PF deprecated Select expects ReactElement children for filter result
                    return filtered.map((anchor) => (
                      <SelectOption key={anchor} value={anchor}>
                        {anchor}
                      </SelectOption>
                    ));
                  }}
                  onSelect={(_event, selection) => {
                    field.onChange(String(selection));
                    setOpen(false);
                  }}
                  typeAheadAriaLabel={t("trustAnchorId")}
                  isDisabled={readOnly}
                >
                  {trustAnchors.map((anchor) => (
                    <SelectOption key={anchor} value={anchor}>
                      {anchor}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
            {errors.config?.trustAnchorId?.message && (
              <FormErrorText message={String(errors.config.trustAnchorId.message)} />
            )}
          </FormGroup>
        </>
      )}
    </div>
  );
};
