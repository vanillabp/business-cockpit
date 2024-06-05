import { Configuration as GuiConfiguration, LoginApi } from './gui';
import { useAppContext } from '../AppContext';
import {
  buildFetchApi,
  OnMessageFunction,
  SseContextInterface,
  SseProvider,
  ToastFunction,
  useSse,
  WakeupSseCallback
} from '@vanillabp/bc-shared';
import { createContext } from 'react';
import {
  Configuration as OfficialApiConfiguration,
  OfficialTasklistApi,
  OfficialWorkflowlistApi
} from '@vanillabp/bc-official-gui-client';

const SSE_UPDATE_URL = "/gui/api/v1/updates";

const getLoginGuiApi = (
  toast: ToastFunction,
  wakeupSseCallback?: WakeupSseCallback
): LoginApi => {
  const config = new GuiConfiguration({
    basePath: '/gui/api/v1',
    fetchApi: buildFetchApi(toast, wakeupSseCallback),
  });
  return new LoginApi(config);
};

const getTasklistGuiApi = (
    toast: ToastFunction,
    wakeupSseCallback?: WakeupSseCallback,
    kind?: string,
): OfficialTasklistApi => {
  const config = new OfficialApiConfiguration({
    basePath: `/gui/api/v1${kind !== undefined ? `/${kind}` : ''}`,
    fetchApi: buildFetchApi(toast, wakeupSseCallback),
  });
  return new OfficialTasklistApi(config);
};

const getWorkflowlistGuiApi = (
    toast: ToastFunction,
    wakeupSseCallback?: WakeupSseCallback
): OfficialWorkflowlistApi => {
    const config = new OfficialApiConfiguration({
        basePath: '/gui/api/v1',
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
  
  const { toast } = useAppContext();
  
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
    getLoginGuiApi,
    getTasklistGuiApi,
    getWorkflowlistGuiApi
  };

