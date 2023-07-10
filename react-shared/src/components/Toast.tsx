import { Notification, Timeout } from "grommet";
import React, { useCallback, useEffect, Dispatch } from "react";
import { useTranslation } from "react-i18next";
import { StatusType } from 'grommet';

export type Toast = {
  namespace: string;
  title: string | undefined | null;
  message: string;
  status?: StatusType;
  timeout?: number;
};

export type ToastAction = {
  type: 'toast',
  toast: Toast | undefined;
};

interface MessageToastProps {
  dispatch: Dispatch<ToastAction>;
  msg: Toast;
};

const MessageToast = ({ dispatch, msg }: MessageToastProps) => {
  const { t, i18n } = useTranslation(msg.namespace);

  const close = useCallback(() => dispatch({ type: 'toast', toast: undefined }), [dispatch]);
  
  useEffect(() => {
    let timeout = msg.timeout !== undefined ? msg.timeout : msg.message.length * 50;
    if (timeout < 3000) timeout = 3000;
    const timer: Timeout = window.setTimeout(close, timeout);
    return () => window.clearTimeout(timer);
  }, [msg, close]);
  
  return (
    <Notification
        toast={{ autoClose: false }}
        title={msg.title ? t(msg.title) as string : undefined}
        message={t(msg.message)}
        status={msg.status ? msg.status : 'unknown'}
        onClose={close}
    />
  );
}
      
export { MessageToast };
