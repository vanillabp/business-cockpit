import { MutableRefObject, useMemo } from "react";
import { WakeupSseCallback } from "@vanillabp/bc-shared";
import {
  OfficialTasklistApi,
  OfficialWorkflowlistApi,
  OfficialWorkflowModulesApi
} from "@vanillabp/bc-official-gui-client";
import { useAppContext } from "../AppContext";
import { getTasklistGuiApi, getWorkflowlistGuiApi, getWorkflowModulesGuiApi } from "../client/guiClient";

const useWorkflowModulesApi = (wakeupSseCallback?: MutableRefObject<WakeupSseCallback>): OfficialWorkflowModulesApi => {

  const { toast } = useAppContext();
  const api = useMemo(
      () => getWorkflowModulesGuiApi(
          toast,
          wakeupSseCallback?.current),
      [ toast, wakeupSseCallback ]
  );
  return api;

};

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
  useWorkflowModulesApi,
};
