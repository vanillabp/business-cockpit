import { Route, Routes, useNavigate, useParams } from "react-router-dom";
import { ListOfWorkflows, WorkflowPage } from '@vanillabp/bc-ui';
import i18n from 'i18next';
import i18next from 'i18next';
import { useLayoutEffect } from "react";
import { useAppContext } from "../../AppContext";
import { useGuiSse } from "../../client/guiClient";
import { navigateToWorkflow, openTask } from "../../utils/navigate";
import { useTranslation } from "react-i18next";
import { useStandardWorkflowlistApi } from "../../utils/standardApis";

i18n.addResources('en', 'workflowlist', {
      "title.long": 'Workflows',
      "title.short": 'Workflows',
      "total": "Total:",
      "no": "No.",
      "name": "Workflow",
      "column_title": "Name",
      "module-unknown": "Unknown module",
      "retry-loading-module-hint": "Unfortunately, the workflow cannot be shown at the moment!",
      "retry-loading-module": "Retry loading...",
      "does-not-exist": "The requested workflow does not exist!"
    });
i18n.addResources('de', 'workflowlist', {
      "title.long": 'Vorgänge',
      "title.short": 'Vorgänge',
      "total": "Anzahl:",
      "no": "Nr.",
      "name": "Vorgang",
      "column_title": "Name",
      "module-unknown": "Unbekanntes Modul",
      "retry-loading-module-hint": "Leider ist derzeit kein Zugriff auf den Vorgang möglich!",
      "retry-loading-module": "Laden nochmals probieren...",
      "does-not-exist": "Der angeforderte Vorgang existiert nicht!"
    });

const RouteBasedWorkflowPage = () => {
  const { showLoadingIndicator, toast } = useAppContext();
  const workflowId: string | undefined = useParams()['*'];
  const { t: tApp } = useTranslation('app');
  const { t } = useTranslation('workflowlist');
  return (<WorkflowPage
              workflowId={ workflowId }
              showLoadingIndicator={ showLoadingIndicator }
              toast={ toast }
              t={ t }
              useWorkflowlistApi={ useStandardWorkflowlistApi }
              openTask={
                (userTask) =>
                    openTask(userTask, toast, tApp) } />)
}

const Main = () => {

  const { setAppHeaderTitle, showLoadingIndicator, toast } = useAppContext();
  const { t: tApp } = useTranslation('app');
  const { t } = useTranslation('workflowlist');
  const navigate = useNavigate();

  useLayoutEffect(() => {
    setAppHeaderTitle('workflowlist', false);
  }, [ setAppHeaderTitle ]);

  return (
    <Routes>
      <Route index element={<ListOfWorkflows
                                showLoadingIndicator={ showLoadingIndicator }
                                useWorkflowlistApi={ useStandardWorkflowlistApi }
                                useGuiSse={ useGuiSse }
                                currentLanguage={ i18next.language }
                                t={ t }
                                defaultSort={ 'title' }
                                openTask={
                                  (userTask) =>
                                      openTask(userTask, toast, tApp) }
                                navigateToWorkflow={
                                  (workflow) =>
                                      navigateToWorkflow(workflow, toast, tApp, navigate) }/>} />
      <Route path="/:workflowId" element={<RouteBasedWorkflowPage />} />

    </Routes>);
}

export default Main;
