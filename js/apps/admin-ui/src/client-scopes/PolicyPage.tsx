import ClientScopePolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopePolicyRepresentation";
import {
  ActionGroup,
  ActionList,
  ActionListItem,
  AlertVariant,
  Button,
  ButtonVariant,
  Checkbox,
  DropdownItem,
  FormGroup,
  InputGroup,
  PageSection,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { Controller, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  FormErrorText,
  HelpItem,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { FormAccess } from "../components/form/FormAccess";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useAdminClient } from "../admin-client";
import { useNavigate, useParams } from "react-router-dom";
import { toClientScope } from "./routes/ClientScope";
import { useRealm } from "../context/realm-context/RealmContext";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { Link } from "react-router-dom";
import { toEditPolicy } from "./routes/EditPolicy";

const PolicyPage = () => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation("");
  const { addAlert, addError } = useAlerts();
  const { id, policyId } = useParams<any>();
  const navigate = useNavigate();
  const { realm } = useRealm();

  const form = useForm<ClientScopePolicyRepresentation>();

  const {
    handleSubmit,
    control,
    register,
    formState: { errors },
    setValue,
    reset,
  } = form;

  const clientScopePolicyValues = useWatch({
    control,
    name: "clientScopePolicyValues",
    defaultValue: [
      {
        value: "",
        regex: false,
        negateOutput: false,
      },
    ],
  }) as Record<string, any>;

  useFetch(
    async () => {
      if (policyId) {
        const policy = await adminClient.clientScopes.findPolicy({
          id: id!,
          policyId: policyId,
        });
        return { policy: policy };
      } else {
        return { policy: null };
      }
    },
    ({ policy }) => {
      if (policy) {
        reset(policy);
      }
    },
    [],
  );
  const [toggleDeletePolicyDialog, DeletePolicyConfirm] = useConfirmDialog({
    titleKey: "deleteScopePolicy",
    messageKey: "deleteScopePolicyConfirm",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.clientScopes.delPolicy({
          policyId: policyId!,
          id: id!,
        });
        addAlert(t("deleteScopePolicySuccess"), AlertVariant.success);
        navigate(
          toClientScope({
            realm,
            id: id!,
            tab: "policies",
          }),
        );
      } catch (error) {
        addError("deleteScopePolicyError", error);
      }
    },
  });
  function isFieldErrorWithPropertyValue(error: any) {
    return (
      typeof error === "object" &&
      error !== null &&
      "value" in error &&
      error.value !== undefined
    );
  }

  const save = async (clientScopePolicy: ClientScopePolicyRepresentation) => {
    if (policyId) {
      try {
        await adminClient.clientScopes.updatePolicy(
          { id: id!, policyId: policyId! },
          { ...clientScopePolicy, id: policyId! },
        );
        addAlert(t("scopePolicySaveSuccess"), AlertVariant.success);
      } catch (error) {
        addError(t("scopePolicySaveError"), error);
      }
    } else {
      try {
        const createdPolicy = await adminClient.clientScopes.createPolicy(
          { id: id! },
          clientScopePolicy,
        );
        addAlert(t("scopePolicyCreateSuccess"), AlertVariant.success);
        navigate(
          toEditPolicy({
            realm,
            id: id!,
            policyId: createdPolicy.id!,
          }),
        );
      } catch (error) {
        addError(t("scopePolicyCreateError"), error);
      }
    }
  };

  return (
    <>
      <DeletePolicyConfirm />
      <ViewHeader
        titleKey={
          policyId ? t("editScopePolicy") : t("createClientScopePolicy")
        }
        divider={true}
        dropdownItems={
          policyId
            ? [
                <DropdownItem key="delete" onClick={toggleDeletePolicyDialog}>
                  {t("delete")}
                </DropdownItem>,
              ]
            : undefined
        }
      />
      <PageSection variant="light">
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(save)}
          className="pf-u-mt-lg"
        >
          <FormGroup
            label={t("userAttribute")}
            labelIcon={
              <HelpItem
                helpText={t("userAttributeHelp")}
                fieldLabelId="userAttribute"
              />
            }
            fieldId="kc-user-attribute"
            isRequired
          >
            <TextInput
              isRequired
              id="kc-user-attribute"
              data-testid="userAttribute"
              validated={
                errors.userAttribute
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              {...register("userAttribute", { required: true })}
            />
            {errors.userAttribute && <FormErrorText message={t("required")} />}
          </FormGroup>
          <FormGroup
            label={t("clientScopePoliciesValues")}
            isRequired
            labelIcon={
              <HelpItem
                helpText={t("clientScopePoliciesValuesHelp")}
                fieldLabelId={"clientScopePoliciesValues"}
              />
            }
          >
            <Table aria-label="Simple table">
              <Thead>
                <Tr>
                  <Th>{t("policyValue")}</Th>
                  <Th>{t("regex")}</Th>
                  <Th>{t("negative")}</Th>
                  <Th></Th>
                </Tr>
              </Thead>
              <Tbody>
                {clientScopePolicyValues.map((policy: any, index: number) => (
                  <Tr key={index}>
                    <Td>
                      <Controller
                        name={`clientScopePolicyValues.${index}.value`}
                        defaultValue={clientScopePolicyValues[index].value}
                        control={control}
                        rules={{
                          required: {
                            value: true,
                            message: t("required"),
                          },
                        }}
                        render={({ field }) => (
                          <TextInput
                            name={`clientScopePolicyValues.${index}.value`}
                            placeholder={t("policyValuePlaceholder")}
                            onChange={field.onChange}
                            value={clientScopePolicyValues[index].value}
                            validated={
                              errors.clientScopePolicyValues &&
                              (isFieldErrorWithPropertyValue(
                                errors.clientScopePolicyValues[index],
                              )
                                ? ValidatedOptions.error
                                : ValidatedOptions.default)
                            }
                          />
                        )}
                      />
                    </Td>
                    <Td>
                      <Controller
                        name={`clientScopePolicyValues.${index}.regex`}
                        defaultValue={clientScopePolicyValues[index].regex}
                        control={control}
                        render={({ field }) => (
                          <InputGroup>
                            <Checkbox
                              name={`clientScopePolicyValues.${index}.regex`}
                              value={clientScopePolicyValues[index].regex}
                              isChecked={field.value.toString() === "true"}
                              onChange={field.onChange}
                              id={`regex-checkbox${index}`}
                            />
                          </InputGroup>
                        )}
                      />
                    </Td>
                    <Td>
                      <Controller
                        name={`clientScopePolicyValues.${index}.negateOutput`}
                        defaultValue={
                          clientScopePolicyValues[index].negateOutput
                        }
                        control={control}
                        render={({ field }) => (
                          <InputGroup>
                            <Checkbox
                              name={`clientScopePolicyValues.${index}.negateOutput`}
                              isChecked={field.value.toString() === "true"}
                              value={
                                clientScopePolicyValues[index].negateOutput
                              }
                              onChange={field.onChange}
                              id={`negateOutput-checkbox${index}`}
                            />
                          </InputGroup>
                        )}
                      />
                    </Td>
                    <Td>
                      <Button
                        variant="link"
                        title={t("removeAttribute")}
                        isDisabled={clientScopePolicyValues.length === 1}
                        onClick={() => {
                          clientScopePolicyValues.splice(index, 1);
                          setValue(
                            "clientScopePolicyValues",
                            clientScopePolicyValues,
                          );
                        }}
                      >
                        <MinusCircleIcon />
                      </Button>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
            <ActionList>
              <ActionListItem>
                <Button
                  className="pf-u-px-0 pf-u-mt-sm"
                  variant="link"
                  icon={<PlusCircleIcon />}
                  onClick={() => {
                    clientScopePolicyValues.push({
                      value: "",
                      regex: false,
                      negateOutput: false,
                    });
                    setValue(
                      "clientScopePolicyValues",
                      clientScopePolicyValues,
                    );
                  }}
                >
                  {t("addPolicyValue")}
                </Button>
              </ActionListItem>
            </ActionList>
          </FormGroup>
          <ActionGroup>
            <Button
              data-testid="new-mapper-save-button"
              variant="primary"
              type="submit"
            >
              {policyId ? t("save") : t("create")}
            </Button>
            <Button
              data-testid="new-mapper-cancel-button"
              variant="link"
              component={(props) => (
                <Link
                  {...props}
                  to={toClientScope({
                    realm,
                    id: id!,
                    tab: "policies",
                  })}
                />
              )}
            >
              {t("cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
};

export default PolicyPage;
