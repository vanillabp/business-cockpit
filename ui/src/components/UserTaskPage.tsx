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
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { OfficialTasklistApi, UserTask } from "@vanillabp/bc-official-gui-client";

const loadUserTask = (
    tasklistApi: OfficialTasklistApi,
    userTaskId: string,
    setUserTask: (userTask: UserTask | null) => void,
) => {
  tasklistApi.getUserTask({ userTaskId })
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
  children?: (userTask: BcUserTask, Form: () => ReactElement) => JSX.Element;
};

const UserTaskPage: FC<UserTaskPageProps> = ({
    userTaskId,
    showLoadingIndicator,
    toast,
    openTask,
    navigateToWorkflow,
    useTasklistApi,
    children,
}: UserTaskPageProps) => {

  const { t: tApp } = useTranslation('app');
  const navigate = useNavigate();
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
        showLoadingIndicator={ showLoadingIndicator } />;
  }

  if (userTask === null) {
    return <NoUserTaskGiven
        showLoadingIndicator={ showLoadingIndicator }
        retry={ () => loadUserTask(tasklistApi, userTaskId, setUserTask) }/>;
  }

  document.title = userTask!.title.de;

  if (module?.retry) {
    return <NoUserTaskGiven
              retry={ module.retry }
              showLoadingIndicator={ showLoadingIndicator } />
  }
  if (!module || (module.buildTimestamp === undefined)) {
    return <NoUserTaskGiven
              loading
              showLoadingIndicator={ showLoadingIndicator} />
  }
    
  const Form = module.UserTaskForm!;
  const bcUserTask: BcUserTask = {
      ...userTask,
      open: () => openTask(userTask, toast, tApp),
      navigateToWorkflow: () => navigateToWorkflow(userTask, toast, tApp, navigate),
    };

  const ParameterizedForm = () => <Form userTask={ bcUserTask } />;
  return children === undefined
      ? <UserTaskAppLayout>
          <ParameterizedForm />
        </UserTaskAppLayout>
      : children(bcUserTask, ParameterizedForm);

}

export { UserTaskPage };
