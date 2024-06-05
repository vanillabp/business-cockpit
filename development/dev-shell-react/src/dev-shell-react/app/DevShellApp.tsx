import React, { Suspense, useCallback } from 'react';
import { Box, Grommet, ThemeType } from 'grommet';
import { BrowserRouter as Router, Outlet, Route, Routes } from 'react-router-dom';
import { useAppContext } from '../DevShellAppContext.js';
import i18n from '../i18n.js';
import '../i18n.js';
import { CurrentUser } from './CurrentUser.js';
import { GuiSseProvider } from '../client/guiClient.js';
import {
  BcWorkflowModule,
  ColumnsOfUserTaskFunction,
  ColumnsOfWorkflowFunction,
  LoadingIndicator,
  MessageToast,
  Toast,
  useKeepNowUpToDate,
  UserTaskAppLayout,
  UserTaskForm,
  UserTaskListCell,
  WorkflowListCell,
  WorkflowModuleComponent,
  WorkflowPage
} from '@vanillabp/bc-shared';
import { Header as UserTaskHeader } from '../usertask/Header.js';
import { Header as WorkflowHeader } from '../workflow/Header.js';
import { useTranslation } from 'react-i18next';
import { UserTaskAppContextConsumer, UserTaskAppContextProvider } from '../usertask/UserTaskAppContext.js';
import { WorkflowAppContextConsumer, WorkflowAppContextProvider } from '../workflow/WorkflowAppContext.js';
import { WorkflowAppLayout } from '../workflow/WorkflowAppLayout.js';
import { Main } from './Main.js';
import { List as UserTaskList } from "../usertask/List.js";
import { List as WorkflowList } from "../workflow/List.js";

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
      "link-usertask": "task",
      "url-usertask": "task",
      "url-icon": "icon",
      "url-list": "list",
      "link-workflow": "workflow",
      "url-workflow": "workflow",
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
      "link-usertask": "Aufgabe",
      "url-usertask": "aufgabe",
      "url-icon": "symbol",
      "url-list": "liste",
      "link-workflow": "Vorgang",
      "url-workflow": "vorgang",
    });
    
const DevShellApp = ({
  workflowModule,
  theme,
  officialGuiApiUrl,
  userTaskForm,
  userTaskListColumns,
  userTaskListCell,
  workflowListColumns,
  workflowListCell,
  workflowPage,
  additionalComponents,
}: {
  workflowModule: BcWorkflowModule,
  theme: ThemeType,
  officialGuiApiUrl: string,
  userTaskForm: UserTaskForm,
  userTaskListColumns: ColumnsOfUserTaskFunction,
  userTaskListCell: UserTaskListCell,
  workflowListColumns: ColumnsOfWorkflowFunction,
  workflowListCell: WorkflowListCell,
  workflowPage: WorkflowPage,
  additionalComponents?: Record<string, WorkflowModuleComponent>,
}) => {

  const { state, dispatch } = useAppContext();
  const { t } = useTranslation(appNs);
  const toast = useCallback((toast: Toast) => {
    dispatch({ type: 'toast', toast });
  }, [ dispatch ]);

  useKeepNowUpToDate();
  
  const UserTaskFormComponent = userTaskForm;
  const WorkflowPageComponent = workflowPage;

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
                      index
                      element={ <Main
                          additionalComponents={ Object.keys(additionalComponents ?? {}) } /> } />
                  {
                    additionalComponents !== undefined
                        ? Object.keys(additionalComponents)
                            .map(componentName => {
                              const Component = additionalComponents[componentName];
                              return (
                                  <Route
                                      key={ componentName }
                                      path={ componentName }
                                      element={ <Component
                                          key={ componentName }
                                          workflowModule={ workflowModule }
                                          toast={ toast }
                                      /> } />);
                            })
                        : <></>
                  }
                  <Route
                      path={ t('url-usertask') as string }
                      element={
                          <UserTaskAppLayout
                              header={<UserTaskHeader />}>
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
                                { (userTask) => <UserTaskFormComponent userTask={ userTask } /> }
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
                            <UserTaskAppContextConsumer>
                              { (userTask) => <UserTaskList
                                  userTask={ userTask }
                                  userTaskColumns={ userTaskListColumns(userTask) }
                                  UserTaskListCell={ userTaskListCell } /> }
                            </UserTaskAppContextConsumer>
                        } />
                    </Route>
                    <Route
                        index
                        element={<div>Empty</div>} />
                  </Route>
                  <Route
                      path={ t('url-workflow') as string }
                      element={
                          <WorkflowAppLayout
                              header={<WorkflowHeader />}>
                            <Suspense fallback={ <div>loading</div> }>
                              <WorkflowAppContextProvider
                                  officialGuiApiUrl={ officialGuiApiUrl }>
                                <Outlet />
                              </WorkflowAppContextProvider>
                            </Suspense>
                          </WorkflowAppLayout>
                      }>
                    <Route path=":workflowId">
                      <Route
                          index
                          element={
                              <WorkflowAppContextConsumer>
                                { (workflow) => <WorkflowPageComponent workflow={ workflow } /> }
                              </WorkflowAppContextConsumer>
                        } />
                      <Route
                          path={ t('url-icon') as string }
                          element={
                              <div>Icon</div>
                        } />
                      <Route
                          path={ t('url-list') as string }
                          element={
                            <WorkflowAppContextConsumer>
                              { (workflow) => <WorkflowList
                                  workflow={ workflow }
                                  workflowColumns={ workflowListColumns(workflow) }
                                  WorkflowListCell={ workflowListCell } /> }
                            </WorkflowAppContextConsumer>
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
