import React, { useMemo, MutableRefObject, PropsWithChildren, useContext } from 'react';
import { getOfficialWorkflowlistApi } from '../client/guiClient.js';
import { WakeupSseCallback } from '@vanillabp/bc-shared';
import { useParams } from 'react-router-dom';
import { OfficialTasklistApi, Workflow } from '@vanillabp/bc-official-gui-client';
import { useAppContext } from '../DevShellAppContext.js';
import { OfficialWorkflowlistApi } from '@vanillabp/bc-official-gui-client';

const WorkflowAppContext = React.createContext<{
  workflowId: string;
  workflow: Workflow;
} | undefined>(undefined);

let workflow: Workflow | null | undefined = undefined;
let inprogress: Promise<Workflow | null> | undefined = undefined;

const loadWorkflow = (
  workflowlistApi: OfficialWorkflowlistApi,
  workflowId: string
): Workflow | null | undefined => {
  
  if (Boolean(workflow)
      && (workflow!.id !== workflowId)) {
    workflow = undefined;
    inprogress = undefined;
  }
  
  if (inprogress === undefined) {
    if (!Boolean(workflowId)) {
      return null;
    }

    inprogress = new Promise<Workflow | null>((resolve, reject) => {
        workflowlistApi.getWorkflow({ workflowId })
            .then((value: Workflow | null) => {
              workflow = value;
              resolve(workflow);
            }).catch((error: any) => {
              workflow = null;
              console.warn(error);
              reject(error);
            });
      });
  }

  // return a promise to trigger <Suspend> element
  if (workflow === undefined) {
    throw inprogress;
  }
  
  return workflow;
  
};

const WorkflowAppContextProvider = ({
  officialGuiApiUrl,
  children
}: PropsWithChildren<{officialGuiApiUrl: string}>) => {
  const workflowlistApi = useOfficialWorkflowlistApi(officialGuiApiUrl);
  const workflowId: string | undefined = useParams()['workflowId'];

  const workflow = loadWorkflow(
      workflowlistApi,
      workflowId!);

  const value = {
      workflowId: workflowId!,
      workflow: workflow!
    };

  return workflow === null
      ? <div>Unknown workflow</div>
      : <WorkflowAppContext.Provider value={value}>
          {children}
        </WorkflowAppContext.Provider>;
};

const useOfficialWorkflowlistApi = (
    basePath: string,
    wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): OfficialWorkflowlistApi => {

  const { dispatch } = useAppContext();
  const api = useMemo(() => getOfficialWorkflowlistApi(
      basePath, dispatch, wakeupSseCallback?.current),
      [ dispatch, wakeupSseCallback ]);
  return api;
  
};

const useWorkflowAppContext = () => {
  const context = React.useContext(WorkflowAppContext);
  if (context === undefined) {
    throw new Error('useWorkflowAppContext must be used within a <WorkflowAppContext>...</WorkflowAppContext>');
  }
  return context;
}

const WorkflowAppContextConsumer = ({
  children
}: { children: (workflow: Workflow) => JSX.Element }) => {
  const context = useContext(WorkflowAppContext);
  return children(context!.workflow);
};

export {
    useOfficialWorkflowlistApi,
    WorkflowAppContextProvider,
    WorkflowAppContextConsumer,
    useWorkflowAppContext
  };
