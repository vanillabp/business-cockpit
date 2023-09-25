import { useMemo, MutableRefObject } from 'react';
import { useAppContext } from '../../AppContext';
import { getWorkflowlistGuiApi } from '../../client/guiClient';
import { WorkflowlistApi } from '../../client/gui';
import { WakeupSseCallback } from '@vanillabp/bc-shared';

const useWorkflowlistApi = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): WorkflowlistApi => {

  const { dispatch } = useAppContext();
  const api = useMemo(
      () => getWorkflowlistGuiApi(
          dispatch,
          wakeupSseCallback?.current),
      [ dispatch, wakeupSseCallback ]
  );
  return api;
  
};

export {
  useWorkflowlistApi,
};
