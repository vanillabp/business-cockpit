import { ListItem, ListItemData, Module, ModuleDefinition } from '../index.js';
import i18n from 'i18next';
import { useTranslation } from 'react-i18next';
import { Column, DefaultListCell, DefaultListCellAwareProps, WarningListCell } from '@vanillabp/bc-shared';
import { FC } from "react";

i18n.addResources('en', 'listcell', {
      "workflowmodule_unknown": "Unknown",
      "workflowmodule_retry": "Retry",
      "typeofitem_unsupported": "Wrong type",
    });
i18n.addResources('de', 'listcell', {
      "workflowmodule_unknown": "Unbekannt",
      "workflowmodule_retry": "Laden",
      "typeofitem_unsupported": "Typfehler",
    });

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
}

const ListCell = <T extends ListItemData & ModuleDefinition, >({
  modulesAvailable,
  column,
  defaultLanguage = 'en',
  currentLanguage,
  typeOfItem,
  item,
}: ListCellParameters<T>) => {
  
  const { t } = useTranslation('listcell');
  
  const module = modulesAvailable.find((module => item.data.workflowModule === module.workflowModule));
  if (module === undefined) {
    return <WarningListCell
        error={ true }
        message={ t('workflowmodule_unknown') } />;
  }
  
  if (module.retry) {
    return <WarningListCell
        message={ t('workflowmodule_retry') } />;
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
