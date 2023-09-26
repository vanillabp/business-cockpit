import { Toast } from "@vanillabp/bc-shared";
import { TFunction } from "i18next";
import { NavigateFunction } from "react-router-dom";
import { UserTask } from "@vanillabp/bc-official-gui-client";

export type OpenTaskFunction = (
    userTask: UserTask,
    toast: (toast: Toast) => void,
    t: TFunction
) => void;

export type NavigateToWorkflowFunction = (
    userTask: UserTask,
    toast: (toast: Toast) => void,
    t: TFunction,
    navigate: NavigateFunction,
) => void;
