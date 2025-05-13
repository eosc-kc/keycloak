import type IdentityFederationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityFederationRepresentation";
import {
  FormGroup,
  NumberInput,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useState } from "react";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { HelpItem } from "ui-shared";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { useRealm } from "../../context/realm-context/RealmContext";
import environment from "../../environment";
import { PrincipalTable } from "../../identity-providers/component/PrincipalTable";
import { SwitchField } from "../../identity-providers/component/SwitchField";

const assertionsEncryptedOptions = ["true", "false", "optional"];

type IdentityProviderFederationConfigProps = {
  type: string;
};

const IdentityProviderFederationConfig = ({
  type,
}: IdentityProviderFederationConfigProps) => {
  const {
    register,
    control,
    formState: { errors },
  } = useFormContext<IdentityFederationRepresentation>();
  const { realm } = useRealm();
  const { t } = useTranslation();
  const syncModes = ["import", "force"];
  const [syncModeOpen, setSyncModeOpen] = useState(false);
  const [namedPolicyDropdownOpen, setNamedPolicyDropdownOpen] = useState(false);
  const [wantAssertionsEncryptedOpen, setWantAssertionsEncryptedOpen] =
    useState(false);

  return (
    <>
      <FormGroup
        label={t("identity-federations:serviceProviderEntityId")}
        fieldId="kc-service-provider-entity-id"
        labelIcon={
          <HelpItem
            helpText={t("identity-federations-help:serviceProviderEntityId")}
            fieldLabelId="identity-federations:serviceProviderEntityId"
          />
        }
        isRequired
        helperTextInvalid={t("common:required")}
        validated={errors.config?.entityId ? "error" : "default"}
      >
        <KeycloakTextInput
          data-testid="serviceProviderEntityId"
          id="kc-service-provider-entity-id"
          validated={errors.config?.entityId ? "error" : "default"}
          defaultValue={`${environment.authServerUrl}/realms/${realm}`}
          {...register("config.entityId", { required: true })}
        />
      </FormGroup>
      <FormGroup
        className="pf-u-pb-3xl"
        label={t("identity-federations:syncMode")}
        labelIcon={
          <HelpItem
            helpText={t("identity-federations-help:syncMode")}
            fieldLabelId="identity-federations:syncMode"
          />
        }
        fieldId="syncMode"
      >
        <Controller
          name="config.syncMode"
          defaultValue={syncModes[0].toUpperCase()}
          control={control}
          render={({ field }) => (
            <Select
              toggleId="syncMode"
              required
              direction="up"
              onToggle={() => setSyncModeOpen(!syncModeOpen)}
              onSelect={(_, value) => {
                field.onChange(value as string);
                setSyncModeOpen(false);
              }}
              selections={t(
                `identity-providers:syncModes.${field.value.toLowerCase()}`,
              )}
              variant={SelectVariant.single}
              aria-label={t("syncMode")}
              isOpen={syncModeOpen}
            >
              {syncModes.map((option) => (
                <SelectOption
                  selected={option === field.value}
                  key={option}
                  value={option.toUpperCase()}
                >
                  {t(`identity-providers:syncModes.${option}`)}
                </SelectOption>
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("identity-federations:nameIdPolicyFormat")}
        labelIcon={
          <HelpItem
            helpText={t("identity-federations-help:nameIdPolicyFormat")}
            fieldLabelId="identity-federations:nameIdPolicyFormat"
          />
        }
        fieldId="kc-nameIdPolicyFormat"
        helperTextInvalid={t("common:required")}
      >
        <Controller
          name="config.nameIDPolicyFormat"
          defaultValue={
            type === "edit"
              ? "isNull"
              : "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"
          }
          control={control}
          render={({ field }) => (
            <Select
              toggleId="kc-nameIdPolicyFormat"
              onToggle={(isExpanded) => setNamedPolicyDropdownOpen(isExpanded)}
              isOpen={namedPolicyDropdownOpen}
              onSelect={(_, value) => {
                field.onChange(value);
                setNamedPolicyDropdownOpen(false);
              }}
              selections={field.value}
              variant={SelectVariant.single}
            >
              <SelectOption
                data-testid="empty-option"
                value={"isNull"}
                isPlaceholder={!field.value}
              >
                {t("identity-providers:empty")}
              </SelectOption>
              <SelectOption
                data-testid="persistent-option"
                value={"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"}
                isPlaceholder={!!field.value}
              >
                {t("identity-providers:persistent")}
              </SelectOption>
              <SelectOption
                data-testid="transient-option"
                value="urn:oasis:names:tc:SAML:2.0:nameid-format:transient"
              >
                {t("identity-providers:transient")}
              </SelectOption>
              <SelectOption
                data-testid="email-option"
                value="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"
              >
                {t("identity-providers:email")}
              </SelectOption>
              <SelectOption
                data-testid="kerberos-option"
                value="urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos"
              >
                {t("identity-providers:kerberos")}
              </SelectOption>

              <SelectOption
                data-testid="x509-option"
                value="urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName"
              >
                {t("identity-providers:x509")}
              </SelectOption>

              <SelectOption
                data-testid="windowsDomainQN-option"
                value="urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName"
              >
                {t("identity-providers:windowsDomainQN")}
              </SelectOption>

              <SelectOption
                data-testid="unspecified-option"
                value={"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified"}
              >
                {t("identity-providers:unspecified")}
              </SelectOption>
            </Select>
          )}
        ></Controller>
      </FormGroup>
      <PrincipalTable required={true} readOnly={false} />
      <SwitchField
        field="config.postBindingResponse"
        label="httpPostBindingResponse"
        isReadOnly={false}
      />
      <SwitchField
        field="config.postBindingLogout"
        label="httpPostBindingLogout"
        isReadOnly={false}
      />
      <SwitchField
        field="config.wantAuthnRequestsSigned"
        label="wantAuthnRequestsSigned"
        isReadOnly={false}
      />
      <SwitchField
        field="config.wantLogoutRequestsSigned"
        label="wantLogoutRequestsSigned"
        isReadOnly={false}
      />
      <SwitchField
        field="config.wantAssertionsSigned"
        label="wantAssertionsSigned"
        isReadOnly={false}
      />
      <FormGroup
        label={t("identity-federations:wantAssertionsEncrypted")}
        isRequired
        labelIcon={
          <HelpItem
            helpText={t("identity-federations-help:wantAssertionsEncrypted")}
            fieldLabelId="identity-providers:syncModeOverride"
          />
        }
      >
        <Controller
          name="config.wantAssertionsEncrypted"
          defaultValue={assertionsEncryptedOptions[0]}
          control={control}
          render={({ field }) => (
            <Select
              toggleId="config.wantAssertionsEncrypted"
              required
              direction="down"
              onToggle={() =>
                setWantAssertionsEncryptedOpen(!wantAssertionsEncryptedOpen)
              }
              onSelect={(_, value) => {
                field.onChange(value.toString());
                setWantAssertionsEncryptedOpen(false);
              }}
              selections={field.value}
              variant={SelectVariant.single}
              aria-label={t("identity-federations:wantAssertionsEncrypted")}
              isOpen={wantAssertionsEncryptedOpen}
            >
              {assertionsEncryptedOptions.map((option) => (
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
        />
      </FormGroup>
      <FormGroup
        label={t("identity-federations:attributeConsumingServiceIndex")}
        labelIcon={
          <HelpItem
            helpText={t(
              "identity-federations-help:attributeConsumingServiceIndex",
            )}
            fieldLabelId="identity-federations:attributeConsumingServiceIndex"
          />
        }
      >
        <Controller
          name="config.attributeConsumingServiceIndex"
          defaultValue={1}
          control={control}
          render={({ field }) => {
            const v = Number(field.value);
            return (
              <NumberInput
                data-testid="attributeConsumingServiceIndex"
                inputName="attributeConsumingServiceIndex"
                min={0}
                max={2147483}
                value={v}
                readOnly
                onPlus={() => field.onChange(v + 1)}
                onMinus={() => field.onChange(v - 1)}
                onChange={(event) => {
                  const value = Number(
                    (event.target as HTMLInputElement).value,
                  );
                  field.onChange(value < 0 ? 0 : value);
                }}
              />
            );
          }}
        />
      </FormGroup>
      <SwitchField
        field="config.omitAttributeConsumingServiceIndexAuthn"
        label="omitAttributeConsumingServiceIndexAuthn"
        data-testid="omitAttributeConsumingServiceIndexAuthn"
        isReadOnly={false}
      />
      <FormGroup
        label={t("identity-federations:attributeConsumingServiceName")}
        labelIcon={
          <HelpItem
            helpText={t(
              "identity-federations-help:attributeConsumingServiceName",
            )}
            fieldLabelId="identity-federations:attributeConsumingServiceName"
          />
        }
        helperTextInvalid={t("common:required")}
      >
        <KeycloakTextInput
          {...register("config.attributeConsumingServiceName")}
        />
      </FormGroup>
      <SwitchField
        field="config.signSpMetadata"
        label="signSpMetadata"
        isReadOnly={false}
      />
      <SwitchField
        field="config.passSetMfa"
        label="passSetMfa"
        isReadOnly={false}
      />
    </>
  );
};

export default IdentityProviderFederationConfig;
