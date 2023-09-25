import React, { Suspense, useEffect, lazy } from 'react';
import { Box, Grommet } from 'grommet';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import { useAppContext } from '../AppContext';
import { useTranslation } from 'react-i18next';
import i18n from '../i18n';
import '../i18n';
import { CurrentUser } from './CurrentUser';
import { GuiSseProvider } from '../client/guiClient';
import { MessageToast } from '@vanillabp/bc-shared';
import { LoadingIndicator } from '@vanillabp/bc-shared';
import { useKeepNowUpToDate } from '@vanillabp/bc-shared';
import { Login } from './Login';
import { ProtectedRoute } from '../app/ProtectedRoute';
import { theme } from '@vanillabp/bc-shared';

const MainApp = lazy(() => import('../main-app/MainApp'));
const UserTaskApp = lazy(() => import('../usertask-app/UserTaskApp'));

const appNs = 'app';

i18n.addResources('en', appNs, {
      "title.long": 'VanillaBP Business Cockpit',
      "title.short": 'BC',
      "error": "Error",
      "unexpected": "An unexpected event occured. Please try again later.",
      "validation": "You have enter data we could not process. Please fix them and try again.",
      "forbidden": "This action is forbidden. If you think this is an error then please retry but logout and login before. If the error still persists then get in contact with the management board.",
      "not-found": "The requested page is unknown!",
      "not-found hint": "Maybe use used a link in a mail which is already expired.",
      "url-tasklist": "tasks",
      "url-usertask": "task",
      "url-workflowlist": "workflows",
      "unsupported-ui-uri-type_title": "Open item",
      "unsupported-ui-uri-type_message": "Internal error: The item refers to an unsupported UI-URI-type!",
    });
i18n.addResources('de', appNs, {
      "title.long": 'VanillaBP Business Cockpit',
      "title.short": 'BC',
      "error": "Fehler",
      "unexpected": "Ein unerwartetes Ereignis ist aufgetreten. Bitte versuche es später nochmals.",
      "validation": "Du hast Daten angegeben, die wir nicht verarbeiten konnten. Bitte korrigiere sie und versuche es nochmal.",
      "forbidden": "Diese Aktion ist verboten. Wenn du denkst, dass es sich um einen Fehler handelt, dann versuche es nochmals und melde dich davor ab und wieder an. Besteht das Problem weiterhin, dann kontaktiere bitte den Vereinsvorstand.",
      "not-found": "Die angeforderte Seite ist unbekannt!",
      "not-found hint": "Eventuell hast du einen Link aus einer Mail verwendet, der bereits veraltet ist.",
      "url-tasklist": "aufgaben",
      "url-usertask": "aufgabe",
      "url-workflowlist": "vorgaenge",
      "unsupported-ui-uri-type_title": "Element öffnen",
      "unsupported-ui-uri-type_message": "Internes Problem: Das Element bezieht sich auf einen nicht unterstützten UI-URI-Typ!",
    });
    
type AppProps = {};

const App: React.FC<AppProps> = (_props: AppProps): JSX.Element => {

  const { state, fetchAppInformation, dispatch } = useAppContext();
  
  useKeepNowUpToDate();

  useEffect(() => {
    fetchAppInformation();
  }, [ fetchAppInformation ]);
  
  const { t, i18n } = useTranslation('app');
  if (state!.appInformation !== null) {
    ['en', 'de']
        .forEach(lng => {
          i18n.addResource(lng, appNs, 'title.long', state.appInformation!.titleLong!);
          i18n.addResource(lng, appNs, 'title.short', state.appInformation!.titleShort!);
          i18n.addResource(lng, appNs, 'homepage', state.appInformation!.homepageUrl!);
        });
  }

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
                  <Route path={ `${ t('url-usertask') }/*` } element={<UserTaskApp />} />
                  <Route path='login' element={<Login />} />
                  <Route element={<ProtectedRoute />}>
                    <Route path="*" element={<MainApp />} />
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


export default App;
