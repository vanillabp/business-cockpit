import React from 'react';
import { bpmnProcessId as Test_bpmnProcessId, workflowListColumns as Test_workflowListColumns } from './Test';
import { WarningListCell, WorkflowListCell, ColumnsOfWorkflowFunction } from '@vanillabp/bc-shared';

//@ts-expect-error
const buildVersion = process.env.BUILD_VERSION;
//@ts-expect-error
const buildTimestamp = new Date(process.env.BUILD_TIMESTAMP);

const workflowListColumns: ColumnsOfWorkflowFunction = workflow => {
  if (workflow.bpmnProcessId === Test_bpmnProcessId) {
    return Test_workflowListColumns;
  }
  return undefined;
};

const WorkflowListCellComponent: WorkflowListCell = ({
  item,
  column,
  defaultCell
}) => {
  const DefaultCell = defaultCell;
  return item.data.bpmnProcessId === Test_bpmnProcessId
      ? <DefaultCell item={ item } column={ column } />
      : <WarningListCell message={ `unknown BPMN process ID '${item.data.bpmnProcessId}'` } />;
}

export {
  buildVersion,
  buildTimestamp,
  workflowListColumns,
  WorkflowListCellComponent as WorkflowListCell
};
