import { useEffect, useMemo, useRef, useState } from 'react';
import { User as UserDto, UserTask, UserTaskEvent } from '@vanillabp/bc-official-gui-client';
import { Box, CheckBox, ColumnConfig, Grid, Text, TextInput, Tip } from 'grommet';
import {
  BcUserTask,
  colorForEndedItemsOrUndefined,
  Column,
  debounce,
  EventMessage,
  EventSourceMessage,
  GuiSseHook,
  Link,
  ListItemStatus,
  ShowLoadingIndicatorFunction,
  TextListCell,
  useOnClickOutside,
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
  TasklistApi,
  TasklistApiHook,
  TypeOfItem,
  useFederationModules
} from '../index.js';
import { TranslationFunction } from "../types/translate";
import { Blank, ContactInfo, FormView, Hide, Refresh, User as UserIcon } from "grommet-icons";
import { User } from "./User.js";

const loadUserTasks = async (
  tasklistApi: TasklistApi,
  setNumberOfUserTasks: (number: number) => void,
  pageSize: number,
  pageNumber: number,
  initialTimestamp: Date | undefined,
  mapToBcUserTask: (userTask: UserTask) => BcUserTask,
): Promise<ListItems<UserTask>> => {
  
  const result = await tasklistApi
        .getUserTasks(new Date().getTime().toString(), pageNumber, pageSize, initialTimestamp);
        
  setNumberOfUserTasks(result!.page.totalElements);

  return {
      serverTimestamp: result.serverTimestamp,
      items: result.userTasks.map(userTask => mapToBcUserTask(userTask)),
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
  initialTimestamp: Date | undefined,
  mapToBcUserTask: (userTask: UserTask) => BcUserTask,
  allSelected: boolean,
): Promise<ListItems<UserTask>> => {

  const result = await tasklistApi.getUserTasksUpdate(
      new Date().getTime().toString(),
      numberOfItems,
      knownItemsIds,
      initialTimestamp);
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
  t,
  disabled,
  markAsRead,
  markAsUnread,
}: {
  t: TranslationFunction,
  disabled: boolean,
  markAsRead: () => void,
  markAsUnread: () => void,
}) => {
  const color = disabled ? 'light-4' : 'dark-3';
  const textColor = disabled ? 'dark-4' : 'dark-1';

  return (<Box
              direction="row"
              round={ { size: '0.4rem' } }
              border={ { color } }
              elevation="small">
            <Tip
                content={ t('mark_as_read') }>
              <Box
                  hoverIndicator="light-2"
                  focusIndicator={ false }
                  onClick={ markAsRead }
                  width="2rem"
                  height="2rem"
                  round={ { size: '0.4rem', corner: 'left' } }
                  align="center"
                  justify="center">
                <FormView
                    color={ textColor } />
              </Box>
            </Tip>
            <Tip
                content={ t('mark_as_unread') }>
              <Box
                  hoverIndicator="light-2"
                  focusIndicator={ false }
                  onClick={ markAsUnread }
                  width="2rem"
                  height="2rem"
                  round={ { size: '0.4rem', corner: 'right' } }
                  align="center"
                  justify="center"
                  border={ { color, side: 'left' } }>
                <Hide
                    color={ textColor } />
              </Box>
            </Tip>
          </Box>);

}

const ClaimButtons = ({
  t,
  disabled,
  claimTasks,
  unclaimTasks,
}: {
  t: TranslationFunction,
  disabled: boolean,
  claimTasks: () => void,
  unclaimTasks: () => void,
}) => {
  const color = disabled ? 'light-4' : 'dark-3';
  const textColor = disabled ? 'dark-4' : 'dark-1';

  return (<Box
              direction="row"
              elevation="small"
              round={ { size: '0.4rem' } }
              border={ { color } }>
            <Tip
                content={ t('claim_tasks') }>
              <Box
                  hoverIndicator="light-2"
                  focusIndicator={ false }
                  onClick={ claimTasks }
                  height="2rem"
                  pad={ { horizontal: '0.4rem' } }
                  gap="0.4rem"
                  round={ { size: '0.4rem', corner: 'left' } }
                  align="center"
                  direction="row"
                  justify="center">
                <UserIcon
                    size="20rem"
                    color={ textColor } />
                <Text
                    truncate='tip'
                    color={ textColor }>
                  { t('claim_task' ) }
                </Text>
              </Box>
            </Tip>
            <Tip
                content={ t('unclaim_tasks') }>
              <Box
                  hoverIndicator="light-2"
                  focusIndicator={ false }
                  onClick={ unclaimTasks }
                  round={ { size: '0.4rem', corner: 'right' } }
                  style={ { position: 'relative' } }
                  width="2rem"
                  height="2rem"
                  align="center"
                  justify="center"
                  border={ { color, side: 'left' } }>
                <UserIcon
                    style={ { position: 'absolute' } }
                    color={ textColor }
                    size="20rem" />
                <Blank
                    style={ { position: 'absolute' } }
                    color={ textColor }
                    size="20rem">
                  <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <line x1="24" y1="4" x2="4" y2="24" strokeWidth="2" />
                  </svg>
                </Blank>
              </Box>
            </Tip>
          </Box>);

}

