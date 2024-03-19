import { Box, RadioButtonGroup, TextInput } from "grommet";
import { BcWorkflow, Column, DefaultListCell, ListItemStatus, WorkflowListCell } from "@vanillabp/bc-shared";
import { ChangeEvent, useState } from "react";

const SESSION_STORAGE_KEY = "devShellColumns";

const FakeColumn = ({
  index,
  responsive,
  selected,
  setSelected,
  column,
  workflow,
  workflowColumns,
  WorkflowListCell,
}: {
  index: number,
  responsive: string,
  selected: boolean,
  setSelected: (select: boolean) => void,
  column: string,
  workflow: BcWorkflow,
  workflowColumns: Column[] | undefined,
  WorkflowListCell: WorkflowListCell,
}) => {
  const workflowColumnFound = workflowColumns?.filter(c => c.path === column);
  if (workflowColumnFound?.length === 0) return <Box key={column}>{column} not found</Box>
  const workflowColumn = workflowColumnFound![0];
  const now = new Date();
  const adoptedWorkflow = {
    ...workflow,
    dueDate: index % 2 === 0
        ? undefined
        : new Date(now.getTime() + 24 * 3600000)
  };
  return (
      <Box
          key={workflowColumn.path}
          width={workflowColumn.width !== '' ? workflowColumn.width : "100%"}>
        <WorkflowListCell
            currentLanguage="de"
            t={ key => key }
            defaultCell={ DefaultListCell }
            isPhone={ responsive === 'phone' }
            isTablet={ responsive == 'tablet' }
            item={ {
              id: adoptedWorkflow.id,
              data: adoptedWorkflow,
              read: index % 2 === 0 ? new Date() : undefined,
              status: ListItemStatus.INITIAL,
              number: index,
              selected
            } }
            selectItem={ select => setSelected(select) }
            column={ workflowColumn } />
      </Box>);
}

const List = ({
  workflow,
  workflowColumns,
  WorkflowListCell,
}: {
  workflow: BcWorkflow,
  workflowColumns: Column[] | undefined,
  WorkflowListCell: WorkflowListCell,
}) => {
  const defaultColumns = workflowColumns?.map(column => column.path).join(", ") ?? null;
  const [ columns, setColumns ] = useState<string | null>(
      window.sessionStorage.getItem(SESSION_STORAGE_KEY) ?? defaultColumns);
  const columnsChanged = (event: ChangeEvent<HTMLInputElement>) => {
    const newColumns = event.target.value;
    window.sessionStorage.setItem(SESSION_STORAGE_KEY, newColumns);
    setColumns(newColumns);
  };
  const [ selected , _setSelected ] = useState<Array<string>>(new Array<string>());
  const setSelected = (id: string, select: boolean) => _setSelected(
      [ ...selected.filter(item => item !== id), ...(select ? [ id ] : []) ]);
  const [ responsive, setResponsive ] = useState<string>('computer');

  const generateColumns = (index: number) =>
      columns
          ?.split(",")
          .map(column => column.trim())
          .filter(column => column.length > 0)
          .map((column) => <FakeColumn
              key={ column }
              index={ index }
              responsive={ responsive }
              selected={ selected.filter(item => item === workflow.id).length !== 0 }
              setSelected={ (select: boolean) => setSelected(workflow.id, select) }
              column={ column }
              workflow={ workflow }
              workflowColumns={ workflowColumns }
              WorkflowListCell={ WorkflowListCell } />);

  return (
      <Box
          direction="column"
          justify="start"
          gap="small">
        <Box
            background="grey"
            gap="small"
            pad="small">
          <Box
              direction="row"
              gap="small">
            <i>Default columns</i>:
            { defaultColumns }
          </Box>
          <TextInput
              value={ columns! }
              onChange={ columnsChanged }
              placeholder="enter list of columns (path) as a comma separated list" />
          <Box
              direction="column"
              gap="small">
            <RadioButtonGroup
                direction="row"
                name="responsive"
                options={ [ 'phone', 'tablet', 'computer' ] }
                value={ responsive }
                onChange={(event) => setResponsive(event.target.value)}
            />
          </Box>
        </Box>
        <Box
            direction="column">
          <Box
              fill="horizontal"
              direction="row"
              justify="start">
            {
              generateColumns(0)
            }
          </Box>
          <Box
              fill="horizontal"
              direction="row"
              justify="start">
            {
              generateColumns(1)
            }
          </Box>
        </Box>
      </Box>
    );
}

export { List }
