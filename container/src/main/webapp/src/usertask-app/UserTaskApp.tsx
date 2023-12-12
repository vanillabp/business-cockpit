import React, { useLayoutEffect } from 'react';
import { Box } from 'grommet';
import { Route, Routes, useNavigate, useParams } from 'react-router-dom';
import { useAppContext } from '../AppContext';
import { NoUserTaskGiven, UserTaskPage } from '@vanillabp/bc-ui';
import { navigateToWorkflow, openTask } from "../utils/navigate";
import i18n from "i18next";
import { useTranslation } from "react-i18next";
import { useStandardTasklistApi } from "../utils/standardApis";
import Footer from './Footer';
import Header from './Header';

i18n.addResources('en', 'usertask', {
  "module-unknown": "Unknown module",
  "retry-loading-module-hint": "Unfortunately, the task cannot be shown at the moment!",
  "retry-loading-module": "Retry loading...",
  "typeofitem_unsupported": "Wrong type",
  "does-not-exist": "The requested task does not exist!"
});
i18n.addResources('de', 'usertask', {
  "module-unknown": "Unbekanntes Modul",
  "retry-loading-module-hint": "Leider ist derzeit kein Zugriff auf die Aufgabe möglich!",
  "retry-loading-module-": "Laden nochmals probieren...",
  "typeofitem_unsupported": "Typfehler",
  "does-not-exist": "Die angeforderte Aufgabe existiert nicht!"
});

const RouteBasedUserTaskApp = () => {
  const { showLoadingIndicator, toast } = useAppContext();
  const userTaskId: string | undefined = useParams()['*'];
  const { t } = useTranslation('usertask');
  const { t: tApp } = useTranslation('app');
  const navigate = useNavigate();

  if (userTaskId === undefined) {
    return <NoUserTaskGiven
              t={ t }
              showLoadingIndicator={ showLoadingIndicator } />;
  }

  return (
    <UserTaskPage
      userTaskId={ userTaskId }
      useTasklistApi={ useStandardTasklistApi }
      showLoadingIndicator={ showLoadingIndicator }
      toast={ toast }
      t={ t }
      openTask={ (userTask) => openTask(userTask, toast, tApp) }
      navigateToWorkflow={ (userTask) => navigateToWorkflow(userTask, toast, tApp, navigate) }
      header={ <Header/> }
      footer={ <Footer/> }
    />
  )
}

const UserTaskApp = () => {

  const { setAppHeaderTitle, showLoadingIndicator } = useAppContext();
  const { t } = useTranslation('usertask');

  useLayoutEffect(() => {
    setAppHeaderTitle('app');
  }, [ setAppHeaderTitle ]);

  return (
      <Box
          direction='row'
          fill
          style={ { display: 'unset' } } /* to avoid removing bottom margin of inner boxes */
          overflow={ { horizontal: 'hidden' } }>
        <Routes>
          <Route index element={<NoUserTaskGiven
                                  t={ t }
                                  showLoadingIndicator={ showLoadingIndicator } />} />
          <Route path="/:userTaskId" element={ <RouteBasedUserTaskApp /> } />
        </Routes>
      </Box>);

};

export default UserTaskApp;
