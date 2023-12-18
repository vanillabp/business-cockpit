import { MutableRefObject, useMemo } from "react";
import { WakeupSseCallback } from "@vanillabp/bc-shared";
import { OfficialTasklistApi, OfficialWorkflowlistApi } from "@vanillabp/bc-official-gui-client";
import { useAppContext } from "../AppContext";
import { getTasklistGuiApi, getWorkflowlistGuiApi } from "../client/guiClient";

const useWorkflowlistApi = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): OfficialWorkflowlistApi => {

  const { dispatch } = useAppContext();
  const api = useMemo(
      () => getWorkflowlistGuiApi(
          dispatch,
          wakeupSseCallback?.current),
      [ dispatch, wakeupSseCallback ]
  );
  return api;

};

const useTasklistApi = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): OfficialTasklistApi => {

  const { dispatch } = useAppContext();
  const api = useMemo(() => getTasklistGuiApi(dispatch, wakeupSseCallback?.current), [ dispatch, wakeupSseCallback ]);
  return api;

};

const useSpecializedTasklistApi = (kind: string, wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): OfficialTasklistApi => {

  const { dispatch } = useAppContext();
  return useMemo(() => getTasklistGuiApi(dispatch, wakeupSseCallback?.current, kind), [ kind, dispatch, wakeupSseCallback ]);

};

export {
  useWorkflowlistApi,
  useTasklistApi,
  useSpecializedTasklistApi,
};
