import {
  Button,
  FormGroup,
  SelectGroup,
  SelectOption,
} from "@patternfly/react-core";
import {
  HelpItem,
  KeycloakSelect,
  SelectVariant,
} from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { useFieldArray, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../../components/form/FormAccess";

import "../../realm-settings-section.css";

const SCIM_CORE_ATTRIBUTES = [
  "name.middleName",
  "name.honorificPrefix",
  "name.honorificSuffix",
  "name.formatted",
  "nickName",
  "profileUrl",
  "title",
  "externalId",
  "userType",
  "timezone",
  "preferredLanguage",
];

const SCIM_ENTERPRISE_ATTRIBUTES = [
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber",
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter",
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:organization",
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:division",
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:department",
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value",
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.displayName",
];

type ScimAttributeGroup = {
  labelKey: string;
  attributes: string[];
};

const SCIM_ATTRIBUTE_GROUPS: ScimAttributeGroup[] = [
  {
    labelKey: "scimCoreUserSchema",
    attributes: SCIM_CORE_ATTRIBUTES,
  },
  {
    labelKey: "scimEnterpriseUserSchema",
    attributes: SCIM_ENTERPRISE_ATTRIBUTES,
  },
];

const PREDEFINED_SCIM_ATTRIBUTES = SCIM_ATTRIBUTE_GROUPS.flatMap(
  (group) => group.attributes,
);

export const SCIM_ANNOTATION_KEY = "kc.scim.schema.attribute";

export const normalizeScimAnnotationValue = (value: unknown): string[] => {
  if (Array.isArray(value)) {
    return value.filter((item): item is string => typeof item === "string");
  }

  return typeof value === "string" ? [value] : [];
};

export const serializeScimAnnotationValue = (values: string[]) => {
  if (values.length === 0) {
    return undefined;
  }

  return values.length === 1 ? values[0] : values;
};

export const AttributeScimSettings = () => {
  const { t } = useTranslation();
  const { control } = useFormContext();
  const [selectKey, setSelectKey] = useState(0);

  const [open, setOpen] = useState(false);
  const [filterValue, setFilterValue] = useState("");

  const { append, update } = useFieldArray({
    control,
    name: "annotations",
  });

  const annotationValues: Array<{ key: string; value?: unknown }> =
    useWatch({
      name: "annotations",
      control,
      defaultValue: [],
    }) ?? [];

  const scimIndex = annotationValues.findIndex(
    (annotation) => annotation.key === SCIM_ANNOTATION_KEY,
  );

  const scimValues = normalizeScimAnnotationValue(
    scimIndex >= 0 ? annotationValues[scimIndex]?.value : undefined,
  );

  const handleScimChange = (values: string[]) => {
    const value = serializeScimAnnotationValue(values);

    if (values.length === 0) {
      if (scimIndex >= 0) {
        update(scimIndex, {
          key: SCIM_ANNOTATION_KEY,
          value: undefined,
        });
      }

      return;
    }

    if (scimIndex >= 0) {
      update(scimIndex, {
        key: SCIM_ANNOTATION_KEY,
        value,
      });
    } else {
      append({
        key: SCIM_ANNOTATION_KEY,
        value,
      });
    }
  };

  const filteredGroups = SCIM_ATTRIBUTE_GROUPS.map((group) => ({
    ...group,
    attributes: filterValue
      ? group.attributes.filter((attribute) =>
          attribute.toLowerCase().includes(filterValue.toLowerCase()),
        )
      : group.attributes,
  }))
    .map((group) => ({
      ...group,
      attributes: group.attributes.filter(
        (attribute) => !scimValues.includes(attribute),
      ),
    }))
    .filter((group) => group.attributes.length > 0);

  const customValue = filterValue.trim();

  const canAddCustomValue =
    customValue.length > 0 &&
    !scimValues.includes(customValue) &&
    !PREDEFINED_SCIM_ATTRIBUTES.includes(customValue);

  const handleAddCustomValue = () => {
    if (!canAddCustomValue) {
      return;
    }

    handleScimChange([...scimValues, customValue]);
    setFilterValue("");
    setSelectKey((currentKey) => currentKey + 1);
    setOpen(false);
  };

  return (
    <FormAccess role="manage-realm" isHorizontal>
      <FormGroup
        label={t("scimAttributeMapping")}
        labelIcon={
          <HelpItem
            helpText={t("scimAttributeMappingHelp")}
            fieldLabelId="scimAttributeMapping"
          />
        }
        fieldId="kc-scim-attribute"
      >
        <KeycloakSelect
          key={selectKey}
          isOpen={open}
          onToggle={setOpen}
          onSelect={(value) => {
            // TypeaheadSelect passes undefined when Enter is pressed because
            // the top-level options are SelectGroup components.
            if (value === undefined || value === null) {
              const firstAvailableValue = filteredGroups[0]?.attributes[0];

              if (firstAvailableValue) {
                handleScimChange([...scimValues, firstAvailableValue]);
                setFilterValue("");
                setOpen(false);
                return;
              }

              handleAddCustomValue();
              return;
            }

            const selectedValue = String(value);

            // TypeaheadSelect uses an empty selection to clear its filter.
            if (!selectedValue) {
              setFilterValue("");
              return;
            }

            const nextValues = scimValues.includes(selectedValue)
              ? scimValues.filter((item) => item !== selectedValue)
              : [...scimValues, selectedValue];

            handleScimChange(nextValues);
            setFilterValue("");
            setOpen(false);
          }}
          selections={scimValues}
          variant={SelectVariant.typeaheadMulti}
          chipGroupProps={{
            numChips: 3,
            expandedText: t("hide"),
            collapsedText: t("showRemaining"),
          }}
          onFilter={setFilterValue}
          footer={
            canAddCustomValue ? (
              <Button
                variant="link"
                isInline
                onClick={(event) => {
                  event.stopPropagation();
                  handleAddCustomValue();
                }}
              >
                {t("add")} &quot;{customValue}&quot;
              </Button>
            ) : undefined
          }
        >
          {filteredGroups.length > 0 ? (
            filteredGroups.map((group) => (
              <SelectGroup key={group.labelKey} label={t(group.labelKey)}>
                {group.attributes.map((attribute) => (
                  <SelectOption key={attribute} value={attribute}>
                    {attribute}
                  </SelectOption>
                ))}
              </SelectGroup>
            ))
          ) : (
            <SelectOption isDisabled>
              {t("noMatchingScimAttributes")}
            </SelectOption>
          )}
        </KeycloakSelect>
      </FormGroup>
    </FormAccess>
  );
};
