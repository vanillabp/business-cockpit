import { WakeupSseCallback } from '../components/SseProvider';
import { ToastFunction } from '../components/Toast';

const REFRESH_TOKEN_HEADER = "x-refresh-token";

const doRequest = (
    toast: ToastFunction,
    resolve: (response: Response) => void,
    reject: (error: any) => void,
    input: RequestInfo | URL,
    init: RequestInit | undefined,
    refreshToken?: string | null,
  ) => {

  navigator.locks.request(
      'bc-webapp',
      {
        mode: Boolean(refreshToken) ? 'exclusive' : 'shared'
      },
      async _lock => {
        try {

          const storedRefreshToken = window.localStorage.getItem(REFRESH_TOKEN_HEADER);
          const isRefresh = Boolean(storedRefreshToken) && storedRefreshToken === refreshToken;
          const initWithRefreshToken = !isRefresh
              ? init
              : {
                ...init,
                headers: {
                  ...init?.headers,
                  [REFRESH_TOKEN_HEADER]: storedRefreshToken
                }
              };
          
          // @ts-ignore
          const response = await fetch(input, initWithRefreshToken);
          // save new refresh-token regardless the response code
          // because if it's given, it is valid
          const responseRefreshToken = response.headers.get(REFRESH_TOKEN_HEADER);
          if (responseRefreshToken) {
            window.localStorage.setItem(REFRESH_TOKEN_HEADER, responseRefreshToken);
          }
          
          // if an API sends a redirect, then apply it
          if (response.redirected) {
            document.location.href = response.url;
            return;
          }
          
          if (response.status === 401) {
            // if authentication is not accepted although refresh-token was
            // sent, then clean up that outdated refresh-token
            if (isRefresh && !Boolean(responseRefreshToken)) {
              window.localStorage.removeItem(REFRESH_TOKEN_HEADER);
            }
            // if authentication is not accepted an refresh-token is available
            // then retry using the refresh-token 
            else if (Boolean(storedRefreshToken)) {
              doRequest(toast, resolve, reject, input, init, storedRefreshToken);
              return;
            }
          } else if (response.status >= 500) {
            toast({
                namespace: 'app',
                title: 'error',
                message: 'unexpected'
              });
          } else if (response.status === 403) {
            toast({
                namespace: 'app',
                title: 'error',
                message: 'forbidden'
              });
          }
          
          resolve(response);
          
        } catch (error: any) {
          
          toast({
              namespace: 'app',
              title: 'error',
              message: 'unexpected'
            });
          reject(error);
          
        }
        
      }
    );
  
};

const buildFetchApi = (toast: ToastFunction, wakeupSeeCallback?: WakeupSseCallback): WindowOrWorkerGlobalScope['fetch'] => {
  
  return (input, init): Promise<Response> => {
      return new Promise((resolve, reject) => {
          if (wakeupSeeCallback !== undefined) {
            wakeupSeeCallback();
          }
          doRequest(toast, resolve, reject, input, init);
        });
    };

};

export { buildFetchApi, REFRESH_TOKEN_HEADER };
