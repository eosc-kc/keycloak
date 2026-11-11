import {
  Button,
  FormGroup,
  FormHelperText,
  HelperText,
  HelperTextItem,
  Switch,
  TextArea,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useEffect, useMemo } from "react";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import {
  FormErrorText,
  HelpItem,
  TextControl,
} from "@keycloak/keycloak-ui-shared";

import { FormAccess } from "../../components/form/FormAccess";
import { FixedButtonsGroup } from "../../components/form/FixedButtonGroup";
import {
  decodeAndValidateTrustMark,
  normalizeTrustMarkJwt,
  type TrustMarkFormValues,
} from "./trust-mark-utils";

type TrustMarkFormProps = {
  save: (values: TrustMarkFormValues) => void | Promise<void>;
  cancel: () => void;
  isExisting?: boolean;
};

const formatNumericDate = (value?: number) => {
  if (value === undefined) {
    return "";
  }
  try {
    return new Intl.DateTimeFormat(undefined, {
      dateStyle: "medium",
      timeStyle: "medium",
    }).format(new Date(value * 1000));
  } catch {
    return "";
  }
};

const FieldHelperText = ({ children }: { children: string }) => (
  <FormHelperText>
    <HelperText>
      <HelperTextItem>{children}</HelperTextItem>
    </HelperText>
  </FormHelperText>
);

const ReadOnlyText = ({
  id,
  label,
  value,
  helperText,
  helpText,
}: {
  id: string;
  label: string;
  value?: string;
  helperText: string;
  helpText: string;
}) => (
  <FormGroup
    label={label}
    labelIcon={<HelpItem helpText={helpText} fieldLabelId={id} />}
    fieldId={id}
  >
    <TextInput id={id} aria-label={label} value={value ?? ""} isDisabled />
    <FieldHelperText>{helperText}</FieldHelperText>
  </FormGroup>
);

const ReadOnlyExpiration = ({
  value,
  label,
  helperText,
  helpText,
  neverExpires,
}: {
  value?: number;
  label: string;
  helperText: string;
  helpText: string;
  neverExpires: string;
}) => (
  <FormGroup
    label={label}
    labelIcon={
      <HelpItem helpText={helpText} fieldLabelId="trustMarkExpiresAt" />
    }
    fieldId="trustMarkExpiresAt"
  >
    <TextInput
      id="trustMarkExpiresAt"
      data-testid="trustMarkExpiresAt"
      aria-label={label}
      value={value === undefined ? neverExpires : formatNumericDate(value)}
      isDisabled
    />
    <FieldHelperText>{helperText}</FieldHelperText>
  </FormGroup>
);

const isValidDateTime = (value: string) =>
  value !== "" && Number.isFinite(new Date(value).getTime());

const isPastDateTime = (value: string) =>
  new Date(value).getTime() < Date.now();

