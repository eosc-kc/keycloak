import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  Button,
  FormGroup,
  HelperText,
  HelperTextItem,
  MenuToggle,
  Select,
  SelectOption,
  TextInput,
} from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useRef, useState, useEffect } from "react";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import "../add/discovery-settings.css";
import {
  Table,
  Tbody,
  TbodyProps,
  Td,
  Th,
  Thead,
  Tr,
  TrProps,
} from "@patternfly/react-table";
import styles from "@patternfly/react-styles/css/components/Table/table";

type PrincipalTableProps = {
  readOnly: boolean;
  required?: boolean;
};

const nameIdOptions = {
  "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent": "persistent",
  "urn:oasis:names:tc:SAML:2.0:nameid-format:transient": "transient",
  "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress": "email",
  "urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos": "kerberos",
  "urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName": "x509",
  "urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName":
    "windowsDomainQN",
  "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified": "unspecified",
};
type NameIdValue = keyof typeof nameIdOptions;
const defaultPrincipal = {
  principalType: "SUBJECT",
  nameIDPolicyFormat: "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent",
};

export const PrincipalTable = ({ readOnly, required }: PrincipalTableProps) => {
  const { t } = useTranslation();

  const bodyRef = useRef<HTMLTableSectionElement | null>(null);

  const {
    watch,
    setValue,
    formState: { errors },
    setError,
    clearErrors,
  } = useFormContext<IdentityProviderRepresentation>();

  const multiplePrincipals = watch("config.multiplePrincipals") || "[]";
  const [principal, setPrincipal] = useState<any>(defaultPrincipal);

  const [draggedItemId, setDraggedItemId] = useState<string | null>(null);
  const [draggingToItemIndex, setDraggingToItemIndex] = useState<number | null>(
    null,
  );
  const [isDragging, setIsDragging] = useState(false);

  useEffect(() => {
    if (required && JSON.parse(multiplePrincipals).length < 1) {
      setError("config.multiplePrincipals", { message: t("required") });
    } else {
      clearErrors("config.multiplePrincipals");
    }
  }, [multiplePrincipals]);

  const onDragStart: TrProps["onDragStart"] = (evt) => {
    evt.dataTransfer.effectAllowed = "move";
    evt.dataTransfer.setData("text/plain", evt.currentTarget.id);
    const draggedItemId = evt.currentTarget.id;
    evt.currentTarget.classList.add(styles.modifiers.ghostRow);
    evt.currentTarget.setAttribute("aria-pressed", "true");
    setDraggedItemId(draggedItemId);
    setIsDragging(true);
  };

  function moveAndShift(array: any, indexX: any, indexY: any) {
    const newArray = [...array]; // Create a shallow copy to avoid modifying the original array
    const temp = newArray.splice(indexX, 1);
    newArray.splice(indexY, 0, ...temp);
    // Swap the elements at indexX and indexY
    return newArray;
  }

  const move = (draggedId: number, targetId: number) => {
    Array.from(bodyRef.current?.children ?? []).forEach((el, index) => {
      if (index === draggedId) {
        el.classList.remove(styles.modifiers.ghostRow);
        el.setAttribute("aria-pressed", "false");
      } else if (index === targetId) {
        el.classList.add(styles.modifiers.ghostRow);
        el.setAttribute("aria-pressed", "true");
      }
    });
    const updatePrincipals = JSON.parse(multiplePrincipals);
    setValue(
      "config.multiplePrincipals",
      JSON.stringify(moveAndShift(updatePrincipals, draggedId, targetId)),
    );
    setDraggedItemId(targetId.toString());
  };

  const onDragCancel = () => {
    Array.from(bodyRef.current?.children ?? []).forEach((el) => {
      el.classList.remove(styles.modifiers.ghostRow);
      el.setAttribute("aria-pressed", "false");
    });
    setDraggedItemId(null);
    setDraggingToItemIndex(null);
    setIsDragging(false);
  };

  const isValidDrop = (
    evt: React.DragEvent<HTMLTableSectionElement | HTMLTableRowElement>,
  ) => {
    const ulRect = bodyRef.current?.getBoundingClientRect();
    if (ulRect)
      return (
        evt.clientX > ulRect.x &&
        evt.clientX < ulRect.x + ulRect.width &&
        evt.clientY > ulRect.y &&
        evt.clientY < ulRect.y + ulRect.height
      );
  };

  const onDrop: TrProps["onDrop"] = (evt) => {
    if (!isValidDrop(evt)) {
      onDragCancel();
    }
  };

  const onDragOver: TbodyProps["onDragOver"] = (evt) => {
    evt.preventDefault();
    const curListItem = (evt.target as HTMLTableSectionElement).closest("tr");
    if (
      !curListItem ||
      !bodyRef.current?.contains(curListItem) ||
      curListItem.id === draggedItemId
    ) {
      return null;
    } else {
      const dragId = curListItem.id;
      const newDraggingToItemIndex = Array.from(
        bodyRef.current.children,
      ).findIndex((item) => item.id === dragId);
      if (
        newDraggingToItemIndex !== draggingToItemIndex &&
        newDraggingToItemIndex < JSON.parse(multiplePrincipals).length
      ) {
        move(parseInt(draggedItemId || ""), newDraggingToItemIndex);
        setDraggingToItemIndex(newDraggingToItemIndex);
      }
    }
  };

  const onDragEnd: TrProps["onDragEnd"] = () => {
    Array.from(bodyRef.current?.children ?? []).forEach((el) => {
      el.classList.remove(styles.modifiers.ghostRow);
      el.setAttribute("aria-pressed", "false");
    });
    setDraggedItemId(null);
    setDraggingToItemIndex(null);
    setIsDragging(false);
  };

  return (
    <FormGroup
      label={t("principals")}
      labelIcon={
        <HelpItem helpText={t("principalsHelp")} fieldLabelId="principals" />
      }
      isRequired={required}
      fieldId="kc-principals"
    >
      <Table
        aria-label="Draggable table"
        className={isDragging ? styles.modifiers.dragOver : ""}
      >
        <Thead>
          <Tr>
            <Th>{t("order")}</Th>
            <Th>{t("principalType")}</Th>
            <Th>{t("principalAttributeIdentityFederation")}</Th>
            <Th>{t("actions")}</Th>
          </Tr>
        </Thead>
        <Tbody ref={bodyRef} onDragOver={onDragOver} onDrop={onDragOver}>
          {JSON.parse(multiplePrincipals).map((value: any, key: number) => {
            return (
              <Tr
                key={key}
                id={"" + key}
                draggable
                onDrop={onDrop}
                onDragEnd={onDragEnd}
                onDragStart={onDragStart}
              >
                <Td
                  modifier="fitContent"
                  draggableRow={{
                    id: `draggable-row-${key}`,
                  }}
                />
                <Td>
                  <PrincipalTypeSelector
                    value={value.principalType}
                    onChange={(value: any) => {
                      const updatePrincipals = JSON.parse(multiplePrincipals);
                      updatePrincipals[key].principalType = value;
                      delete updatePrincipals[key].nameIDPolicyFormat;
                      delete updatePrincipals[key].principalAttribute;
                      setValue(
                        "config.multiplePrincipals",
                        JSON.stringify(updatePrincipals),
                      );
                    }}
                  />
                </Td>
                <Td>
                  {value.principalType === "SUBJECT" ? (
                    <NameIdPrincipalSelector
                      value={value.nameIDPolicyFormat}
                      onChange={(value: any) => {
                        const updatePrincipals = JSON.parse(multiplePrincipals);
                        updatePrincipals[key].nameIDPolicyFormat = value;
                        setValue(
                          "config.multiplePrincipals",
                          JSON.stringify(updatePrincipals),
                        );
                      }}
                    />
                  ) : (
                    <TextInput
                      aria-label={"text-input" + key}
                      value={value.principalAttribute}
                      onChange={(value) => {
                        const updatePrincipals = JSON.parse(multiplePrincipals);
                        updatePrincipals[key].principalAttribute = value;
                        setValue(
                          "config.multiplePrincipals",
                          JSON.stringify(updatePrincipals),
                        );
                      }}
                      // isReadOnly={readOnly}
                    />
                  )}
                </Td>
                <Td>
                  <Button
                    size="sm"
                    onClick={() => {
                      const updatePrincipals = JSON.parse(multiplePrincipals);
                      updatePrincipals.splice(key, 1);
                      setValue(
                        "config.multiplePrincipals",
                        JSON.stringify(updatePrincipals),
                      );
                    }}
                  >
                    Delete
                  </Button>
                </Td>
              </Tr>
            );
          })}
          {!readOnly && (
            <Tr>
              <Td />
              <Td>
                <PrincipalTypeSelector
                  value={principal?.principalType}
                  onChange={(value: any) => {
                    setPrincipal(
                      value === "SUBJECT"
                        ? defaultPrincipal
                        : { principalType: value, principalAttribute: "" },
                    );
                  }}
                />
              </Td>
              <Td>
                {principal.principalType === "SUBJECT" ? (
                  <NameIdPrincipalSelector
                    value={principal.nameIDPolicyFormat}
                    onChange={(value: any) => {
                      setPrincipal({ ...principal, nameIDPolicyFormat: value });
                    }}
                  />
                ) : (
                  <TextInput
                    aria-label={"new-principal-attribute"}
                    value={principal.principalAttribute}
                    onChange={(_event, value) => {
                      setPrincipal({ ...principal, principalAttribute: value });
                    }}
                  />
                )}
              </Td>
              <Td modifier="fitContent">
                <Button
                  onClick={() => {
                    const updatePrincipals = JSON.parse(multiplePrincipals);
                    updatePrincipals.push({ ...principal });
                    setValue(
                      "config.multiplePrincipals",
                      JSON.stringify(updatePrincipals),
                    );
                    setPrincipal(defaultPrincipal);
                  }}
                >
                  Add
                </Button>
              </Td>
            </Tr>
          )}
          {readOnly && (
            <Tr>
              <Td colSpan={4}>No active principals</Td>
            </Tr>
          )}
        </Tbody>
      </Table>
      {errors.config?.multiplePrincipals && (
        <HelperText>
          <HelperTextItem variant="error">{t("required")}</HelperTextItem>
        </HelperText>
      )}
    </FormGroup>
  );
};

