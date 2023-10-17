import { useEffect, useMemo, useRef, useState } from 'react';
import { OfficialTasklistApi, UserTask, UserTaskEvent } from '@vanillabp/bc-official-gui-client';
import { Box, Button, CheckBox, ColumnConfig, Grid, Text } from 'grommet';
import {
  BcUserTask,
  colorForEndedItemsOrUndefined,
  Column,
  EventMessage,
  EventSourceMessage,
  GuiSseHook,
  Link,
  ListItemStatus,
  ShowLoadingIndicatorFunction,
  useResponsiveScreen,
  WakeupSseCallback
} from "@vanillabp/bc-shared";
import {
  ListCell,
  ListItem,
  ListItems,
  ModuleDefinition,
  NavigateToWorkflowFunction,
  OpenTaskFunction,
  RefreshItemCallbackFunction,
  ReloadCallbackFunction,
  SearchableAndSortableUpdatingList,
  TasklistApiHook,
  TypeOfItem,
  useFederationModules
} from '../index.js';
import { TranslationFunction } from "../types/translate";
import { FormView, Hide } from "grommet-icons";

const loadUserTasks = async (
  tasklistApi: OfficialTasklistApi,
  setNumberOfUserTasks: (number: number) => void,
  pageSize: number,
  pageNumber: number,
  initialTimestamp: Date | undefined,
  mapToBcUserTask: (userTask: UserTask) => BcUserTask,
): Promise<ListItems<UserTask>> => {
  
  const result = await tasklistApi
        .getUserTasks({ pageNumber, pageSize, initialTimestamp });
        
  setNumberOfUserTasks(result!.page.totalElements);

  return {
      serverTimestamp: result.serverTimestamp,
      items: result.userTasks.map(userTask => mapToBcUserTask(userTask)),
  	};
};

const reloadUserTasks = async (
  tasklistApi: OfficialTasklistApi,
  setNumberOfUserTasks: (number: number) => void,
  existingModuleDefinitions: UserTask[] | undefined,
  setModulesOfTasks: (modules: UserTask[] | undefined) => void,
  existingUserTaskDefinitions: DefinitionOfUserTask | undefined,
  setDefinitionsOfTasks: (definitions: DefinitionOfUserTask | undefined) => void,
  numberOfItems: number,
  knownItemsIds: Array<string>,
  initialTimestamp: Date | undefined,
  mapToBcUserTask: (userTask: UserTask) => BcUserTask,
  allSelected: boolean,
): Promise<ListItems<UserTask>> => {

  const result = await tasklistApi.getUserTasksUpdate({
      userTasksUpdate: {
          size: numberOfItems,
          knownUserTasksIds: knownItemsIds,
          initialTimestamp
        }
    })
  setNumberOfUserTasks(result!.page.totalElements);

  const newModuleDefinitions = result.userTasks
      .filter(userTask => userTask.workflowModule !== undefined)
      .reduce((moduleDefinitions, userTask) => moduleDefinitions.includes(userTask)
          ? moduleDefinitions : moduleDefinitions.concat(userTask), existingModuleDefinitions || []);
  if (existingModuleDefinitions?.length !== newModuleDefinitions.length) {
    setModulesOfTasks(newModuleDefinitions);
    const newUserTaskDefinitions: DefinitionOfUserTask = { ...existingUserTaskDefinitions };
    result.userTasks
        .filter(userTask => userTask.taskDefinition !== undefined)
        .forEach(userTask => newUserTaskDefinitions[`${userTask.workflowModule}#${userTask.taskDefinition}`] = userTask);
    if ((existingUserTaskDefinitions === undefined)
        || Object.keys(existingUserTaskDefinitions).length !== Object.keys(newUserTaskDefinitions).length) {
      setDefinitionsOfTasks(newUserTaskDefinitions);
    }
  }
  
  return {
      serverTimestamp: result.serverTimestamp,
      items: result.userTasks.map(userTask => {
        return {
          ...mapToBcUserTask(userTask),
          selected: allSelected,
        }
      })
    };   

};

