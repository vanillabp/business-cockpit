import { FC, ReactElement, useEffect, useRef, useState } from 'react';
import { BcUserTask, ShowLoadingIndicatorFunction, ToastFunction, UserTaskAppLayout } from '@vanillabp/bc-shared';
import {
  ModuleDefinition,
  NavigateToWorkflowFunction,
  NoUserTaskGiven,
  OpenTaskFunction,
  TasklistApi,
  TasklistApiHook,
  useFederationModule
} from '../index.js';
import { UserTask } from "@vanillabp/bc-official-gui-client";
import { TranslationFunction } from "../types/translate";

const loadUserTask = (
    tasklistApi: TasklistApi,
    userTaskId: string,
    setUserTask: (userTask: BcUserTask | null) => void,
    navigateToWorkflow: (userTask: UserTask) => void,
    openTask: (userTask: UserTask) => void,
) => {
  tasklistApi.getUserTask(userTaskId,true)
      .then((value: UserTask) => {
        const bcUserTask: BcUserTask = {
          ...value,
          open: () => openTask(value),
          navigateToWorkflow: () => navigateToWorkflow(value),
        };
        setUserTask(bcUserTask);
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
  t: TranslationFunction;
  header?: React.ReactNode;
  footer?: React.ReactNode;
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
    header,
    footer
}: UserTaskPageProps) => {

  const tasklistApi = useTasklistApi();
  const [ userTask, setUserTask ] = useState<BcUserTask | null>();
  const formRef = useRef<() => ReactElement>();

  useEffect(() => {
    if (userTaskId == undefined) {
      return;
    }
    loadUserTask(
        tasklistApi,
        userTaskId,
        setUserTask,
        userTask => navigateToWorkflow(userTask),
        userTask => openTask(userTask));
  }, [ userTaskId ]); //eslint-disable-line react-hooks/exhaustive-deps -- should only be executed on change of userTaskId

  const module = useFederationModule(userTask as ModuleDefinition, 'UserTaskForm');

  useEffect(() => {
      if (!module) {
        showLoadingIndicator(true);
        return;
      }
      const Form = module.UserTaskForm!;
      formRef.current = () => <Form userTask={ userTask! } />;
      showLoadingIndicator(false);
    }, [ module, showLoadingIndicator, formRef ]);

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
        retry={ () => loadUserTask(
            tasklistApi,
            userTaskId,
            setUserTask,
            userTask => navigateToWorkflow(userTask),
            userTask => openTask(userTask)) }/>;
  }

  document.title = userTask!.title.de;

  if (module?.retry) {
    return <NoUserTaskGiven
              retry={ module.retry }
              t={ t }
              showLoadingIndicator={ showLoadingIndicator } />
  }
  if (!module || (module.buildTimestamp === undefined) || !formRef.current) {
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

  const ParameterizedForm: () => ReactElement = formRef.current!;
  return children === undefined
      ? <UserTaskAppLayout header={header} footer={footer}>
          <ParameterizedForm />
        </UserTaskAppLayout>
      : children(userTask, ParameterizedForm);

}

export { UserTaskPage };