const AssignButton = ({
  t,
  tasklistApi,
  disabled,
  assign,
}: {
  t: TranslationFunction,
  disabled: boolean,
  tasklistApi: TasklistApi,
  assign: (userId: string) => void,
}) => {
  const [ edit, setEdit ] = useState(false);
  const [ hide, setHide ] = useState(false);
  const [ focus, setFocus ] = useState(false);
  const [ active, setActive ] = useState(false);
  const [ query, setQuery ] = useState('')
  const currentQuery = useRef<string>('');
  const [ users, setUsers ] = useState<Array<UserDto> | undefined>(undefined);
  const inputRef = useRef<HTMLInputElement>(null);

  const loadResult = async () => {
    const result = await tasklistApi.findUsers(currentQuery.current, 10);
    setUsers(result.users);
  }
  const showEdit = () => {
    if (edit) return;
    loadResult();
    setEdit(true);
    setHide(false);
    setFocus(false);
  }
  const finalizeShowEdit = () => {
    inputRef.current!.focus();
    setActive(true);
  }
  const hideEdit = () => {
    if (!edit) return;
    setHide(true);
    setFocus(false);
    setActive(false);
  }
  const finalizeHideEdit = () => {
    setEdit(false);
    setHide(false);
  };

  useOnClickOutside(inputRef, hideEdit);
  const findUsersDebounced = useMemo(() => debounce(loadResult, 300), [ tasklistApi, currentQuery ]);
  const updateResult = (newQuery: string) => {
    currentQuery.current = newQuery;
    setQuery(newQuery);
    findUsersDebounced();
  }

  const color = disabled ? 'light-4' : 'dark-3';
  const textColor = disabled ? 'dark-4' : 'dark-1';

  return (
      <Box
          elevation="small"
          hoverIndicator={ edit ? "white" : "light-2" }
          focusIndicator={ false }
          onClick={ showEdit }
          round={ { size: '0.4rem' } }
          style={ { position: 'relative' } }
          border={ { color } }>
        <Box
            height="2rem"
            width="15rem"
            align="center"
            justify="center"
            style={ { position: 'relative' } }
            overflow="hidden">
          <Box
              gap="0.4rem"
              pad={ { horizontal: '0.4rem' } }
              align="center"
              direction="row">
            <ContactInfo
                color={ textColor }/>
            <Text
                truncate='tip'
                color={ textColor }>
              { t('assign_task') }
            </Text>
          </Box>
          {
            edit
              ? <Box
                    fill
                    overflow="visible"
                    style={ { position: "absolute", top: hide ? '3.85rem' : '-0.15rem' } }
                    animation={ { type: hide ? "slideDown" : "slideUp", size: 'xlarge', duration: 700 } }
                    onAnimationEnd={ hide ? finalizeHideEdit : finalizeShowEdit  }>
                  <Box
                      margin={ { vertical: '0.4rem', horizontal: '0.4rem' } }
                      pad="0.1rem"
                      direction="row"
                      justify="center"
                      background="white">
                    <TextInput
                        plain="full"
                        ref={ inputRef }
                        placeholder={ t('assign_placeholder') }
                        autoFocus={ focus }
                        value={ query }
                        onChange={ event => updateResult(event.target.value) }
                        focusIndicator={ false }
                        reverse />
                    <ContactInfo
                        color={ textColor } />
                  </Box>
                </Box>
              : undefined
          }
        </Box>
        {
          active || hide
              ? <Box
                    animation={ { type: hide ? "fadeOut" : "fadeIn", duration: 700 } }
                    style={ { position: 'absolute', zIndex: 20, top: '1.6rem', left: '-1px', right: '-1px', maxWidth: 'unset' } }>
                  <Box
                      background="white"
                      elevation="small"
                      gap="xsmall"
                      pad={ { top: '0.4rem', horizontal: '0.4rem'  } }
                      round={ { corner: 'bottom', size: '0.4rem' } }
                      border={ [ { color, side: 'left' }, { color, side: 'bottom' }, { color, side: 'right' } ] }
                      animation={ { type: !active ? "fadeOut" : "fadeIn", duration: 700 } }>
                    <Box
                        pad={ { top: '0.2rem' } }
                        height={ { max: '24rem' } }
                        overflow={ { vertical: 'auto'} }>
                      { users === undefined
                          ? t('assign_loading')
                          : users!.map(user => <Box
                              height={ { min: '2rem' } }
                              onClick={ () => assign(user.id!) }>
                            <User
                                user={ user }
                                isUserLoggedIn={ false }
                                iconSize='small'
                                size='medium' />
                          </Box>)
                      }
                    </Box>
                  </Box>
                </Box>
              : undefined
        }
      </Box>);

}

