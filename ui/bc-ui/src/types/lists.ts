import { ReactNode } from "react";
import { UserTask } from "@vanillabp/bc-official-gui-client";
import { Column } from "@vanillabp/bc-shared";

export type ListOfTasksHeaderFooterFunction = (
    isPhone: boolean,
    isTablet: boolean,
    numberOfTasks: number,
    columns: Column[] | undefined,
    sort: string | undefined,
    setSort: (column?: Column) => void,
    sortAscending: boolean,
    setSortAscending: (sortAscending: boolean) => void,
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

export type ClaimTaskFunction = (userTask: UserTask, unclaim: boolean) => void;


export type ListOfWorkflowsHeaderFooterFunction = (
    isPhone: boolean,
    isTablet: boolean,
    numberOfWorkflows: number,
    columns: Column[] | undefined,
    sort: string | undefined,
    setSort: (column?: Column) => void,
    sortAscending: boolean,
    setSortAscending: (sortAscending: boolean) => void,
    selectAll: (select: boolean) => void,
    allSelected: boolean,
    refresh: () => void,
    refreshDisabled: boolean,
    initialKwicQuery: (columnPath?: string) => string,
    limitListToKwic: (columnPath: string | undefined, query?: string) => void,
    kwic: (columnPath: string | undefined, query: string) => Promise<Array<{ item: string, count: number }>>,
) => ReactNode | undefined;