const PrincipalTypeSelector = (props: any) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const { t } = useTranslation();

  return (
    <Select
      toggle={(ref) => (
        <MenuToggle
          ref={ref}
          onClick={() => setIsExpanded(!isExpanded)}
          isExpanded={isExpanded}
          isDisabled={props.readOnly}
          style={
            {
              width: "200px",
            } as React.CSSProperties
          }
        >
          {t(props.value)}
        </MenuToggle>
      )}
      isOpen={isExpanded}
      onOpenChange={(isOpen) => setIsExpanded(isOpen)}
      onSelect={(_, value) => {
        props.onChange(value);
        setIsExpanded(false);
      }}
      // selections={props.value}
      // variant={SelectVariant.single}
      // isDisabled={props.readOnly}
    >
      <SelectOption
        data-testid="subjectNameId-option"
        value="SUBJECT"
        // isPlaceholder
      >
        {t("SUBJECT")}
      </SelectOption>
      <SelectOption data-testid="attributeName-option" value="ATTRIBUTE">
        {t("ATTRIBUTE")}
      </SelectOption>
      <SelectOption
        data-testid="attributeFriendlyName-option"
        value="FRIENDLY_ATTRIBUTE"
      >
        {t("FRIENDLY_ATTRIBUTE")}
      </SelectOption>
    </Select>
  );
};
type Props = {
  value: NameIdValue;
  onChange: (value: NameIdValue) => void;
  readOnly?: boolean;
};

