import { MouseEvent as ReactMouseEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { User as UserDto, UserTask, UserTaskEvent } from '@vanillabp/bc-official-gui-client';
import { Box, CheckBox, ColumnConfig, Drop, Grid, Text, TextInput, Tip } from 'grommet';
import {
  BcUserTask,
  colorForEndedItemsOrUndefined,
  Column,
  debounce,
  ENDED_FONT_COLOR,
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
import {
  Ascend,
  Blank,
  ContactInfo,
  Descend,
  FormTrash,
  FormView,
  Hide,
  Refresh,
  Unsorted,
  User as UserIcon
} from "grommet-icons";
import { User } from "./User.js";

const minWidthOfTitleColumn = '20rem';

interface Columns {
  [key: string]: Column;
}

interface DefinitionOfUserTask {
  [key: string]: UserTask;
}

interface ColumnWidthAdjustments {
  [key: string]: number
}

const loadUserTasks = async (
  tasklistApi: TasklistApi,
  setNumberOfUserTasks: (number: number) => void,
  pageSize: number,
  pageNumber: number,
  initialTimestamp: Date | undefined,
  sort: string | undefined,
  sortAscending: boolean,
  mapToBcUserTask: (userTask: UserTask) => BcUserTask,
): Promise<ListItems<UserTask>> => {
  
  const result = await tasklistApi.getUserTasks(
      new Date().getTime().toString(),
      pageNumber,
      pageSize,
      sort,
      sortAscending,
      initialTimestamp);
        
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
  sort: string | undefined,
  sortAscending: boolean,
  mapToBcUserTask: (userTask: UserTask) => BcUserTask,
  allSelected: boolean,
): Promise<ListItems<UserTask>> => {

  const result = await tasklistApi.getUserTasksUpdate(
      new Date().getTime().toString(),
      numberOfItems,
      knownItemsIds,
      sort,
      sortAscending,
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
  const { isNotPhone } = useResponsiveScreen();

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
                {
                  isNotPhone && (
                        <Text
                            truncate='tip'
                            color={ textColor }>
                          { t('claim_task' ) }
                        </Text>
                    )
                }
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
                              key={ user.id }
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

const ColumnHeader = ({
  currentLanguage,
  column,
  minWidth,
  setColumnWidthAdjustment,
  sort,
  sortAscending,
  setSort,
  setSortAscending,
}: {
  currentLanguage: string,
  column: Column,
  minWidth?: string,
  setColumnWidthAdjustment: (column: string, adjustment: number) => void,
  sort?: boolean,
  sortAscending?: boolean,
  setSort: (column?: Column) => void,
  setSortAscending: (ascending: boolean) => void,
}) => {
  const resize = useRef(-1);
  const [ widthAdjustment, setWidthAdjustment ] = useState(0);
  const startResize = (event: ReactMouseEvent) => {
    event.stopPropagation();
    event.preventDefault();
    resize.current = event.clientX;
    document.body.style.cursor = 'col-resize';
  };
  const moveHandler = useCallback((ev: MouseEvent) => {
    if (resize.current == -1) return;
    const adjustment = widthAdjustment + (ev.clientX - resize.current);
    setColumnWidthAdjustment(column.path, adjustment);
  }, [ resize, setColumnWidthAdjustment, widthAdjustment, column.path ]);
  const upHandler = useCallback((ev: MouseEvent) => {
    if (resize.current == -1) return;
    document.body.style.cursor = 'default';
    const adjustment = widthAdjustment + (ev.clientX - resize.current);
    resize.current = -1;
    setWidthAdjustment(adjustment);
  }, [ resize, setWidthAdjustment, widthAdjustment ]);
  useEffect(() => {
    window.addEventListener("mousemove", moveHandler);
    window.addEventListener("mouseup", upHandler);
    return () => {
      window.removeEventListener("mousemove", moveHandler);
      window.removeEventListener("mouseup", upHandler);
    }
  }, [ setColumnWidthAdjustment, widthAdjustment, setWidthAdjustment ]);
  return (
      <Box
          style={ { minWidth, position: "relative" } }>
        <Box
            direction="row"
            justify="between"
            align="center"
            overflow="hidden"
            style={ { position: "relative" } }>
          <Text
              truncate="tip">{ column.title[currentLanguage] || column.title['en'] }</Text>
          <Box
              align="center"
              direction="row"
              style={ { position: "absolute", top: '-0.5rem', bottom: '-0.5rem', right: '-0.5rem' } }>
            {
              !column.sortable
                  ? undefined
                  : !Boolean(sort)
                  ? <Box
                        focusIndicator={ false }
                        onClick={ event => setSort(column) }>
                      <Unsorted
                          size="32rem" />
                    </Box>
                  : sortAscending
                  ? <Box
                        focusIndicator={ false }
                        onClick={ event => setSortAscending(false) }
                        pad={ { right: '0.5rem' } }>
                      <Ascend size="16rem" />
                    </Box>
                  : <Box
                        focusIndicator={ false }
                        onClick={ event => setSort(undefined) }
                        pad={ { right: '0.5rem' } }>
                      <Descend size="16rem" />
                    </Box>
            }
            { /* <FormFilter /> */ }
          </Box>
        </Box>
        <Box
            align="center"
            onMouseDown={ startResize }
            style={ { cursor: 'col-resize', position: "absolute", top: '-0.5rem', bottom: '-0.5rem', right: '-0.5rem' } }>
          &nbsp;
        </Box>
      </Box>);
};

const ListOfTasks = ({
    showLoadingIndicator,
    useGuiSse,
    useTasklistApi,
    openTask,
    navigateToWorkflow,
    t,
    currentLanguage,
    defaultSort,
    defaultSortAscending,
}: {
    showLoadingIndicator: ShowLoadingIndicatorFunction,
    useGuiSse: GuiSseHook,
    useTasklistApi: TasklistApiHook,
    openTask: OpenTaskFunction,
    navigateToWorkflow: NavigateToWorkflowFunction,
    t: TranslationFunction,
    currentLanguage: string,
    defaultSort?: string,
    defaultSortAscending?: boolean,
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
  const [ languagesOfTitles, setLanguagesOfTitles ] = useState<Array<string>>([currentLanguage]);
  const [ definitionsOfTasks, setDefinitionsOfTasks ] = useState<DefinitionOfUserTask | undefined>(undefined);
  useEffect(() => {
      const loadMetaInformation = async () => {
        const result = await loadUserTasks(tasklistApi, setNumberOfTasks, 100, 0, undefined, undefined, true, mapToBcUserTask);
        const moduleDefinitions = result
            .items
            .reduce((moduleDefinitions, userTask) => moduleDefinitions.includes(userTask)
                ? moduleDefinitions : moduleDefinitions.concat(userTask), new Array<UserTask>());
        setModulesOfTasks(moduleDefinitions);
        const userTaskDefinitions: DefinitionOfUserTask = {};
        const titleLanguages = result
            .items
            .map(userTask => userTaskDefinitions[`${userTask.workflowModule}#${userTask.taskDefinition}`] = userTask)
            .flatMap(userTask => Object.keys(userTask.title))
            .reduce((allLanguages, titleLanguage) => {
              if (!allLanguages.includes(titleLanguage)) {
                allLanguages.push(titleLanguage);
              }
              return allLanguages;
            }, new Array<string>(currentLanguage));
        setDefinitionsOfTasks(userTaskDefinitions);
        setLanguagesOfTitles(titleLanguages);
      };
      if (userTasks.current === undefined) {
        showLoadingIndicator(true);
        loadMetaInformation();
      }
    },
    // tasklistApi is not part of dependency because it changes one time but this is irrelevant to the
    // purpose of preloading modules used by usertasks
    [ userTasks, setNumberOfTasks, setModulesOfTasks, setDefinitionsOfTasks, showLoadingIndicator, refreshIndicator, setLanguagesOfTitles ]);
  
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
            columnsOfTask!
                .filter(column => column.show)
                .forEach(column => { totalColumns[column.path] = column });
            return totalColumns;
          }, {} as Columns);
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
  const [ sort, _setSort ] = useState<string | undefined>(defaultSort);
  const [ sortAscending, _setSortAscending ] = useState(defaultSortAscending === undefined ? true : defaultSortAscending);

  const refreshList = () => {
    userTasks.current = undefined;
    setRefreshNecessary(false);
    setColumnsOfTasks(undefined);
    setRefreshIndicator(new Date());
  }
  const setSort = (column?: Column) => {
    if (column) {
      if (column.path === 'title') {
        _setSort('title.' + languagesOfTitles.join(',title.'))
      } else {
        _setSort(column.path);
      }
    } else {
      _setSort(defaultSort);
    }
    _setSortAscending(defaultSortAscending === undefined ? true : defaultSortAscending);
    refreshList();
  };
  const setSortAscending = (sortAscending: boolean) => {
    _setSortAscending(sortAscending);
    refreshList();
  }

  const unassign = (userTaskId: string, userId: string) => {
    tasklistApi.assignTask(userTaskId, userId, true);
  };

  const [ dropIdentifier, setDropIdentifier ] = useState<string | undefined>(undefined);
  const [ columnWidthAdjustments, setColumnWidthAdjustments ] = useState<ColumnWidthAdjustments>({});
  const getColumnSize = (column: string, width: string) => `max(2rem, calc(${width} + ${columnWidthAdjustments[column] ? columnWidthAdjustments[column] : 0}px))`;
  const setColumnWidthAdjustment = (column: string, adjustment: number) => {
    const current = columnWidthAdjustments[column];
    if (current === adjustment) return;
    setColumnWidthAdjustments({ ...columnWidthAdjustments, [column]: adjustment })
  };

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
          { property: 'title',
            header: <ColumnHeader
                        currentLanguage={ currentLanguage }
                        column={ {
                          path: 'title',
                          show: true,
                          sortable: true,
                          filterable: true,
                          title: { [currentLanguage]: t('column_title') },
                          width: '',
                          priority: -1,
                        }}
                        setColumnWidthAdjustment={ setColumnWidthAdjustment }
                        sort={ sort?.startsWith('title.') } // like 'title.de,title.en'
                        setSort={ setSort }
                        sortAscending={ sortAscending }
                        setSortAscending={ setSortAscending} />,
            plain: true,
            render: (item: ListItem<BcUserTask>) => {
              const titleLanguages = Object.keys(item.data['title']);
              let title;
              if (titleLanguages.includes(currentLanguage)) {
                title = item.data['title'][currentLanguage];
              } else {
                title = item.data['title'][titleLanguages[0]];
              }
              return (
                    <Box
                        fill
                        style={ { minWidth: getColumnSize('title', minWidthOfTitleColumn) } }
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
          header: <ColumnHeader
              currentLanguage={ currentLanguage }
              column={ {
                path: 'assignee',
                show: true,
                sortable: false,
                filterable: false,
                title: { [currentLanguage]: t('column_assignee') },
                width: '',
                priority: -1,
              }}
              setColumnWidthAdjustment={ setColumnWidthAdjustment }
              sort={ false }
              setSort={ setSort }
              sortAscending={ sortAscending }
              setSortAscending={ setSortAscending} />,
          size: getColumnSize("assignee", "10rem"),
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
          render: (item: ListItem<BcUserTask>) => {
            const targetRef = useRef<HTMLDivElement>(null);
            return item.data.candidateUsers && item.data.candidateUsers?.length > 0
                ? <Box
                    fill
                    justify="center"
                    align="center">
                  <Box
                      onMouseEnter={ () => setDropIdentifier(item.id) }
                      ref={ targetRef }>
                    <ContactInfo />
                  </Box>
                  {
                      dropIdentifier
                          && dropIdentifier === item.id
                          && <Drop
                                inline
                                round="xsmall"
                                onMouseLeave={ () => setDropIdentifier(undefined) }
                                target={ targetRef }>
                              <Box
                                  direction="column"
                                  margin="xsmall"
                                  gap="xsmall">
                                {
                                  item.data.candidateUsers?.map(user => <Box
                                                                                    direction="row"
                                                                                    align="center"
                                                                                    justify="center">
                                                                                  { user }
                                                                                  <FormTrash onClick={ () => unassign(item.id, user) } />
                                                                                </Box>)
                                }
                              </Box>
                            </Drop>
                  }
                </Box>
                : undefined
          }
        },
        ...(columnsOfTasks === undefined
            ? []
            : columnsOfTasks!.map(column => ({
                  property: column.path,
                  size: getColumnSize(column.path, column.width),
                  plain: true,
                  header: <ColumnHeader
                              currentLanguage={ currentLanguage }
                              setColumnWidthAdjustment={ setColumnWidthAdjustment }
                              sort={ sort === column.path }
                              setSort={ setSort }
                              sortAscending={ sortAscending }
                              setSortAscending={ setSortAscending }
                              column={ column } />,
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
                      direction="row"
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
                      minWidthOfAutoColumn={ getColumnSize('title', minWidthOfTitleColumn) }
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
                              sort,
                              sortAscending,
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
                              sort,
                              sortAscending,
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
                    align="center"
                    justify="center"
                    background="white">
                  <Text size="xsmall">T</Text>
                </Box>
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
                    align="center"
                    justify="center"
                    background={ { color: 'accent-3', opacity: 0.1 } }>
                  <Text size="xsmall">T</Text>
                </Box>
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
                    align="center"
                    justify="center"
                    background={ { color: 'accent-1', opacity: 0.35 } }>
                  <Text size="xsmall">T</Text>
                </Box>
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
                    align="center"
                    justify="center"
                    background={ { color: 'light-2', opacity: 0.5 } }>
                  <Text size="xsmall" color={ ENDED_FONT_COLOR }>T</Text>
                </Box>
              </Box>
              {
                isNotPhone
                    ? <Box>
                      { t('legend_completed') }
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
                    align="center"
                    justify="center"
                    background={ { color: 'light-2', opacity: 0.5 } }>
                  <Text size="xsmall">T</Text>
                </Box>
              </Box>
              {
                isNotPhone
                    ? <Box>
                      { t('legend_filtered') }
                    </Box>
                    : undefined
              }
            </Box>
          </Box>
        </Box>
      </Grid>);
      
};

export { ListOfTasks };
