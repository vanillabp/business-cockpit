import { Suspense, useEffect, useRef, useState } from 'react';
import {
  ModuleDefinition,
  NoWorkflowGiven,
  OpenTaskFunction,
  TasklistApi,
  TasklistApiHook,
  useFederationModule,
  WorkflowlistApi,
  WorkflowlistApiHook
} from '../index.js';
import {
  BcUserTask,
  BcWorkflow,
  GetUserTasksFunction,
  ShowLoadingIndicatorFunction,
  ToastFunction,
  TranslationFunction
} from '@vanillabp/bc-shared';
import { Box } from 'grommet';

const loadWorkflow = async (
    workflowId: string,
    workflowListApi: WorkflowlistApi,
    tasklistApi: TasklistApi,
    openTask: OpenTaskFunction,
    setWorkflow: (workflow: BcWorkflow) => void,
) => {
  const workflow = await workflowListApi.getWorkflow(workflowId);
  const getUserTasksFunction: GetUserTasksFunction = async (
      activeOnly,
      limitListAccordingToCurrentUsersPermissions
  ) => {
    return (await workflowListApi
        .getUserTasksOfWorkflow(
          workflow.id,
          activeOnly,
          limitListAccordingToCurrentUsersPermissions))
        .map(userTask => ({
          ...userTask,
          open: () => openTask(userTask),
          navigateToWorkflow: () => {}, // don't change view because workflow is already shown
          assign: userId => tasklistApi.assignTask(userTask.id, userId, false),
          unassign: userId => tasklistApi.assignTask(userTask.id, userId, false),
          claim: () => tasklistApi.claimTask(userTask.id, false),
          unclaim: () => tasklistApi.claimTask(userTask.id, true),
        } as BcUserTask));
  };
  const bcWorkflows: BcWorkflow = {
    ...workflow,
    navigateToWorkflow: () => {},
    getUserTasks: getUserTasksFunction
  };
  setWorkflow(bcWorkflows);
};

const WorkflowPage = ({
    workflowId,
    showLoadingIndicator,
    toast,
    useWorkflowlistApi,
    useTasklistApi,
    openTask,
    t,
}: {
    workflowId: string | undefined,
    showLoadingIndicator: ShowLoadingIndicatorFunction,
    toast: ToastFunction,
    useWorkflowlistApi: WorkflowlistApiHook,
    useTasklistApi: TasklistApiHook,
    openTask: OpenTaskFunction,
    t: TranslationFunction,
}) => {

  //const workflowId: string | undefined = useParams()['*'];
  
  const loadingWorkflow = useRef(false);
  const [ workflow, setWorkflow ] = useState<BcWorkflow | undefined | null>(undefined);
  const workflowListApi = useWorkflowlistApi();
  const taskListApi = useTasklistApi();
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
        taskListApi,
        userTask => openTask(userTask),
        setWorkflow);
    }, [ workflowListApi, taskListApi, workflow, loadingWorkflow, showLoadingIndicator, setWorkflow ]);
  
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
            taskListApi,
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
        <Suspense /* catch any uncaught suspensions */>
          <Page workflow={ workflow! } />
        </Suspense>
      </Box>);
};

export { WorkflowPage };
