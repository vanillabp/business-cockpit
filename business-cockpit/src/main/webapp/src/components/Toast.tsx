import { Notification, Timeout } from "grommet";
import React, { useCallback, useEffect, useLayoutEffect } from "react";
import { useTranslation } from "react-i18next";
import { Dispatch, Toast } from '../AppContext';

interface MessageToastProps {
  dispatch: Dispatch
  msg: Toast;
};

const MessageToast = ({ dispatch, msg }: MessageToastProps) => {
  const { t, i18n } = useTranslation(msg.namespace);

  // see https://github.com/i18next/react-i18next/issues/1064
  useLayoutEffect(() => {
    // manually emitting a languageChanged-Event would work around this problem
    i18n.emit("languageChanged");
  }, [ msg, i18n ]);
  
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
