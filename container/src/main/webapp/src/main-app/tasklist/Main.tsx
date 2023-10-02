import { Route, Routes } from "react-router-dom";
import { ListOfTasks } from '@vanillabp/bc-ui';
import i18n from 'i18next';
import { useLayoutEffect } from "react";
import { useAppContext } from "../../AppContext";
import { useTasklistApi } from "../../utils/apis";
import { useGuiSse } from "../../client/guiClient";
import { navigateToWorkflow, openTask } from "../../utils/navigate";

i18n.addResources('en', 'tasklist', {
      "title.long": 'Tasks',
      "title.short": 'Tasks',
    });
i18n.addResources('de', 'tasklist', {
      "title.long": 'Aufgaben',
      "title.short": 'Aufgaben',
    });

const Main = () => {

  const { setAppHeaderTitle, showLoadingIndicator, toast } = useAppContext();

  useLayoutEffect(() => {
    setAppHeaderTitle('tasklist', false);
  }, [ setAppHeaderTitle ]);

  return (
    <Routes>
      <Route path='/' element={<ListOfTasks
                                  showLoadingIndicator={ showLoadingIndicator }
                                  toast={ toast }
                                  useTasklistApi={ useTasklistApi }
                                  useGuiSse={ useGuiSse }
                                  openTask={ openTask }
                                  navigateToWorkflow={ navigateToWorkflow } />} />
    </Routes>);
}

export default Main;
