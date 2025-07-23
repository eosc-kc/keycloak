import OpenIdFederationRepresentation, {
  EntityTypesSupported,
  ClientRegistrationTypesSupported,
} from "@keycloak/keycloak-admin-client/lib/defs/OpenIdFederationRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useState, useEffect } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { convertToFormValues } from "../../util";

type OpenIdFederationGeneralTabProps = {
  save: (openIdFederation: OpenIdFederationRepresentation) => void;
  openIdFederation?: OpenIdFederationRepresentation;
};
const entityTypesSupportedValues: EntityTypesSupported[] = [
  "OPENID_PROVIDER",
  "OPENID_RELAYING_PARTY",
];
const clientRegistrationTypesSupportedValues: ClientRegistrationTypesSupported[] =
  ["EXPLICIT"];

export const OpenIdFederationForm = ({
  save,
  openIdFederation = {
    trustAnchor: "",
    entityTypes: [],
    clientRegistrationTypesSupported: [],
  },
}: OpenIdFederationGeneralTabProps) => {
  const { t } = useTranslation("openid-federation");

  const [
    openClientRegistrationTypesSupported,
    setOpenClientRegistrationTypesSupported,
  ] = useState(false);
  const [openEntityTypes, setOpenEntityTypes] = useState(false);

  const {
    register,
    control,
    handleSubmit,
    setValue,
    formState: { isDirty, errors },
  } = useFormContext<OpenIdFederationRepresentation>();

  const setupForm = () => {
    convertToFormValues(openIdFederation, setValue);
  };

  useEffect(setupForm, []);

  return (
    <PageSection variant="light">
      <FormAccess
        isHorizontal
        role="manage-realm"
        className="pf-u-mt-lg"
        onSubmit={handleSubmit(save)}
      >
        <FormGroup
          label={t("trustAnchor")}
          isRequired
          labelIcon={
            <HelpItem
              helpText={t("openid-federation-help:trustAnchor")}
              fieldLabelId="openid-federation:trustAnchor"
            />
          }
          fieldId="kc-trustAnchor"
          validated={
            errors.trustAnchor
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          helperTextInvalid={t("common:required")}
        >
          <KeycloakTextInput
            id="kc-logo-uri"
            validated={
              errors.trustAnchor
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            {...register(`trustAnchor`, {
              required: true,
            })}
          />
        </FormGroup>
        <FormGroup
          label={t("entityTypes")}
          isRequired
          labelIcon={
            <HelpItem
              helpText={t("openid-federation-help:entityTypes")}
              fieldLabelId="resetEntityTypes"
            />
          }
          validated={
            errors.entityTypes
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          helperTextInvalid={t("common:required")}
          fieldId="entity-types"
        >
          <Controller
            name={`entityTypes`}
            defaultValue={[] as EntityTypesSupported[]}
            control={control}
            rules={{
              required: {
                value: true,
                message: t("common:required"),
              },
            }}
            render={({ field }) => (
              <Select
                maxHeight={375}
                toggleId={"entityTypes"}
                variant={SelectVariant.typeaheadMulti}
                chipGroupProps={{
                  numChips: 3,
                }}
                placeholderText={t("entityTypesPlaceholder")}
                menuAppendTo="parent"
                validated={errors.entityTypes ? "error" : "default"}
                onToggle={(open) => setOpenEntityTypes(open)}
                isOpen={openEntityTypes}
                selections={field.value as string[]}
                onSelect={(_, selectedValue) => {
                  const value: EntityTypesSupported[] | undefined = field.value;
                  field.onChange(
                    value.find((item) => item === selectedValue)
                      ? value.filter((item) => item !== selectedValue)
                      : [...(value ? value : []), selectedValue],
                  );
                }}
                onClear={(event) => {
                  event.stopPropagation();
                  field.onChange([]);
                }}
                typeAheadAriaLabel={t("resetActions")}
              >
                {entityTypesSupportedValues.map((name) => (
                  <SelectOption
                    key={name}
                    value={name}
                    data-testid={`${name}-option`}
                  >
                    {name}
                  </SelectOption>
                ))}
              </Select>
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("clientRegistrationTypesSupported")}
          isRequired
          labelIcon={
            <HelpItem
              helpText={t(
                "openid-federation-help:clientRegistrationTypesSupported",
              )}
              fieldLabelId="resetTypesSupported"
            />
          }
          validated={
            errors.clientRegistrationTypesSupported
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
          helperTextInvalid={t("common:required")}
          fieldId="types-supported"
        >
          <Controller
            name={`clientRegistrationTypesSupported`}
            defaultValue={[] as ClientRegistrationTypesSupported[]}
            control={control}
            rules={{
              required: {
                value: true,
                message: t("common:required"),
              },
            }}
            render={({ field }) => (
              <Select
                maxHeight={375}
                toggleId={
                  "openIdFederationList.clientRegistrationTypesSupported"
                }
                variant={SelectVariant.typeaheadMulti}
                chipGroupProps={{
                  numChips: 3,
                }}
                placeholderText={t(
                  "clientRegistrationTypesSupportedPlaceholder",
                )}
                validated={
                  errors.clientRegistrationTypesSupported ? "error" : "default"
                }
                menuAppendTo="parent"
                onToggle={(open) =>
                  setOpenClientRegistrationTypesSupported(open)
                }
                isOpen={openClientRegistrationTypesSupported}
                selections={field.value as string[]}
                onSelect={(_, selectedValue) => {
                  const value: ClientRegistrationTypesSupported[] | undefined =
                    field.value;
                  field.onChange(
                    value.find((item) => item === selectedValue)
                      ? value.filter((item) => item !== selectedValue)
                      : [...(value ? value : []), selectedValue],
                  );
                }}
                onClear={(event) => {
                  event.stopPropagation();
                  field.onChange([]);
                }}
                typeAheadAriaLabel={t("resetActions")}
              >
                {clientRegistrationTypesSupportedValues.map((name) => (
                  <SelectOption
                    key={name}
                    value={name}
                    data-testid={`${name}-option`}
                  >
                    {name}
                  </SelectOption>
                ))}
              </Select>
            )}
          />
        </FormGroup>
        <ActionGroup>
          <Button
            variant="primary"
            type="submit"
            data-testid="general-tab-save"
            isDisabled={!isDirty}
          >
            {t("common:save")}
          </Button>
          <Button
            data-testid="general-tab-revert"
            variant="link"
            onClick={setupForm}
          >
            {t("common:revert")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
};
