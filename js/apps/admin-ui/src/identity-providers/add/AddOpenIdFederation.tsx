import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import type OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/openIdFederationRepresentation";

import { ActionGroup, AlertVariant, Button, PageSection } from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";

import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";

import { FormAccess } from "../../components/form/FormAccess";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";

import { toIdentityProvider } from "../routes/IdentityProvider";
import { toIdentityProviders } from "../routes/IdentityProviders";

import { OpenIdFederationSettings } from "./OpenIdFederationSettings";
import { OIDCGeneralSettings } from "./OIDCGeneralSettings";

export default function AddIdentityProvider() {
  const { t } = useTranslation();
  const providerId = "openid-federation";

  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();

  const { realm } = useRealm();

  const form = useForm<IdentityProviderRepresentation>({
    // keep values/rules when sections mount/unmount
    shouldUnregister: false,
  });

  const {
    handleSubmit,
    formState: { isDirty },
  } = form;

  const [trustAnchors, setTrustAnchors] = useState<string[]>([]);

  // KC26 style: no useFetch; fetch locally (avoid dependency on legacy hook)
  // If you already have a shared useFetch from keycloak-ui-shared in your repo, you can swap it in.
  useState(() => {
    (async () => {
      try {
        const openIdFederations = await adminClient.openIdFederations.find({
          realm,
        });
        setTrustAnchors(
          (openIdFederations ?? [])
            .map((f: OpenIdFederationRepresentation) => f.trustAnchor)
            .filter((x: any): x is string => Boolean(x)),
        );
      } catch {
        setTrustAnchors([]);
      }
    })();
  });

  const onSubmit = async (provider: IdentityProviderRepresentation) => {
    try {
      await adminClient.identityProviders.create({
        ...provider,
        providerId,
      });

      addAlert(t("createSuccess"), AlertVariant.success);

      navigate(
        toIdentityProvider({
          realm,
          providerId,
          alias: provider.alias!,
          tab: "settings",
        }),
      );
    } catch (error) {
      addError("createError", error);
    }
  };

  return (
    <>
      <ViewHeader titleKey="addOpenIdFederationProvider" />
      <PageSection variant="light">
        <FormProvider {...form}>
          <FormAccess
            role="manage-identity-providers"
            isHorizontal
            onSubmit={handleSubmit(onSubmit)}
          >
            <OIDCGeneralSettings/>
            <OpenIdFederationSettings
              readOnly={false}
              create
              trustAnchors={trustAnchors}
            />

            <ActionGroup>
              <Button
                isDisabled={!isDirty}
                variant="primary"
                type="submit"
                data-testid="createProvider"
              >
                {t("add")}
              </Button>
              <Button
                variant="link"
                data-testid="cancel"
                component={(props) => (
                  <Link {...props} to={toIdentityProviders({ realm })} />
                )}
              >
                {t("cancel")}
              </Button>
            </ActionGroup>
          </FormAccess>
        </FormProvider>
      </PageSection>
    </>
  );
}
