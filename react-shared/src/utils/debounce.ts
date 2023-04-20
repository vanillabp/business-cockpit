type Params = any[];

type Func = (...args: Params) => void;

type Debounce = (
    func: Func,
    timeout?: number,
  ) => Func;

const debounce: Debounce = (
  func,
  timeout = 300
) => {
  let timer: number;
  return (...args: Params) => {
    window.clearTimeout(timer);
    timer = window.setTimeout(() => {
        func(...args);
      }, timeout);
  }
};

interface KeyTimerMap {
    [key: string]: number;
}

const timers: KeyTimerMap = {};

type FuncForKey = () => void;

type DebounceByKey = (
    key: string,
    func?: FuncForKey,
    timeout?: number,
  ) => void;


const debounceByKey: DebounceByKey = (
  key,
  func,
  timeout = 300
) => {
  const prevTimer = timers[key];
  if (prevTimer) {
    window.clearTimeout(prevTimer);
    delete timers[key];
  }
  if (func) {
    timers[key] = window.setTimeout(() => {
        func();
      }, timeout);
  }
};

export {
  debounce,
  debounceByKey
};
