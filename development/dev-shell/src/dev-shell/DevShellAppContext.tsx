import React, { useCallback, Dispatch } from 'react';
import { Toast, ToastAction } from '@bc/shared';

type Action =
    | { type: 'loadingIndicator', show: boolean }
    | ToastAction;
    
type State = {
  toast: Toast | undefined;
  loadingIndicator: boolean;
};

const AppContext = React.createContext<{
  state: State;
  dispatch: Dispatch<Action>;
  toast: (toast: Toast) => void;
  showLoadingIndicator: (show: boolean) => void;
} | undefined>(undefined);

const appContextReducer: React.Reducer<State, Action> = (state, action) => {
  let newState: State;
  switch (action.type) {
  case 'loadingIndicator': {
    newState = {
      ...state,
      loadingIndicator: action.show,
    };
    break;
  }
  case 'toast': {
    newState = {
      ...state,
      toast: action.toast,
    };
    break;
  }
  default: throw new Error(`Unhandled app-context action-type: ${action}`);
  }
  return newState;
};

type AppContextProviderProps = {
  children?: React.ReactNode;
};

const AppContextProvider = ({ children }: AppContextProviderProps) => {
  const [state, dispatch] = React.useReducer(appContextReducer, {
    toast: undefined,
    loadingIndicator: false,
  });

  const showLoadingIndicator = useCallback((show: boolean) => setLoadingIndicator(dispatch, show),
      [ dispatch ]);

  const value = {
    state,
    dispatch,
    toast: (t: Toast) => dispatch({ type: 'toast', toast: t }),
    showLoadingIndicator,
  };
  
  return (<AppContext.Provider value={value}>
     {children}
   </AppContext.Provider>);
};

const setLoadingIndicator = (dispatch: Dispatch<Action>, show: boolean) => {
  dispatch({ type: 'loadingIndicator', show });
}

const useAppContext = () => {
  const context = React.useContext(AppContext);
  if (context === undefined) {
    throw new Error('useAppContext must be used within a <AppContext>...</AppContext>');
  }
  return context;
}

// see https://blog.logrocket.com/react-suspense-data-fetching/
const supportSuspense = <T extends any>(promise: Promise<T>) => {
  
  let status = 'pending';
  let response: T;

  const suspender = promise.then(
    (res) => {
      status = 'success';
      response = res;
    },
    (err) => {
      status = 'error';
      response = err;
    },
  );
  
  const read = () => {
    switch (status) {
      case 'pending':
        throw suspender;
      case 'error':
        throw response;
      default:
        return response;
    }
  }

  // eslint-disable-next-line
  return useCallback(read, [ ]);

}
  
export {
  useAppContext,
  AppContextProvider,
  supportSuspense
}
