import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  PageSection,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form/FormAccess";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toIdentityProvider } from "../routes/IdentityProvider";
import { toIdentityProviders } from "../routes/IdentityProviders";
import { RedirectUrl } from "../component/RedirectUrl";
import { DisplayOrder } from "../component/DisplayOrder";
import { OpenIdFederationSettings } from "./OpenIdFederationSettings";
import { useFetch } from "../../utils/useFetch";
import OpenIdFederationRepresentation from "libs/keycloak-admin-client/lib/defs/OpenIdFederationRepresentation";

export default function AddIdentityProvider() {
  const { t } = useTranslation("identity-providers");
  const providerId = "openid-federation";
  const form = useForm<IdentityProviderRepresentation>();

  const {
    handleSubmit,
    formState: { isDirty },
  } = form;

  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { realm } = useRealm();
  const [trustAnchors, setTrustAnchors] = useState<string[]>([]);

  useFetch(
    async () => {
      try {
        const openIdFederations = await adminClient.openIdFederations.find({
          realm: realm,
        });
        return openIdFederations.map(
          (openIdFederation: OpenIdFederationRepresentation) =>
            openIdFederation.trustAnchor,
        );
      } catch (error) {
        return [];
      }
    },
    setTrustAnchors,
    [],
  );
  const onSubmit = async (provider: IdentityProviderRepresentation) => {
    try {
      await adminClient.identityProviders.create({
        ...provider,
        providerId,
        alias: providerId,
      });
      addAlert(t("createSuccess"), AlertVariant.success);
      navigate(
        toIdentityProvider({
          realm,
          providerId,
          alias: providerId,
          tab: "settings",
        }),
      );
    } catch (error) {
      addError("identity-providers:createError", error);
    }
  };

  return (
    <>
      <ViewHeader titleKey={t("addOpenIdFederationProvider")} />
      <PageSection variant="light">
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(onSubmit)}
        >
          <FormProvider {...form}>
            <RedirectUrl id={providerId} create={true} />
            <DisplayOrder />
            <OpenIdFederationSettings
              readOnly={false}
              create={true}
              trustAnchors={trustAnchors}
            />
          </FormProvider>
          <ActionGroup>
            <Button
              isDisabled={!isDirty}
              variant="primary"
              type="submit"
              data-testid="createProvider"
            >
              {t("common:add")}
            </Button>
            <Button
              variant="link"
              data-testid="cancel"
              component={(props) => (
                <Link {...props} to={toIdentityProviders({ realm })} />
              )}
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
}
