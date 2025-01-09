import { Route, Routes, useNavigate, useParams } from "react-router-dom";
import { ListOfWorkflows, WorkflowPage } from '@vanillabp/bc-ui';
import { i18n } from '@vanillabp/bc-shared';
import { useLayoutEffect } from "react";
import { useAppContext } from "../../AppContext";
import { useGuiSse } from "../../client/guiClient";
import { navigateToWorkflow, openTask } from "../../utils/navigate";
import { useTranslation } from "react-i18next";
import { useStandardTasklistApi, useStandardWorkflowlistApi } from "../../utils/standardApis";

i18n.addResources('en', 'workflowlist', {
      "title.long": 'Workflows',
      "title.short": 'Workflows',
      "boolean-true": 'Yes',
      "boolean-false": 'No',
      "total": "Total:",
      "no": "No.",
      "name": "Workflow",
      "column_title": "Name",
      "module-unknown": "Unknown module",
      "retry-loading-module-hint": "Unfortunately, the workflow cannot be shown at the moment!",
      "retry-loading-module": "Retry loading...",
      "does-not-exist": "The requested workflow does not exist!",
      "legend_new": "New",
      "legend_completed": "Completed",
      "legend_updated": "Updated",
      "legend_unchanged": "Unchanged",
      "legend_filtered": "Removed",
      "kwic_to-many-hits": "More than 20 hits...",
      "kwic_placeholder": "Enter 3 characters...",
      "kwic_no-hit": "No hit",
      "kwic_tooltip": "The words entered are treated case-sensitive",
      "refresh_workflows": "Refresh list",
    });
i18n.addResources('de', 'workflowlist', {
      "title.long": 'Vorgänge',
      "title.short": 'Vorgänge',
      "boolean-true": 'Ja',
      "boolean-false": 'Nein',
      "total": "Anzahl:",
      "no": "Nr.",
      "name": "Vorgang",
      "column_title": "Name",
      "module-unknown": "Unbekanntes Modul",
      "retry-loading-module-hint": "Leider ist derzeit kein Zugriff auf den Vorgang möglich!",
      "retry-loading-module": "Laden nochmals probieren...",
      "does-not-exist": "Der angeforderte Vorgang existiert nicht!",
      "legend_new": "Neu",
      "legend_completed": "Abgeschlossen",
      "legend_updated": "Aktualisiert",
      "legend_unchanged": "Unverändert",
      "legend_filtered": "Entfernt",
      "kwic_to-many-hits": "Mehr als 20 Treffer...",
      "kwic_placeholder": "Tippe mehr als 3 Zeichen...",
      "kwic_no-hit": "Kein Treffer",
      "kwic_tooltip": "Bitte geben Sie für Suchbegriffe die korrekte Groß-/Kleinschreibung an",
      "refresh_workflows": "Liste neu laden",
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
              useTasklistApi={ useStandardTasklistApi }
              openTask={
                (userTask) =>
                    openTask(userTask, toast, tApp) } />)
}

const Main = () => {

  const { setAppHeaderTitle, showLoadingIndicator, toast, state } = useAppContext();
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
                                useTasklistApi={ useStandardTasklistApi }
                                useGuiSse={ useGuiSse }
                                currentLanguage={ i18n.language }
                                currentUser={ state.currentUser }
                                t={ t }
                                defaultSort={ 'title' }
                                excludeIdColumn
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
