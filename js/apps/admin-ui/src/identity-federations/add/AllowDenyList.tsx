import type IdentityFederationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityFederationRepresentation";
import {
  Button,
  FormGroup,
  InputGroup,
  TextInput,
} from "@patternfly/react-core";
import { useState } from "react";
import { useAlerts } from "../../components/alert/Alerts";
import { PlusIcon, MinusIcon } from "@patternfly/react-icons";
import { HelpItem } from "ui-shared";
import { useTranslation } from "react-i18next";
import { useFormContext, useWatch } from "react-hook-form";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import {
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";

type AllowDenyListProps = {
  type: "Allow" | "Deny";
};
type CategoryListType = {
  [key: string]: string[];
};

const AllowDenyList = ({ type }: AllowDenyListProps) => {
  const {
    control,
    setValue,
    formState: { errors },
  } = useFormContext<IdentityFederationRepresentation>();
  const { t } = useTranslation("identity-federations");
  const { t: th } = useTranslation("identity-federations-help");
  const [attributeName, setAttributeName] = useState("");
  const [attributeValue, setAttributeValue] = useState("");
  const [attributeValueArray, setAttributeValueArray] = useState<string[]>([]);
  const { addError } = useAlerts();
  const categoryList = useWatch({
    control,
    name: `category${type}List`,
    defaultValue: {},
  }) as CategoryListType;

  return (
    <>
      <FormGroup
        label={t("perEntityIds")}
        helperTextInvalid={t("common:required")}
        validated={errors.url ? "error" : "default"}
        labelIcon={
          <HelpItem
            helpText={th("perEntityIds")}
            fieldLabelId="identity-federations-help:perEntityIds"
          />
        }
      >
        <MultiLineInput
          name={`entityId${type}List`}
          addButtonLabel={"Add Entity Id"}
          data-testid="classref-field"
        />
      </FormGroup>
      <FormGroup
        label={t("perRegistrationAuthority")}
        helperTextInvalid={t("common:required")}
        validated={errors.url ? "error" : "default"}
        labelIcon={
          <HelpItem
            helpText={th("perRegistrationAuthority")}
            fieldLabelId="identity-federations-help:perRegistrationAuthority"
          />
        }
      >
        <MultiLineInput
          name={`registrationAuthority${type}List`}
          addButtonLabel={"Add Registration Authority"}
          data-testid="classref-field"
        />
      </FormGroup>
      <FormGroup
        label={t("perEntityCategory")}
        labelIcon={
          <HelpItem
            helpText={th("perEntityCategory")}
            fieldLabelId="identity-providers:perEntityCategory"
          />
        }
        fieldId="kc-principals"
        helperTextInvalid={t("common:required")}
      >
        <TableComposable aria-label="Draggable table">
          <Thead>
            <Tr>
              <Th>{t("attributeName")}</Th>
              <Th>{t("attributeValue")}</Th>
              <Th>{t("actions")}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {Object.keys(categoryList).map((attributeName: string) => {
              return (
                <Tr key={attributeName}>
                  <Td>{attributeName}</Td>
                  <Td>
                    {categoryList[attributeName].map(
                      (value: string | number | undefined, index: number) => {
                        return (
                          <InputGroup key={index}>
                            <TextInput
                              value={value}
                              onChange={(value) => {
                                categoryList[attributeName][index] = value;
                                setValue(`category${type}List`, categoryList);
                              }}
                            />
                            <Button
                              id="textAreaButton2"
                              variant="control"
                              onClick={() => {
                                try {
                                  if (categoryList[attributeName].length > 1) {
                                    categoryList[attributeName].splice(
                                      index,
                                      1,
                                    );
                                    setValue(
                                      `category${type}List`,
                                      categoryList,
                                    );
                                  } else {
                                    throw new Error(
                                      "This is a custom error message",
                                    );
                                  }
                                } catch (err) {
                                  addError(
                                    t(
                                      "identity-federations:removeAttributeError",
                                    ),
                                    err,
                                  );
                                }
                              }}
                            >
                              <MinusIcon />
                            </Button>
                          </InputGroup>
                        );
                      },
                    )}
                  </Td>
                  <Td modifier="fitContent">
                    <Button
                      onClick={() => {
                        delete categoryList[attributeName];
                        setValue(`category${type}List`, categoryList);
                      }}
                    >
                      Remove
                    </Button>
                  </Td>
                </Tr>
              );
            })}

            <Tr>
              <Td>
                <TextInput
                  value={attributeName}
                  onChange={(value) => {
                    setAttributeName(value);
                  }}
                />
              </Td>
              <Td>
                {attributeValueArray.map((value, index) => {
                  return (
                    <InputGroup key={index}>
                      <TextInput
                        value={value}
                        onChange={(value) => {
                          attributeValueArray[index] = value;
                          setAttributeValueArray([...attributeValueArray]);
                        }}
                      />
                      <Button
                        id="textAreaButton2"
                        variant="control"
                        onClick={() => {
                          attributeValueArray.splice(index, 1);
                          setAttributeValueArray([...attributeValueArray]);
                        }}
                      >
                        <MinusIcon />
                      </Button>
                    </InputGroup>
                  );
                })}
                <InputGroup>
                  <TextInput
                    value={attributeValue}
                    onChange={(value) => {
                      setAttributeValue(value);
                    }}
                  />
                  <Button
                    id="textAreaButton2"
                    variant="control"
                    onClick={() => {
                      attributeValueArray.push(attributeValue);
                      setAttributeValue("");
                      setAttributeValueArray([...attributeValueArray]);
                    }}
                  >
                    <PlusIcon />
                  </Button>
                </InputGroup>
              </Td>
              <Td modifier="fitContent">
                <Button
                  onClick={() => {
                    categoryList[attributeName] = attributeValueArray;
                    setAttributeValueArray([]);
                    setAttributeName("");
                    setAttributeValue("");
                    setValue(`category${type}List`, categoryList);
                  }}
                  isDisabled={
                    attributeName.length === 0 ||
                    attributeValueArray.length === 0
                  }
                >
                  Add
                </Button>
              </Td>
            </Tr>
          </Tbody>
        </TableComposable>
      </FormGroup>
    </>
  );
};

export default AllowDenyList;