const SetReadStatusButtons = ({
  markAsRead,
  markAsUnread,
}: {
  markAsRead: () => void,
  markAsUnread: () => void,
}) => {

  return <>
          <Button
              hoverIndicator="light-2"
              onClick={ markAsRead }>
            <Box
                pad="0.2rem"
                round={ { size: '0.4rem', corner: 'left' } }
                border>
              <FormView />
            </Box>
          </Button>
          <Button
              hoverIndicator="light-2"
              onClick={ markAsUnread }>
            <Box
                pad="0.2rem"
                round={ { size: '0.4rem', corner: 'right' } }
                border={ [ { side: 'right' }, { side: 'top' }, { side: 'bottom' } ] }>
              <Hide />
            </Box>
          </Button>
        </>;

}

interface DefinitionOfUserTask {
  [key: string]: UserTask;
}

const ListOfTasks = ({
    showLoadingIndicator,
    useGuiSse,
    useTasklistApi,
    openTask,
    navigateToWorkflow,
    t,
    currentLanguage,
}: {
    showLoadingIndicator: ShowLoadingIndicatorFunction,
    useGuiSse: GuiSseHook,
    useTasklistApi: TasklistApiHook,
    openTask: OpenTaskFunction,
    navigateToWorkflow: NavigateToWorkflowFunction,
    t: TranslationFunction,
    currentLanguage: string,
}) => {

  const { isNotPhone, isPhone } = useResponsiveScreen();
console.log('isNotPhone', isNotPhone)
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
  const refreshItemRef = useRef<RefreshItemCallbackFunction | undefined>(undefined);

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
                .map(m => m.userTaskListColumns!(definition));
            if (columnsOfProcess.length === 0) return undefined;
            return columnsOfProcess[0];
          })
        .filter(columnsOfTask => columnsOfTask !== undefined)
        .reduce((totalColumns, columnsOfTask) => {
            columnsOfTask!.forEach(column => {
                // @ts-ignore
                totalColumns[column.path] = column });
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

  const [ allSelected, setAllSelected ] = useState(false);

  const columns: ColumnConfig<ListItem<BcUserTask>>[] =
      [
          { property: 'id',
            primary: true,
            pin: true,
            size: '2.2rem',
            plain: true,
            header: <Box
                        align="center">
                      <CheckBox
                          checked={ allSelected }
                          onChange={ event => {
                            (refreshItemRef.current!)(
                              userTasks
                                  .current!
                                  .reduce((allItemIds, item) => {
                                    item.selected = event.currentTarget.checked;
                                    allItemIds.push(item.id);
                                    return allItemIds;
                                  }, new Array<string>())
                            );
                            setAllSelected(event.currentTarget.checked);
                          } } />
                    </Box>,
            render: (item: ListItem<BcUserTask>) => (
                <Box
                    align="center">
                  <CheckBox
                      checked={ item.selected }
                      onChange={ event => {
                        item.selected = event.currentTarget.checked;
                        (refreshItemRef.current!)([ item.id ]);
                        const currentlyAllSelected = userTasks
                            .current!
                            .reduce((allSelected, userTask) => allSelected && userTask.selected, true);
                        if (currentlyAllSelected != allSelected) {
                          setAllSelected(currentlyAllSelected);
                        }
                      } } />
                </Box>)
          },
          { property: 'name',
            header: t('name'),
            size: `calc(100% - 2.2rem${columnsOfTasks === undefined ? 'x' : columnsOfTasks!.reduce((r, column) => `${r} - ${column.width}`, '')})`,
            render: (item: ListItem<BcUserTask>) => {
                const title = item.data['title'][currentLanguage] || item.data['title']['en'];
              return (
                    <Box
                        fill
                        pad="xsmall">
                      <Text
                          color={ colorForEndedItemsOrUndefined(item) }
                          weight={ item.read === undefined ? 'bold' : 'normal' }
                          truncate="tip">
                        {
                          item.status === ListItemStatus.ENDED
                              ? <>{ title }</>
                              : <Link
                                    // @ts-ignore
                                    onClick={ item.data.open }>
                                  { title }
                                </Link>
                        }
                      </Text>
                    </Box>);
                }
          },
          ...(columnsOfTasks === undefined
              ? []
              : columnsOfTasks!.map(column => ({
                    property: column.path,
                    header: column.title[currentLanguage] || column.title['en'],
                    size: column.width,
                    plain: true,
                    render: (item: ListItem<BcUserTask>) => <ListCell
                                                              modulesAvailable={ modules! }
                                                              column={ column }
                                                              currentLanguage={ currentLanguage }
                                                              typeOfItem={ TypeOfItem.TaskList }
                                                              showUnreadAsBold={ true }
                                                              t={ t }
                                                              // @ts-ignore
                                                              item={ item } />
                  }))
          )
      ];
      
  const mapToBcUserTask = (userTask: UserTask): BcUserTask => {
      return {
          ...userTask,
          open: () => openTask(userTask),
          navigateToWorkflow: () => navigateToWorkflow(userTask),
        };
    };

  const markAsRead = (unread: boolean) => {
    const read = unread ? undefined : new Date();
    const userTaskMarkedIds = userTasks
        .current!
        .filter(userTask => userTask.selected)
        .map(userTask => {
          userTask.read = read;
          userTask.selected = false;
          return userTask.id;
        })
        .reduce((userTaskIds, userTaskId) => {
            userTaskIds.push(userTaskId);
            return userTaskIds;
        }, new Array<string>());
    (refreshItemRef.current!)(userTaskMarkedIds);
    setAllSelected(false);
    userTaskMarkedIds.forEach(userTaskId => tasklistApi
        .usertaskUserTaskIdMarkAsReadPatch({ userTaskId, unread }));
  };
  
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
                        showLoadingIndicator={ showLoadingIndicator }
                        columns={ columns }
                        itemsRef={ userTasks }
                        updateListRef={ updateListRef }
                        refreshItemRef={ refreshItemRef }
                        retrieveItems={ (pageNumber, pageSize, initialTimestamp) => 
// @ts-ignore
                            loadUserTasks(
                                tasklistApi,
                                setNumberOfTasks,
                                pageSize,
                                pageNumber,
                                initialTimestamp,
                                mapToBcUserTask) }
                        reloadItems={ (numberOfItems, updatedItemsIds, initialTimestamp) =>
// @ts-ignore
                            reloadUserTasks(
                                tasklistApi,
                                setNumberOfTasks,
                                modulesOfTasks,
                                setModulesOfTasks,
                                definitionsOfTasks,
                                setDefinitionsOfTasks,
                                numberOfItems,
                                updatedItemsIds,
                                initialTimestamp,
                                mapToBcUserTask) }
                        additionalHeader={
                            <Box
                                fill
                                background='white'
                                direction="row"
                                align="center"
                                justify="start"
                                style={ { minHeight: '3rem', maxHeight: '3rem'} }
                                pad={ { horizontal: 'xsmall' } }>
                              <SetReadStatusButtons
                                  markAsRead={ () => markAsRead(false) }
                                  markAsUnread={ () => markAsRead(true) }/>
                            </Box>
                        }
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
              </Box>
              {
                isNotPhone
                    ? <Box>
                      { t('legend_unchanged') }
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
                      { t('legend_new') }
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
                    background={ { color: 'accent-1', opacity: 0.35 } } />
              </Box>
              {
                isNotPhone
                    ? <Box>
                      { t('legend_updated') }
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
                      { t('legend_completed') }
                      </Box>
                    : undefined
              }
            </Box>
          </Box>
        </Box>
      </Grid>);
      
};

export { ListOfTasks };
