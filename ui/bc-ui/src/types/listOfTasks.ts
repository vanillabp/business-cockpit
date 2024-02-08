import { ReactNode } from "react";
import { UserTask } from "@vanillabp/bc-official-gui-client";

export type ListOfTasksHeaderFooterFunction = (
    isPhone: boolean,
    isTablet: boolean,
    numberOfTasks: number,
    selectAll: (select: boolean) => void,
    allSelected: boolean,
    refresh: () => void,
    refreshDisabled: boolean,
    markAsRead: () => void,
    markAsUnread: () => void,
    markAsReadDisabled: boolean,
    claimTasks: () => void,
    unclaimTasks: () => void,
    claimTasksDisabled: boolean,
    assignTasks: (userId: string) => void,
    assignDisabled: boolean,
) => ReactNode | undefined;

export type AssignTaskFunction = (userTask: UserTask, userId: string, unassign: boolean) => void;
