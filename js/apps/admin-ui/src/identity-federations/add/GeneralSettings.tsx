import type IdentityFederationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityFederationRepresentation";
import { FormGroup, NumberInput } from "@patternfly/react-core";
import { useEffect } from "react";
import {
  HelpItem,
  SelectControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";

const categories = ["All", "Identity Providers", "Clients"];
const validateUrl = (uri: string | undefined, error: string) =>
  ((uri?.startsWith("https://") || uri?.startsWith("http://")) &&
    !uri.includes("*")) ||
  uri === "" ||
  error;
type GeneralSettingsProps = {
  type?: string;
};

const GeneralSettings = ({ type }: GeneralSettingsProps) => {
  const { register, trigger, setValue } =
    useFormContext<IdentityFederationRepresentation>();
  const { t } = useTranslation();

  useEffect(() => {
    setValue("updateFrequencyInMins", 30);
    void trigger("updateFrequencyInMins");
  }, []);

  return (
    <>
      <TextControl
        name="url"
        label={t("importFromURL")}
        labelIcon={t("importFromURLHelp")}
        rules={{ validate: (uri) => validateUrl(uri, t("required")) }}
      />
      <TextControl
        name="alias"
        isDisabled={type === "edit"}
        label={t("alias")}
        labelIcon={t("identityFederationAliasHelp")}
        rules={{ required: t("required") }}
      />
      <FormGroup
        label={t("updateFrequency")}
        labelIcon={
          <HelpItem
            helpText={t("updateFrequencyHelp")}
            fieldLabelId="updateFrequency"
          />
        }
        isRequired
      >
        <Controller
          {...register("updateFrequencyInMins", {
            required: { value: true, message: t("common:required") },
          })}
          rules={{ min: 1 }}
          render={({ field }) => {
            const MIN_VALUE = 1;
            const value = field.value ?? 30;
            const setValue = (newValue: number) =>
              field.onChange(Math.max(newValue, MIN_VALUE));
            return (
              <NumberInput
                id="updateFrequencyInMins"
                value={value}
                min={MIN_VALUE}
                onPlus={() => setValue(value + 1)}
                onMinus={() => setValue(value - 1)}
                onChange={(event) => {
                  const newValue = Number(event.currentTarget.value);
                  setValue(!isNaN(newValue) ? newValue : 30);
                }}
              />
            );
          }}
        />
      </FormGroup>
      <SelectControl
        id="kc-type"
        name="category"
        label={t("category")}
        labelIcon={t("scopeTypeHelp")}
        controller={{ defaultValue: categories[0] }}
        options={categories.map((key) => ({
          key,
          value: key,
        }))}
      />
    </>
  );
};

export default GeneralSettings;
