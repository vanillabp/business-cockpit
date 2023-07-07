import React, { useState, useRef, useMemo, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import i18n from '../../i18n';
import { ListItem, ListItems, ReloadCallbackFunction, SearchableAndSortableUpdatingList } from '../../components/SearchableAndSortableUpdatingList';
import { useTasklistApi } from './TasklistAppContext';
import { TasklistApi, UserTask, UserTaskEvent } from '../../client/gui';
import { useGuiSse } from '../../client/guiClient';
import { Grid, Box, CheckBox, ColumnConfig } from 'grommet';
import { useResponsiveScreen } from "@vanillabp/bc-shared";
import { EventSourceMessage, WakeupSseCallback } from '@vanillabp/bc-shared';
import { Link, toLocalDateString, toLocaleTimeStringWithoutSeconds } from '@vanillabp/bc-shared';
import { useAppContext } from "../../AppContext";
import { Column, ModuleDefinition, useFederationModule, useFederationModules } from '../../utils/module-federation';

i18n.addResources('en', 'tasklist/list', {
      "total": "Total:",
      "no": "No.",
      "name": "task",
      "project": "Project",
      "due": "Due",
      "unsupported-ui-uri-type_title": "Open task",
      "unsupported-ui-uri-type_message": "Internal error: The task refers to an unsupported UI-URI-type!",
    });
i18n.addResources('de', 'tasklist/list', {
      "total": "Anzahl:",
      "no": "Nr.",
      "name": "Aufgabe",
      "project": "Projekt",
      "due": "Fällig",
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
  const { toast } = useAppContext();
  
  const wakeupSseCallback = useRef<WakeupSseCallback>(undefined);
  const tasklistApi = useTasklistApi(wakeupSseCallback);
  
  const updateListRef = useRef<ReloadCallbackFunction | undefined>(undefined);
  const updateList = useMemo(() => async (ev: EventSourceMessage<UserTaskEvent>) => {
      if (!updateListRef.current) return;
      const listOfUpdatedTasks = ev.data.map(userTaskEvent => userTaskEvent.event.id);
      updateListRef.current(listOfUpdatedTasks);
    }, [ updateListRef ]);
  wakeupSseCallback.current = useGuiSse<UserTaskEvent>(
      updateList,
      /^UserTask$/
    );

  const [ columnDefinitions, setColumnDefinitions ] = useState<Array<Column> | undefined>(undefined);
  const userTasks = useRef<Array<ListItem<UserTask>> | undefined>(undefined);
  const [ numberOfTasks, setNumberOfTasks ] = useState<number>(0);
  const [ modulesOfTasks, setModulesOfTasks ] = useState<ModuleDefinition[] | undefined>(undefined);
  const [ definitionsOfTasks, setDefinitionsOfTasks ] = useState<DefinitionOfUserTask | undefined>(undefined);
  useEffect(() => {
      const loadMetaInformation = async () => {
        const result = await tasklistApi
            .getUserTasks({ pageNumber: 0, pageSize: 100 });
        setNumberOfTasks(result.page.totalElements);
        const moduleDefinitions = result
            .userTasks
            .reduce((moduleDefinitions, userTask) => moduleDefinitions.includes(userTask as ModuleDefinition)
                ? moduleDefinitions : moduleDefinitions.concat(userTask as ModuleDefinition), new Array<ModuleDefinition>());
        setModulesOfTasks(moduleDefinitions);
        const userTaskDefinitions: DefinitionOfUserTask = {};
        result
            .userTasks
            .forEach(userTask => userTaskDefinitions[`${userTask.workflowModule}#${userTask.taskDefinition}`] = userTask);
        setDefinitionsOfTasks(userTaskDefinitions);
      };
      if (userTasks.current === undefined) {
        loadMetaInformation();
      }
    }, [ userTasks, tasklistApi, setNumberOfTasks, setModulesOfTasks ]);
  
  const modules = useFederationModules(modulesOfTasks, 'List');
  useEffect(() => {
    if (modules === undefined) {
      return;
    }
    if (definitionsOfTasks === undefined) {
      return;
    }
    console.log(Object
        .keys(definitionsOfTasks)
        .map(definition => {
          return definitionsOfTasks[definition]
          })
        .map(definition => {
            const columnsOfProcess = modules
                .filter(module => module.moduleId === definition.workflowModule)
                .filter(module => module.taskListColumns![definition.bpmnProcessId])
                .map(module => module.taskListColumns![definition.bpmnProcessId]);
            if (columnsOfProcess.length === 0) return undefined;
            return columnsOfProcess[0][definition.taskDefinition];
          })
        .filter(columnsOfTask => columnsOfTask !== undefined)
        .reduce((totalColumns, columnsOfTask) => {
            columnsOfTask!.forEach(column => { totalColumns[column.id] = column });
            return totalColumns;
          }, {}));
  }, [ modules, definitionsOfTasks ]);
  
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
            header: <Box
                    pad="xsmall">
                  <CheckBox />
                </Box>,
            render: (_item: ListItem<UserTask>) => (
                <Box pad="xsmall">
                  <CheckBox />
                </Box>)
          },
          { property: 'number',
            header: t('no'),
            size: '3rem'
          },
          { property: 'name',
            header: t('name'),
            size: 'calc(100% - 30.2rem)',
            render: (item: ListItem<UserTask>) => (
                <Box>
                  <Link
                      onClick={ () => openTask(item.data) }
                      truncate="tip">
                    { item.data['title'].de }
                  </Link>
                </Box>)
          },
          { property: 'project',
              header: t('project'),
              size: '15rem',
              render: (item: ListItem<UserTask>) => (
                  <Box>
                      { item?.data['details']?.project?.name || "-" }
                  </Box>)
          },
          { property: 'due',
            header: t('due'),
            size: '10rem',
            render: (item: ListItem<UserTask>) => (
                <Box>
                    { item?.data.dueDate ?
                        ( <span
                            title={ toLocalDateString(item?.data.dueDate) + " " + toLocaleTimeStringWithoutSeconds(item?.data.dueDate) }
                          >{ toLocalDateString(item?.data.dueDate) }</span> )
                        : ( '-' )
                    }
                </Box>)
          }
      ];

  return (
      <Grid
          rows={ [ 'auto', '2rem' ] }
          fill>
        {
          (userTasks.current !== undefined) && (columnDefinitions !== undefined)
              ? <Box>Loading</Box>
              : <Box>
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
                                numberOfItems,
                                updatedItemsIds) }
                      />
                  </Box>
        }
        <Box
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
