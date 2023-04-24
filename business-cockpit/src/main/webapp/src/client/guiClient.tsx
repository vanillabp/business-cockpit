import { Configuration as GuiConfiguration, LoginApi, TasklistApi } from './gui';
import { useAppContext } from '../AppContext';
import { OnMessageFunction, SseContextInterface, SseProvider, useSse, WakeupSseCallback } from '@bc/shared';
import { createContext, Dispatch } from 'react';
import { ToastAction, buildFetchApi, doLogout } from '@bc/shared';

const SSE_UPDATE_URL = "/gui/api/v1/updates";

const getLoginGuiApi = (
  dispatch: Dispatch<ToastAction>,
  wakeupSseCallback?: WakeupSseCallback
): LoginApi => {
  const config = new GuiConfiguration({
    basePath: '/gui/api/v1',
    fetchApi: buildFetchApi(dispatch, wakeupSseCallback),
  });
  return new LoginApi(config);
};

const getTasklistGuiApi = (
  dispatch: Dispatch<ToastAction>,
  wakeupSseCallback?: WakeupSseCallback
): TasklistApi => {
  const config = new GuiConfiguration({
    basePath: '/gui/api/v1',
    fetchApi: buildFetchApi(dispatch, wakeupSseCallback),
  });
  return new TasklistApi(config);
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
    getLoginGuiApi,
    getTasklistGuiApi,
    doLogout,
  };

