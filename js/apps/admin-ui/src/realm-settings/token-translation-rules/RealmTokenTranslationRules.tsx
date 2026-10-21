import { Tab, TabTitleText } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { ScopedTokenTranslationRulesList } from "../../identity-providers/token-translation-rules/TokenTranslationRulesList";
import {
  RoutableTabs,
  useRoutableTab,
} from "../../components/routable-tabs/RoutableTabs";
import {
  toAddRealmTokenTranslationRule,
  toEditRealmTokenTranslationRule,
  toRealmTokenTranslationRules,
} from "../routes/TokenTranslationRules";

type RealmTokenTranslationRulesProps = {
  realm: string;
};

export const RealmTokenTranslationRules = ({
  realm,
}: RealmTokenTranslationRulesProps) => {
  const { t } = useTranslation();
  const globalRoute = toRealmTokenTranslationRules({
    realm,
    subType: "global",
  });
  const defaultRoute = toRealmTokenTranslationRules({
    realm,
    subType: "default",
  });
  const globalTab = useRoutableTab(globalRoute);
  const defaultTab = useRoutableTab(defaultRoute);

  return (
    <RoutableTabs defaultLocation={globalRoute} mountOnEnter unmountOnExit>
      <Tab
        title={<TabTitleText>{t("global")}</TabTitleText>}
        data-testid="global-token-translation-rules-tab"
        {...globalTab}
      >
        <ScopedTokenTranslationRulesList
          parentId={realm}
          subType="global"
          toCreate={toAddRealmTokenTranslationRule({
            realm,
            subType: "global",
          })}
          toDetail={(id) =>
            toEditRealmTokenTranslationRule({
              realm,
              subType: "global",
              id,
            })
          }
        />
      </Tab>
      <Tab
        title={<TabTitleText>{t("defaultTokenTranslationRules")}</TabTitleText>}
        data-testid="default-token-translation-rules-tab"
        {...defaultTab}
      >
        <ScopedTokenTranslationRulesList
          parentId={realm}
          subType="default"
          toCreate={toAddRealmTokenTranslationRule({
            realm,
            subType: "default",
          })}
          toDetail={(id) =>
            toEditRealmTokenTranslationRule({
              realm,
              subType: "default",
              id,
            })
          }
        />
      </Tab>
    </RoutableTabs>
  );
};
