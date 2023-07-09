import React from 'react';
import { Column, Module } from '../utils/module-federation';
import { Data, ListItem } from './SearchableAndSortableUpdatingList';
import { Box, Text } from 'grommet';
import i18n from '../i18n';
import { useTranslation } from 'react-i18next';
import { Alert, StatusCritical } from 'grommet-icons';
import { useResponsiveScreen } from '@vanillabp/bc-shared';

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

interface TaskAwareData extends WorkflowAwareData {
  taskDefinition: string;
}

interface WorkflowAwareData extends Data {
  workflowModule: string;
  bpmnProcessId: string;
}

interface ListCellParameters<T extends WorkflowAwareData> {
  modulesAvailable: Module[];
  column: Column;
  defaultLanguage?: string;
  currentLanguage: string;
  typeOfItem: TypeOfItem;
  item: ListItem<T>;
}

const ListCell = <T extends WorkflowAwareData>({
  modulesAvailable,
  column,
  defaultLanguage = 'en',
  currentLanguage,
  typeOfItem,
  item,
}: ListCellParameters<T>) => {
  
  const { isNotPhone } = useResponsiveScreen();
  const { t } = useTranslation('listcell');
  
  const module = modulesAvailable.find((module => item.data.workflowModule === module.moduleId));
  if (module === undefined) {
    return (
        <Box
            direction='row'
            justify='end'>
          <StatusCritical color='status-critical' />
          {
            isNotPhone
                ? <Text truncate="tip">{ t('workflowmodule_unknown')}</Text>
                : undefined
          }
        </Box>);
  }
  
  if (module.retry) {
    return (
        <Box
            border={ { color: 'status-warning', size: '1px' } }
            direction='row'
            justify='end'>
          <Alert color='status-warning' />
          {
            isNotPhone
                ? <Text truncate="tip">{ t('workflowmodule_retry')}</Text>
                : undefined
          }
        </Box>);
  }
  
  if (typeOfItem !== TypeOfItem.TaskList) {
    return (
        <Box
            border={ { color: 'status-warning', size: '1px' } }
            direction='row'
            justify='end'>
          <Alert color='status-warning' />
          {
            isNotPhone
                ? <Text truncate="tip">{ t('typeofitem_unsupported')}</Text>
                : undefined
          }
        </Box>);
  }

  if (!Boolean(module.TaskListCell)) {
    console.warn(`Workflow-module ${module.moduleId} has no TaskListCell defined!`);
    return (
        <Box
            border={ { color: 'status-warning', size: '1px' } }
            direction='row'
            justify='end'>
          <Alert color='status-warning' />
          {
            isNotPhone
                ? <Text truncate="tip">{ t('typeofitem_unsupported')}</Text>
                : undefined
          }
        </Box>);
  }
  
  const Cell = module.TaskListCell!;
  const data = item.data as unknown as TaskAwareData;
  
  return <Cell
            bpmnProcessId={ data.bpmnProcessId }
            taskDefinition={ data.taskDefinition }
            path={ column.path } />;
  
}

export { ListCell };
