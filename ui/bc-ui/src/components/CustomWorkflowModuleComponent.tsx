import { ModuleDefinition, UiUriType, useFederationModule } from "../utils/index.js";
import {
  ShowLoadingIndicatorFunction,
  ToastFunction,
  TranslationFunction,
  WorkflowModuleComponent
} from "@vanillabp/bc-shared";
import { WorkflowModule } from "@vanillabp/bc-official-gui-client";
import { NoElementGivenByModule } from "./NoElementGivenByModule.js";
import { useMemo } from "react";

const CustomWorkflowModuleComponent = ({
  showLoadingIndicator,
  toast,
  workflowModule,
  useCase,
  t,
  showRetryButtonOnError = true,
  entryPoint = '/remoteEntry.js'
}: {
  showLoadingIndicator: ShowLoadingIndicatorFunction,
  toast: ToastFunction,
  workflowModule: WorkflowModule,
  useCase: string,
  t: TranslationFunction,
  showRetryButtonOnError?: boolean,
  entryPoint?: string,
}) => {
  const module = useMemo<ModuleDefinition>(() => ({
    uiUriType: UiUriType.WebpackMfReact,
    uiUri: `${workflowModule.uri!}${entryPoint}`,
    workflowModuleId: workflowModule.id,
    workflowModuleUri: workflowModule.uri!
  }), [ workflowModule.id, workflowModule.uri, entryPoint ]);

  const federatedModule = useFederationModule(module, useCase);

  if (federatedModule?.retry) {
    console.error("Could not load module!");
    return (showRetryButtonOnError
        ? <NoElementGivenByModule
            t={ t }
            loading={ false }
            showLoadingIndicator={ showLoadingIndicator }
            retry={ federatedModule.retry } />
        : undefined);
  }

  const Component = (federatedModule && federatedModule[useCase]) as WorkflowModuleComponent;
  if (!Component) {
    return (showRetryButtonOnError
        ? <NoElementGivenByModule
            t={ t }
            loading={ true }
            showLoadingIndicator={ showLoadingIndicator } />
        : undefined);
  }

  return (
      <Component
          toast={ toast }
          workflowModule={ workflowModule } />);
}

export { CustomWorkflowModuleComponent }
