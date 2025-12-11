import type IdentityFederationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityFederationRepresentation";
import { FormGroup, NumberInput } from "@patternfly/react-core";
import {
  HelpItem,
  SelectControl,
  TextControl,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext, useWatch } from "react-hook-form";
import { useRealm } from "../../context/realm-context/RealmContext";
import { SwitchField } from "../../identity-providers/component/SwitchField";
import { Environment } from "../../environment";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { PrincipalTable } from "../../identity-providers/component/PrincipalTable";

const assertionsEncryptedOptions = ["true", "false", "optional"];

type IdentityProviderFederationConfigProps = {
  readOnly?: boolean;
  type: string;
};

const IdentityProviderFederationConfig = ({
  readOnly = false,
}: IdentityProviderFederationConfigProps) => {
  const { control } = useFormContext<IdentityFederationRepresentation>();
  const { realm } = useRealm();
  const { t } = useTranslation();
  const SYNC_MODES = ["IMPORT", "FORCE"];
  const { environment } = useEnvironment<Environment>();
  const principalType = useWatch({
    control,
    name: "config.principalType",
  });
  return (
    <>
      <TextControl
        name="config.entityId"
        defaultValue={`${environment.serverBaseUrl}/realms/${realm}`}
        label={t("serviceProviderEntityId")}
        labelIcon={t("identityFederationServiceProviderEntityIdHelp")}
        rules={{ required: t("required") }}
      />

      <SelectControl
        name="config.syncMode"
        label={t("syncMode")}
        labelIcon={t("syncModeHelp")}
        options={SYNC_MODES.map((syncMode) => ({
          key: syncMode,
          value: t(`syncModes.${syncMode.toLocaleLowerCase()}`),
        }))}
        controller={{
          defaultValue: SYNC_MODES[0],
          rules: { required: t("required") },
        }}
      />
      <SelectControl
        name="config.nameIDPolicyFormat"
        label={t("nameIdPolicyFormat")}
        labelIcon={t("identityFederationNameIdPolicyFormatHelp")}
        controller={{
          defaultValue: "isNull",
        }}
        options={[
          {
            key: "isNull",
            value: t("nameIdPolicyFormatEmpty"),
          },
          {
            key: "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent",
            value: t("persistent"),
          },
          {
            key: "urn:oasis:names:tc:SAML:2.0:nameid-format:transient",
            value: t("transient"),
          },
          {
            key: "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
            value: t("email"),
          },
          {
            key: "urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos",
            value: t("kerberos"),
          },
          {
            key: "urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName",
            value: t("x509"),
          },
          {
            key: "urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName",
            value: t("windowsDomainQN"),
          },
          {
            key: "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified",
            value: t("unspecified"),
          },
        ]}
      />
      {principalType?.includes("ATTRIBUTE") && (
        <TextControl
          name="config.principalAttribute"
          label={t("principalAttribute")}
          labelIcon={t("principalAttributeHelp")}
          readOnly={readOnly}
        />
      )}
      <PrincipalTable required={true} readOnly={false} />
      <DefaultSwitchControl
        name="config.postBindingResponse"
        label={t("httpPostBindingAuthnRequest")}
        labelIcon={t("httpPostBindingAuthnRequestHelp")}
        isDisabled={readOnly}
        stringify
      />

      <DefaultSwitchControl
        name="config.postBindingLogout"
        label={t("httpPostBindingLogout")}
        labelIcon={t("httpPostBindingLogoutHelp")}
        isDisabled={readOnly}
        stringify
      />

      <DefaultSwitchControl
        name="config.wantAuthnRequestsSigned"
        label={t("wantAuthnRequestsSigned")}
        labelIcon={t("wantAuthnRequestsSignedHelp")}
        isDisabled={readOnly}
        stringify
      />
      <DefaultSwitchControl
        name="config.wantLogoutRequestsSigned"
        label={t("wantLogoutRequestsSigned")}
        labelIcon={t("wantLogoutRequestsSignedHelp")}
        isDisabled={readOnly}
        stringify
      />
      <DefaultSwitchControl
        name="config.wantAssertionsSigned"
        label={t("wantAssertionsSigned")}
        isDisabled={readOnly}
        stringify
      />
      <SelectControl
        id="kc-type"
        name="config.wantAssertionsEncrypted"
        label={t("wantAssertionsEncrypted")}
        labelIcon={t("wantAssertionsEncryptedHelp")}
        controller={{ defaultValue: assertionsEncryptedOptions[0] }}
        options={assertionsEncryptedOptions.map((key) => ({
          key,
          value: key,
        }))}
      />
      <FormGroup
        label={t("attributeConsumingServiceIndex")}
        labelIcon={
          <HelpItem
            helpText={t("attributeConsumingServiceIndexHelp")}
            fieldLabelId="attributeConsumingServiceIndex"
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
      <DefaultSwitchControl
        name="config.omitAttributeConsumingServiceIndexAuthn"
        label={t("omitAttributeConsumingServiceIndexAuthn")}
        labelIcon={t("omitAttributeConsumingServiceIndexAuthnFederationHelp")}
        isDisabled={readOnly}
        stringify
      />
      <TextControl
        name="config.attributeConsumingServiceName"
        label={t("attributeConsumingServiceName")}
        labelIcon={t("attributeConsumingServiceNameHelp")}
        rules={{ required: t("required") }}
      />
      <DefaultSwitchControl
        name="config.signSpMetadata"
        label={t("signServiceProviderMetadata")}
        isDisabled={readOnly}
        stringify
      />
      <SwitchField
        field="config.passSetMfa"
        label="passSetMfaFederation"
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
