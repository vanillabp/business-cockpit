import {
  ColumnsOfUserTaskFunction,
  ColumnsOfWorkflowFunction,
  UserTaskForm,
  UserTaskListCell,
  WorkflowListCell,
  WorkflowPage
} from '@vanillabp/bc-shared';
import { useEffect, useState } from 'react';

export type UseCase = 'UserTaskList' | 'UserTaskForm' | 'WorkflowList' | 'WorkflowPage';

export enum UiUriType {
    External = 'EXTERNAL',
    WebpackMfReact = 'WEBPACK_MF_REACT'
};

export interface ModuleDefinition {
  workflowModule: string;
  uiUriType: UiUriType;
  uiUri: string;
};

export interface TasklistCellProps {
  bpmnProcessId: string;
  taskDefinition: string;
  path: string;
};

export interface WorkflowCellProps {
  bpmnProcessId: string;
  path: string;
};

export interface Module {
  moduleId: string;
  workflowModule: string;
  retry?: (callback?: () => void) => void;
  buildVersion?: string;
  buildTimestamp?: Date;
  userTaskListColumns?: ColumnsOfUserTaskFunction;
  workflowListColumns?: ColumnsOfWorkflowFunction;
  UserTaskForm?: UserTaskForm;
  UserTaskListCell?: UserTaskListCell;
  WorkflowListCell?: WorkflowListCell;
  WorkflowPage?: WorkflowPage;
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
  moduleId: string,
  url: string
) => {
  const existingElement = document.getElementById(moduleId);

  if (existingElement) {
    //@ts-expect-error
    if (window[moduleId]) return Promise.resolve(true);

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
  element.id = "___" + moduleId;

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
  moduleId: string
) => {
  const element = document.getElementById("___" + moduleId);
  if (element) document.head.removeChild(element);
};

const fetchModule = async (
  moduleId: string,
  uiUri: string,
  useCase: UseCase
): Promise<Module> => {

  await attachScript(moduleId, uiUri);
  
  const webpackModuleId = moduleId.replaceAll('-', '_');
  //@ts-expect-error
  await __webpack_init_sharing__("default");
  //@ts-expect-error
  const container = window[webpackModuleId];
  if (!container.isInitialized) {
    container.isInitialized = true;
    //@ts-expect-error
    await container.init(__webpack_share_scopes__.default);
  }
  //@ts-expect-error
  const factory = await window[webpackModuleId].get(useCase);

  return factory();
};

const loadModule = (
  moduleId: string,
  moduleDefinition: ModuleDefinition,
  useCase: UseCase
): ComponentProducer => {
  const callbacks: Array<HandlerFunction> = [];
  
  const retry = async (retryCallback?: () => void) => {
    if (module.buildTimestamp !== undefined) return;
    try {
      if (moduleDefinition.uiUriType === UiUriType.WebpackMfReact) {
        detachScript(moduleId);
        const webpackModule =  await fetchModule(moduleDefinition.workflowModule, moduleDefinition.uiUri, useCase);
        module = {
          ...webpackModule,
          workflowModule: moduleDefinition.workflowModule,
          moduleId
        };
        modules[moduleId] = module;
      } else {
        throw new Error(`Unsupported UiUriType: ${moduleDefinition.uiUriType}!`);
      }
      publish();
    } catch (error) {
      console.error(error);
      module.retry = retry;
      publish();
    } finally {
      if (retryCallback) {
        retryCallback();
      }
    }
  };

  let module: Module;
  if (Object.keys(modules).includes(moduleId)) {
    module = modules[moduleId];
  } else {
    module = { moduleId, workflowModule: moduleDefinition.workflowModule };
    modules[moduleId] = module;
  }
  
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
  moduleId: string,
  moduleDefinition: ModuleDefinition,
  useCase: UseCase
): ComponentProducer => {

  let result = modules[moduleId];
  if (result !== undefined) {
    return {
        subscribe: (handler: HandlerFunction) => handler(result), 
        unsubscribe: (handler: HandlerFunction) => handler(result), 
      };
  }
  
  return loadModule(moduleId, moduleDefinition, useCase);
  
};

const useFederationModule = (
  moduleDefinition: ModuleDefinition | undefined,
  useCase: UseCase
): Module | undefined => {
  
  const moduleId = moduleDefinition !== undefined ? `${moduleDefinition.workflowModule}#${useCase}` : 'undefined';
  const [module, setModule] = useState(modules[moduleId]);
  
  useEffect(() => {
      if (moduleDefinition === undefined) {
        return undefined;
      }
      const { subscribe, unsubscribe } = getModule(moduleId, moduleDefinition, useCase);
      const handler = (loadedModule: Module) => setModule(loadedModule);
      subscribe(handler);
      return () => unsubscribe(handler);
    }, [ setModule, moduleDefinition, useCase ]); //eslint-disable-line react-hooks/exhaustive-deps -- moduleId is derived from moduleDefinition
  
  return module;

};

const useFederationModules = (
  moduleDefinitions: ModuleDefinition[] | undefined,
  useCase: UseCase
): Module[] | undefined => {

  const [modules, setModules] = useState<Module[] | undefined>(undefined);

  useEffect(() => {

      if (moduleDefinitions === undefined) {
        return;
      }
      if (moduleDefinitions.length === 0) {
        setModules([]);
        return;
      }
      
      const distinctModuleDefinitions = moduleDefinitions
          .reduce(
              (moduleIds, userTask) => moduleIds.includes(userTask.workflowModule)
                  ? moduleIds : moduleIds.concat(userTask.workflowModule),
              new Array<string>())
          .map(moduleId => moduleDefinitions.find(moduleDefinition => moduleDefinition.workflowModule === moduleId)!);

      const result: Module[] = [];
      distinctModuleDefinitions
          .forEach((moduleDefinition, i) => {
              result[i] = {
                moduleId: `${moduleDefinition.workflowModule}#${useCase}`,
                workflowModule: moduleDefinition.workflowModule
              }
          });
      
      const unsubscribers = distinctModuleDefinitions.map((moduleDefinition, index) => {
          const { subscribe, unsubscribe } = getModule(result[index].moduleId, moduleDefinition, useCase);
          const handler = (loadedModule: Module) => {
              result[index] = loadedModule;
              if (result.length < distinctModuleDefinitions.length) {
                return;
              }
              const anyModuleStillLoading = result
                  .reduce(
                  (anyModuleStillLoading, module) => anyModuleStillLoading || (module === undefined) || ((module.buildTimestamp === undefined) && (module.retry === undefined)),
                  false);
              if (anyModuleStillLoading) {
                return;
              }
              setModules(result);
            };
          subscribe(handler);
          return () => unsubscribe(handler);
        });
        
      return () => unsubscribers.forEach(unsubscribe => unsubscribe());
      
    }, [ moduleDefinitions, useCase, setModules ]);
  
  return modules;
  
};

export { useFederationModule, useFederationModules };
