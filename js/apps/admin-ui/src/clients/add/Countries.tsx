import { useTranslation } from "react-i18next";
import { useFetch, SelectControl } from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";
import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";
import { useAdminClient } from "../../admin-client";

export const Countries = () => {
  const { t } = useTranslation();
  const [countries, setCountries] = useState<any>({});
  const { adminClient } = useAdminClient();
  useFetch(
    async () => {
      const countries = await fetchAdminUI(adminClient, "countries");
      return { countries };
    },
    ({ countries }) => {
      if (countries && !Object.keys(countries).includes("error")) {
        setCountries(countries);
      }
    },
    [],
  );

  return (
    <SelectControl
      name={convertAttributeNameToForm<FormFields>("attributes.country")}
      label={t("country")}
      controller={{
        defaultValue: "",
      }}
      labelIcon={t("countryHelp")}
      options={Object.keys(countries).map((option) => ({
        key: option,
        value: countries[option],
      }))}
    />
  );
};
