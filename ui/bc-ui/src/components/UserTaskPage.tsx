import { FC, Suspense, useEffect, useState } from 'react';
import {
  BcUserTask,
  ShowLoadingIndicatorFunction,
  ToastFunction,
  TranslationFunction,
  UserTaskAppLayout,
  UserTaskForm
} from '@vanillabp/bc-shared';
import {
    AssignTaskFunction, ClaimTaskFunction,
    ModuleDefinition,
    NavigateToWorkflowFunction,
    NoUserTaskGiven,
    OpenTaskFunction,
    TasklistApi,
    TasklistApiHook,
    useFederationModule
} from '../index.js';
import { UserTask } from "@vanillabp/bc-official-gui-client";

const loadUserTask = (
    tasklistApi: TasklistApi,
    userTaskId: string,
    setUserTask: (userTask: BcUserTask | null) => void,
    navigateToWorkflow: (userTask: UserTask) => void,
    openTask: (userTask: UserTask) => void,
    assign: (userTask: UserTask, userId: string) => void,
    unassign: (userTask: UserTask, userId: string) => void,
    claim: (userTask: UserTask) => void,
    unclaim: (userTask: UserTask) => void
) => {
  tasklistApi.getUserTask(userTaskId,true)
      .then((value: UserTask) => {
        const bcUserTask: BcUserTask = {
          ...value,
          open: () => openTask(value),
          navigateToWorkflow: () => navigateToWorkflow(value),
          assign: userId => assign(value, userId),
          unassign: userId => unassign(value, userId),
          claim: () => claim(value),
          unclaim: () => unclaim(value)
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
  assignTask: AssignTaskFunction;
  claimTask: ClaimTaskFunction;
  useTasklistApi: TasklistApiHook;
  t: TranslationFunction;
  header?: React.ReactNode;
  footer?: React.ReactNode;
  children?: (userTask: BcUserTask, Form: UserTaskForm) => JSX.Element;
};

const UserTaskPage: FC<UserTaskPageProps> = ({
    userTaskId,
    showLoadingIndicator,
    toast,
    openTask,
    navigateToWorkflow,
    useTasklistApi,
    assignTask,
    claimTask,
    t,
    children,
    header,
    footer
}: UserTaskPageProps) => {

  const tasklistApi = useTasklistApi();
  const [ userTask, setUserTask ] = useState<BcUserTask | null>();

  useEffect(() => {
    if (userTaskId == undefined) {
      return;
    }
    loadUserTask(
        tasklistApi,
        userTaskId,
        setUserTask,
        userTask => navigateToWorkflow(userTask),
        userTask => openTask(userTask),
        (userTask, userId) => assignTask(userTask, userId, false),
        (userTask, userId) => assignTask(userTask, userId, true),
        (userTask) => claimTask(userTask, false),
        (userTask) => claimTask(userTask, true),
    );
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
        retry={ () => loadUserTask(
            tasklistApi,
            userTaskId,
            setUserTask,
            userTask => navigateToWorkflow(userTask),
            userTask => openTask(userTask),
            (userTask, userId) => assignTask(userTask, userId, false),
            (userTask, userId) => assignTask(userTask, userId, true),
            (userTask) => claimTask(userTask, false),
            (userTask) => claimTask(userTask, true),) }/>;
  }

  document.title = userTask!.title.de;

  if (module?.retry) {
    return <NoUserTaskGiven
              retry={ module.retry }
              t={ t }
              showLoadingIndicator={ showLoadingIndicator } />
  }

  if (!module || (module.buildTimestamp === undefined) || !module?.UserTaskForm) {
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
      assign: (userId) => assignTask(userTask, userId, false),
      unassign: (userId) => assignTask(userTask, userId, true),
      claim: () => claimTask(userTask, false),
      unclaim: () => claimTask(userTask, true)
    };

  return (
      <Suspense /* catch any uncaught suspensions */>
        {
          children === undefined
              ? <UserTaskAppLayout header={header} footer={footer}>
                  <Form userTask={ bcUserTask } />
                </UserTaskAppLayout>
              : children(userTask, Form)
        }
      </Suspense>);

}

export { UserTaskPage };