export const TrustMarkForm = ({
  save,
  cancel,
  isExisting = false,
}: TrustMarkFormProps) => {
  const { t } = useTranslation();
  const {
    clearErrors,
    control,
    handleSubmit,
    trigger,
    formState: { isValid, isSubmitting },
  } = useFormContext<TrustMarkFormValues>();

  const trustMarkType = useWatch({ control, name: "trustMarkType" }) ?? "";
  const selfSigned = useWatch({ control, name: "selfSigned" }) ?? false;
  const jwt = useWatch({ control, name: "jwt" }) ?? "";
  const validation = useMemo(
    () => decodeAndValidateTrustMark(jwt, trustMarkType),
    [jwt, trustMarkType],
  );

  useEffect(() => {
    if (!selfSigned && jwt) {
      void trigger("jwt");
    }
  }, [jwt, selfSigned, trustMarkType, trigger]);

  useEffect(() => {
    if (selfSigned) {
      clearErrors("jwt");
      void trigger(isExisting ? ["iat", "exp"] : "exp");
    } else {
      clearErrors(["iat", "exp"]);
      if (jwt) {
        void trigger("jwt");
      }
    }
  }, [clearErrors, isExisting, jwt, selfSigned, trigger]);

  const decoded = !selfSigned ? validation.decoded : undefined;

  return (
    <FormAccess
      isHorizontal
      role="manage-realm"
      className="pf-u-mt-lg"
      onSubmit={handleSubmit(save)}
    >
      <TextControl
        name="trustMarkType"
        label={t("trustMarkType")}
        labelIcon={t("trustMarkTypeHelp")}
        helperText={t("trustMarkTypeHint")}
        rules={{ required: t("required") }}
      />
      <FormGroup
        label={t("trustMarkSelfSigned")}
        labelIcon={
          <HelpItem
            helpText={t("trustMarkSelfSignedHelp")}
            fieldLabelId="trustMarkSelfSigned"
          />
        }
        fieldId="trustMarkSelfSigned"
        isRequired
      >
        <Controller
          name="selfSigned"
          control={control}
          render={({ field }) => (
            <Switch
              id="trustMarkSelfSigned"
              data-testid="trustMarkSelfSigned"
              label={t("on")}
              labelOff={t("off")}
              isChecked={field.value}
              isDisabled={isExisting}
              onChange={(_, checked) => field.onChange(checked)}
            />
          )}
        />
        <FieldHelperText>{t("trustMarkSelfSignedHint")}</FieldHelperText>
      </FormGroup>

      {!selfSigned && (
        <FormGroup
          label={t("trustMarkJwt")}
          labelIcon={
            <HelpItem
              helpText={t("trustMarkJwtHelp")}
              fieldLabelId="trustMarkJwt"
            />
          }
          fieldId="trustMarkJwt"
          isRequired
        >
          <Controller
            name="jwt"
            control={control}
            rules={{
              required: t("required"),
              validate: (value) => {
                const result = decodeAndValidateTrustMark(value, trustMarkType);
                return result.error ? t(result.error) : true;
              },
            }}
            render={({ field, fieldState }) => (
              <>
                <TextArea
                  {...field}
                  id="trustMarkJwt"
                  data-testid="trustMarkJwt"
                  aria-label={t("trustMarkJwt")}
                  onChange={(_, value) =>
                    field.onChange(normalizeTrustMarkJwt(value))
                  }
                  rows={10}
                  resizeOrientation="vertical"
                  validated={
                    fieldState.error
                      ? ValidatedOptions.error
                      : ValidatedOptions.default
                  }
                />
                {fieldState.error && (
                  <FormErrorText message={fieldState.error.message ?? ""} />
                )}
                <FieldHelperText>{t("trustMarkJwtHint")}</FieldHelperText>
              </>
            )}
          />
        </FormGroup>
      )}

      {selfSigned && (
        <>
          {isExisting && (
            <TextControl
              name="iat"
              label={t("trustMarkIssuedAt")}
              labelIcon={t("trustMarkIssuedAtHelp")}
              type="datetime-local"
              step={1}
              helperText={t("trustMarkIssuedAtHint")}
              isDisabled
              rules={{
                required: t("required"),
                validate: (value) =>
                  isValidDateTime(value) || t("trustMarkInvalidDateTime"),
              }}
            />
          )}
          <TextControl
            name="exp"
            label={t("trustMarkExpiresAt")}
            labelIcon={t("trustMarkExpiresAtHelp")}
            type="datetime-local"
            step={1}
            helperText={t("trustMarkExpiresAtHint")}
            rules={{
              validate: (value) =>
                !value ||
                (isValidDateTime(value) && !isPastDateTime(value)) ||
                t("trustMarkExpirationInPast"),
            }}
          />
          <TextControl
            name="logoUri"
            label={t("trustMarkLogoUri")}
            labelIcon={t("trustMarkLogoUriHelp")}
            helperText={t("trustMarkLogoUriHint")}
          />
          <TextControl
            name="ref"
            label={t("trustMarkReference")}
            labelIcon={t("trustMarkReferenceHelp")}
            helperText={t("trustMarkReferenceHint")}
          />
          <FormGroup
            label={t("trustMarkDelegation")}
            labelIcon={
              <HelpItem
                helpText={t("trustMarkDelegationHelp")}
                fieldLabelId="trustMarkDelegation"
              />
            }
            fieldId="trustMarkDelegation"
          >
            <Controller
              name="delegation"
              control={control}
              render={({ field }) => (
                <TextArea
                  {...field}
                  id="trustMarkDelegation"
                  data-testid="trustMarkDelegation"
                  aria-label={t("trustMarkDelegation")}
                  rows={4}
                  resizeOrientation="vertical"
                />
              )}
            />
            <FieldHelperText>{t("trustMarkDelegationHint")}</FieldHelperText>
          </FormGroup>
        </>
      )}

      {decoded && (
        <>
          <ReadOnlyText
            id="trustMarkIssuer"
            label={t("trustMarkIssuer")}
            value={decoded.issuer}
            helperText={t("trustMarkIssuerHint")}
            helpText={t("trustMarkIssuerHelp")}
          />
          <ReadOnlyText
            id="trustMarkSubject"
            label={t("trustMarkSubject")}
            value={decoded.subject}
            helperText={t("trustMarkSubjectHint")}
            helpText={t("trustMarkSubjectHelp")}
          />
          <ReadOnlyText
            id="trustMarkIssuedAt"
            label={t("trustMarkIssuedAt")}
            value={formatNumericDate(decoded.issuedAt)}
            helperText={t("trustMarkIssuedAtHint")}
            helpText={t("trustMarkIssuedAtHelp")}
          />
          <ReadOnlyExpiration
            label={t("trustMarkExpiresAt")}
            value={decoded.expiresAt}
            helperText={t("trustMarkExpiresAtHint")}
            helpText={t("trustMarkExpiresAtHelp")}
            neverExpires={t("tokenLifespan.never")}
          />
          <ReadOnlyText
            id="trustMarkLogoUri"
            label={t("trustMarkLogoUri")}
            value={decoded.logoUri}
            helperText={t("trustMarkLogoUriHint")}
            helpText={t("trustMarkLogoUriHelp")}
          />
          <ReadOnlyText
            id="trustMarkReference"
            label={t("trustMarkReference")}
            value={decoded.ref}
            helperText={t("trustMarkReferenceHint")}
            helpText={t("trustMarkReferenceHelp")}
          />
          <FormGroup
            label={t("trustMarkDelegation")}
            labelIcon={
              <HelpItem
                helpText={t("trustMarkDelegationHelp")}
                fieldLabelId="trustMarkDelegation"
              />
            }
            fieldId="trustMarkDelegation"
          >
            <TextArea
              id="trustMarkDelegation"
              aria-label={t("trustMarkDelegation")}
              value={decoded.delegation ?? ""}
              rows={4}
              isDisabled
              resizeOrientation="vertical"
            />
            <FieldHelperText>{t("trustMarkDelegationHint")}</FieldHelperText>
          </FormGroup>
        </>
      )}

      <FixedButtonsGroup
        name="trust-mark"
        isSubmit
        isDisabled={!isValid || (!selfSigned && !decoded) || isSubmitting}
      >
        <Button type="button" variant="link" onClick={cancel}>
          {t("cancel")}
        </Button>
      </FixedButtonsGroup>
    </FormAccess>
  );
};
