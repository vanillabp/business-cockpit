import { Route, Routes, useNavigate } from "react-router-dom";
import { ListOfTasks } from '@vanillabp/bc-ui';
import { i18n } from '@vanillabp/bc-shared';
import { useLayoutEffect } from "react";
import { useAppContext } from "../../AppContext";
import { useGuiSse } from "../../client/guiClient";
import { navigateToWorkflow, openTask } from "../../utils/navigate";
import { useTranslation } from "react-i18next";
import {
  useCurrentUsersTasksTasklistApi,
  useStandardTasklistApi,
  useUsersRoleTasksTasksTasklistApi
} from "../../utils/standardApis";

i18n.addResources('en', 'tasklist', {
      "title.long": 'Tasks',
      "title.short": 'Tasks',
      "total": "Total:",
      "column_no": "No.",
      "column_title": "Task",
      "column_assignee": "Assignee",
      "column_candidates": "Candidates",
      "module-unknown": "Unknown module",
      "retry-loading-module-hint": "Unfortunately, the task cannot be shown at the moment!",
      "retry-loading-module": "Retry loading...",
      "typeofitem_unsupported": "Wrong type",
      "legend_new": "New",
      "legend_completed": "Completed",
      "legend_updated": "Updated",
      "legend_unchanged": "Unchanged",
      "legend_filtered": "Removed",
      "claim_task": "Claim",
      "claim_tasks": "Claim selected tasks",
      "unclaim_tasks": "Return selected tasks",
      "mark_as_unread": "Mark selected tasks as 'unread'",
      "mark_as_read": "Mark selected tasks as 'read'",
      "assign_task": "Assign",
      "assign_placeholder": "Min. 3 characters...",
      "assign_loading": "Loading suggestions...",
      "refresh_tasks": "Reload tasks",
    });
i18n.addResources('de', 'tasklist', {
      "title.long": 'Aufgaben',
      "title.short": 'Aufgaben',
      "total": "Anzahl:",
      "column_no": "Nr.",
      "column_title": "Aufgabe",
      "column_assignee": "Bearbeiter",
      "column_candidates": "Kandidaten",
      "module-unknown": "Unbekanntes Modul",
      "retry-loading-module-hint": "Leider ist derzeit kein Zugriff auf die Aufgabe möglich!",
      "retry-loading-module-": "Laden nochmals probieren...",
      "typeofitem_unsupported": "Typfehler",
      "legend_new": "Neu",
      "legend_completed": "Abgeschlossen",
      "legend_updated": "Aktualisiert",
      "legend_unchanged": "Unverändert",
      "legend_filtered": "Entfernt",
      "claim_task": "Übernehmen",
      "claim_tasks": "Gewählte Aufgaben übernehmen",
      "unclaim_tasks": "Gewählte Aufgabe zurückgeben",
      "mark_as_unread": "Gewählte Aufgaben als 'Ungelesen' markieren",
      "mark_as_read": "Gewählte Aufgaben als 'Gelesen' markieren",
      "assign_task": "Zuweisen",
      "assign_placeholder": "Mind. 3 Zeichen...",
      "assign_loading": "Lade Vorschläge...",
      "refresh_tasks": "Aufgabenliste neu laden",
    });

const CustomListOfTasks = ({ useTasklistApi }) => {
  const { showLoadingIndicator, toast } = useAppContext();
  const { t: tApp } = useTranslation('app');
  const { t } = useTranslation('tasklist');
  const navigate = useNavigate();

  return <ListOfTasks
      showLoadingIndicator={showLoadingIndicator}
      useTasklistApi={useTasklistApi}
      useGuiSse={useGuiSse}
      t={t}
      currentLanguage={i18n.language}
      defaultSort={"dueDate"}
      openTask={
        (userTask) =>
            openTask(userTask, toast, tApp)}
      navigateToWorkflow={
        (userTask) =>
            navigateToWorkflow(userTask, toast, tApp, navigate)}/>;

}

const Main = () => {

  const { setAppHeaderTitle } = useAppContext();
  const { t: tApp } = useTranslation('app');

  useLayoutEffect(() => {
    setAppHeaderTitle('tasklist', false);
  }, [ setAppHeaderTitle ]);

  return (
    <Routes>
      <Route
          path={ tApp('url-tasklist-for-current-user') }
          element={ <CustomListOfTasks useTasklistApi={ useCurrentUsersTasksTasklistApi } /> } />
      <Route
          path={ tApp('url-tasklist-by-users-roles') }
          element={ <CustomListOfTasks useTasklistApi={ useUsersRoleTasksTasksTasklistApi } /> } />
      <Route
          path='/'
          element={<CustomListOfTasks useTasklistApi={ useStandardTasklistApi } /> } />
    </Routes>);
}

export default Main;
