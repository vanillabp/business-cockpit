import { ModuleDefinition, UiUriType, useFederationModule } from "../utils/index.js";
import {
  ShowLoadingIndicatorFunction,
  ToastFunction,
  WorkflowModuleComponent
} from "@vanillabp/bc-shared";
import { WorkflowModule } from "@vanillabp/bc-official-gui-client";
import { NoElementGivenByModule } from "./NoElementGivenByModule.js";
import { useMemo } from "react";
import {TranslationFunction} from "@vanillabp/bc-types";

const CustomWorkflowModuleComponent = ({
  showLoadingIndicator,
  toast,
  workflowModule,
  useCase,
  t,
  entryPoint = '/remoteEntry.js'
}: {
  showLoadingIndicator: ShowLoadingIndicatorFunction,
  toast: ToastFunction,
  workflowModule: WorkflowModule,
  useCase: string,
  t: TranslationFunction,
  entryPoint?: string,
}) => {
  // The entry point is a path below the module's proxy route, the way a task or a case reports
  // one. Loading a module puts the route in front of it, so this component hands over the path
  // and not a finished address.
  const module = useMemo<ModuleDefinition>(() => ({
    uiUriType: UiUriType.WebpackMfReact,
    uiUri: entryPoint,
    workflowModuleId: workflowModule.id,
    workflowModuleUri: workflowModule.uri!
  }), [ workflowModule.id, workflowModule.uri, entryPoint ]);

  const federatedModule = useFederationModule(module, useCase);

  if (federatedModule?.retry) {
    console.error("Could not load module!");
    return (
        <NoElementGivenByModule
            t={ t }
            loading={ false }
            showLoadingIndicator={ showLoadingIndicator }
            retry={ federatedModule.retry } />);
  }

  const Component = (federatedModule && federatedModule[useCase]) as WorkflowModuleComponent;
  if (!Component) {
    return (
        <NoElementGivenByModule
            t={ t }
            loading={ true }
            showLoadingIndicator={ showLoadingIndicator } />);
  }

  return (
      <Component
          toast={ toast }
          workflowModule={ workflowModule } />);
}

export { CustomWorkflowModuleComponent }
