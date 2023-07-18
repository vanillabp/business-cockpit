import React, { useState, useRef, useMemo, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import i18n from '../../i18n';
import { ListItem, ListItems, ReloadCallbackFunction, SearchableAndSortableUpdatingList } from '../../components/SearchableAndSortableUpdatingList';
import { useTasklistApi } from './TasklistAppContext';
import { TasklistApi, UserTask, UserTaskEvent } from '../../client/gui';
import { useGuiSse } from '../../client/guiClient';
import { Grid, Box, CheckBox, ColumnConfig } from 'grommet';
import { useResponsiveScreen } from "@vanillabp/bc-shared";
import { EventSourceMessage, EventMessage, WakeupSseCallback } from '@vanillabp/bc-shared';
import { Link } from '@vanillabp/bc-shared';
import { Column } from '@vanillabp/bc-shared';
import { useAppContext } from "../../AppContext";
import { ModuleDefinition, useFederationModules } from '../../utils/module-federation';
import i18next from 'i18next';
import { ListCell, TypeOfItem } from '../../components/ListCell';

i18n.addResources('en', 'tasklist/list', {
      "total": "Total:",
      "no": "No.",
      "name": "task",
      "unsupported-ui-uri-type_title": "Open task",
      "unsupported-ui-uri-type_message": "Internal error: The task refers to an unsupported UI-URI-type!",
    });
i18n.addResources('de', 'tasklist/list', {
      "total": "Anzahl:",
      "no": "Nr.",
      "name": "Aufgabe",
      "unsupported-ui-uri-type_title": "Aufgabe öffnen",
      "unsupported-ui-uri-type_message": "Internes Problem: Die Aufgabe bezieht sich auf einen nicht unterstützten UI-URI-Typ!",
    });

const loadUserTasks = async (
  tasklistApi: TasklistApi,
  setNumberOfUserTasks: (number: number) => void,
  pageSize: number,
  pageNumber: number,
): Promise<ListItems<UserTask>> => {
  
  const result = await tasklistApi
        .getUserTasks({ pageNumber, pageSize });
        
  setNumberOfUserTasks(result!.page.totalElements);

  return {
      serverTimestamp: result.serverTimestamp,
      items: result.userTasks
  	};
};

const reloadUserTasks = async (
  tasklistApi: TasklistApi,
  setNumberOfUserTasks: (number: number) => void,
  existingModuleDefinitions: UserTask[] | undefined,
  setModulesOfTasks: (modules: UserTask[] | undefined) => void,
  existingUserTaskDefinitions: DefinitionOfUserTask | undefined,
  setDefinitionsOfTasks: (definitions: DefinitionOfUserTask | undefined) => void,
  numberOfItems: number,
  knownItemsIds: Array<string>,
): Promise<ListItems<UserTask>> => {

  const result = await tasklistApi.getUserTasksUpdate({
      userTasksUpdate: {
          size: numberOfItems,
          knownUserTasksIds: knownItemsIds
        }
    })
  
  setNumberOfUserTasks(result!.page.totalElements);

  const newModuleDefinitions = result
      .userTasks
      .filter(userTask => userTask.workflowModule !== undefined)
      .reduce((moduleDefinitions, userTask) => moduleDefinitions.includes(userTask)
          ? moduleDefinitions : moduleDefinitions.concat(userTask), existingModuleDefinitions || []);
  if (existingModuleDefinitions?.length !== newModuleDefinitions.length) {
    setModulesOfTasks(newModuleDefinitions);
    const newUserTaskDefinitions: DefinitionOfUserTask = { ...existingUserTaskDefinitions };
    result
        .userTasks
        .filter(userTask => userTask.taskDefinition !== undefined)
        .forEach(userTask => newUserTaskDefinitions[`${userTask.workflowModule}#${userTask.taskDefinition}`] = userTask);
    if ((existingUserTaskDefinitions === undefined)
        || Object.keys(existingUserTaskDefinitions).length !== Object.keys(newUserTaskDefinitions).length) {
      setDefinitionsOfTasks(newUserTaskDefinitions);
    }
  }
  
  return {
      serverTimestamp: result.serverTimestamp,
      items: result.userTasks
    };   
};

interface DefinitionOfUserTask {
  [key: string]: UserTask;
}

const ListOfTasks = () => {

  const { isNotPhone } = useResponsiveScreen();
  const { t } = useTranslation('tasklist/list');
  const { t: tApp } = useTranslation('app');
  const { toast, showLoadingIndicator } = useAppContext();
  
  const wakeupSseCallback = useRef<WakeupSseCallback>(undefined);
  const tasklistApi = useTasklistApi(wakeupSseCallback);
  
  const updateListRef = useRef<ReloadCallbackFunction | undefined>(undefined);
  const updateList = useMemo(() => async (ev: EventSourceMessage<Array<EventMessage<UserTaskEvent>>>) => {
      if (!updateListRef.current) return;
      const listOfUpdatedTasks = ev.data.map(userTaskEvent => userTaskEvent.event.id);
      updateListRef.current(listOfUpdatedTasks);
    }, [ updateListRef ]);
  wakeupSseCallback.current = useGuiSse<Array<EventMessage<UserTaskEvent>>>(
      updateList,
      /^UserTask$/
    );

  const userTasks = useRef<Array<ListItem<UserTask>> | undefined>(undefined);
  const [ numberOfTasks, setNumberOfTasks ] = useState<number>(0);
  const [ modulesOfTasks, setModulesOfTasks ] = useState<UserTask[] | undefined>(undefined);
  const [ definitionsOfTasks, setDefinitionsOfTasks ] = useState<DefinitionOfUserTask | undefined>(undefined);
  useEffect(() => {
      const loadMetaInformation = async () => {
        const result = await tasklistApi
            .getUserTasks({ pageNumber: 0, pageSize: 100 });
        setNumberOfTasks(result.page.totalElements);
        const moduleDefinitions = result
            .userTasks
            .reduce((moduleDefinitions, userTask) => moduleDefinitions.includes(userTask)
                ? moduleDefinitions : moduleDefinitions.concat(userTask), new Array<UserTask>());
        setModulesOfTasks(moduleDefinitions);
        const userTaskDefinitions: DefinitionOfUserTask = {};
        result
            .userTasks
            .forEach(userTask => userTaskDefinitions[`${userTask.workflowModule}#${userTask.taskDefinition}`] = userTask);
        setDefinitionsOfTasks(userTaskDefinitions);
      };
      if (userTasks.current === undefined) {
        showLoadingIndicator(true);
        loadMetaInformation();
      }
    }, [ userTasks, tasklistApi, setNumberOfTasks, setModulesOfTasks, setDefinitionsOfTasks, showLoadingIndicator ]);
  
  const [ columnsOfTasks, setColumnsOfTasks ] = useState<Array<Column> | undefined>(undefined); 
  const modules = useFederationModules(modulesOfTasks as Array<ModuleDefinition> | undefined, 'UserTaskList');
  useEffect(() => {
    if (modules === undefined) {
      return;
    }
    if (definitionsOfTasks === undefined) {
      return;
    }
    const totalColumns = Object
        .keys(definitionsOfTasks)
        .map(definition => definitionsOfTasks[definition])
        .map(definition => {
            const columnsOfProcess = modules
                .filter(m => m !== undefined)
                .filter(m => m.workflowModule === definition.workflowModule)
                .filter(m => m.userTaskListColumns !== undefined)
                .map(m => m.userTaskListColumns(definition));
            if (columnsOfProcess.length === 0) return undefined;
            return columnsOfProcess[0];
          })
        .filter(columnsOfTask => columnsOfTask !== undefined)
        .reduce((totalColumns, columnsOfTask) => {
            columnsOfTask!.forEach(column => { totalColumns[column.path] = column });
            return totalColumns;
          }, {});
    const existingColumnsSignature = columnsOfTasks === undefined
        ? ' ' // initial state is different then updates
        : columnsOfTasks.map(c => c.path).join('|');
    const orderedColumns = (Object
        .values(totalColumns) as Array<Column>)
        .sort((a, b) => a.priority - b.priority);
    const newColumnsSignature = orderedColumns.map(c => c.path).join('|');
    if (existingColumnsSignature === newColumnsSignature) {
      return;
    }
    setColumnsOfTasks(orderedColumns);
  }, [ modules, definitionsOfTasks, columnsOfTasks, setColumnsOfTasks ]);
  
  const openTask = async (userTask: UserTask) => {
      if (userTask.uiUriType !== 'WEBPACK_MF_REACT') {
        toast({
            namespace: 'tasklist/list',
            title: t('unsupported-ui-uri-type_title'),
            message: t('unsupported-ui-uri-type_message'),
            status: 'critical'
          });
        return;
      }
      
      const targetWindowName = `usertask-app-${userTask.id}`;
      const targetUrl = `/${ tApp('url-usertask') }/${userTask.id}`;
      const targetWindow = window.open(targetUrl, targetWindowName);
      if (targetWindow) {
        targetWindow.focus();
      }
    };

  const columns: ColumnConfig<ListItem<UserTask>>[] =
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
            render: (_item: ListItem<UserTask>) => (
                <Box
                    align="center">
                  <CheckBox />
                </Box>)
          },
          { property: 'name',
            header: t('name'),
            size: `calc(100% - 2.2rem${columnsOfTasks === undefined ? 'x' : columnsOfTasks!.reduce((r, column) => `${r} - ${column.width}`, '')})`,
            render: (item: ListItem<UserTask>) => (
                <Box
                    fill
                    pad="xsmall">
                  <Link
                      onClick={ () => openTask(item.data) }
                      truncate="tip">
                    { item.data['title'][i18next.language] || item.data['title']['en'] }
                  </Link>
                </Box>)
          },
          ...(columnsOfTasks === undefined
              ? []
              : columnsOfTasks!.map(column => ({
                    property: column.path,
                    header: column.title[i18next.language] || column.title['en'],
                    size: column.width,
                    plain: true,
                    render: (item: ListItem<UserTask>) => <ListCell
                                                              modulesAvailable={ modules! }
                                                              column={ column }
                                                              currentLanguage={ i18next.language }
                                                              typeOfItem={ TypeOfItem.TaskList }
                                                              item={ item } />
                  }))
          )
      ];
  
  return (
      <Grid
          key="grid"
          rows={ [ 'auto', '2rem' ] }
          fill>
        {
          (columnsOfTasks === undefined)
              ? <Box key="list"></Box>
              : <Box key="list">
                    <SearchableAndSortableUpdatingList
                        t={ t }
                        columns={ columns }
                        itemsRef={ userTasks }
                        updateListRef= { updateListRef }
                        retrieveItems={ (pageNumber, pageSize) => 
// @ts-ignore
                            loadUserTasks(
                                tasklistApi,
                                setNumberOfTasks,
                                pageSize,
                                pageNumber) }
                        reloadItems={ (numberOfItems, updatedItemsIds) =>
// @ts-ignore
                            reloadUserTasks(
                                tasklistApi,
                                setNumberOfTasks,
                                modulesOfTasks,
                                setModulesOfTasks,
                                definitionsOfTasks,
                                setDefinitionsOfTasks,
                                numberOfItems,
                                updatedItemsIds) }
                      />
                  </Box>
        }
        <Box
            key="footer"
            direction='row'
            justify='between'
            align="center">
          <Box
              pad='xsmall'>
            { t('total') } { numberOfTasks }
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
                <Box
                    width="1rem"
                    height="100%"
                    background="light-2" />
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
                <Box
                    width="1rem"
                    height="100%"
                    background={ { color: 'accent-3', opacity: 0.3 } } />
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
                <Box
                    width="1rem"
                    height="100%"
                    background={ { color: 'accent-1', opacity: 0.35 } } />
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
                    background={ { color: 'accent-4', opacity: 0.15 } } />
                <Box
                    width="1rem"
                    height="100%"
                    background={ { color: 'accent-4', opacity: 0.3 } } />
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

export { ListOfTasks };
