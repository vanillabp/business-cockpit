import { Route, Routes, useNavigate } from "react-router-dom";
import { ListOfTasks } from '@vanillabp/bc-ui';
import i18n from 'i18next';
import i18next from 'i18next';
import { useLayoutEffect } from "react";
import { useAppContext } from "../../AppContext";
import { useGuiSse } from "../../client/guiClient";
import { navigateToWorkflow, openTask } from "../../utils/navigate";
import { useTranslation } from "react-i18next";
import { useStandardTasklistApi } from "../../utils/standardApis";

i18n.addResources('en', 'tasklist', {
      "title.long": 'Tasks',
      "title.short": 'Tasks',
      "total": "Total:",
      "no": "No.",
      "name": "task",
      "module-unknown": "Unknown module",
      "retry-loading-module-hint": "Unfortunately, the task cannot be shown at the moment!",
      "retry-loading-module": "Retry loading...",
      "typeofitem_unsupported": "Wrong type",
      "legend_new": "New",
      "legend_completed": "Completed",
      "legend_updated": "Updated",
      "legend_unchanged": "Unchanged",
    });
i18n.addResources('de', 'tasklist', {
      "title.long": 'Aufgaben',
      "title.short": 'Aufgaben',
      "total": "Anzahl:",
      "no": "Nr.",
      "name": "Aufgabe",
      "module-unknown": "Unbekanntes Modul",
      "retry-loading-module-hint": "Leider ist derzeit kein Zugriff auf die Aufgabe möglich!",
      "retry-loading-module-": "Laden nochmals probieren...",
      "typeofitem_unsupported": "Typfehler",
      "legend_new": "Neu",
      "legend_completed": "Abgeschlossen",
      "legend_updated": "Aktualisiert",
      "legend_unchanged": "Unverändert",
    });

const Main = () => {

  const { setAppHeaderTitle, showLoadingIndicator, toast } = useAppContext();
  const { t: tApp } = useTranslation('app');
  const { t } = useTranslation('tasklist');
  const navigate = useNavigate();

  useLayoutEffect(() => {
    setAppHeaderTitle('tasklist', false);
  }, [ setAppHeaderTitle ]);

  return (
    <Routes>
      <Route path='/' element={<ListOfTasks
                                  showLoadingIndicator={ showLoadingIndicator }
                                  useTasklistApi={ useStandardTasklistApi }
                                  useGuiSse={ useGuiSse }
                                  t={ t }
                                  currentLanguage={ i18next.language }
                                  openTask={
                                      (userTask) =>
                                          openTask(userTask, toast, tApp) }
                                  navigateToWorkflow={
                                      (userTask) =>
                                          navigateToWorkflow(userTask, toast, tApp, navigate) } />} />
    </Routes>);
}

export default Main;
