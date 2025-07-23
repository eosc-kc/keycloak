import { ClipboardCopy, FormGroup } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";
import { addTrailingSlash } from "../../util";
import { useFormContext, useWatch } from "react-hook-form";
import IdentityProviderRepresentation from "libs/keycloak-admin-client/lib/defs/identityProviderRepresentation";

export const RedirectUrl = ({ id }: { id: string }) => {
  const { t } = useTranslation("identity-providers");
  const { control } = useFormContext<IdentityProviderRepresentation>();

  const providerId = useWatch({
    control,
    name: "providerId",
  });
  const { realm } = useRealm();
  const callbackUrl = `${addTrailingSlash(
    adminClient.baseUrl,
  )}realms/${realm}/broker`;

  return (
    <FormGroup
      label={t("redirectURI")}
      labelIcon={
        <HelpItem
          helpText={t("identity-providers-help:redirectURI")}
          fieldLabelId="identity-providers:redirectURI"
        />
      }
      fieldId="kc-redirect-uri"
    >
      <ClipboardCopy isReadOnly>
        {`${callbackUrl}/` +
          (providerId === "openid-federation"
            ? "federation-endpoint"
            : `${id}/endpoint`)}
      </ClipboardCopy>
    </FormGroup>
  );
};
