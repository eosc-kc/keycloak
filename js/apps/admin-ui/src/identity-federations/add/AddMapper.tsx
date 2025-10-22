import type IdentityProviderMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperRepresentation";
import type { IdentityProviderMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/identityProviderMapperTypeRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  DropdownItem,
  PageSection,
} from "@patternfly/react-core";
import { TextControl, useFetch } from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useAlerts, KeycloakSpinner } from "@keycloak/keycloak-ui-shared";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { FormAccess } from "../../components/form/FormAccess";
import type { AttributeForm } from "../../components/key-value-form/AttributeForm";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import useLocaleSort, { mapByKey } from "../../utils/useLocaleSort";
import { useParams } from "../../utils/useParams";
import {
  IdentityFederationEditMapperParams,
  toIdentityFederationEditMapper,
} from "../routes/EditMapper";
import { toIdentityFederation } from "../routes/IdentityFederation";
import { AddMapperForm } from "./AddMapperForm";

export type IdPMapperRepresentationWithAttributes =
  IdentityProviderMapperRepresentation & AttributeForm;

export type Role = RoleRepresentation & {
  clientId?: string;
};

export default function AddMapper() {
  const { t } = useTranslation();
  const { adminClient } = useAdminClient();

  const form = useForm<IdPMapperRepresentationWithAttributes>({
    shouldUnregister: true,
  });
  const {
    handleSubmit,
    register,
    formState: { errors },
  } = form;
  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const localeSort = useLocaleSort();

  const { realm } = useRealm();

  useFetch(
    () =>
      Promise.all([
        id
          ? adminClient.identityFederations.findOneMapper({ internalId, id })
          : null,
        adminClient.identityFederations.findMapperTypes(),
      ]),
    ([mapper, mapperTypes]) => {
      const mappers = localeSort(
        Object.values(
          mapperTypes,
        ) as IdentityProviderMapperTypeRepresentation[],
        mapByKey("name"),
      );
      if (mapper) {
        setCurrentMapper(
          mappers.find(({ id }) => id === mapper.identityProviderMapper),
        );
        setupForm(mapper);
      } else {
        setCurrentMapper(mappers[0]);
      }
      setMapperTypes(mappers);
    },
    [],
  );

  const { id, providerId, internalId } =
    useParams<IdentityFederationEditMapperParams>();

  const [mapperTypes, setMapperTypes] =
    useState<IdentityProviderMapperTypeRepresentation[]>();

  const [currentMapper, setCurrentMapper] =
    useState<IdentityProviderMapperTypeRepresentation>();

  const save = async (idpMapper: IdentityProviderMapperRepresentation) => {
    const mapper = convertFormValuesToObject(idpMapper);

    const identityProviderMapper = {
      ...mapper,
      config: {
        ...mapper.config,
      },
      federationId: internalId!,
    };

    if (id) {
      try {
        await adminClient.identityFederations.updateMapper(
          {
            id: id!,
            internalId: internalId!,
          },
          { ...identityProviderMapper },
        );
        addAlert(t("mapperSaveSuccess"), AlertVariant.success);
      } catch (error) {
        addError(t("mapperSaveError"), error);
      }
    } else {
      try {
        const createdMapper =
          await adminClient.identityFederations.createMapper({
            identityProviderMapper,
            federationId: internalId!,
          });

        addAlert(t("mapperCreateSuccess"), AlertVariant.success);
        navigate(
          toIdentityFederationEditMapper({
            realm,
            internalId,
            providerId: providerId,
            id: createdMapper.id,
          }),
        );
      } catch (error) {
        addError(t("mapperCreateErrorFederation"), error);
      }
    }
  };

  const [toggleDeleteMapperDialog, DeleteMapperConfirm] = useConfirmDialog({
    titleKey: "deleteProviderMapper",
    messageKey: t("deleteMapperConfirm", {
      mapper: currentMapper?.name,
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.identityFederations.delMapper({
          internalId: internalId,
          id: id!,
        });
        addAlert(t("deleteMapperSuccess"), AlertVariant.success);
        navigate(
          toIdentityFederation({
            providerId,
            internalId,
            tab: "mappers",
            realm,
          }),
        );
      } catch (error) {
        addError("deleteMapperError", error);
      }
    },
  });

  const setupForm = (mapper: IdentityProviderMapperRepresentation) => {
    convertToFormValues(mapper, form.setValue);
  };

  if (!mapperTypes || !currentMapper) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant="light">
      <DeleteMapperConfirm />
      <ViewHeader
        className="kc-add-mapper-title"
        titleKey={
          id
            ? t("editIdPMapper", {
                providerId:
                  providerId[0].toUpperCase() + providerId.substring(1),
              })
            : t("addIdPMapper", {
                providerId:
                  providerId[0].toUpperCase() + providerId.substring(1),
              })
        }
        dropdownItems={
          id
            ? [
                <DropdownItem key="delete" onClick={toggleDeleteMapperDialog}>
                  {t("delete")}
                </DropdownItem>,
              ]
            : undefined
        }
        divider
      />
      <FormAccess
        role="manage-identity-providers"
        isHorizontal
        onSubmit={handleSubmit(save)}
        className="pf-v5-u-mt-lg"
      >
        <FormProvider {...form}>
          {id && (
            <TextControl
              name="name"
              isDisabled={!!id}
              label={t("id")}
              value={currentMapper.id}
              rules={{ required: t("required") }}
            />
          )}
          {currentMapper.properties && (
            <>
              <AddMapperForm
                form={form}
                id={id}
                mapperTypes={mapperTypes}
                updateMapperType={setCurrentMapper}
                mapperType={currentMapper}
              />
              <DynamicComponents properties={currentMapper.properties!} />
            </>
          )}
        </FormProvider>

        <ActionGroup>
          <Button
            data-testid="new-mapper-save-button"
            variant="primary"
            type="submit"
          >
            {t("save")}
          </Button>
          <Button
            data-testid="new-mapper-cancel-button"
            variant="link"
            component={(props) => (
              <Link
                {...props}
                to={toIdentityFederation({
                  realm,
                  providerId,
                  internalId: internalId!,
                  tab: "mappers",
                })}
              />
            )}
          >
            {t("cancel")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
}
