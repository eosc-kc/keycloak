import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { FormGroup } from "@patternfly/react-core";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { convertAttributeNameToForm } from "../../util";
import useFormatDate from "../../utils/useFormatDate";

type OpenIdFederationSettingsProps = {
  readOnly: boolean;
};
export const OpenIdFederationSettings = ({
  readOnly,
}: OpenIdFederationSettingsProps) => {
  const { t } = useTranslation("identity-providers");
  const formatDate = useFormatDate();
  const { register, control } =
    useFormContext<IdentityProviderRepresentation>();

  const config = useWatch({
    control,
    name: convertAttributeNameToForm("config"),
  });

  return (
    <div className="pf-c-form pf-m-horizontal">
      <FormGroup label={t("authorityHints")} fieldId="kc-authority-hints">
        <KeycloakTextInput
          type="url"
          data-testid="authorityHints"
          id="kc-authority-hints"
          isReadOnly={readOnly}
          {...register("config.authorityHints")}
        />
      </FormGroup>
      <FormGroup label={t("trustAnchorId")} fieldId="kc-trust-anchor-id">
        <KeycloakTextInput
          type="url"
          data-testid="trustAnchorId"
          id="kc-trust-anchor-id"
          isReadOnly={readOnly}
          {...register("config.trustAnchorId")}
        />
      </FormGroup>
      <FormGroup label={t("expirationTime")} fieldId="kc-expiration-time">
        <KeycloakTextInput
          type="url"
          data-testid="expirationTime"
          id="kc-expiration-time"
          isReadOnly={readOnly}
          // Parse the expiration time from seconds to milliseconds
          value={formatDate(
            new Date(parseInt(config["expiration.time"]) * 1000),
          )}
        />
      </FormGroup>
    </div>
  );
};
