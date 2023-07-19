import React, { useEffect, useState } from 'react';
import { useAppContext } from '../../AppContext';
import { useParams } from 'react-router-dom';
import { ModuleDefinition, useFederationModule } from '../../utils/module-federation';
import { Workflow } from '../../client/gui';
import { NoWorkflowGiven } from './NoWorkflowGiven';
import { useWorkflowlistApi } from './WorkflowlistAppContext';

const WorkflowPage = () => {
  
  const { showLoadingIndicator } = useAppContext();
  const workflowId: string | undefined = useParams()['*'];
  
  const [ loadingWorkflow, setLoadingWorkflow ] = useState(false);
  const [ workflow, setWorkflow ] = useState<Workflow | undefined | null>(undefined);
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
          setWorkflow(workflow);
        };
      setLoadingWorkflow(true);
      showLoadingIndicator(true);
      loadWorkflow();
    }, [ workflowListApi, workflowId, workflow, loadingWorkflow, showLoadingIndicator, setWorkflow, setLoadingWorkflow ]);
  
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
