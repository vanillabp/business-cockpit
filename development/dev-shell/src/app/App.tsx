import React, { Suspense } from 'react';
import { Box, Grommet } from 'grommet';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import { useAppContext } from '../AppContext';
import i18n from '../i18n';
import '../i18n';
import { CurrentUser } from './CurrentUser';
import { GuiSseProvider } from '../client/guiClient';
import { MessageToast } from '@bc/shared/components/Toast';
import { Test } from '@bc/shared/components/Test';
import { LoadingIndicator } from '@bc/shared/components/LoadingIndicator';
import { useKeepNowUpToDate } from '@bc/shared/utils/now-hook';
import { theme } from '@bc/shared/theme';

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
    });
    
const App = () => {

  const { state, dispatch } = useAppContext();

  useKeepNowUpToDate();
  
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
                  <Route path="*" element={<Test />} />
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
