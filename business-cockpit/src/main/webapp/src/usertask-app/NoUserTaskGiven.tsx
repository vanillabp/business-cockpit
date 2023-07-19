import React from 'react';
import { UserTaskAppLayout } from '@vanillabp/bc-shared';
import i18n from '../i18n';
import { NoElementGivenByModule } from '../components/NoElementGivenByModule';

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
}: {
  loading?: boolean,
  retry?: (callback?: () => void) => void
}) => {
  
  return (
      <UserTaskAppLayout>
        <NoElementGivenByModule
            loading={ loading }
            retry={ retry }
            translationNs='no-usertask-given' />
      </UserTaskAppLayout>);

}

export { NoUserTaskGiven };
