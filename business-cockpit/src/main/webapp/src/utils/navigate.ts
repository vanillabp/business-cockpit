import { Toast } from "@vanillabp/bc-shared";
import { TFunction } from "i18next";
import { NavigateFunction } from 'react-router-dom';
import { UserTask, Workflow } from "@vanillabp/bc-official-gui-client";

interface KnownWindowsOpened {
   [key: string]: Window;
}

const windowsOpened: KnownWindowsOpened = {};

/**
 * Brings the window an item was opened in before to the front.
 *
 * @return Whether the item is already shown in a window of its own
 */
const shownInAWindowAlready = (
  key: string
): boolean => {
  const previousWindow: Window | undefined = windowsOpened[key];
  if (previousWindow === undefined) {
    return false;
  }
  if (!previousWindow.closed) {
    previousWindow.focus();
    return true;
  }
  delete windowsOpened[key];
  return false;
};

const openInAWindowOfItsOwn = (
  key: string,
  windowName: string,
  url: string
) => {
  const targetWindow = window.open(url, windowName);
  if (targetWindow) {
    windowsOpened[key] = targetWindow;
    targetWindow.focus();
  }
};

const reportMissingUri = (
  toast: (toast: Toast) => void,
  t: TFunction
) => toast({
    namespace: 'tasklist/list',
    title: t('no-ui-uri_title'),
    message: t('no-ui-uri_message'),
    status: 'critical'
  });

const reportUnsupportedType = (
  toast: (toast: Toast) => void,
  t: TFunction
) => toast({
    namespace: 'tasklist/list',
    title: t('unsupported-ui-uri-type_title'),
    message: t('unsupported-ui-uri-type_message'),
    status: 'critical'
  });

const openTask = (
  userTask: UserTask,
  toast: (toast: Toast) => void,
  t: TFunction
) => {
  const key = `usertask-${userTask.id}`;
  if (shownInAWindowAlready(key)) {
    return;
  }

  const windowName = `usertask-app-${userTask.id}`;

  // the task is worked on in another application, so the cockpit opens that application's own
  // page. It is a window of its own like a federated form, and it is managed like one, which is
  // why a second click brings the window that is already open to the front.
  if (userTask.uiUriType === 'EXTERNAL') {
    if (!userTask.uiUri) {
      reportMissingUri(toast, t);
      return;
    }
    openInAWindowOfItsOwn(key, windowName, userTask.uiUri);
    return;
  }

  if (userTask.uiUriType !== 'WEBPACK_MF_REACT') {
    reportUnsupportedType(toast, t);
    return;
  }

  openInAWindowOfItsOwn(key, windowName, `/${ t('url-usertask') }/${userTask.id}`);
};
    
const navigateToWorkflow = (
  workflowDefinition: UserTask | Workflow,
  toast: (toast: Toast) => void,
  t: TFunction,
  navigate: NavigateFunction,
) => {
  const givenAUserTask = 'workflowId' in workflowDefinition;
  const workflowId = givenAUserTask
      ? (workflowDefinition as UserTask).workflowId
      : workflowDefinition.id;

  // A user task says where the task is shown, and that is not where the case is shown. So its type
  // decides nothing here and the cockpit goes to its own workflow page, which loads the case and
  // knows where that one belongs.
  if (!givenAUserTask) {
    // the case is followed in another application, so the cockpit opens that application's own
    // status page, in a window of its own and managed like a federated form
    if (workflowDefinition.uiUriType === 'EXTERNAL') {
      const key = `workflow-${workflowId}`;
      if (shownInAWindowAlready(key)) {
        return;
      }
      if (!workflowDefinition.uiUri) {
        reportMissingUri(toast, t);
        return;
      }
      openInAWindowOfItsOwn(key, `workflow-app-${workflowId}`, workflowDefinition.uiUri);
      return;
    }
    if (workflowDefinition.uiUriType !== 'WEBPACK_MF_REACT') {
      reportUnsupportedType(toast, t);
      return;
    }
  }

  navigate(`/${ t('url-workflowlist') }/${workflowId}`)
};

export { openTask, navigateToWorkflow };
