import type IdentityFederationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityFederationRepresentation";
import {
  FormGroup,
  NumberInput,
} from "@patternfly/react-core";
import { useState, useEffect } from "react";
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
  const {
    register,
    trigger,
    control,
    setValue,
    formState: { errors },
  } = useFormContext<IdentityFederationRepresentation>();
  const { t } = useTranslation();

  const [categoriesOpen, setCategoriesOpen] = useState(false);
  useEffect(() => {
    setValue("updateFrequencyInMins", 30);
    trigger("updateFrequencyInMins");
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
        {/* <Controller
          name="category"
          defaultValue={categories[0]}
          control={control}
          render={({ field }) => (
            <Select
              //   toggleId="categories"
              required
              direction="down"
              onToggle={() => setCategoriesOpen(!categoriesOpen)}
              onSelect={(_, value) => {
                field.onChange(value?.toString());
                setCategoriesOpen(false);
              }}
              selections={field.value}
              variant={"default"}
              aria-label={t("category")}
              isOpen={categoriesOpen}
            >
              {categories.map((option) => (
                <SelectOption
                  selected={option === field.value}
                  key={option}
                  data-testid={option}
                  value={option}
                >
                  {option}
                </SelectOption>
              ))}
            </Select>
          )}
        /> */}
    </>
  );
};

export default GeneralSettings;
