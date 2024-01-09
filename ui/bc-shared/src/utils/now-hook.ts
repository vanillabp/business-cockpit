import { useEffect, useRef } from "react";

let now = new Date();

type EachSecondHook = (lastNow: Date) => void;

const hooks = new Array<{ hook: EachSecondHook, lastNow: Date }>();

const useKeepNowUpToDate = () => {
  
  useEffect(() => {
      const timer = window.setInterval(() => {
          now = new Date();
          hooks.forEach(hook => {
              const lastNow = hook.lastNow;
              hook.lastNow = now;
              hook.hook(lastNow);
            });
        }, 1000);
      return () => window.clearInterval(timer);
    }, [ ]);  // eslint-disable-line react-hooks/exhaustive-deps
  
};

const registerEachSecondHook = (eachSecondHook: EachSecondHook) => {
  hooks.push({ hook: eachSecondHook, lastNow: now });
};

const unregisterEachSecondHook = (eachSecondHook: EachSecondHook) => {
  const hookIndex = hooks.findIndex(hook => hook.hook === eachSecondHook);
  hooks.splice(hookIndex, 1);
};

export {
  now,
  useKeepNowUpToDate,
  registerEachSecondHook,
  unregisterEachSecondHook
};

