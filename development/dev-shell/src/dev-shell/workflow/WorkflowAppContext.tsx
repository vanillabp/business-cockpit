import React, { useMemo, MutableRefObject, PropsWithChildren, useContext, useRef } from 'react';
import { getOfficialWorkflowlistApi } from '../client/guiClient.js';
import { BcWorkflow, GetUserTasksFunction, WakeupSseCallback } from '@vanillabp/bc-shared';
import { NavigateFunction, useNavigate, useParams } from 'react-router-dom';
import { useAppContext } from '../DevShellAppContext.js';
import { OfficialWorkflowlistApi, Workflow } from '@vanillabp/bc-official-gui-client';
import { useTranslation } from 'react-i18next';
import { appNs } from '../app/DevShellApp.js';

const WorkflowAppContext = React.createContext<{
  workflowId: string;
  workflow: BcWorkflow;
} | undefined>(undefined);

let workflow: BcWorkflow | null | undefined = undefined;
let inprogress: Promise<BcWorkflow | null> | undefined = undefined;

interface Navigate {
  fn: NavigateFunction;
}

const loadWorkflow = (
  workflowlistApi: OfficialWorkflowlistApi,
  workflowId: string,
  tApp: (t: string) => string,
): BcWorkflow | null | undefined => {
  
  if (Boolean(workflow)
      && (workflow!.id !== workflowId)) {
    workflow = undefined;
    inprogress = undefined;
  }
  
  if (inprogress === undefined) {
    if (!Boolean(workflowId)) {
      return null;
    }

    const openTask = (userTaskId: string) => window.location.href = `/${ tApp('url-usertask') }/${userTaskId}`;
    
    inprogress = new Promise<BcWorkflow | null>((resolve, reject) => {
        workflowlistApi.getWorkflow({ workflowId })
            .then((value: Workflow) => {
              const getWorkflowFunction: GetUserTasksFunction = async (
                  activeOnly,
                  limitListAccordingToCurrentUsersPermissions
                ) => {
                  return (await workflowlistApi
                      .getUserTasksOfWorkflow({
                          workflowId: workflowId,
                          activeOnly,
                          llatcup: limitListAccordingToCurrentUsersPermissions,
                      }))
                      .map(userTask => ({
                        ...userTask,
                        open: () => openTask(userTask.id),
                      }));
                };
              workflow = {
                  ...value,
                  getUserTasks: getWorkflowFunction,
                };
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
  const { t: tApp } = useTranslation(appNs);

  const workflow = loadWorkflow(
      workflowlistApi,
      workflowId!,
      tApp);

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
}: { children: (workflow: BcWorkflow) => JSX.Element }) => {
  const context = useContext(WorkflowAppContext);
  return children(context!.workflow);
};

export {
    useOfficialWorkflowlistApi,
    WorkflowAppContextProvider,
    WorkflowAppContextConsumer,
    useWorkflowAppContext
  };
