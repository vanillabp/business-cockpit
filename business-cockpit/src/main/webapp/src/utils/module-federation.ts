import { UserTaskForm } from '@vanillabp/bc-shared';
import React, { useState, useEffect, useCallback } from 'react';

export type UseCase = 'List' | 'Form';

export interface Title {
  [key: string]: string;
}

export interface Column {
  id: string;
  title: Title;
  path: string;
};

export interface TasklistCellProps {
  bpmnProcessId: string;
  formKey: string;
  columnId: string;
};

export interface Module {
  workflowModuleId?: string;
  retry?: () => void;
  buildVersion?: string;
  buildTimestamp?: Date;
  taskListColumns?: Column[];
  UserTaskForm?: UserTaskForm;
  TaskListCell?: React.FC<TasklistCellProps>; 
};

interface Modules {
  [key: string]: Module;
}

const modules: Modules = {};

type HandlerFunction = (loadedModule: Module) => void;
type ComponentProducer = {
  subscribe: (callback: HandlerFunction) => void,
  unsubscribe: (callback: HandlerFunction) => void
};

// inspired by https://github.com/hasanayan/react-dynamic-remote-component

export const attachScript = (
  workflowModuleId: string,
  url: string
) => {
  const existingElement = document.getElementById(workflowModuleId);

  if (existingElement) {
    //@ts-expect-error
    if (window[workflowModuleId]) return Promise.resolve(true);

    return new Promise((resolve) => {
      existingElement.onload = (e) => {
        resolve(true);
      };
    });
  }
  
  const element = document.createElement("script");
  element.src = url;
  element.type = "text/javascript";
  element.async = true;
  element.id = "___" + workflowModuleId;

  const scriptLoadPromise = new Promise<HTMLScriptElement>(
    (resolve, reject) => {
      element.onload = () => resolve(element);
      element.onerror = (e) => {
        reject(e);
      };
    }
  );

  document.head.appendChild(element);

  return scriptLoadPromise;
};

export const detachScript = (
  workflowModuleId: string
) => {
  const element = document.getElementById("___" + workflowModuleId);
  if (element) document.head.removeChild(element);
};

const fetchModule = async (
  workflowModuleId: string,
  useCase: UseCase
): Promise<Module> => {

  await attachScript(workflowModuleId, `/wm/${workflowModuleId}/remoteEntry.js`);
  
  //@ts-expect-error
  await __webpack_init_sharing__("default");
  //@ts-expect-error
  const container = window[workflowModuleId];
  if (!container.isInitialized) {
    container.isInitialized = true;
    //@ts-expect-error
    await container.init(__webpack_share_scopes__.default);
  }
  //@ts-expect-error
  const factory = await window[workflowModuleId].get(useCase);

  return factory();
};

const loadModule = (
  moduleId: string,
  workflowModuleId: string,
  useCase: UseCase
): ComponentProducer => {
  const callbacks: Array<HandlerFunction> = [];
  
  const retry = async () => {
    if (module.UserTaskForm !== undefined) return;
    try {
      module = await fetchModule(workflowModuleId, useCase);
      publish();
    } catch (error) {
      console.error(error);
      module.retry = retry;
      publish();
    }
  };

  let module: Module = { workflowModuleId, retry };
  modules[moduleId] = module;
    
  const publish = () => callbacks.forEach(callback => callback(module));
  
  const subscribe = (callback: HandlerFunction) => {
      if (callbacks.includes(callback)) return;
      callbacks.push(callback);
    };

  const unsubscribe = (callback: HandlerFunction) => {
      const index = callbacks.indexOf(callback);
      if (index === -1) return;
      callbacks.splice(index, 1);     
    };
    
  retry();
    
  return { subscribe, unsubscribe };
};

const getModule = (
  workflowModuleId: string,
  useCase: UseCase
): ComponentProducer => {
  const moduleId = `${workflowModuleId}#${useCase}`;
  let result = modules[moduleId];
  if ((result !== undefined)
      && (result.retry === undefined)) {
    return {
        subscribe: (handler: HandlerFunction) => handler(result), 
        unsubscribe: (handler: HandlerFunction) => handler(result), 
      };
  }
  
  return loadModule(moduleId, workflowModuleId, useCase);
  
};

const useFederationModule = (
  workflowModuleId: string,
  useCase: UseCase
): Module => {

  const [module, setModule] = useState(modules[workflowModuleId]);

  const memoizedGetModule = useCallback(
      () => getModule(workflowModuleId, useCase),
      [ workflowModuleId, useCase ]);
  
  useEffect(() => {
      const { subscribe, unsubscribe } = memoizedGetModule();
      const handler = (loadedModule: Module) => setModule(loadedModule);
      subscribe(handler);
      return () => unsubscribe(handler);
    }, [ memoizedGetModule, setModule ]);
  
  return module;

};

export { useFederationModule };
