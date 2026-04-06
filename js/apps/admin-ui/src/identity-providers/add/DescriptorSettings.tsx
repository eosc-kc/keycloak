import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  ExpandableSection,
  MenuToggle,
  MenuToggleElement,
  Select,
  SelectList,
  SelectOption,
} from "@patternfly/react-core";
import { useRef, useState } from "react";
import {
  Controller,
  FormProvider,
  useFormContext,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  NumberControl,
  SelectControl,
  TextAreaControl,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { DefaultSwitchControl } from "../../components/SwitchControl";

import "./discovery-settings.css";
import { AutoUpdateFields } from "../component/AutoUpdateFields";
import { PrincipalTable } from "../component/PrincipalTable";

type DescriptorSettingsProps = {
  readOnly: boolean;
};

const Fields = ({ readOnly }: DescriptorSettingsProps) => {
  const { t } = useTranslation();

  const form = useFormContext<IdentityProviderRepresentation>();
  const { control } = form;

  const wantAuthnSigned = useWatch({
    control,
    name: "config.wantAuthnRequestsSigned",
  });

  const wantAssertionsEncrypted = useWatch({
    control,
    name: "config.wantAssertionsEncrypted",
  });

  const validateSignature = useWatch({
    control,
    name: "config.validateSignature",
  });

  const useMetadataDescriptorUrl = useWatch({
    control,
    name: "config.useMetadataDescriptorUrl",
  });

  const [isNameIdPolicyOpen, setIsNameIdPolicyOpen] = useState(false);

  const nameIdPolicyOptions = [
    { key: "isNull", value: t("emptyNameIdPolicy") },
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
  ];

  return (
    <div className="pf-v5-c-form pf-m-horizontal">
      <FormProvider {...form}>
        {!readOnly && <AutoUpdateFields protocol={"saml"} />}
        <TextControl
          name="config.entityId"
          label={t("serviceProviderEntityId")}
          labelIcon={t("serviceProviderEntityIdHelp")}
        />
        <TextControl
          name="config.idpEntityId"
          label={t("identityProviderEntityId")}
          labelIcon={t("identityProviderEntityIdHelp")}
          data-testid="identityProviderEntityId"
          id="kc-identity-provider-entity-id"
        />
        <TextControl
          name="config.singleSignOnServiceUrl"
          label={t("ssoServiceUrl")}
          labelIcon={t("ssoServiceUrlHelp")}
          type="url"
          readOnly={readOnly}
          rules={{ required: t("required") }}
        />
        <TextControl
          name="config.artifactResolutionServiceUrl"
          label={t("artifactResolutionServiceUrl")}
          labelIcon={t("artifactResolutionServiceUrlHelp")}
          type="url"
          isDisabled={readOnly}
        />
        <TextControl
          name="config.singleLogoutServiceUrl"
          label={t("singleLogoutServiceUrl")}
          labelIcon={t("singleLogoutServiceUrlHelp")}
          type="url"
          readOnly={readOnly}
        />
        <DefaultSwitchControl
          name="config.backchannelSupported"
          label={t("backchannelLogout")}
          isDisabled={readOnly}
          stringify
        />
        <DefaultSwitchControl
          name="config.sendIdTokenOnLogout"
          label={t("sendIdTokenOnLogout")}
          defaultValue={"true"}
          isDisabled={readOnly}
          stringify
        />
        <DefaultSwitchControl
          name="config.sendClientIdOnLogout"
          label={t("sendClientIdOnLogout")}
          isDisabled={readOnly}
          stringify
        />

        <Controller
          name="config.nameIDPolicyFormat"
          control={control}
          defaultValue={
            !readOnly
              ? "isNull"
              : "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"
          }
          render={({ field }) => {
            const selectedKey = field.value || "isNull";
            const selectedOption =
              nameIdPolicyOptions.find(
                (option) => option.key === selectedKey,
              ) || nameIdPolicyOptions[0];

            return (
              <div className="pf-v5-c-form__group">
                <div className="pf-v5-c-form__group-label">
                  <label
                    className="pf-v5-c-form__label"
                    htmlFor="kc-nameIdPolicyFormat"
                  >
                    <span className="pf-v5-c-form__label-text">
                      {t("nameIdPolicyFormat")}
                    </span>
                  </label>
                </div>
                <div className="pf-v5-c-form__group-control">
                  <Select
                    id="kc-nameIdPolicyFormat"
                    isOpen={isNameIdPolicyOpen}
                    selected={selectedKey}
                    onOpenChange={(isOpen) => setIsNameIdPolicyOpen(isOpen)}
                    onSelect={(_event, value) => {
                      field.onChange(value as string);
                      setIsNameIdPolicyOpen(false);
                    }}
                    toggle={(toggleRef) => (
                      <MenuToggle
                        ref={toggleRef}
                        onClick={() =>
                          setIsNameIdPolicyOpen(!isNameIdPolicyOpen)
                        }
                        isExpanded={isNameIdPolicyOpen}
                        isDisabled={readOnly}
                      >
                        {selectedOption.value}
                      </MenuToggle>
                    )}
                  >
                    <SelectList>
                      {nameIdPolicyOptions.map((option) => (
                        <SelectOption
                          key={option.key}
                          value={option.key}
                          isSelected={selectedKey === option.key}
                          data-testid={
                            option.key === "isNull" ? "empty-option" : undefined
                          }
                        >
                          {option.value}
                        </SelectOption>
                      ))}
                    </SelectList>
                  </Select>
                </div>
              </div>
            );
          }}
        />

        <PrincipalTable readOnly={readOnly} />
        <DefaultSwitchControl
          name="config.allowCreate"
          label={t("allowCreate")}
          isDisabled={readOnly}
          stringify
        />

        <DefaultSwitchControl
          name="config.postBindingResponse"
          label={t("httpPostBindingResponse")}
          isDisabled={readOnly}
          stringify
        />

        <DefaultSwitchControl
          name="config.artifactBindingResponse"
          label={t("artifactBindingResponse")}
          isDisabled={readOnly}
          stringify
        />

        <DefaultSwitchControl
          name="config.postBindingAuthnRequest"
          label={t("httpPostBindingAuthnRequest")}
          isDisabled={readOnly}
          stringify
        />

        <DefaultSwitchControl
          name="config.postBindingLogout"
          label={t("httpPostBindingLogout")}
          isDisabled={readOnly}
          stringify
        />

        <DefaultSwitchControl
          name="config.postBindingLogoutReceivingRequest"
          label={t("postBindingLogoutReceivingRequest")}
          labelIcon={t("postBindingLogoutReceivingRequestHelp")}
          isDisabled={readOnly}
          stringify
        />

        <DefaultSwitchControl
          name="config.wantAuthnRequestsSigned"
          label={t("wantAuthnRequestsSigned")}
          isDisabled={readOnly}
          stringify
        />

        {wantAuthnSigned === "true" && (
          <>
            <SelectControl
              name="config.signatureAlgorithm"
              label={t("signatureAlgorithm")}
              labelIcon={t("signatureAlgorithmHelp")}
              isDisabled={readOnly}
              controller={{
                defaultValue: "RSA_SHA256",
              }}
              options={[
                "RSA_SHA1",
                "RSA_SHA256",
                "RSA_SHA256_MGF1",
                "RSA_SHA512",
                "RSA_SHA512_MGF1",
                "DSA_SHA1",
              ]}
            />
            <SelectControl
              name="config.xmlSigKeyInfoKeyNameTransformer"
              label={t("samlSignatureKeyName")}
              labelIcon={t("samlSignatureKeyNameHelp")}
              isDisabled={readOnly}
              controller={{
                defaultValue: t("keyID"),
              }}
              options={["NONE", t("keyID"), t("certSubject")]}
            />
          </>
        )}
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
        <DefaultSwitchControl
          name="config.wantAssertionsEncrypted"
          label={t("wantAssertionsEncrypted")}
          isDisabled={readOnly}
          stringify
        />
        {wantAssertionsEncrypted === "true" && (
          <SelectControl
            name="config.encryptionAlgorithm"
            label={t("encryptionAlgorithm")}
            labelIcon={t("encryptionAlgorithmHelp")}
            isDisabled={readOnly}
            controller={{
              defaultValue: "RSA-OAEP",
            }}
            options={["RSA-OAEP", "RSA1_5"]}
          />
        )}

        <DefaultSwitchControl
          name="config.forceAuthn"
          label={t("forceAuthentication")}
          isDisabled={readOnly}
          stringify
        />
        <DefaultSwitchControl
          name="config.validateSignature"
          label={t("validateSignature")}
          isDisabled={readOnly}
          stringify
        />
        {validateSignature === "true" && (
          <>
            <TextControl
              name="config.metadataDescriptorUrl"
              label={t("metadataDescriptorUrl")}
              labelIcon={t("metadataDescriptorUrlHelp")}
              type="url"
              readOnly={readOnly}
              rules={{
                required: {
                  value: useMetadataDescriptorUrl === "true",
                  message: t("required"),
                },
              }}
            />
            <DefaultSwitchControl
              name="config.useMetadataDescriptorUrl"
              label={t("useMetadataDescriptorUrl")}
              isDisabled={readOnly}
              stringify
            />
            {useMetadataDescriptorUrl !== "true" && (
              <TextAreaControl
                name="config.signingCertificate"
                label={t("validatingX509Certs")}
                labelIcon={t("validatingX509CertsHelp")}
                readOnly={readOnly}
              />
            )}
          </>
        )}
        <DefaultSwitchControl
          name="config.signSpMetadata"
          label={t("signServiceProviderMetadata")}
          isDisabled={readOnly}
          stringify
        />
        <DefaultSwitchControl
          name="config.loginHint"
          label={t("passSubject")}
          isDisabled={readOnly}
          stringify
        />
        <NumberControl
          name="config.allowedClockSkew"
          label={t("allowedClockSkew")}
          labelIcon={t("allowedClockSkewHelp")}
          controller={{ defaultValue: 0, rules: { min: 0, max: 2147483 } }}
          isDisabled={readOnly}
        />
        <NumberControl
          name="config.attributeConsumingServiceIndex"
          label={t("attributeConsumingServiceIndex")}
          labelIcon={t("attributeConsumingServiceIndexHelp")}
          controller={{ defaultValue: 0, rules: { min: 0, max: 2147483 } }}
          isDisabled={readOnly}
        />
        <DefaultSwitchControl
          name="config.omitAttributeConsumingServiceIndexAuthn"
          label={t("omitAttributeConsumingServiceIndexAuthn")}
          labelIcon={t("omitAttributeConsumingServiceIndexAuthnHelp")}
          isDisabled={readOnly}
          stringify
        />
        <TextControl
          name="config.attributeConsumingServiceName"
          label={t("attributeConsumingServiceName")}
          labelIcon={t("attributeConsumingServiceNameHelp")}
          readOnly={readOnly}
        />
      </FormProvider>
    </div>
  );
};

export const DescriptorSettings = ({ readOnly }: DescriptorSettingsProps) => {
  const { t } = useTranslation();
  const [isExpanded, setIsExpanded] = useState(false);

  return readOnly ? (
    <ExpandableSection
      className="keycloak__discovery-settings__metadata"
      toggleText={isExpanded ? t("hideMetaData") : t("showMetaData")}
      onToggle={(_event, isOpen) => setIsExpanded(isOpen)}
      isExpanded={isExpanded}
    >
      <Fields readOnly={readOnly} />
    </ExpandableSection>
  ) : (
    <Fields readOnly={readOnly} />
  );
};
