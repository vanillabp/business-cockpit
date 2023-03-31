import React, { Suspense, useEffect, lazy } from 'react';
import { Box, Grommet, Heading, Text, ThemeType } from 'grommet';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import { useAppContext } from '../AppContext';
import { AppHeader } from './menu/AppHeader';
import { Main } from './Main';
import { useTranslation } from 'react-i18next';
import i18n from '../i18n';
import '../i18n';
import { ProtectedRoute } from './ProtectedRoute';
import { CurrentUser } from './CurrentUser';
import { GuiSseProvider } from '../client/guiClient';
import { Login } from './Login';
import { css } from 'styled-components';
import { MessageToast } from '../components/Toast';
import { LoadingIndicator } from '../components/LoadingIndicator';
import { useKeepNowUpToDate } from '../utils/now-hook';

export const theme: ThemeType = {
  global: {
    colors: {
      brand: '#e2e2e2',
      'accent-1': '#ffe699',
      'accent-2': '#bf9000',
      'accent-3': '#663300',
      'accent-4': '#333333',
      'placeholder': '#bbbbbb',
      'light-5': '#c7c7c7',
      'light-6': '#b4b4b4',
    },
    font: {
      family: 'Roboto',
      size: '18px',
      height: '20px',
    },
    focus: {
      border:  {
        color: '#e0a244',
      },
      outline: {
        color: '#e0a244',
      }
    }
  },
  table: {
    header: {
      border: undefined,
    },
    body: {
      extend: css`
        overflow: visible;
      `
    },
  },
  heading: {
    color: '#444444',
    extend: css`
      margin-top: 0;
    `
  },
  formField: {
    label: {
      requiredIndicator: true,
    }
  },
  textArea: {
    extend: css`
      font-weight: normal;
      ::placeholder {
        font-weight: normal;
        color: ${props => props.theme.global.colors.placeholder};
      }
    `,
  },
  maskedInput: {
    extend: css`
      ::placeholder {
        font-weight: normal;
        color: ${props => props.theme.global.colors.placeholder};
      }
    `,
  },
  textInput: {
    extend: css`
      ::placeholder {
        font-weight: normal;
        color: ${props => props.theme.global.colors.placeholder};
      }
    `,
    placeholder: {
      extend: css`
          font-weight: normal;
          color: ${props => props.theme.global.colors.placeholder};
        `
    }
  },
  button: {
    default: {
      background: '#ffffff',
      border: { color: 'accent-1', width: '3px' },
      color: 'accent-3'
    },
    primary: {
      background: 'accent-1',
      border: { color: 'accent-2', width: '3px' },
      color: 'accent-3'
    },
    secondary: {
      background: 'accent-2',
      border: { color: 'accent-3', width: '3px' },
      color: 'accent-1'
    },
    hover: {
      default: {
        background: 'accent-1',
        color: 'accent-3'
      },
      primary: {
        background: 'accent-2',
        color: 'accent-1'
      },
      secondary: {
        background: 'accent-3',
        color: 'accent-1'
      }
    },
    disabled: {
      opacity: 1,
      color: 'dark-4',
      background: 'light-2',
      border: { color: 'light-4' }
    }
  },
  accordion: {
    heading: {
      margin: 'small'
    },
    icons: {
      color: 'accent-3'
    }
  },
  dataTable: {
    pinned: {
      header: {
        background: {
          color: 'accent-2',
          opacity: 'strong'
        },
        extend: css`
          z-index: 19;
        `
      },
    }
  },
  page: {
    wide: {
      width: {
        min: 'small',
        max: 'xlarge'
      }
    }
  },
  paragraph: {
    extend: css`
      margin-top: 0;
    `
  },
  checkBox: {
    size: '20px'
  },
};

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
    });
    
const TaskList = lazy(() => import('../tasklist/Main'));

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
            <AppHeader />
            <Box
                direction='row'
                style={ { display: 'unset' } } /* to avoid removing bottom margin of inner boxes */
                overflow={ { horizontal: 'hidden' } }>
              <Suspense fallback={<LoadingIndicator />}>
                <CurrentUser>
                  <Routes>
                    <Route element={<ProtectedRoute />}>
                      <Route path={t('url-tasklist') + '/*'} element={<TaskList />} />
                    </Route>
                    <Route path='/login' element={<Login />} />
                    <Route element={<ProtectedRoute />}>
                      <Route path='/' element={<Main />} />
                    </Route>
                    <Route path='*' element={
                      <Box
                          direction='column'
                          fill='horizontal'
                          flex='shrink'
                          align='center'
                          gap='medium'
                          pad='medium'
                          width='medium'>
                        <Heading level='3'>{t('not-found')}</Heading>
                        <Text>{t('not-found hint')}</Text>
                      </Box>
                    } />
                  </Routes>
                  {
                    state.loadingIndicator
                        ? <LoadingIndicator />
                        : <></>
                  }
                </CurrentUser>
              </Suspense>
            </Box>
          </Box>
        </Router>
      </GuiSseProvider>
    </Grommet>
  );
};


export default App;
