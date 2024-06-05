import {
  ColumnsOfUserTaskFunction,
  ColumnsOfWorkflowFunction,
  UserTaskForm,
  UserTaskListCell,
  WorkflowListCell,
  WorkflowPage
} from '@vanillabp/bc-shared';
import { useEffect, useState } from 'react';

export enum UiUriType {
  External = 'EXTERNAL',
  WebpackMfReact = 'WEBPACK_MF_REACT'
};

export interface ModuleDefinition {
  workflowModuleId: string;
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
  workflowModuleId: string;
  retry?: (callback?: () => void) => void;
  buildVersion?: string;
  buildTimestamp?: Date;
  userTaskListColumns?: ColumnsOfUserTaskFunction;
  workflowListColumns?: ColumnsOfWorkflowFunction;
  UserTaskForm?: UserTaskForm;
  UserTaskListCell?: UserTaskListCell;
  WorkflowListCell?: WorkflowListCell;
  WorkflowPage?: WorkflowPage;
  callbacks: Record<string, Array<HandlerFunction>>;
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
    webpackModuleId: string,
    url: string
) => {
  const elementId = "___" + webpackModuleId;
  const existingElement = document.getElementById(elementId);

  if (existingElement) {
    if ((window as any)[webpackModuleId]) return Promise.resolve(true);
    return Promise.reject(`Failed in previous attempt: ${webpackModuleId}`);
  }

  return new Promise<HTMLScriptElement>(
      (resolve, reject) => {
        const element = document.createElement("script");
        element.src = url;
        element.type = "text/javascript";
        element.async = true;
        element.id = elementId;
        element.onload = () => resolve(element);
        element.onerror = (e) => reject(e);
        document.head.appendChild(element);
      }
  );
};

export const detachScript = (
    webpackModuleId: string
) => {
  const element = document.getElementById("___" + webpackModuleId);
  if (element) document.head.removeChild(element);
};

const fetchModule = async (
    workflowModuleId: string,
    uiUri: string,
    initialTry: boolean,
    useCase: string
): Promise<Module> => {
  const webpackModuleId = workflowModuleId.replaceAll('-', '_');
  if (!initialTry) {
    detachScript(webpackModuleId);
  }
  let container = (window as any)[webpackModuleId];
  if (container === undefined) {
    await attachScript(webpackModuleId, uiUri);
    //@ts-expect-error
    await __webpack_init_sharing__("default");
    container = (window as any)[webpackModuleId];
  }
  if (!container.isInitialized) {
    container.isInitialized = true;
    //@ts-expect-error
    await container.init(__webpack_share_scopes__.default);
  } else if (!('isInitialized' in container)) {
    throw new Error(`Failed in last attempt: ${webpackModuleId}`);
  }
  const factory = await (window as any)[webpackModuleId].get(useCase);
  return factory();
};

const loadModule = (
    moduleId: string,
    moduleDefinition: ModuleDefinition,
    useCase: string
): ComponentProducer => {

  let module: Module = modules[moduleId];
  if (module === undefined) {
    module = {
      moduleId,
      workflowModuleId: moduleDefinition.workflowModuleId,
      callbacks: {},
    };
    modules[moduleId] = module;
  }
  if (module.callbacks[useCase] === undefined) {
    module.callbacks[useCase] = [];
  }

  const publish = () => module.callbacks[useCase].forEach(callback => callback(modules[moduleId]));

  const retry = async (retryCallback?: () => void, initialTry?: boolean) => {
    try {
      if (module.buildTimestamp !== undefined) return;
      if (moduleDefinition.uiUriType === UiUriType.WebpackMfReact) {
        const webpackModule =  await fetchModule(moduleDefinition.workflowModuleId, moduleDefinition.uiUri, initialTry || false, useCase);
        modules[moduleId] = {
          ...webpackModule,
          ...module
        };
      } else {
        throw new Error(`Unsupported UiUriType: ${moduleDefinition.uiUriType}!`);
      }
    } catch (error) {
      modules[moduleId] = {
        ...module,
        retry
      }
    } finally {
      publish();
      if (retryCallback) {
        retryCallback();
      }
    }
  };

  const subscribe = (callback: HandlerFunction) => {
    if (modules[moduleId].callbacks[useCase].includes(callback)) return;
    modules[moduleId].callbacks[useCase].push(callback);
  };

  const unsubscribe = (callback: HandlerFunction) => {
    const index = modules[moduleId].callbacks[useCase].indexOf(callback);
    if (index === -1) return;
    modules[moduleId].callbacks[useCase].splice(index, 1);
  };

  retry(undefined, true);

  return { subscribe, unsubscribe };
};

const getModule = (
    moduleId: string,
    moduleDefinition: ModuleDefinition,
    useCase: string
): ComponentProducer => {

  let result = modules[moduleId];
  if (result !== undefined) {
    const subscribe = (callback: HandlerFunction) => {
      if (modules[moduleId].callbacks[useCase].includes(callback)) return;
      modules[moduleId].callbacks[useCase].push(callback);
    };

    const unsubscribe = (callback: HandlerFunction) => {
      const index = modules[moduleId].callbacks[useCase].indexOf(callback);
      if (index === -1) return;
      modules[moduleId].callbacks[useCase].splice(index, 1);
    };

    return {
      subscribe,
      unsubscribe
    };
  }

  return loadModule(moduleId, moduleDefinition, useCase);

};

const useFederationModule = (
    moduleDefinition: ModuleDefinition | undefined,
    useCase: string
): Module | undefined => {

  const moduleId = moduleDefinition !== undefined ? `${moduleDefinition.workflowModuleId}#${useCase}` : 'undefined';
  const [module, setModule] = useState(modules[moduleId]);

  useEffect(() => {
    if (moduleDefinition === undefined) {
      return undefined;
    }
    const { subscribe, unsubscribe } = getModule(moduleId, moduleDefinition, useCase);
    const handler = (loadedModule: Module) => { setModule(loadedModule); };
    subscribe(handler);
    return () => { unsubscribe(handler) };
  }, [ setModule, moduleDefinition, useCase ]); //eslint-disable-line react-hooks/exhaustive-deps -- moduleId is derived from moduleDefinition

  return module;

};

const useFederationModules = (
    moduleDefinitions: ModuleDefinition[] | undefined,
    useCase: string
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
            (moduleIds, userTask) => moduleIds.includes(userTask.workflowModuleId)
                ? moduleIds : moduleIds.concat(userTask.workflowModuleId),
            new Array<string>())
        .map(moduleId => moduleDefinitions.find(moduleDefinition => moduleDefinition.workflowModuleId === moduleId)!);

    const result: Module[] = [];
    distinctModuleDefinitions
        .forEach((moduleDefinition, i) => {
          result[i] = {
            moduleId: `${moduleDefinition.workflowModuleId}#${useCase}`,
            workflowModuleId: moduleDefinition.workflowModuleId,
            callbacks: {}
          }
        });

    const unsubscribers = distinctModuleDefinitions.map((moduleDefinition, index) => {
      const { subscribe, unsubscribe } = getModule(result[index].moduleId, moduleDefinition, useCase);
      const handler = (loadedModule: Module) => {
        result[index] = loadedModule;
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
