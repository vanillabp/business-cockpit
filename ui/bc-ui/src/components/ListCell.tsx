import { ListItem, ListItemData, Module, ModuleDefinition } from '../index.js';
import {
  Column,
  DefaultListCellAwareProps,
  DefaultListCellProps,
  TranslationFunction,
  useResponsiveScreen,
  WarningListCell,
} from '@vanillabp/bc-shared';
import { FC, memo } from "react";

export enum TypeOfItem {
  TaskList,
  WorkflowList
}

const isVanillaBpColumn = (column: Column) => {
  return column.path === 'id'
      || column.path === 'assignee'
      || column.path === 'candidateUsers'
      || column.path === 'title';
}

interface Warnings {
  [key: string]: boolean;
}
const warningsForUserTaskCells: Warnings = {};
const warningsForWorkflowCells: Warnings = {};

interface ListCellParameters<T extends ListItemData & ModuleDefinition, > {
  modulesAvailable: Module[];
  column: Column;
  currentLanguage: string;
  nameOfList?: string;
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
  currentLanguage,
  nameOfList,
  typeOfItem,
  item,
  t,
  showUnreadAsBold,
  defaultListCell,
  selectItem,
}: ListCellParameters<T>) => {
  const { isPhone, isTablet } = useResponsiveScreen();

  const module = modulesAvailable.find((module => item.data.workflowModuleId === module.workflowModuleId));
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
      if (warningsForUserTaskCells[item.data.workflowModuleId] === undefined) {
        console.info(`Workflow-module ${ item.data.workflowModuleId } has no UserTaskListCell defined!`);
        warningsForUserTaskCells[item.data.workflowModuleId] = true;
      }
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
      if (warningsForWorkflowCells[item.data.workflowModuleId] === undefined) {
        console.info(`Workflow-module ${ item.data.workflowModuleId } has no WorkflowListCell defined!`);
        warningsForWorkflowCells[item.data.workflowModuleId] = true;
      }
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
            t={ t }
            item={ item }
            column={ column }
            showUnreadAsBold={ showUnreadAsBold }
            currentLanguage={ currentLanguage }
            defaultCell={ defaultListCell }
            nameOfList={ nameOfList }
            isPhone={ isPhone }
            isTablet={ isTablet }
            selectItem={ (select: boolean) => selectItem(item, select) } />;
  
}

export { ListCell };
