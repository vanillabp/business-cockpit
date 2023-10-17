import { FC, ReactElement, useEffect, useState } from 'react';
import { BcUserTask, ShowLoadingIndicatorFunction, ToastFunction, UserTaskAppLayout } from '@vanillabp/bc-shared';
import {
  ModuleDefinition,
  NavigateToWorkflowFunction,
  NoUserTaskGiven,
  OpenTaskFunction,
  TasklistApiHook,
  useFederationModule
} from '../index.js';
import { OfficialTasklistApi, UserTask } from "@vanillabp/bc-official-gui-client";
import { TranslationFunction } from "../types/translate";

const loadUserTask = (
    tasklistApi: OfficialTasklistApi,
    userTaskId: string,
    setUserTask: (userTask: UserTask | null) => void,
) => {
  tasklistApi.getUserTask({ userTaskId, markAsRead: true })
      .then((value: UserTask | null) => {
        setUserTask(value);
      }).catch((error: any) => {
    setUserTask(null);
    console.warn(error);
  });
};

interface UserTaskPageProps {
  userTaskId: string;
  showLoadingIndicator: ShowLoadingIndicatorFunction;
  toast: ToastFunction;
  openTask: OpenTaskFunction;
  navigateToWorkflow: NavigateToWorkflowFunction;
  useTasklistApi: TasklistApiHook;
  t: TranslationFunction,
  children?: (userTask: BcUserTask, Form: () => ReactElement) => JSX.Element;
};

const UserTaskPage: FC<UserTaskPageProps> = ({
    userTaskId,
    showLoadingIndicator,
    toast,
    openTask,
    navigateToWorkflow,
    useTasklistApi,
    t,
    children,
}: UserTaskPageProps) => {

  const tasklistApi = useTasklistApi();
  const [ userTask, setUserTask ] = useState<UserTask | null>();

  useEffect(() => {
    if (userTaskId == undefined) {
      return;
    }
    loadUserTask(tasklistApi, userTaskId, setUserTask);
  }, [ userTaskId ]); //eslint-disable-line react-hooks/exhaustive-deps -- should only be executed on change of userTaskId

  const module = useFederationModule(userTask as ModuleDefinition, 'UserTaskForm');

  useEffect(() => {
      if (!module) {
        showLoadingIndicator(true);
        return;
      }
      showLoadingIndicator(false);
    }, [ module, showLoadingIndicator ]);

  if (userTask === undefined) {
    return <NoUserTaskGiven
        loading={ true }
        t={ t }
        showLoadingIndicator={ showLoadingIndicator } />;
  }

  if (userTask === null) {
    return <NoUserTaskGiven
        showLoadingIndicator={ showLoadingIndicator }
        t={ t }
        retry={ () => loadUserTask(tasklistApi, userTaskId, setUserTask) }/>;
  }

  document.title = userTask!.title.de;

  if (module?.retry) {
    return <NoUserTaskGiven
              retry={ module.retry }
              t={ t }
              showLoadingIndicator={ showLoadingIndicator } />
  }
  if (!module || (module.buildTimestamp === undefined)) {
    return <NoUserTaskGiven
              loading
              t={ t }
              showLoadingIndicator={ showLoadingIndicator} />
  }
    
  const Form = module.UserTaskForm!;
  const bcUserTask: BcUserTask = {
      ...userTask,
      open: () => openTask(userTask),
      navigateToWorkflow: () => navigateToWorkflow(userTask),
    };

  const ParameterizedForm = () => <Form userTask={ bcUserTask } />;
  return children === undefined
      ? <UserTaskAppLayout>
          <ParameterizedForm />
        </UserTaskAppLayout>
      : children(bcUserTask, ParameterizedForm);

}

export { UserTaskPage };
