import i18n from 'i18next';
import { NoElementGivenByModule } from './index.js';
import { ShowLoadingIndicatorFunction } from "@vanillabp/bc-shared";

i18n.addResources('en', 'no-workflow-given', {
      "hint": "Unfortunately, the workflow cannot be shown at the moment!",
      "retry": "Retry loading...",
      "does-not-exist": "The requested workflow does not exist!"
    });
i18n.addResources('de', 'no-workflow-given', {
      "hint": "Leider ist derzeit kein Zugriff auf den Vorgang möglich!",
      "retry": "Laden nochmals probieren...",
      "does-not-exist": "Der angeforderte Vorgang existiert nicht!"
    });

const NoWorkflowGiven = ({
  loading = false,
  retry,
  showLoadingIndicator,
}: {
  loading?: boolean,
  retry?: (callback?: () => void) => void,
  showLoadingIndicator: ShowLoadingIndicatorFunction,
}) => {
  return (
      <NoElementGivenByModule
          loading={ loading }
          showLoadingIndicator={ showLoadingIndicator }
          retry={ retry }
          translationNs='no-workflow-given' />);

}

export { NoWorkflowGiven };
