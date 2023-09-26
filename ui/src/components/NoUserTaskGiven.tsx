import { ShowLoadingIndicatorFunction, UserTaskAppLayout } from '@vanillabp/bc-shared';
import i18n from 'i18next';
import { NoElementGivenByModule } from './index.js';

i18n.addResources('en', 'no-usertask-given', {
      "hint": "Unfortunately, the task cannot be shown at the moment!",
      "retry": "Retry loading...",
      "does-not-exist": "The requested task does not exist!"
    });
i18n.addResources('de', 'no-usertask-given', {
      "hint": "Leider ist derzeit kein Zugriff auf die Aufgabe möglich!",
      "retry": "Laden nochmals probieren...",
      "does-not-exist": "Die angeforderte Aufgabe existiert nicht!"
    });

const NoUserTaskGiven = ({
  loading = false,
  retry,
  showLoadingIndicator,
}: {
  loading?: boolean,
  retry?: (callback?: () => void) => void,
  showLoadingIndicator: ShowLoadingIndicatorFunction,
}) => {
  return (
      <UserTaskAppLayout>
        <NoElementGivenByModule
            loading={ loading }
            showLoadingIndicator={ showLoadingIndicator }
            retry={ retry }
            translationNs='no-usertask-given' />
      </UserTaskAppLayout>);
}

export { NoUserTaskGiven };
