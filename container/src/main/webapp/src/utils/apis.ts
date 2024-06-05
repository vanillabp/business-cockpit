import { MutableRefObject, useMemo } from "react";
import { WakeupSseCallback } from "@vanillabp/bc-shared";
import { OfficialTasklistApi, OfficialWorkflowlistApi } from "@vanillabp/bc-official-gui-client";
import { useAppContext } from "../AppContext";
import { getTasklistGuiApi, getWorkflowlistGuiApi } from "../client/guiClient";

const useWorkflowlistApi = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): OfficialWorkflowlistApi => {

  const { toast } = useAppContext();
  const api = useMemo(
      () => getWorkflowlistGuiApi(
          toast,
          wakeupSseCallback?.current),
      [ toast, wakeupSseCallback ]
  );
  return api;

};

const useTasklistApi = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): OfficialTasklistApi => {

  const { toast } = useAppContext();
  const api = useMemo(() => getTasklistGuiApi(toast, wakeupSseCallback?.current), [ toast, wakeupSseCallback ]);
  return api;

};

const useSpecializedTasklistApi = (kind: string, wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): OfficialTasklistApi => {

  const { toast } = useAppContext();
  return useMemo(() => getTasklistGuiApi(toast, wakeupSseCallback?.current, kind), [ kind, toast, wakeupSseCallback ]);

};

export {
  useWorkflowlistApi,
  useTasklistApi,
  useSpecializedTasklistApi,
};
