import React, { useMemo, MutableRefObject, PropsWithChildren, ReactNode, ReactElement, useContext, FunctionComponentElement } from 'react';
import { getOfficialTasklistApi } from '../client/guiClient.js';
import { WakeupSseCallback } from '@bc/shared';
import { useParams } from 'react-router-dom';
import { OfficialTasklistApi, UserTask } from '@bc/official-gui-client';
import { useAppContext } from '../DevShellAppContext.js';

const UserTaskAppContext = React.createContext<{
  userTaskId: string;
  userTask: UserTask;
} | undefined>(undefined);

let userTask: UserTask | null | undefined = undefined;
let inprogress: Promise<UserTask | null> | undefined = undefined;

const loadUserTask = (
  tasklistApi: OfficialTasklistApi,
  userTaskId: string
): UserTask | null | undefined => {
  
  if (Boolean(userTask)
      && (userTask!.id !== userTaskId)) {
    userTask = undefined;
    inprogress = undefined;
  }
  
  if (inprogress === undefined) {
    if (!Boolean(userTaskId)) {
      return null;
    }

    inprogress = new Promise<UserTask | null>((resolve, reject) => {
        tasklistApi.getUserTask({ userTaskId })
            .then((value: UserTask | null) => {
              userTask = value;
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

  const userTask = loadUserTask(
      tasklistApi,
      userTaskId!);

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
}: { children: (userTask: UserTask) => JSX.Element }) => {
  const context = useContext(UserTaskAppContext);
  return children(context!.userTask);
};

export {
    useOfficialTasklistApi,
    UserTaskAppContextProvider,
    UserTaskAppContextConsumer,
    useUserTaskAppContext
  };
