import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  PageSection,
  InputGroup,
  TextInput,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../components/form/FormAccess";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { MinusIcon, PlusIcon } from "@patternfly/react-icons";

type RealmSettingsThemesTabProps = {
  realm: RealmRepresentation;
  save: (realm: RealmRepresentation) => void;
};

export const OpenIdEndpointConfigurationTab = ({
  realm,
  save,
}: RealmSettingsThemesTabProps) => {
  const { t } = useTranslation();

  const [newClaim, setNewClaim] = useState("");
  const { control, handleSubmit, setValue, reset } =
    useForm<RealmRepresentation>();

  const setupForm = () => {
    reset({
      ...realm,
      claimsSupported: realm.claimsSupported || [],
    });
    setNewClaim("");
  };

  useEffect(() => {
    setupForm();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [realm]);

  const onSubmit = (form: RealmRepresentation) => {
    // Make sure we filter out empty strings
    const cleanedClaims =
      form.claimsSupported?.map((c) => c.trim()).filter(Boolean) || [];

    save({
      ...realm,
      ...form,
      claimsSupported: cleanedClaims,
    });
  };

  return (
    <PageSection variant="light">
      <FormAccess
        isHorizontal
        role="manage-realm"
        className="pf-u-mt-lg"
        onSubmit={handleSubmit(onSubmit)}
      >
        <FormGroup
          label={t("claimsSupported")}
          fieldId="kc-claims-supported"
          labelIcon={
            <HelpItem
              helpText={t("claimsSupportedHelp")}
              fieldLabelId="claimsSupported"
            />
          }
        >
          <Controller
            name="claimsSupported"
            control={control}
            defaultValue={realm.claimsSupported || []}
            render={({ field }) => {
              const claimsSupported = field.value || [];

              return (
                <>
                  {claimsSupported.map((claim: string, index: number) => (
                    <InputGroup key={index}>
                      <TextInput
                        value={claim}
                        onChange={(_event, value) => {
                          const updated = [...claimsSupported];
                          updated[index] = value;
                          field.onChange(updated);
                        }}
                        aria-label={t("edit-claim-input")}
                      />
                      <Button
                        icon={<MinusIcon />}
                        aria-label={t("remove-claim-button")}
                        onClick={() => {
                          const updated = claimsSupported.filter(
                            (_c, i) => i !== index,
                          );
                          field.onChange(updated);
                        }}
                        variant="control"
                      />
                    </InputGroup>
                  ))}
                  <InputGroup>
                    <TextInput
                      value={newClaim}
                      onChange={(_event, value) => {
                        setNewClaim(value);
                      }}
                      aria-label={t("new-claim-input")}
                    />
                    <Button
                      icon={<PlusIcon />}
                      aria-label={t("add-claim-button")}
                      onClick={() => {
                        const trimmed = newClaim.trim();
                        if (trimmed) {
                          const updated = [...claimsSupported, trimmed];
                          setNewClaim("");
                          field.onChange(updated);
                        }
                      }}
                      variant="control"
                    />
                  </InputGroup>
                </>
              );
            }}
          />
        </FormGroup>

        <ActionGroup>
          <Button
            variant="primary"
            type="submit"
            data-testid="openid-configuration-tab-save"
            aria-label={t("realm-settings:submit")}
          >
            {t("save")}
          </Button>
          <Button
            variant="link"
            onClick={() => {
              setupForm();
            }}
          >
            {t("revert")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
};
