import { ListItem, ListItemData, Module, ModuleDefinition } from '../index.js';
import { Column, DefaultListCellAwareProps, DefaultListCellProps, WarningListCell } from '@vanillabp/bc-shared';
import { FC, memo } from "react";
import { TranslationFunction } from "../types/translate";

export enum TypeOfItem {
  TaskList,
  WorkflowList
}

const isVanillaBpColumn = (column: Column) => {
  return column.path === 'id'
      || column.path === 'assignee'
      || column.path === 'candidateUsers'
      || column.path.startsWith('title.');
}

interface ListCellParameters<T extends ListItemData & ModuleDefinition, > {
  modulesAvailable: Module[];
  column: Column;
  defaultLanguage?: string;
  currentLanguage: string;
  typeOfItem: TypeOfItem;
  item: ListItem<T>;
  t: TranslationFunction;
  defaultListCell: FC<DefaultListCellProps<T>>;
  showUnreadAsBold?: boolean;
  selectItem: (item: ListItem<T>, select: boolean) => void;
}

const ListCell = <T extends ListItemData & ModuleDefinition, >({
  modulesAvailable,
  column,
  defaultLanguage = 'en',
  currentLanguage,
  typeOfItem,
  item,
  t,
  showUnreadAsBold,
  defaultListCell,
  selectItem,
}: ListCellParameters<T>) => {

  const module = modulesAvailable.find((module => item.data.workflowModule === module.workflowModule));
  if ((module === undefined)
      && !isVanillaBpColumn(column)) {
    return <WarningListCell
        error={ true }
        message={ t('module-unknown') } />;
  }
  
  if (module?.retry
      && !isVanillaBpColumn(column)) {
    return <WarningListCell
        message={ t('retry-loading-module-hint') } />;
  }
  
  let Cell: FC<DefaultListCellAwareProps<any>>;
  if (typeOfItem === TypeOfItem.TaskList) {
    if (!Boolean(module?.UserTaskListCell)) {
      console.warn(`Workflow-module ${item.data.workflowModule} has no UserTaskListCell defined!`);
      Cell = defaultListCell;
    } else {
      Cell = memo(
          module!.UserTaskListCell!,
          (prevProps, nextProps) => {
            if (prevProps.column !== nextProps.column) return false;
            if (prevProps.showUnreadAsBold !== nextProps.showUnreadAsBold) return false;
            if (prevProps.item.id !== nextProps.item.id) return false;
            return (prevProps.item?.data.version === nextProps.item?.data.version);
          });
    }
  } else if (typeOfItem === TypeOfItem.WorkflowList) {
    if (!Boolean(module?.WorkflowListCell)) {
      console.warn(`Workflow-module ${item.data.workflowModule} has no WorkflowListCell defined!`);
      Cell = defaultListCell;
    } else {
      Cell = memo(
          module!.WorkflowListCell!,
          (prevProps, nextProps) => {
            if (prevProps.column !== nextProps.column) return false;
            if (prevProps.showUnreadAsBold !== nextProps.showUnreadAsBold) return false;
            if (prevProps.item.id !== nextProps.item.id) return false;
            return (prevProps.item?.data.version === nextProps.item?.data.version);
          });
    }
  } else {
    return <WarningListCell
        message={ t('typeofitem_unsupported') } />;
  }
  return <Cell
            item={ item }
            column={ column }
            showUnreadAsBold={ showUnreadAsBold }
            currentLanguage={ currentLanguage }
            defaultLanguage={ defaultLanguage }
            defaultCell={ defaultListCell }
            selectItem={ (select: boolean) => selectItem(item, select) } />;
  
}

export { ListCell };
