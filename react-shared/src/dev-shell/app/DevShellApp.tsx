import React, { Suspense } from 'react';
import { Box, Grommet } from 'grommet';
import { BrowserRouter as Router, Outlet, Route, Routes } from 'react-router-dom';
import { useAppContext } from '../DevShellAppContext.js';
import i18n from '../i18n.js';
import '../i18n.js';
import { CurrentUser } from './CurrentUser.js';
import { GuiSseProvider } from '../client/guiClient.js';
import { MessageToast } from '../../components/Toast.js';
import { LoadingIndicator } from '../../components/LoadingIndicator.js';
import { useKeepNowUpToDate } from '../../utils/now-hook.js';
import { theme } from '../../theme/index.js';
import { UserTaskAppLayout } from '../../components/UserTaskAppLayout.js';
import { Header } from './Header.js';
import { useTranslation } from 'react-i18next';
import { UserTaskAppContextConsumer, UserTaskAppContextProvider } from './UserTaskAppContext.js';
import { UserTaskForm } from '../../types/UserTaskForm.js';

const appNs = 'app';

i18n.addResources('en', appNs, {
      "title.long": 'VanillaBP Business Cockpit Dev Shell',
      "title.short": 'BC DevShell',
      "error": "Error",
      "unexpected": "An unexpected event occured. Please try again later.",
      "validation": "You have enter data we could not process. Please fix them and try again.",
      "forbidden": "This action is forbidden. If you think this is an error then please retry but logout and login before. If the error still persists then get in contact with the management board.",
      "not-found": "The requested page is unknown!",
      "not-found hint": "Maybe use used a link in a mail which is already expired.",
      "url-usertask": "task",
      "url-icon": "icon",
      "url-list": "list",
    });
i18n.addResources('de', appNs, {
      "title.long": 'VanillaBP Business Cockpit Dev Shell',
      "title.short": 'BC DevShell',
      "error": "Fehler",
      "unexpected": "Ein unerwartetes Ereignis ist aufgetreten. Bitte versuche es später nochmals.",
      "validation": "Du hast Daten angegeben, die wir nicht verarbeiten konnten. Bitte korrigiere sie und versuche es nochmal.",
      "forbidden": "Diese Aktion ist verboten. Wenn du denkst, dass es sich um einen Fehler handelt, dann versuche es nochmals und melde dich davor ab und wieder an. Besteht das Problem weiterhin, dann kontaktiere bitte den Vereinsvorstand.",
      "not-found": "Die angeforderte Seite ist unbekannt!",
      "not-found hint": "Eventuell hast du einen Link aus einer Mail verwendet, der bereits veraltet ist.",
      "url-usertask": "aufgabe",
      "url-icon": "symbol",
      "url-list": "liste",
    });
    
const DevShellApp = ({
  officialGuiApiUrl,
  userTaskForm
}: {
  officialGuiApiUrl: string,
  userTaskForm: UserTaskForm,
}) => {

  const { state, dispatch } = useAppContext();
  const { t } = useTranslation(appNs);

  useKeepNowUpToDate();
  
  const Form = userTaskForm;

  return (
    <Grommet
        theme={theme}
        full>
      {state.toast && (
        <MessageToast dispatch={dispatch} msg={state.toast} />
      )}
      <GuiSseProvider>
        <Router>
          <Box
              fill>
            <Suspense fallback={<LoadingIndicator />}>
              <CurrentUser>
                <Routes>
                  <Route
                      path={ t('url-usertask') as string }
                      element={
                          <UserTaskAppLayout
                              header={<Header />}>
                            <Suspense fallback={ <div>loading</div> }>
                              <UserTaskAppContextProvider
                                  officialGuiApiUrl={ officialGuiApiUrl }>
                                <Outlet />
                              </UserTaskAppContextProvider>
                            </Suspense>
                          </UserTaskAppLayout>
                      }>
                    <Route path=":userTaskId">
                      <Route
                          index
                          element={
                              <UserTaskAppContextConsumer>
                                { (userTask) => <Form userTask={ userTask } /> }
                              </UserTaskAppContextConsumer>
                        } />
                      <Route
                          path={ t('url-icon') as string }
                          element={
                              <div>Icon</div>
                        } />
                      <Route
                          path={ t('url-list') as string }
                          element={
                              <div>List</div>
                        } />
                    </Route>
                    <Route
                        index
                        element={<div>Empty</div>} />
                  </Route>
                </Routes>
                {
                  state.loadingIndicator
                      ? <LoadingIndicator />
                      : <></>
                }
              </CurrentUser>
            </Suspense>
          </Box>
        </Router>
      </GuiSseProvider>
    </Grommet>
  );
};


export { DevShellApp, appNs };
