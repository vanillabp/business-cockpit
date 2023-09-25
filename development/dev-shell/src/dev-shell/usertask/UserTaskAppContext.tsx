import React, { useMemo, MutableRefObject, PropsWithChildren, useContext } from 'react';
import { getOfficialTasklistApi } from '../client/guiClient.js';
import { BcUserTask, WakeupSseCallback } from '@vanillabp/bc-shared';
import { useParams } from 'react-router-dom';
import { OfficialTasklistApi, UserTask } from '@vanillabp/bc-official-gui-client';
import { useAppContext } from '../DevShellAppContext.js';
import { useTranslation } from 'react-i18next';
import { appNs } from '../app/DevShellApp.js';

const UserTaskAppContext = React.createContext<{
  userTaskId: string;
  userTask: BcUserTask;
} | undefined>(undefined);

let userTask: BcUserTask | null | undefined = undefined;
let inprogress: Promise<BcUserTask | null> | undefined = undefined;

const loadUserTask = (
  tasklistApi: OfficialTasklistApi,
  userTaskId: string,
  tApp: (t: string) => string,
): BcUserTask | null | undefined => {
  
  if (Boolean(userTask)
      && (userTask!.id !== userTaskId)) {
    userTask = undefined;
    inprogress = undefined;
  }
  
  if (inprogress === undefined) {
    if (!Boolean(userTaskId)) {
      return null;
    }

    const openTask = (userTaskId: string) => window.location.href = `/${ tApp('url-usertask') }/${userTaskId}`;
    const openWorkflow = (workflowId: string) => window.location.href = `/${ tApp('url-workflow') }/${workflowId}`;

    inprogress = new Promise<BcUserTask | null>((resolve, reject) => {
        tasklistApi.getUserTask({ userTaskId })
            .then((value: UserTask | null) => {
              userTask = {
                  ...value,
                  open: () => openTask(value?.id!),
                  navigateToWorkflow: () => openWorkflow(value?.workflowId!),
                } as BcUserTask;
              resolve(userTask);
            }).catch((error: any) => {
              userTask = null;
              console.warn(error);
              reject(error);
            });
      });
  }

  // return a promise to trigger <Suspend> element
  if (userTask === undefined) {
    throw inprogress;
  }
  
  return userTask;
  
};

const UserTaskAppContextProvider = ({
  officialGuiApiUrl,
  children
}: PropsWithChildren<{officialGuiApiUrl: string}>) => {
  const tasklistApi = useOfficialTasklistApi(officialGuiApiUrl);
  const userTaskId: string | undefined = useParams()['userTaskId'];
  const { t: tApp } = useTranslation(appNs);

  const userTask = loadUserTask(
      tasklistApi,
      userTaskId!,
      tApp);

  const value = {
      userTaskId: userTaskId!,
      userTask: userTask!
    };

  return userTask === null
      ? <div>Unknown task</div>
      : <UserTaskAppContext.Provider value={value}>
          {children}
        </UserTaskAppContext.Provider>;
};

const useOfficialTasklistApi = (
    basePath: string,
    wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): OfficialTasklistApi => {

  const { dispatch } = useAppContext();
  const api = useMemo(() => getOfficialTasklistApi(
      basePath, dispatch, wakeupSseCallback?.current),
      [ dispatch, wakeupSseCallback ]);
  return api;
  
};

const useUserTaskAppContext = () => {
  const context = React.useContext(UserTaskAppContext);
  if (context === undefined) {
    throw new Error('useUserTaskAppContext must be used within a <UserTaskAppContext>...</UserTaskAppContext>');
  }
  return context;
}

const UserTaskAppContextConsumer = ({
  children
}: { children: (userTask: BcUserTask) => JSX.Element }) => {
  const context = useContext(UserTaskAppContext);
  return children(context!.userTask);
};

export {
    useOfficialTasklistApi,
    UserTaskAppContextProvider,
    UserTaskAppContextConsumer,
    useUserTaskAppContext
  };
