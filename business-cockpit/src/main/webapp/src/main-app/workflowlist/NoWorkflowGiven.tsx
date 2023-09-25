import React from 'react';
import i18n from '../../i18n';
import { NoElementGivenByModule } from '../../components/NoElementGivenByModule';

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
}: {
  loading?: boolean,
  retry?: (callback?: () => void) => void
}) => {
  
  return (
      <NoElementGivenByModule
          loading={ loading }
          retry={ retry }
          translationNs='no-workflow-given' />);

}

export { NoWorkflowGiven };
