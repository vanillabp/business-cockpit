import { fetchEventSource, EventSourceMessage as FetchEventSourceMessage, FetchEventSourceInit } from "@microsoft/fetch-event-source";
import { Timeout } from "grommet";
import { Context, useContext, useEffect, useRef } from "react";

type SseURL = string;

type SseConnectionID = string;

export interface EventSourceMessage<T> {
    id: string;
    event: string;
    data: T;
    retry?: number;
}

/**
 * If SSE connection is lost (e.g. due to unreliable mobile network)
 * then reconnect is retried every 15 seconds. In case of having
 * a successful REST-call during that period, a SSE potientially
 * caused by this REST-call would be lost. To avoid this situation
 * the wakeup-callback can be used by REST-clients to trigger
 * SSE reconnect immediatly instead of waiting to the end of the
 * 15 seconds period. If network is available the SSE connection
 * will be established before doing the REST-call and therefore
 * any potentially triggered SSE can be received and processed.
 */
export type WakeupSseCallback = (() => void) | undefined;

export type OnMessageFunction<T> = (ev: EventSourceMessage<T>) => void;

type OnMessageSignature = {
  onMessage: OnMessageFunction<any>,
  messageName?: string | RegExp
};

type GetConnectionFunction = (
    onMessage: OnMessageFunction<any>,
    messageName?: string | RegExp
  ) => SseConnectionID;
      
type ReleaseConnectionFunction = (connectionId: SseConnectionID) => void;

export type SseContextInterface = {
  wakeupSseCallback: WakeupSseCallback,
  getConnection: GetConnectionFunction;
  releaseConnection: ReleaseConnectionFunction;
};

interface SseProviderProps extends Omit<FetchEventSourceInit, 'fetch'> {
  Context: Context<SseContextInterface>;
  buildFetchApi: () => WindowOrWorkerGlobalScope['fetch'];
  url: SseURL;
};

const onmessage = (
  ev: FetchEventSourceMessage,
  connections: Record<SseConnectionID, OnMessageSignature>
) => {
  Object
      .keys(connections)
      .map(connectionId => connections[connectionId] )
      .filter(onMessageSignature => onMessageSignature === undefined // connection deleted meanwhile
          ? false                                                    // means no notification
          : typeof onMessageSignature.messageName === 'string'       // messageName as string
          ? onMessageSignature.messageName === ev.event              // matches given event or
          : onMessageSignature.messageName instanceof RegExp         // as regular expression
          ? onMessageSignature.messageName.test(ev.event)            // matches given event
          : true)                                                    // no messageName means no filtering
      .forEach(onMessageSignature => {
          try {
            onMessageSignature.onMessage(
                {
                  ...ev,
                  data: parseJSONData(ev.data)
                });
          } catch (e) {
            console.error("Error on processing event", ev, e);
          }
        });
};

const parseJSONData = (data: string): any => {
  if (!Boolean(data)) {
    return undefined;
  }
  return JSON.parse(data);
}

const SseProvider = ({ url, Context, buildFetchApi, children, ...rest }: React.PropsWithChildren<SseProviderProps>) => {

  const connections = useRef<Record<SseConnectionID, OnMessageSignature>>({});
  const abortController = useRef<AbortController | undefined>(undefined);
  const closeConnectionTimer = useRef<Timeout | undefined>(undefined);
  const retryConnectTimer = useRef<Timeout | undefined>(undefined);
  
  const releaseConnection: ReleaseConnectionFunction = (connectionId) => {
    delete connections.current[connectionId];
    if (Object.keys(connections.current).length === 0) {
      if (closeConnectionTimer.current !== undefined) {
        window.clearTimeout(closeConnectionTimer.current);
      }
      closeConnectionTimer.current = window.setTimeout(() => {
          abortController.current?.abort();
          abortController.current = undefined;
          closeConnectionTimer.current = undefined;
        }, 5000); // close connection after 5 seconds
    }
  };
  
  const buildEventSource = () => {
    abortController.current = new AbortController();
    fetchEventSource(
        url, {
          // since we use locking fetch-api the server has to sent an
          // ping-message immediatelly, to make the client release the lock
          fetch: buildFetchApi(),
          headers: {
            "cache-control": "no-cache"
          },
          signal: abortController.current.signal,
          onmessage: ev => onmessage(ev, connections.current),
          openWhenHidden: true,
          onclose: () => {
              throw new Error(); // if the server closes the connection unexpectedly, retry in "onerror"
            },
          onerror: (error) => {
              // @ts-ignore
              throw new Error('retry', { cause: error });
            },
          ...rest,
        }).catch((reason) => {
          if (reason.message === 'retry') {
            console.warn('Lost server connection, will rety in 15 seconds', reason.cause);
            retryConnectTimer.current = window.setTimeout(buildEventSource, 15000);
            return;
          }
          console.log('Lost server connection reason and won\'t retry', reason);
        });
  };
  
  const getConnection: GetConnectionFunction = (onMessage, messageName) => {
    if (closeConnectionTimer.current !== undefined) {
      window.clearTimeout(closeConnectionTimer.current);
      closeConnectionTimer.current = undefined;
    }
    if (abortController.current === undefined) {
      buildEventSource();
    }

    const connectionId = new Date().getTime().toString();
    connections.current = {
        ...connections.current,
        [connectionId]: {
            onMessage,
            messageName
          }
      };
    
    return connectionId;
  };
  
  const wakeupSseCallback = () => {
      if (retryConnectTimer.current === undefined) {
        return;
      }
      window.clearTimeout(retryConnectTimer.current);
      retryConnectTimer.current = undefined;
      buildEventSource();
    };
  
  return (
      <Context.Provider
          value={ {
              wakeupSseCallback,
              getConnection,
              releaseConnection,
            } }>
        {children}
      </Context.Provider>
    );
  
};

const useSse = <T, >(
  Context: Context<SseContextInterface>,
  onMessage: OnMessageFunction<T>,
  messageName?: string | RegExp
): WakeupSseCallback => {
  
  const sseContext = useContext(Context);
  
  useEffect(() => {
      const connectionId = sseContext.getConnection(onMessage, messageName);
      return () => sseContext.releaseConnection(connectionId);
    },
    [ sseContext, onMessage, messageName ]);
  
  return sseContext.wakeupSseCallback;
  
};

export { SseProvider, useSse };
