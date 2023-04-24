import React, { useMemo, MutableRefObject, PropsWithChildren } from 'react';
import { getTasklistGuiApi } from '../client/guiClient';
import { TasklistApi, UserTask } from '../client/gui';
import { WakeupSseCallback } from '@bc/shared';
import { useParams } from 'react-router-dom';
import { NoUserTaskGiven } from './NoUserTaskGiven';
import { useAppContext } from '../AppContext';

const UserTaskAppContext = React.createContext<{
  userTaskId: string;
  userTask: UserTask | null;
} | undefined>(undefined);

let userTask: UserTask | null | undefined = undefined;
let inprogress: Promise<UserTask | null> | undefined = undefined;

const loadUserTask = (
  tasklistApi: TasklistApi,
  userTaskId: string
): UserTask | null | undefined => {
  
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

const UserTaskAppContextProvider = ({ children }: PropsWithChildren<{}>) => {
  const tasklistApi = useTasklistApi();
  const userTaskId: string | undefined = useParams()['*'];
  
  const userTask = loadUserTask(
      tasklistApi,
      userTaskId!);

  const value = {
      userTaskId: userTaskId!,
      userTask: userTask!
    };

  return userTask === null
      ? <NoUserTaskGiven />
      : <UserTaskAppContext.Provider value={value}>
          {children}
        </UserTaskAppContext.Provider>;
};

const useTasklistApi = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): TasklistApi => {

  const { dispatch } = useAppContext();
  const api = useMemo(() => getTasklistGuiApi(dispatch, wakeupSseCallback?.current), [ dispatch, wakeupSseCallback ]);
  return api;
  
};

const useUserTaskAppContext = () => {
  const context = React.useContext(UserTaskAppContext);
  if (context === undefined) {
    throw new Error('useUserTaskAppContext must be used within a <UserTaskAppContext>...</UserTaskAppContext>');
  }
  return context;
}

export {
    useTasklistApi,
    UserTaskAppContextProvider,
    useUserTaskAppContext
  };
