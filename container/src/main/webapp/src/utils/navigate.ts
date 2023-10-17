import { Toast } from "@vanillabp/bc-shared";
import { TFunction } from "i18next";
import { NavigateFunction } from 'react-router-dom';
import { UserTask, Workflow } from "@vanillabp/bc-official-gui-client";

interface KnownWindowsOpened {
   [key: string]: Window;
}

const windowsOpened: KnownWindowsOpened = {};

const openTask = (
  userTask: UserTask,
  toast: (toast: Toast) => void,
  t: TFunction
) => {
  let previousWindow: Window | undefined = windowsOpened[userTask.id];
  if (previousWindow !== undefined) {
    if (!previousWindow.closed) {
      previousWindow.focus();
      return;
    }
    delete windowsOpened[userTask.id];
    previousWindow = undefined;
  }
  
  if (userTask.uiUriType !== 'WEBPACK_MF_REACT') {
    toast({
        namespace: 'tasklist/list',
        title: t('unsupported-ui-uri-type_title'),
        message: t('unsupported-ui-uri-type_message'),
        status: 'critical'
      });
    return;
  }
  
  const targetWindowName = `usertask-app-${userTask.id}`;
  const targetUrl = `/${ t('url-usertask') }/${userTask.id}`;
  const targetWindow = window.open(targetUrl, targetWindowName);
  if (targetWindow) {
    windowsOpened[userTask.id] = targetWindow;
    targetWindow.focus();
  }
};
    
const navigateToWorkflow = (
  workflowDefinition: UserTask | Workflow,
  toast: (toast: Toast) => void,
  t: TFunction,
  navigate: NavigateFunction,
) => {
  const workflowId = 'workflowId' in workflowDefinition
      ? (workflowDefinition as UserTask).workflowId
      : workflowDefinition.id;
  if (workflowDefinition.uiUriType !== 'WEBPACK_MF_REACT') {
    toast({
        namespace: 'tasklist/list',
        title: t('unsupported-ui-uri-type_title'),
        message: t('unsupported-ui-uri-type_message'),
        status: 'critical'
      });
    return;
  }
  navigate(`/${ t('url-workflowlist') }/${workflowId}`)
};

export { openTask, navigateToWorkflow };
