import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Title,
  ValidatedOptions,
} from "@patternfly/react-core";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { convertAttributeNameToForm } from "../../util";
import useFormatDate from "../../utils/useFormatDate";
import { useState } from "react";

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
  const { t } = useTranslation("identity-providers");
  const [open, setOpen] = useState(false);
  const formatDate = useFormatDate();
  const {
    register,
    control,
    formState: { errors },
  } = useFormContext<IdentityProviderRepresentation>();

  const config = useWatch({
    control,
    name: convertAttributeNameToForm("config"),
  });

  const createOptions = (options: string[]) => {
    return [
      ...options.map((option) => (
        <SelectOption key={option} value={option}>
          {option}
        </SelectOption>
      )),
    ];
  };

  return (
    <div className="pf-c-form pf-m-horizontal">
      {!create ? (
        <>
          <FormGroup label={t("authorityHints")} fieldId="kc-authority-hints">
            <KeycloakTextInput
              type="url"
              data-testid="authorityHints"
              id="kc-authority-hints"
              isReadOnly={readOnly}
              {...register("config.authorityHints")}
            />
          </FormGroup>
          <FormGroup label={t("trustAnchorId")} fieldId="kc-trust-anchor-id">
            <KeycloakTextInput
              type="url"
              data-testid="trustAnchorId"
              id="kc-trust-anchor-id"
              isReadOnly={readOnly}
              {...register("config.trustAnchorId")}
            />
          </FormGroup>
          <FormGroup label={t("expirationTime")} fieldId="kc-expiration-time">
            <KeycloakTextInput
              type="url"
              data-testid="expirationTime"
              id="kc-expiration-time"
              isReadOnly={readOnly}
              // Parse the expiration time from seconds to milliseconds
              value={formatDate(
                new Date(parseInt(config["expiration.time"]) * 1000),
              )}
            />
          </FormGroup>
        </>
      ) : (
        <>
          <Title headingLevel="h2" size="xl" className="kc-form-panel__title">
            {t("openIdFederationSettings")}
          </Title>
          <FormGroup
            label={t("issuer")}
            fieldId="kc-issuer"
            isRequired
            validated={
              errors.config?.issuer
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            helperTextInvalid={t("common:required")}
          >
            <KeycloakTextInput
              type="url"
              data-testid="issuer"
              id="kc-issuer"
              validated={
                errors.config?.issuer
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              isReadOnly={readOnly}
              {...register("config.issuer", { required: true })}
            />
          </FormGroup>
          <FormGroup
            label={t("trustAnchorId")}
            fieldId="kc-issuer"
            isRequired
            validated={
              errors.config?.trustAnchorId
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            helperTextInvalid={t("common:required")}
          >
            <Controller
              name={"config.trustAnchorId"}
              defaultValue={""}
              control={control}
              rules={{ required: true }}
              render={({ field }) => (
                <Select
                  toggleId={"config.trustAnchorId"}
                  variant={SelectVariant.typeahead}
                  onToggle={(open) => setOpen(open)}
                  defaultValue={""}
                  isOpen={open}
                  selections={field.value}
                  onFilter={(_, value) => {
                    if (!value) {
                      return createOptions(trustAnchors);
                    }
                    const input = value.toLowerCase();
                    return createOptions(
                      trustAnchors.filter((trustAnchor) =>
                        trustAnchor.toLowerCase().includes(input),
                      ),
                    );
                  }}
                  onSelect={(_, value) => {
                    field.onChange(value.toString());
                    setOpen(false);
                  }}
                  typeAheadAriaLabel={"t(label!)"}
                >
                  {trustAnchors.map((anchor) => (
                    <SelectOption key={anchor} value={anchor}>
                      {anchor}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
        </>
      )}
    </div>
  );
};
