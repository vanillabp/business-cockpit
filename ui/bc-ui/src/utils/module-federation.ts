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
  [key: string]: any;
};

interface InternalModule extends Module {
  callbacks: Array<HandlerFunction>;
};

interface Modules {
  [key: string]: InternalModule;
}

interface PromiseFunctions {
  reject: (error: any) => void;
  resolve: () => void;
}

interface LoadedScript {
  error?: any,
  promises: Array<PromiseFunctions>;
}
interface LoadedScripts {
  [key: string]: LoadedScript;
}

const loadedScripts: LoadedScripts = {};
const modules: Modules = {};

type HandlerFunction = (loadedModule: InternalModule) => void;
type ComponentProducer = {
  subscribe: (callback: HandlerFunction) => void,
  unsubscribe: (callback: HandlerFunction) => void
};

// inspired by https://github.com/hasanayan/react-dynamic-remote-component

export const attachScript = (
    webpackModuleId: string,
    url: string,
    initialTry: boolean,
) => {
  const elementId = "___" + webpackModuleId;
  const existingElement = document.getElementById(elementId);

  if (existingElement) {
    const loadedScript = loadedScripts[webpackModuleId];
    if ((loadedScript.error !== undefined)
        && !initialTry) {
      detachScript(webpackModuleId);
    } else {
      if (loadedScript.error) {
        return Promise.reject(loadedScript.error);
      }
      if (loadedScript.promises.length === 0) {
        return Promise.resolve();
      }
      return new Promise<void>(
          (resolve, reject) => {
            loadedScript.promises.push({
              resolve,
              reject,
            })
          }
      );
    }
  }

  return new Promise<void>(
      (resolve, reject) => {
        const loadedScript: LoadedScript = {
          promises: [ {
            resolve,
            reject,
          } ],
        };
        loadedScripts[webpackModuleId] = loadedScript;
        const element = document.createElement("script");
        element.src = url;
        element.type = "text/javascript";
        element.async = true;
        element.id = elementId;
        element.onload = async () => {
          //@ts-expect-error
          await __webpack_init_sharing__("default");
          const pfs = [ ...loadedScript.promises ];
          loadedScript.promises = [];
          pfs.forEach(pf => pf.resolve());
        }
        element.onerror = (e) => {
          const pfs = [ ...loadedScript.promises ];
          loadedScript.promises = [];
          loadedScript.error = e;
          pfs.forEach(pf => pf.reject(loadedScript.error));
        }
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
): Promise<InternalModule> => {
  const webpackModuleId = workflowModuleId.replaceAll('-', '_');
  let container = (window as any)[webpackModuleId];
  if (container === undefined) {
    await attachScript(webpackModuleId, uiUri, initialTry);
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

  let module = modules[moduleId];
  if (module === undefined) {
    module = {
      moduleId,
      workflowModuleId: moduleDefinition.workflowModuleId,
      callbacks: [],
    };
    modules[moduleId] = module;
  }

  const publish = () => module.callbacks.forEach(callback => callback(modules[moduleId]));

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
      console.error("Error loading module:", error);

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
    if (modules[moduleId].callbacks.includes(callback)) return;
    modules[moduleId].callbacks.push(callback);
  };

  const unsubscribe = (callback: HandlerFunction) => {
    const index = modules[moduleId].callbacks.indexOf(callback);
    if (index === -1) return;
    modules[moduleId].callbacks.splice(index, 1);
  };

  retry(undefined, true);

  return { subscribe, unsubscribe };
};

const getModule = (
    moduleId: string,
    moduleDefinition: ModuleDefinition,
    useCase: string
): ComponentProducer => {

  let module = modules[moduleId];
  if (module !== undefined) {
    let subscribe: undefined | ((callback: HandlerFunction) => void) = undefined;
    let unsubscribe: undefined | ((callback: HandlerFunction) => void) = undefined;

    if (module.buildTimestamp || module.retry) {
      subscribe = (callback: HandlerFunction) => callback(module);
      unsubscribe = (callback: HandlerFunction) => callback(module);
    } else {
      subscribe = (callback: HandlerFunction) => {
        if (modules[moduleId].callbacks.includes(callback)) return;
        modules[moduleId].callbacks.push(callback);
      };
      unsubscribe = (callback: HandlerFunction) => {
        const index = modules[moduleId].callbacks.indexOf(callback);
        if (index === -1) return;
        modules[moduleId].callbacks.splice(index, 1);
      };
    }

    return {
      subscribe,
      unsubscribe
    };
  }

  return loadModule(moduleId, moduleDefinition, useCase);

};

const useFederationModule = (
    moduleDefinition: ModuleDefinition | undefined | null,
    useCase: string
): Module | undefined => {

  const moduleId = Boolean(moduleDefinition !== undefined) ? `${moduleDefinition!.workflowModuleId}#${useCase}` : 'undefined';
  const [module, setModule] = useState(modules[moduleId]);

  useEffect(() => {
    if (!Boolean(moduleDefinition)) {
      return undefined;
    }
    const { subscribe, unsubscribe } = getModule(moduleId, moduleDefinition!, useCase);
    const handler = (loadedModule: InternalModule) => setModule(loadedModule);
    subscribe(handler);
    return () => unsubscribe(handler);
  }, [ setModule, moduleDefinition, useCase ]); //eslint-disable-line react-hooks/exhaustive-deps -- moduleId is derived from moduleDefinition

  return module;

};

const useFederationModules = (
    moduleDefinitions: ModuleDefinition[] | undefined | null,
    useCase: string
): Module[] | undefined => {

  const [modules, setModules] = useState<Module[] | undefined>(undefined);

  useEffect(() => {

    if (!Boolean(moduleDefinitions)) {
      return;
    }
    if (moduleDefinitions!.length === 0) {
      setModules([]);
      return;
    }

    const distinctModuleDefinitions = moduleDefinitions!
        .reduce(
            (moduleIds, userTask) => moduleIds.includes(userTask.workflowModuleId)
                ? moduleIds : moduleIds.concat(userTask.workflowModuleId),
            new Array<string>())
        .map(moduleId => moduleDefinitions!.find(moduleDefinition => moduleDefinition.workflowModuleId === moduleId)!);

    const result: InternalModule[] = [];
    distinctModuleDefinitions
        .forEach((moduleDefinition, i) => {
          result[i] = {
            moduleId: `${moduleDefinition.workflowModuleId}#${useCase}`,
            workflowModuleId: moduleDefinition.workflowModuleId,
            callbacks: []
          }
        });

    const unsubscribers = distinctModuleDefinitions.map((moduleDefinition, index) => {
      const { subscribe, unsubscribe } = getModule(result[index].moduleId, moduleDefinition, useCase);
      const handler = (loadedModule: InternalModule) => {
        result[index] = loadedModule;
        const anyModuleStillLoading = result
            .reduce(
                (anyModuleStillLoading, module) => anyModuleStillLoading || (module === undefined) || ((module.buildTimestamp === undefined) && (module.retry === undefined)),
                false);
        if (anyModuleStillLoading) {
          return;
        }
        if (JSON.stringify(result) !== JSON.stringify(modules)) {
          setModules(result);
        }
      };
      subscribe(handler);
      return () => unsubscribe(handler);
    });

    return () => unsubscribers.forEach(unsubscribe => unsubscribe());

  }, [ moduleDefinitions, useCase, setModules ]);

  return modules;

};

export { useFederationModule, useFederationModules };
