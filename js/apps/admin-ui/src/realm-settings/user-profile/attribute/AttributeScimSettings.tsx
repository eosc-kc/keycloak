import { FormGroup, SelectGroup, SelectOption } from "@patternfly/react-core";
import { useFieldArray, useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../../components/form/FormAccess";
import {
  HelpItem,
  KeycloakSelect,
  SelectVariant,
} from "@keycloak/keycloak-ui-shared";
import { useParams } from "../../../utils/useParams";
import type { AttributeParams } from "../../routes/Attribute";
import { useUserProfile } from "../UserProfileContext";

import "../../realm-settings-section.css";
import { useState } from "react";

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
  const { attributeName } = useParams<AttributeParams>();
  const { config } = useUserProfile();
  const [open, setOpen] = useState(false);
  const [filterValue, setFilterValue] = useState("");
  const { append, update } = useFieldArray({
    control,
    name: "annotations",
  });
  const annotationValues: Array<{ key: string; value?: unknown }> =
    useWatch({ name: "annotations", control, defaultValue: [] }) ?? [];
  const scimIndex = annotationValues.findIndex(
    (a) => a.key === SCIM_ANNOTATION_KEY,
  );
  const scimValues = normalizeScimAnnotationValue(
    scimIndex >= 0 ? annotationValues[scimIndex]?.value : undefined,
  );

  const handleScimChange = (values: string[]) => {
    const value = serializeScimAnnotationValue(values);

    if (values.length === 0) {
      if (scimIndex >= 0) {
        update(scimIndex, { key: SCIM_ANNOTATION_KEY, value: undefined });
      }
      return;
    }

    if (scimIndex >= 0) {
      update(scimIndex, { key: SCIM_ANNOTATION_KEY, value });
    } else {
      append({ key: SCIM_ANNOTATION_KEY, value });
    }
  };

  const takenScimAttributes = (config?.attributes ?? [])
    .filter((attr) => attr.name !== attributeName)
    .flatMap((attr) => {
      const annotationValue = attr.annotations?.[SCIM_ANNOTATION_KEY];
      return normalizeScimAnnotationValue(annotationValue);
    })
    .filter(Boolean);

  const availableGroups = SCIM_ATTRIBUTE_GROUPS.map((group) => ({
    ...group,
    attributes: group.attributes.filter(
      (attr) => !takenScimAttributes.includes(attr),
    ),
  }));

  const filteredGroups = availableGroups
    .map((group) => ({
      ...group,
      attributes: filterValue
        ? group.attributes.filter((attr) =>
            attr.toLowerCase().includes(filterValue.toLowerCase()),
          )
        : group.attributes,
    }))
    .filter((group) => group.attributes.length > 0);

  const hasOptions = filteredGroups.some((g) => g.attributes.length > 0);

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
          isOpen={open}
          onToggle={(b) => setOpen(b)}
          onSelect={(value) => {
            const selectedValue = String(value);
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
          onFilter={(value) => {
            setFilterValue(value);
          }}
          onClear={() => {
            handleScimChange([]);
          }}
        >
          {hasOptions ? (
            filteredGroups.map((group) => (
              <SelectGroup key={group.labelKey} label={t(group.labelKey)}>
                {group.attributes.map((attr) => (
                  <SelectOption key={attr} value={attr}>
                    {attr}
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
