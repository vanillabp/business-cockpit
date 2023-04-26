import { useAppContext } from '../DevShellAppContext.js';
import { buildFetchApi } from '../../utils/fetchApi.js';
import {
    OnMessageFunction,
    SseContextInterface,
    SseProvider,
    useSse,
    WakeupSseCallback
  } from '../../components/SseProvider.js';
import { createContext, Dispatch } from 'react';
import { ToastAction } from '../../components/Toast.js';
import { OfficialTasklistApi, Configuration as GuiConfiguration } from '@bc/official-gui-client';

const SSE_UPDATE_URL = "/gui/api/v1/updates";

const getOfficialTasklistApi = (
  basePath: string,
  dispatch: Dispatch<ToastAction>,
  wakeupSseCallback?: WakeupSseCallback
): OfficialTasklistApi => {
  const config = new GuiConfiguration({
    basePath,
    fetchApi: buildFetchApi(dispatch, wakeupSseCallback),
  });
  return new OfficialTasklistApi(config);
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
  
  return (<SseProvider
              url={ SSE_UPDATE_URL }
              Context={ GuiSseContext }
              buildFetchApi={ () => buildFetchApi(dispatch) }
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
  };