const NameIdPrincipalSelector: React.FC<Props> = (props) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const { t } = useTranslation();

  // 4) Typed keys (avoids string widening from Object.entries)
  const keys = Object.keys(nameIdOptions) as NameIdValue[];

  return (
    <Select
      toggle={(ref) => (
        <MenuToggle
          ref={ref as React.Ref<HTMLButtonElement>}
          onClick={() => setIsExpanded((v) => !v)}
          isExpanded={isExpanded}
          isDisabled={props.readOnly}
          style={{ width: "200px" } as React.CSSProperties}
        >
          {t(nameIdOptions[props.value])}
        </MenuToggle>
      )}
      isOpen={isExpanded}
      onOpenChange={(isOpen) => setIsExpanded(isOpen)}
      shouldFocusToggleOnSelect
      onSelect={(_, value) => {
        if (typeof value === "string" && value in nameIdOptions) {
          props.onChange(value as NameIdValue);
        }
        setIsExpanded(false);
      }}
    >
      {keys.map((key) => {
        const label = nameIdOptions[key];
        return (
          <SelectOption key={key} data-testid={`${label}-option`} value={key}>
            {t(label)}
          </SelectOption>
        );
      })}
    </Select>
  );
};
// const NameIdPrincipalSelector = (props:
//     value: NameIdValue;
//   onChange: (value: NameIdValue) => void;
//   readOnly?: boolean;
// ) => {
//   const [isExpanded, setIsExpanded] = useState(false);
//   const { t } = useTranslation("identity-providers");
//   return (
//     <Select
//       toggle={(ref) => (
//         <MenuToggle
//           ref={ref}
//           onClick={() => setIsExpanded(!isExpanded)}
//           isExpanded={isExpanded}
//           isDisabled={props.readOnly}
//           style={
//             {
//               width: "200px",
//             } as React.CSSProperties
//           }
//         >
//           {/* {t(nameIdOptions[props.value])} */}
//           {nameIdOptions[props.value]}
//         </MenuToggle>
//       )}
//       isOpen={isExpanded}
//       onSelect={(_, value) => {
//         props.onChange(value);
//         setIsExpanded(false);
//       }}
//     >
//       {...Object.entries(nameIdOptions).map((entry) => {
//         const [key, value] = entry;
//         return (
//           <SelectOption
//             key={key}
//             data-testid={value + "-option"}
//             value={key}
//             // isPlaceholder={value === "persistent"}
//           >
//             {t(value)}
//           </SelectOption>
//         );
//       })}
//     </Select>
//   );
// };
