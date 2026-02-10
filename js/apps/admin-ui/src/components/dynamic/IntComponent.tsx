import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { NumberComponentProps } from "./components";

export const IntComponent = ({
  name,
  label,
  helpText,
  convertToName,
  ...props
}: NumberComponentProps) => {
  const { t } = useTranslation();

  return (
    <TextControl
      name={convertToName(name!)}
      type="text"
      inputMode="numeric"
      pattern="[0-9]*"
      label={t(label!)}
      labelIcon={t(helpText!)}
      data-testid={name}
      onInput={(e) => {
        e.currentTarget.value = e.currentTarget.value.replace(/\D/g, "");
      }}
      rules={{
        required: {
          value: !!props.required,
          message: t("required"),
        },
        validate: (v) => /^\d*$/.test(String(v ?? "")) || t("shouldBeANumber"),
      }}
      {...props}
    />
  );
};
