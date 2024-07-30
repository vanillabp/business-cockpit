import { useAppContext } from '../DevShellAppContext.js';
import {
  buildFetchApi,
  OnMessageFunction,
  SseContextInterface,
  SseProvider,
  Toast,
  ToastFunction,
  useSse,
  WakeupSseCallback
} from '@vanillabp/bc-shared';
import { createContext, useCallback } from 'react';
import {
  Configuration as GuiConfiguration,
  OfficialTasklistApi,
  OfficialWorkflowlistApi
} from '@vanillabp/bc-official-gui-client';

const SSE_UPDATE_URL = "/gui/api/v1/updates";

const getOfficialTasklistApi = (
  basePath: string,
  toast: ToastFunction,
  wakeupSseCallback?: WakeupSseCallback
): OfficialTasklistApi => {
  const config = new GuiConfiguration({
    basePath,
    fetchApi: buildFetchApi(toast, wakeupSseCallback),
  });
  return new OfficialTasklistApi(config);
};

const getOfficialWorkflowlistApi = (
  basePath: string,
  toast: ToastFunction,
  wakeupSseCallback?: WakeupSseCallback
): OfficialWorkflowlistApi => {
  const config = new GuiConfiguration({
    basePath,
    fetchApi: buildFetchApi(toast, wakeupSseCallback),
  });
  return new OfficialWorkflowlistApi(config);
};

interface GuiSseContextInterface extends SseContextInterface { };

const GuiSseContext = createContext<GuiSseContextInterface>(
  {
    wakeupSseCallback: () => undefined,
    getConnection: () => '',
    releaseConnection: () => undefined,
  }
);

const GuiSseProvider = ({ children, ...rest }: React.PropsWithChildren<{}>) => {
  
  const { dispatch } = useAppContext();
  const toast = useCallback((toast: Toast) => dispatch({ type: 'toast', toast }), [ dispatch ]);
  return (<SseProvider
              url={ SSE_UPDATE_URL }
              Context={ GuiSseContext }
              buildFetchApi={ () => buildFetchApi(toast) }
              { ...rest }>
            { children }
          </SseProvider>);
          
};

const useGuiSse = <T, >(
    //dependencies: DependencyList,
    onMessage: OnMessageFunction<T>,
    messageName?: string | RegExp
  ): WakeupSseCallback => useSse(
    GuiSseContext,
    //dependencies,
    onMessage,
    messageName
  );  

export {
    GuiSseProvider,
    useGuiSse,
    getOfficialTasklistApi,
    getOfficialWorkflowlistApi,
  };

