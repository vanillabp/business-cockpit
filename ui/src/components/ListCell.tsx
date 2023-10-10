import { ListItem, ListItemData, Module, ModuleDefinition } from '../index.js';
import { Column, DefaultListCell, DefaultListCellAwareProps, WarningListCell } from '@vanillabp/bc-shared';
import { FC } from "react";
import { TranslationFunction } from "../types/translate";

export enum TypeOfItem {
  TaskList,
  WorkflowList
}

interface ListCellParameters<T extends ListItemData> {
  modulesAvailable: Module[];
  column: Column;
  defaultLanguage?: string;
  currentLanguage: string;
  typeOfItem: TypeOfItem;
  item: ListItem<T>;
  t: TranslationFunction;
}

const ListCell = <T extends ListItemData & ModuleDefinition, >({
  modulesAvailable,
  column,
  defaultLanguage = 'en',
  currentLanguage,
  typeOfItem,
  item,
  t
}: ListCellParameters<T>) => {
  
  const module = modulesAvailable.find((module => item.data.workflowModule === module.workflowModule));
  if (module === undefined) {
    return <WarningListCell
        error={ true }
        message={ t('module-unknown') } />;
  }
  
  if (module.retry) {
    return <WarningListCell
        message={ t('retry-loading-module-hint') } />;
  }
  
  let Cell: FC<DefaultListCellAwareProps<any>>;
  if (typeOfItem === TypeOfItem.TaskList) {
    if (!Boolean(module.UserTaskListCell)) {
      console.warn(`Workflow-module ${module.workflowModule} has no UserTaskListCell defined!`);
      return <WarningListCell
          message={ t('typeofitem_unsupported') } />;
    }
    Cell = module.UserTaskListCell!;
  } else if (typeOfItem === TypeOfItem.WorkflowList) {
    if (!Boolean(module.WorkflowListCell)) {
      console.warn(`Workflow-module ${module.workflowModule} has no UserTaskListCell defined!`);
      return <WarningListCell
          message={ t('typeofitem_unsupported') } />;
    }
    Cell = module.WorkflowListCell!;
  } else {
    return <WarningListCell
        message={ t('typeofitem_unsupported') } />;
  }
  
  return <Cell
            item={ item }
            column={ column }
            defaultCell={ DefaultListCell } />;
  
}

export { ListCell };
