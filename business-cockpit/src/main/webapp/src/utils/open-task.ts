import { Toast } from "@vanillabp/bc-shared";
import { UserTask } from "client/gui";
import { TFunction } from "i18next";

const openTask = (
  userTask: UserTask,
  toast: (toast: Toast) => void,
  t: TFunction
) => {
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
        targetWindow.focus();
      }
    };

export { openTask };
