import React, { useEffect, useState } from 'react';
import { useAppContext } from '../../AppContext';
import { useParams } from 'react-router-dom';
import { ModuleDefinition, useFederationModule } from '../../utils/module-federation';
import { NoWorkflowGiven } from './NoWorkflowGiven';
import { useWorkflowlistApi } from './WorkflowlistAppContext';
import { BcWorkflow, GetUserTasksFunction } from '@vanillabp/bc-shared';
import { openTask } from '../../utils/open-task';
import { useTranslation } from 'react-i18next';

const WorkflowPage = () => {
  
  const { t: tApp } = useTranslation('app');
  const { toast, showLoadingIndicator } = useAppContext();
  const workflowId: string | undefined = useParams()['*'];
  
  const [ loadingWorkflow, setLoadingWorkflow ] = useState(false);
  const [ workflow, setWorkflow ] = useState<BcWorkflow | undefined | null>(undefined);
  const workflowListApi = useWorkflowlistApi();
  useEffect(() => {
      if (workflowId === undefined) {
        return;
      }
      if (workflow !== undefined) {
        return;
      }
      if (loadingWorkflow) {
        return;
      }
      const loadWorkflow = async () => {
          const workflow = await workflowListApi.getWorkflow({ workflowId });
          const getWorkflowFunction: GetUserTasksFunction = async (
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
                  }));
            };
          const bcWorkflows: BcWorkflow = {
            ...workflow,
            getUserTasks: getWorkflowFunction
          };
          setWorkflow(bcWorkflows);
        };
      setLoadingWorkflow(true);
      showLoadingIndicator(true);
      loadWorkflow();
    }, [ toast, tApp, workflowListApi, workflowId, workflow, loadingWorkflow, showLoadingIndicator, setWorkflow, setLoadingWorkflow ]);
  
  const module = useFederationModule(workflow as ModuleDefinition, 'WorkflowPage');
  useEffect(() => {
      if (!module) {
        return;
      }
      if ((module.buildTimestamp === undefined) && (module.retry === undefined)) {
        return;
      }
      showLoadingIndicator(false);
    }, [ module, showLoadingIndicator ]);

  if (module?.retry) {
    return <NoWorkflowGiven retry={ module.retry } />
  }
  if (!module || (module.buildTimestamp === undefined)) {
    return <NoWorkflowGiven loading />
  }
    
  const Page = module.WorkflowPage!;
  
  return <Page workflow={ workflow! } />
};

export { WorkflowPage };
