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
import { useTranslation } from 'react-i18next';
import { Box } from 'grommet';

const WorkflowPage = ({
    workflowId,
    showLoadingIndicator,
    toast,
    useWorkflowlistApi,
    openTask,
}: {
    workflowId: string | undefined,
    showLoadingIndicator: ShowLoadingIndicatorFunction,
    toast: ToastFunction,
    useWorkflowlistApi: WorkflowlistApiHook,
    openTask: OpenTaskFunction,
}) => {
  
  const { t: tApp } = useTranslation('app');
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
      const loadWorkflow = async () => {
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
                    open: () => openTask(userTask, toast, tApp),
                    navigateToWorkflow: () => {}, // don't change view because workflow is already shown
                  } as BcUserTask));
            };
          const bcWorkflows: BcWorkflow = {
            ...workflow,
            getUserTasks: getUserTasksFunction
          };
          setWorkflow(bcWorkflows);
        };
      loadingWorkflow.current = true;
      showLoadingIndicator(true);
      loadWorkflow();
    }, [ toast, tApp, workflowListApi, workflowId, workflow, loadingWorkflow, showLoadingIndicator, setWorkflow ]);
  
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

  if (module?.retry) {
    return <NoWorkflowGiven
              retry={ module.retry }
              showLoadingIndicator={ showLoadingIndicator } />
  }
  if (!module || (module.buildTimestamp === undefined)) {
    return <NoWorkflowGiven
              loading
              showLoadingIndicator={ showLoadingIndicator } />
  }
    
  const Page = module.WorkflowPage!;
  
  return (
      <Box fill>
        <Page workflow={ workflow! } />
      </Box>);
};

export { WorkflowPage };
