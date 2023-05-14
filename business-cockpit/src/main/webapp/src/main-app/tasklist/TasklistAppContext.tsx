import { useMemo, MutableRefObject } from 'react';
import { useAppContext } from '../../AppContext';
import { getTasklistGuiApi } from '../../client/guiClient';
import { TasklistApi } from '../../client/gui';
import { WakeupSseCallback } from '@vanillabp/bc-shared';

const useTasklistApi = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): TasklistApi => {

  const { dispatch } = useAppContext();
  const api = useMemo(() => getTasklistGuiApi(dispatch, wakeupSseCallback?.current), [ dispatch, wakeupSseCallback ]);
  return api;
  
};

export {
    useTasklistApi,
  };
