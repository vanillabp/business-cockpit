import { Route, Routes, useParams } from "react-router-dom";
import { ListOfWorkflows, OpenTaskFunction, WorkflowlistApiHook, WorkflowPage } from '@vanillabp/bc-ui';
import i18n from 'i18next';
import { useLayoutEffect } from "react";
import { useAppContext } from "../../AppContext";
import { useGuiSse } from "../../client/guiClient";
import { useWorkflowlistApi } from "../../utils/apis";
import { navigateToWorkflow, openTask } from "../../utils/navigate";
import { ShowLoadingIndicatorFunction, ToastFunction } from "@vanillabp/bc-shared";

i18n.addResources('en', 'workflowlist', {
      "title.long": 'Workflows',
      "title.short": 'Workflows',
    });
i18n.addResources('de', 'workflowlist', {
      "title.long": 'Vorgänge',
      "title.short": 'Vorgänge',
    });

const RouteBasedWorkflowPage = ({
  showLoadingIndicator,
  toast,
  useWorkflowlistApi,
  openTask,
}: {
  showLoadingIndicator: ShowLoadingIndicatorFunction,
  toast: ToastFunction,
  useWorkflowlistApi: WorkflowlistApiHook,
  openTask: OpenTaskFunction,
}) => {
  const workflowId: string | undefined = useParams()['*'];
  return (<WorkflowPage
              workflowId={ workflowId }
              showLoadingIndicator={ showLoadingIndicator }
              toast={ toast }
              useWorkflowlistApi={ useWorkflowlistApi }
              openTask={ openTask } />)
}

const Main = () => {

  const { setAppHeaderTitle, showLoadingIndicator, toast } = useAppContext();

  useLayoutEffect(() => {
    setAppHeaderTitle('workflowlist', false);
  }, [ setAppHeaderTitle ]);

  return (
    <Routes>
      <Route index element={<ListOfWorkflows
                                showLoadingIndicator={ showLoadingIndicator }
                                toast={ toast }
                                useWorkflowlistApi={ useWorkflowlistApi }
                                useGuiSse={ useGuiSse }
                                openTask={ openTask }
                                navigateToWorkflow={ navigateToWorkflow } />} />
      <Route path="/:workflowId" element={<RouteBasedWorkflowPage
                                              showLoadingIndicator={ showLoadingIndicator }
                                              toast={ toast }
                                              useWorkflowlistApi={ useWorkflowlistApi }
                                              openTask={ openTask }
                                          />} />
    </Routes>);
}

export default Main;
