import React, { useCallback, useMemo } from 'react';
import { User, LoginApi, AppInformation } from './client/gui';
import { getLoginGuiApi } from './client/guiClient';
import { StatusType } from 'grommet';

type Action =
    | { type: 'updateAppInformation', appInformation: AppInformation | null }
    | { type: 'updateCurrentUser', user: User | null }
    | { type: 'showMenu', visibility: boolean }
    | { type: 'loadingIndicator', show: boolean }
    | { type: 'toast', toast: Toast | undefined }
    | { type: 'updateTitle', title: string, intern?: boolean };
export type Dispatch = (action: Action) => void;
export type Toast = {
  namespace: string;
  title: string | undefined | null;
  message: string;
  status?: StatusType;
  timeout?: number;
};
type State = {
  appInformation: AppInformation | null;
  currentUser: User | null | undefined;
  showMenu: boolean;
  title: string;
  intern: boolean;
  toast: Toast | undefined;
  loadingIndicator: boolean;
};

const AppContext = React.createContext<{
  state: State;
  dispatch: Dispatch;
  toast: (toast: Toast) => void;
  fetchAppInformation: () => void;
  fetchCurrentUser: (resolve: (value: User | null) => void, reject: (error: any) => void, forceUpdate?: boolean) => void;
  showMenu: (visibility: boolean) => void;
  setAppHeaderTitle: (title: string, intern?: boolean) => void;
  showLoadingIndicator: (show: boolean) => void;
} | undefined>(undefined);

const appContextReducer: React.Reducer<State, Action> = (state, action) => {
  let newState: State;
  switch (action.type) {
  case 'updateCurrentUser':
    newState = {
      ...state,
      currentUser: action.user,
    };
    break;
  case 'updateAppInformation':
    newState = {
      ...state,
      appInformation: action.appInformation,
    };
    break;
  case 'showMenu': {
    newState = {
      ...state,
      showMenu: action.visibility,
    };
    break;
  }
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
  case 'updateTitle': {
    newState = {
      ...state,
      title: action.title,
      intern: action.intern ? true : false,
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
    currentUser: undefined,
    appInformation: null,
    showMenu: false,
    title: 'app',
    toast: undefined,
    intern: false,
    loadingIndicator: false,
  });

  const loginApi = useMemo(() => getLoginGuiApi(dispatch), [ dispatch ]);
  
  const fetchAppInformation = useCallback(() => fetchAppInformationFromLoginApi(state.appInformation, dispatch, loginApi),
      [ loginApi, state.appInformation ]);
  const fetchCurrentUser = useCallback((resolve: (value: User | null) => void, reject: (error: any) => void, forceUpdate?: boolean) => fetchCurrentUserFromGui(state.currentUser, dispatch, loginApi, resolve, reject, forceUpdate),
      [ loginApi, state.currentUser ]);
  const showMenu = useCallback((visibility: boolean) => setShowMenu(dispatch, visibility),
      [ dispatch ]);
  const setAppHeaderTitle = useCallback((title: string, intern?: boolean) => updateTitle(dispatch, title, intern),
      [ dispatch ]); 
  const showLoadingIndicator = useCallback((show: boolean) => setLoadingIndicator(dispatch, show),
      [ dispatch ]);

  const value = {
    state,
    dispatch,
    loginApi,
    toast: (t: Toast) => dispatch({ type: 'toast', toast: t }),
    fetchAppInformation,
    fetchCurrentUser,
    showMenu,
    showLoadingIndicator,
    setAppHeaderTitle,
  };
  
  return (<AppContext.Provider value={value}>
     {children}
   </AppContext.Provider>);
};

const fetchAppInformationFromLoginApi = async (appInformation: AppInformation | null, dispatch: Dispatch, loginApi: LoginApi) => {
  if (appInformation !== null) {
    return;
  }
  try {
    const appInformation = await loginApi.appInformation();
    dispatch({ type: 'updateAppInformation', appInformation });
    window.document.title = appInformation.titleLong;
  } catch (error) {
    console.error(error);
  }
}

const fetchCurrentUserFromGui = async (currentUser: User | null | undefined,
    dispatch: Dispatch,
    loginApi: LoginApi,
    resolve: (value: User | null) => void,
    reject: (error: any) => void,
    forceUpdate?: boolean) => {
  if (!forceUpdate && currentUser != null) {
    resolve(currentUser);
    return;
  }
  try {
    const user = await loginApi.currentUser();
    dispatch({ type: 'updateCurrentUser', user });
    resolve(user);
  } catch (error: any) {
    if (error['status'] !== 404) {
      reject(error);
    }
    dispatch({ type: 'updateCurrentUser', user: null });
    resolve(null);
  }
}

const setShowMenu = (dispatch: Dispatch, visibility: boolean) => {
  dispatch({ type: 'showMenu', visibility });
}

const setLoadingIndicator = (dispatch: Dispatch, show: boolean) => {
  dispatch({ type: 'loadingIndicator', show });
}

const updateTitle = (dispatch: Dispatch, title: string, intern?: boolean) => {
  dispatch({ type: 'updateTitle', title, intern });
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
  AppContextProvider,
  useAppContext,
  supportSuspense,
}
