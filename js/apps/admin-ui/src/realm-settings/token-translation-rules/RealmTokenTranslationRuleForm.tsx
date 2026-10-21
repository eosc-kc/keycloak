import { TokenTranslationRuleForm } from "../../identity-providers/token-translation-rules/TokenTranslationRuleForm";
import { useParams } from "../../utils/useParams";
import {
  type RealmTokenTranslationRuleParams,
  toEditRealmTokenTranslationRule,
  toRealmTokenTranslationRules,
} from "../routes/TokenTranslationRules";

export default function RealmTokenTranslationRuleForm() {
  const { realm, subType, id } = useParams<RealmTokenTranslationRuleParams>();
  const list = toRealmTokenTranslationRules({ realm, subType });

  return (
    <TokenTranslationRuleForm
      parentId={realm}
      subType={subType}
      backToList={list}
      toEdit={(createdId) =>
        toEditRealmTokenTranslationRule({
          realm,
          subType,
          id: createdId,
        })
      }
      manageRole="manage-realm"
      id={id}
    />
  );
}
