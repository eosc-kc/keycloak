import { Button, ActionGroup } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../components/form/FormAccess";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { convertAttributeNameToForm } from "../../util";
import type { FormFields, SaveOptions } from "../ClientDetails";

type IshareSettingsProps = {
    save: (options?: SaveOptions) => void;
    reset: () => void;
};

export const IshareSettings = ({
                                   save,
                                   reset,
                               }: IshareSettingsProps) => {
    const { t } = useTranslation();

    return (
        <FormAccess role="manage-clients" isHorizontal>
            <DefaultSwitchControl
                name={convertAttributeNameToForm<FormFields>(
                    "attributes.ishareEnabled",
                )}
                label={t("ishare")}
                labelIcon={t("ishareHelp")}
                stringify
            />
            <ActionGroup>
                <Button
                    variant="secondary"
                    id="ishareSave"
                    data-testid="ishareSave"
                    onClick={() => save()}
                >
                    {t("save")}
                </Button>
                <Button
                    id="ishareRevert"
                    data-testid="ishareRevert"
                    variant="link"
                    onClick={reset}
                >
                    {t("revert")}
                </Button>
            </ActionGroup>
        </FormAccess>
    );
};
