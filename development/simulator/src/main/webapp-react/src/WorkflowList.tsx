import { bpmnProcessId as Test_bpmnProcessId, workflowListColumns as Test_workflowListColumns } from './Test';
import { ColumnsOfWorkflowFunction, WarningListCell, WorkflowListCell } from '@vanillabp/bc-shared';

const buildVersion = process.env.BUILD_VERSION;
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
  defaultCell,
  ...props
}) => {
  const DefaultCell = defaultCell;
  return item.data.bpmnProcessId === Test_bpmnProcessId
      ? <DefaultCell item={ item } column={ column } { ...props } />
      : <WarningListCell message={ `unknown BPMN process ID '${item.data.bpmnProcessId}'` } />;
}

export {
  buildVersion,
  buildTimestamp,
  workflowListColumns,
  WorkflowListCellComponent as WorkflowListCell
};
