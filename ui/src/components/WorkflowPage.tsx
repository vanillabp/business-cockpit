import { useEffect, useRef, useState } from 'react';
import {
  ModuleDefinition,
  NoWorkflowGiven,
  OpenTaskFunction,
  useFederationModule,
  WorkflowlistApiHook
} from '../index.js';
import {
  BcUserTask,
  BcWorkflow,
  GetUserTasksFunction,
  ShowLoadingIndicatorFunction,
  ToastFunction
} from '@vanillabp/bc-shared';
import { Box } from 'grommet';
import { TranslationFunction } from "../types/translate";
import { OfficialWorkflowlistApi } from "@vanillabp/bc-official-gui-client";

const loadWorkflow = async (
    workflowId: string,
    workflowListApi: OfficialWorkflowlistApi,
    openTask: OpenTaskFunction,
    setWorkflow: (workflow: BcWorkflow) => void,
) => {
  const workflow = await workflowListApi.getWorkflow({ workflowId });
  const getUserTasksFunction: GetUserTasksFunction = async (
      activeOnly,
      limitListAccordingToCurrentUsersPermissions
  ) => {
    return (await workflowListApi
        .getUserTasksOfWorkflow({
          workflowId: workflow.id,
          activeOnly,
          llatcup: limitListAccordingToCurrentUsersPermissions,
        }))
        .map(userTask => ({
          ...userTask,
          open: () => openTask(userTask),
          navigateToWorkflow: () => {}, // don't change view because workflow is already shown
        } as BcUserTask));
  };
  const bcWorkflows: BcWorkflow = {
    ...workflow,
    getUserTasks: getUserTasksFunction
  };
  setWorkflow(bcWorkflows);
};

const WorkflowPage = ({
    workflowId,
    showLoadingIndicator,
    toast,
    useWorkflowlistApi,
    openTask,
    t,
}: {
    workflowId: string | undefined,
    showLoadingIndicator: ShowLoadingIndicatorFunction,
    toast: ToastFunction,
    useWorkflowlistApi: WorkflowlistApiHook,
    openTask: OpenTaskFunction,
    t: TranslationFunction,
}) => {

  //const workflowId: string | undefined = useParams()['*'];
  
  const loadingWorkflow = useRef(false);
  const [ workflow, setWorkflow ] = useState<BcWorkflow | undefined | null>(undefined);
  const workflowListApi = useWorkflowlistApi();
  useEffect(() => {
      if (workflowId === undefined) {
        return;
      }
      if (workflow !== undefined) {
        return;
      }
      if (loadingWorkflow.current) {
        return;
      }
      loadingWorkflow.current = true;
      showLoadingIndicator(true);
    loadWorkflow(
        workflowId,
        workflowListApi,
        userTask => openTask(userTask),
        setWorkflow);
    }, [ workflowListApi, workflowId, workflow, loadingWorkflow, showLoadingIndicator, setWorkflow ]);
  
  const module = useFederationModule(workflow as ModuleDefinition, 'WorkflowPage');
  useEffect(() => {
      if (!module) {
        return;
      }
      if ((module.buildTimestamp === undefined) && (module.retry === undefined)) {
        return;
      }
      showLoadingIndicator(false);
    }, [ module, module?.buildTimestamp, module?.retry, showLoadingIndicator ]);

  if (workflow === undefined) {
    return <NoWorkflowGiven
        loading
        t={ t }
        showLoadingIndicator={ showLoadingIndicator } />;
  }

  if (workflow === null) {
    return <NoWorkflowGiven
        showLoadingIndicator={ showLoadingIndicator }
        t={ t }
        retry={ () => loadWorkflow(
            workflowId!,
            workflowListApi,
            userTask => openTask(userTask),
            setWorkflow) } />;
  }

  if (module?.retry) {
    return <NoWorkflowGiven
              retry={ module.retry }
              t={ t }
              showLoadingIndicator={ showLoadingIndicator } />
  }
  if (!module || (module.buildTimestamp === undefined)) {
    return <NoWorkflowGiven
              loading
              t={ t }
              showLoadingIndicator={ showLoadingIndicator } />
  }
    
  const Page = module.WorkflowPage!;
  
  return (
      <Box fill>
        <Page workflow={ workflow! } />
      </Box>);
};

export { WorkflowPage };
