import React, { useState, useRef, useMemo, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import i18n from '../../i18n';
import { ListItem, ListItems, ReloadCallbackFunction, SearchableAndSortableUpdatingList } from '../../components/SearchableAndSortableUpdatingList';
import { useWorkflowlistApi } from "./WorkflowlistAppContext";
import { useGuiSse } from '../../client/guiClient';
import { Grid, Box, CheckBox, ColumnConfig, Text } from 'grommet';
import { BcUserTask, BcWorkflow, Column, EventMessage, GetUserTasksFunction, ListItemStatus, colorForEndedItemsOrUndefined, useResponsiveScreen } from "@vanillabp/bc-shared";
import { EventSourceMessage, WakeupSseCallback } from '@vanillabp/bc-shared';
import { Link, } from '@vanillabp/bc-shared';
import { WorkflowlistApi, Workflow, WorkflowEvent } from "../../client/gui";
import { useAppContext } from '../../AppContext';
import { ModuleDefinition, useFederationModules } from '../../utils/module-federation';
import { ListCell, TypeOfItem } from '../../components/ListCell';
import i18next from 'i18next';
import { useNavigate } from 'react-router-dom';
import { openTask } from '../../utils/navigate';

i18n.addResources('en', 'workflowlist/list', {
      "total": "Total:",
      "no": "No.",
      "name": "Workflow",
      "project": "Project",
      "gremium": "Committee",
      "unsupported-ui-uri-type_title": "Open workflow",
      "unsupported-ui-uri-type_message": "Internal error: The workflow refers to an unsupported UI-URI-type!",
    });
i18n.addResources('de', 'workflowlist/list', {
      "total": "Anzahl:",
      "no": "Nr.",
      "name": "Workflow",
      "project": "Projekt",
      "gremium": "Gremium",
      "unsupported-ui-uri-type_title": "Workflow öffnen",
      "unsupported-ui-uri-type_message": "Internes Problem: Der Workflow bezieht sich auf einen nicht unterstützten UI-URI-Typ!",
    });

const loadWorkflows = async (
  workflowlistApi: WorkflowlistApi,
  setNumberOfWorkflows: (number: number) => void,
  pageSize: number,
  pageNumber: number,
  initialTimestamp: Date | undefined,
  mapToBcWorkflow: (workflow: Workflow) => BcWorkflow,
): Promise<ListItems<Workflow>> => {
  const result = await workflowlistApi
        .getWorkflows({ pageNumber, pageSize, initialTimestamp });

  setNumberOfWorkflows(result!.page.totalElements);

  return {
      serverTimestamp: result.serverTimestamp,
      items: result.workflows.map(workflow => mapToBcWorkflow(workflow))
    };
};

const reloadWorkflows = async (
  workflowlistApi: WorkflowlistApi,
  setNumberOfWorkflows: (number: number) => void,
  existingModuleDefinitions: Workflow[] | undefined,
  setModulesOfWorkflows: (modules: Workflow[] | undefined) => void,
  existingWorkflowDefinitions: DefinitionOfWorkflow | undefined,
  setDefinitionsOfWorkflows: (definitions: DefinitionOfWorkflow | undefined) => void,
  numberOfItems: number,
  knownItemsIds: Array<string>,
  initialTimestamp: Date | undefined,
  mapToBcWorkflow: (workflow: Workflow) => BcWorkflow,
): Promise<ListItems<Workflow>> => {

  const result = await workflowlistApi.getWorkflowsUpdate({
      workflowsUpdate: {
          size: numberOfItems,
          knownWorkflowIds: knownItemsIds,
          initialTimestamp
        }
    })

  setNumberOfWorkflows(result!.page.totalElements);

  const newModuleDefinitions = result
      .workflows
      .filter(workflow => workflow.workflowModule !== undefined)
      .reduce((moduleDefinitions, workflow) => moduleDefinitions.includes(workflow)
          ? moduleDefinitions : moduleDefinitions.concat(workflow), existingModuleDefinitions || []);
  if (existingModuleDefinitions?.length !== newModuleDefinitions.length) {
    setModulesOfWorkflows(newModuleDefinitions);
    const newWorkflowDefinitions: DefinitionOfWorkflow = { ...existingWorkflowDefinitions };
    result
        .workflows
        .forEach(workflow => newWorkflowDefinitions[`${workflow.workflowModule}#${workflow.bpmnProcessId}`] = workflow);
    if ((existingWorkflowDefinitions === undefined)
        || Object.keys(existingWorkflowDefinitions).length !== Object.keys(newWorkflowDefinitions).length) {
      setDefinitionsOfWorkflows(newWorkflowDefinitions);
    }
  }
  
  return {
      serverTimestamp: result.serverTimestamp,
      items: result.workflows.map(workflow => mapToBcWorkflow(workflow))
    };
};

interface DefinitionOfWorkflow {
  [key: string]: Workflow;
}

const ListOfWorkflows = () => {

  const { isNotPhone } = useResponsiveScreen();
  const { t: tApp } = useTranslation('app');
  const { t } = useTranslation('workflowlist/list');
  const { toast, showLoadingIndicator } = useAppContext();
  const navigate = useNavigate();
  
  const wakeupSseCallback = useRef<WakeupSseCallback>(undefined);
  const workflowlistApi = useWorkflowlistApi(wakeupSseCallback);

  const updateListRef = useRef<ReloadCallbackFunction | undefined>(undefined);
  const updateList = useMemo(() => async (ev: EventSourceMessage<Array<EventMessage<WorkflowEvent>>>) => {
      if (!updateListRef.current) return;
      const listOfUpdatedWorkflows = ev.data.map(workflowEvent => workflowEvent.event.id);
      updateListRef.current(listOfUpdatedWorkflows);
    }, [ updateListRef ]);
  wakeupSseCallback.current = useGuiSse<Array<EventMessage<WorkflowEvent>>>(
      updateList,
      /^Workflow$/
  );

  const workflows = useRef<Array<ListItem<Workflow>> | undefined>(undefined);
  const [ numberOfWorkflows, setNumberOfWorkflows ] = useState<number>(-1);
  const [ modulesOfWorkflows, setModulesOfWorkflows ] = useState<Workflow[] | undefined>(undefined);
  const [ definitionsOfWorkflows, setDefinitionsOfWorkflows ] = useState<DefinitionOfWorkflow | undefined>(undefined);
  useEffect(() => {
      const loadMetaInformation = async () => {
        const result = await workflowlistApi
            .getWorkflows({ pageNumber: 0, pageSize: 100 });
        setNumberOfWorkflows(result.page.totalElements);
        const moduleDefinitions = result
            .workflows
            .reduce((moduleDefinitions, workflow) => moduleDefinitions.includes(workflow)
                ? moduleDefinitions : moduleDefinitions.concat(workflow), new Array<Workflow>());
        setModulesOfWorkflows(moduleDefinitions);
        const workflowDefinitions: DefinitionOfWorkflow = {};
        result
            .workflows
            .forEach(workflow => workflowDefinitions[`${workflow.workflowModule}#${workflow.bpmnProcessId}`] = workflow);
        setDefinitionsOfWorkflows(workflowDefinitions);
      };
      if (workflows.current === undefined) {
        showLoadingIndicator(true);
        loadMetaInformation();
      }
    }, [ workflows, workflowlistApi, setNumberOfWorkflows, setModulesOfWorkflows, setDefinitionsOfWorkflows, showLoadingIndicator ]);

  const [ columnsOfWorkflows, setColumnsOfWorkflows ] = useState<Array<Column> | undefined>(undefined); 
  const modules = useFederationModules(modulesOfWorkflows as Array<ModuleDefinition> | undefined, 'WorkflowList');
  useEffect(() => {
    if (modules === undefined) {
      return;
    }
    if (definitionsOfWorkflows === undefined) {
      return;
    }
    const totalColumns = Object
        .keys(definitionsOfWorkflows)
        .map(definition => {
          return definitionsOfWorkflows[definition]
          })
        .map(definition => {
            const columnsOfWorkflow = modules
                .filter(m => m !== undefined)
                .filter(m => m.workflowModule === definition.workflowModule)
                .filter(m => m.workflowListColumns !== undefined)
                .map(module => module.workflowListColumns!(definition));
            if (columnsOfWorkflow.length === 0) return undefined;
            return columnsOfWorkflow[0];
          })
        .filter(columnsOfWorkflow => columnsOfWorkflow !== undefined)
        .reduce((totalColumns, columnsOfWorkflow) => {
            columnsOfWorkflow!.forEach(column => { totalColumns[column.path] = column });
            return totalColumns;
          }, {});
    const existingColumnsSignature = columnsOfWorkflows === undefined
        ? ' ' // initial state is different then updates
        : columnsOfWorkflows.map(c => c.path).join('|');
    const orderedColumns = (Object
        .values(totalColumns) as Array<Column>)
        .sort((a, b) => a.priority - b.priority);
    const newColumnsSignature = orderedColumns.map(c => c.path).join('|');
    if (existingColumnsSignature === newColumnsSignature) {
      return;
    }
    setColumnsOfWorkflows(orderedColumns);
  }, [ modules, definitionsOfWorkflows, columnsOfWorkflows, setColumnsOfWorkflows ]);

  const openWorkflow = async (workflow: Workflow) => {
    navigate(`./${workflow.id}`);
  };
    
  const columns: ColumnConfig<ListItem<BcWorkflow>>[] =
      [
          { property: 'id',
            primary: true,
            pin: true,
            size: '2.2rem',
            plain: true,
            header: <Box
                        align="center">
                      <CheckBox />
                    </Box>,
            render: (_item: ListItem<BcWorkflow>) => (
                <Box
                    align="center">
                  <CheckBox />
                </Box>)
          },
          { property: 'name',
            header: t('name'),
            size: `calc(100% - 2.2rem${columnsOfWorkflows === undefined ? 'x' : columnsOfWorkflows!.reduce((r, column) => `${r} - ${column.width}`, '')})`,
            render: (item: ListItem<BcWorkflow>) => {
                const title = item.data['title'][i18next.language] || item.data['title']['en'];
                return (
                    <Box
                        fill
                        pad="xsmall">
                      <Text
                          color={ colorForEndedItemsOrUndefined(item) }
                          truncate="tip">
                        {
                          item.status === ListItemStatus.ENDED
                              ? <>{ title }</>
                              : <Link
                                    onClick={ () => openWorkflow(item.data) }>
                                  { title }
                                </Link>
                        }
                      </Text>
                    </Box>);
              }
          },
          ...(columnsOfWorkflows === undefined
              ? []
              : columnsOfWorkflows!.map(column => ({
                    property: column.path,
                    header: column.title[i18next.language] || column.title['en'],
                    size: column.width,
                    plain: true,
                    render: (item: ListItem<BcWorkflow>) => <ListCell
                                                              modulesAvailable={ modules! }
                                                              column={ column }
                                                              currentLanguage={ i18next.language }
                                                              typeOfItem={ TypeOfItem.WorkflowList }
                                                              item={ item } />
                  }))
          )
      ];
  
  const mapToBcWorkflow = (workflow: Workflow): BcWorkflow => {
      const getUserTasksFunction: GetUserTasksFunction = async (
          activeOnly,
          limitListAccordingToCurrentUsersPermissions
        ) => {
          return (await workflowlistApi
              .getUserTasksOfWorkflow({
                  workflowId: workflow.id,
                  activeOnly,
                  llatcup: limitListAccordingToCurrentUsersPermissions,
              }))
              .map(userTask => ({
                ...userTask,
                open: () => openTask(userTask, toast, tApp),
                navigateToWorkflow: () => {}, // don't change view because workflow is already shown
              } as BcUserTask));
        };
      return {
          ...workflow,
          getUserTasks: getUserTasksFunction,
        };
    };
  
  return (
      <Grid
          rows={ [ 'auto', '2rem' ] }
          fill>
        <Box>
          <SearchableAndSortableUpdatingList
              t={ t }
              columns={ columns }
              itemsRef={ workflows }
              updateListRef= { updateListRef }
              retrieveItems={ (pageNumber, pageSize, initialTimestamp) => 
// @ts-ignore
                  loadWorkflows(
                      workflowlistApi,
                      setNumberOfWorkflows,
                      pageSize,
                      pageNumber,
                      initialTimestamp,
                      mapToBcWorkflow) }
              reloadItems={ (numberOfItems, updatedItemsIds, initialTimestamp) =>
// @ts-ignore
                  reloadWorkflows(
                      workflowlistApi,
                      setNumberOfWorkflows,
                      modulesOfWorkflows,
                      setModulesOfWorkflows,
                      definitionsOfWorkflows,
                      setDefinitionsOfWorkflows,
                      numberOfItems,
                      updatedItemsIds,
                      initialTimestamp,
                      mapToBcWorkflow) }
            />
        </Box>
        <Box
            direction='row'
            justify='between'
            align="center">
          <Box
              pad='xsmall'>
            { t('total') } { numberOfWorkflows }
          </Box>
          <Box
              direction='row'
              gap='medium'
              pad='xsmall'
              align="center">
            <Box
                direction="row"
                align="center"
                gap='xsmall'>
              <Box
                  direction='row'
                  height="1rem"
                  border={ { color: 'light-4', size: '1px' } }>
                <Box
                    width="1rem"
                    height="100%"
                    background="white" />
              </Box>
              {
                isNotPhone
                    ? <Box>
                        Unverändert
                      </Box>
                    : undefined
              }
            </Box>
            <Box
                direction="row"
                align="center"
                gap='xsmall'>
              <Box
                  direction='row'
                  height="1rem"
                  border={ { color: 'light-4', size: '1px' } }>
                <Box
                    width="1rem"
                    height="100%"
                    background={ { color: 'accent-3', opacity: 0.1 } } />
              </Box>
              {
                isNotPhone
                    ? <Box>
                        Neu
                      </Box>
                    : undefined
              }
            </Box>
            <Box
                direction="row"
                align="center"
                gap='xsmall'>
              <Box
                  direction='row'
                  height="1rem"
                  border={ { color: 'light-4', size: '1px' } }>
                <Box
                    width="1rem"
                    height="100%"
                    background={ { color: 'accent-1', opacity: 0.15 } } />
              </Box>
              {
                isNotPhone
                    ? <Box>
                        Aktualisiert
                      </Box>
                    : undefined
              }
            </Box>
            <Box
                direction="row"
                align="center"
                gap='xsmall'>
              <Box
                  direction='row'
                  height="1rem"
                  border={ { color: 'light-4', size: '1px' } }>
                <Box
                    width="1rem"
                    height="100%"
                    background={ { color: 'light-2', opacity: 0.5 } } />
              </Box>
              {
                isNotPhone
                    ? <Box>
                        Abgeschlossen
                      </Box>
                    : undefined
              }
            </Box>
          </Box>
        </Box>
      </Grid>);
};

export { ListOfWorkflows };
