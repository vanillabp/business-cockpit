import { useEffect } from "react";

let now = new Date();

type EachSecondHook = (lastNow: Date) => void;

const hooks = new Array<EachSecondHook>();

const useKeepNowUpToDate = () => {
      
  useEffect(() => {
      const timer = window.setInterval(() => {
          const lastNow = now;
          now = new Date();
          hooks.forEach(hook => hook(lastNow));
        }, 1000);
      return () => window.clearInterval(timer);
    }, [ ]);  // eslint-disable-line react-hooks/exhaustive-deps
  
};

const registerEachSecondHook = (eachSecondHook: EachSecondHook) => {
  hooks.push(eachSecondHook);
};

const unregisterEachSecondHook = (eachSecondHook: EachSecondHook) => {
  const hookIndex = hooks.findIndex(hook => hook === eachSecondHook);
  hooks.splice(hookIndex, 1);
};

export {
  now,
  useKeepNowUpToDate,
  registerEachSecondHook,
  unregisterEachSecondHook
};