const RefreshButton = ({
  t,
  disabled,
  refresh,
}: {
  t: TranslationFunction,
  disabled: boolean,
  refresh: () => void,
}) => {
  const color = disabled ? 'light-4' : 'dark-3';
  const textColor = disabled ? 'dark-4' : 'dark-1';

  return (<Box
              hoverIndicator={ disabled ? 'light-2' : "accent-1" }
              focusIndicator={ false }
              onClick={ refresh }
              direction="row"
              elevation="small"
              background={ !disabled ? { color: "accent-1", opacity: "strong" } : undefined }
              round={ { size: '0.4rem' } }
              border={ { color } }>
            <Tip
                content={ t('refresh_tasks') }>
              <Box
                  height="2rem"
                  pad={ { horizontal: '0.4rem' } }
                  align="center"
                  direction="row"
                  justify="center">
                <Refresh
                    size="20rem"
                    color={ textColor } />
              </Box>
            </Tip>
          </Box>);

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
  const wakeupSseCallback = useRef<WakeupSseCallback>(undefined);
  const tasklistApi = useTasklistApi(wakeupSseCallback);
  const [ refreshIndicator, setRefreshIndicator ] = useState<Date>(new Date());

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
        const result = await loadUserTasks(tasklistApi, setNumberOfTasks, 100, 0, undefined, mapToBcUserTask);
        const moduleDefinitions = result
            .items
            .reduce((moduleDefinitions, userTask) => moduleDefinitions.includes(userTask)
                ? moduleDefinitions : moduleDefinitions.concat(userTask), new Array<UserTask>());
        setModulesOfTasks(moduleDefinitions);
        const userTaskDefinitions: DefinitionOfUserTask = {};
        result
            .items
            .forEach(userTask => userTaskDefinitions[`${userTask.workflowModule}#${userTask.taskDefinition}`] = userTask);
        setDefinitionsOfTasks(userTaskDefinitions);
      };
      if (userTasks.current === undefined) {
        showLoadingIndicator(true);
        loadMetaInformation();
      }
    },
    // tasklistApi is not part of dependency because it changes one time but this is irrelevant to the
    // purpose of preloading modules used by usertasks
    [ userTasks, setNumberOfTasks, setModulesOfTasks, setDefinitionsOfTasks, showLoadingIndicator, refreshIndicator ]);
  
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
  }, [ modules, definitionsOfTasks, columnsOfTasks, setColumnsOfTasks, refreshIndicator ]);

  const [ allSelected, setAllSelected ] = useState(false);
  const [ anySelected, setAnySelected ] = useState(false);
  const [ refreshNecessary, setRefreshNecessary ] = useState(false);

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
                            if (anySelected !== event.currentTarget.checked) {
                              setAnySelected(event.currentTarget.checked);
                            }
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
                        if (currentlyAllSelected !== allSelected) {
                          setAllSelected(currentlyAllSelected);
                        }
                        const currentlyAnySelected = userTasks
                            .current!
                            .reduce((anySelected, userTask) => anySelected || userTask.selected, false);
                        if (anySelected !== currentlyAnySelected) {
                          setAnySelected(currentlyAnySelected);
                        }
                      } } />
                </Box>)
          },
          { property: 'name',
            header: t('column_name'),
            render: (item: ListItem<BcUserTask>) => {
                const title = item.data['title'][currentLanguage] || item.data['title']['en'];
              return (
                    <Box
                        fill
                        style={ { minWidth: '10rem' } }
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
        { property: 'assignee',
          header: t('column_assignee'),
          size: '10rem',
          plain: true,
          render: (item: ListItem<BcUserTask>) => <TextListCell item={ item } value={ item.data.assignee } />
        },
        { property: 'canidateUsers',
          header: <Box
                      fill
                      align="center">
                    <Tip
                        content={ t('column_candidates') }>
                      <ContactInfo />
                    </Tip>
                  </Box>,
          size: '3rem',
          plain: true,
          render: (item: ListItem<BcUserTask>) => item.data.candidateUsers && item.data.candidateUsers?.length > 0
              ? <Box
                    fill
                    justify="center"
                    align="center">
                  <Tip
                      content={ item.data.candidateUsers?.join(', ') }>
                    <ContactInfo />
                  </Tip>
                </Box>
              : undefined
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
    setAnySelected(false);
    tasklistApi.markUserTasksAsRead(userTaskMarkedIds, unread);
  };

  const claim = (unclaim: boolean) => {
    const userTaskMarkedIds = userTasks
        .current!
        .filter(userTask => userTask.selected)
        .map(userTask => {
          userTask.selected = false;
          return userTask.id;
        })
        .reduce((userTaskIds, userTaskId) => {
          userTaskIds.push(userTaskId);
          return userTaskIds;
        }, new Array<string>());
    (refreshItemRef.current!)(userTaskMarkedIds);
    setAllSelected(false);
    setAnySelected(false);
    tasklistApi.claimTasks(userTaskMarkedIds, unclaim);
  };

  const assign = (userId: string) => {
    const userTaskMarkedIds = userTasks
        .current!
        .filter(userTask => userTask.selected)
        .map(userTask => {
          userTask.selected = false;
          return userTask.id;
        })
        .reduce((userTaskIds, userTaskId) => {
          userTaskIds.push(userTaskId);
          return userTaskIds;
        }, new Array<string>());
    (refreshItemRef.current!)(userTaskMarkedIds);
    setAllSelected(false);
    setAnySelected(false);
    tasklistApi.assignTasks(userTaskMarkedIds, userId);
  };

  const refreshList = () => {
    userTasks.current = undefined;
    setRefreshNecessary(false);
    setColumnsOfTasks(undefined);
    setRefreshIndicator(new Date());
  }

  return (
      <Grid
          key="grid"
          rows={ [ 'auto', '2rem' ] }
          fill>
        {
          (columnsOfTasks === undefined)
              ? <Box key="list"></Box>
              : <Box key="list">
                  <Box
                      fill
                      background='white'
                      direction={ isNotPhone ? "row" : "column" }
                      align="center"
                      justify="start"
                      gap="small"
                      style={ { minHeight: '3rem', maxHeight: '3rem'} }
                      pad={ { horizontal: 'xsmall' } }>
                    <SetReadStatusButtons
                        t={ t }
                        disabled={ !anySelected }
                        markAsRead={ () => markAsRead(false) }
                        markAsUnread={ () => markAsRead(true) }/>
                    <ClaimButtons
                        t={ t }
                        disabled={ !anySelected }
                        claimTasks={ () => claim(false) }
                        unclaimTasks={ () => claim(true) } />
                    <AssignButton
                        tasklistApi={ tasklistApi }
                        t={ t }
                        disabled={ !anySelected }
                        assign={ assign }/>
                    <RefreshButton
                        t={ t }
                        refresh={ refreshList }
                        disabled={ !refreshNecessary } />
                  </Box>
                  <SearchableAndSortableUpdatingList
                      showLoadingIndicator={ showLoadingIndicator }
                      columns={ columns }
                      itemsRef={ userTasks }
                      updateListRef={ updateListRef }
                      refreshItemRef={ refreshItemRef }
                      refreshNecessaryCallback={ () => setRefreshNecessary(true) }
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
