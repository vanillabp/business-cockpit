import { FC, useEffect, useMemo, useRef, useState } from 'react';
import { User as UserDto, UserTask, UserTaskEvent } from '@vanillabp/bc-official-gui-client';
import { Box, CheckBox, ColumnConfig, Drop, Grid, Grommet, Text, TextInput, Tip } from 'grommet';
import {
  BcUserTask,
  colorForEndedItemsOrUndefined,
  colorRowAccordingToUpdateStatus,
  Column,
  debounce,
  DefaultListCell,
  DefaultListCellProps,
  DefaultListHeaderAwareProps,
  ENDED_FONT_COLOR,
  EventMessage,
  EventSourceMessage,
  GuiSseHook,
  Link,
  ListCell as StyledListCell,
  ListItemStatus,
  ShowLoadingIndicatorFunction,
  TranslationFunction,
  useOnClickOutside,
  useResponsiveScreen,
  WakeupSseCallback,
} from "@vanillabp/bc-shared";
import {
  ListCell,
  ListItem,
  ListItems,
  ListOfTasksHeaderFooterFunction,
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
import { Blank, ContactInfo, FormTrash, FormView, Hide, Refresh, User as UserIcon } from "grommet-icons";
import { User } from "./User.js";
import { ListColumnHeader } from "./ListColumnHeader.js";
import { BackgroundType, ColorType } from "grommet/utils";
import { useTheme } from "styled-components";

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
      .filter(userTask => userTask.workflowModuleId !== undefined)
      .reduce((moduleDefinitions, userTask) => moduleDefinitions.includes(userTask)
          ? moduleDefinitions : moduleDefinitions.concat(userTask), existingModuleDefinitions || []);
  if (existingModuleDefinitions?.length !== newModuleDefinitions.length) {
    setModulesOfTasks(newModuleDefinitions);
    const newUserTaskDefinitions: DefinitionOfUserTask = { ...existingUserTaskDefinitions };
    result.userTasks
        .filter(userTask => userTask.taskDefinition !== undefined)
        .forEach(userTask => newUserTaskDefinitions[`${userTask.workflowModuleId}#${userTask.taskDefinition}`] = userTask);
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

const DefaultFooter = ({
  t,
  isPhone,
  isTablet,
  numberOfTasks
}: {
  t: TranslationFunction,
  isPhone: boolean,
  isTablet: boolean,
  numberOfTasks: number
}) => {

  const isNotPhone = !isPhone;

  return (
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
      </Box>);

}

const DefaultHeader = ({
  tasklistApi,
  t,
  refresh,
  refreshDisabled,
  markAsRead,
  markAsUnread,
  markAsReadDisabled,
  claimTasks,
  unclaimTasks,
  claimTasksDisabled,
  assignTasks,
  assignDisabled
}: {
  tasklistApi: TasklistApi,
  t: TranslationFunction,
  refresh: () => void,
  refreshDisabled: boolean,
  markAsRead: () => void,
  markAsUnread: () => void,
  markAsReadDisabled: boolean,
  claimTasks: () => void,
  unclaimTasks: () => void,
  claimTasksDisabled: boolean,
  assignTasks: (userId: string) => void,
  assignDisabled: boolean,
}) => {

  return (
      <Box
          fill
          background='white'
          direction="row"
          align="center"
          justify="start"
          gap="small"
          pad={ { horizontal: 'xsmall' } }>
        <SetReadStatusButtons
            t={ t }
            disabled={ markAsReadDisabled }
            markAsRead={ markAsRead }
            markAsUnread={ markAsUnread }/>
        <ClaimButtons
            t={ t }
            disabled={ claimTasksDisabled }
            claimTasks={ claimTasks }
            unclaimTasks={ unclaimTasks } />
        <AssignButton
            tasklistApi={ tasklistApi }
            t={ t }
            disabled={ assignDisabled }
            assign={ assignTasks }/>
        <RefreshButton
            t={ t }
            refresh={ refresh }
            disabled={ refreshDisabled } />
      </Box>);

}

const CandidateUsersListCell: FC<DefaultListCellProps<BcUserTask>> = ({
  item,
}) => {

  const targetRef = useRef<HTMLDivElement>(null);
  const [ dropIdentifier, setDropIdentifier ] = useState<string | undefined>(undefined);
  const background = colorRowAccordingToUpdateStatus(item);

  return (
      <StyledListCell
          background={ background }
          align="center">
        {
            item.data.candidateUsers && item.data.candidateUsers?.length > 0
                ? <>
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
                                    <FormTrash onClick={ () => item.data.unassign(user) } />
                                  </Box>)
                                }
                              </Box>
                          </Drop>
                    }
                  </>
                : undefined
        }
      </StyledListCell>);

}

const SelectDefaultListCell: FC<DefaultListCellProps<BcUserTask>> = ({
  item,
  selectItem,
}) => {
  const background = colorRowAccordingToUpdateStatus(item);
  return (
      <StyledListCell
          background={ background }
          align="center">
        <CheckBox
            checked={ item.selected }
            onChange={ event => selectItem(event.currentTarget.checked) } />
      </StyledListCell>);
}

const TitleDefaultListCell: FC<DefaultListCellProps<BcUserTask>> = ({
  item,
  currentLanguage,
}) => {
  const titleLanguages = Object.keys(item.data['title']);
  let title;
  if (titleLanguages.includes(currentLanguage)) {
    title = item.data['title'][currentLanguage];
  } else {
    title = item.data['title'][titleLanguages[0]];
  }
  const background = colorRowAccordingToUpdateStatus(item);
  return (
      <StyledListCell
          align="left"
          background={ background }>
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
      </StyledListCell>);
}

const UserTaskDefaultListCell: FC<DefaultListCellProps<BcUserTask>> = ({ column, ...props }) => {
  let Cell: FC<DefaultListCellProps<BcUserTask>>;
  if (column.path === 'id') {
    Cell = SelectDefaultListCell;
  } else if (column.path === 'candidateUsers') {
    Cell = CandidateUsersListCell;
  } else if (column.path === 'title') {
    Cell = TitleDefaultListCell;
  } else {
    Cell = DefaultListCell;
  }

  return (
      <Cell
          column={ column }
          { ...props } />);

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
    name,
    columns,
    children,
    headerHeight = '3rem',
    footer,
    footerHeight = '2rem',
    rowSeparator = true,
    applyBackgroundColor = true,
    showColumnHeaders = true,
    excludeIdColumn = false,
    columnHeader,
    columnHeaderBackground = 'dark-3',
    columnHeaderSeparator,
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
    name?: string,
    columns?: string[];
    children?: ListOfTasksHeaderFooterFunction,
    headerHeight?: string,
    footer?: ListOfTasksHeaderFooterFunction,
    footerHeight?: string,
    rowSeparator?: boolean | string,
    applyBackgroundColor?: boolean,
    showColumnHeaders?: boolean,
    excludeIdColumn?: boolean,
    columnHeader?: FC<DefaultListHeaderAwareProps<any>>,
    columnHeaderBackground?: BackgroundType,
    columnHeaderSeparator?: ColorType | null,
}) => {

  const { isPhone, isTablet } = useResponsiveScreen();
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
            .map(userTask => userTaskDefinitions[`${userTask.workflowModuleId}#${userTask.taskDefinition}`] = userTask)
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
                .filter(m => m.workflowModuleId === definition.workflowModuleId)
                .filter(m => m.userTaskListColumns !== undefined)
                .map(m => m.userTaskListColumns!(definition));
            if (columnsOfProcess.length === 0) return undefined;
            return columnsOfProcess[0];
          })
        .filter(columnsOfTask => columnsOfTask !== undefined)
        .reduce((totalColumns, columnsOfTask) => {
            columnsOfTask!
                .forEach(column => { totalColumns[column.path] = column });
            return totalColumns;
          }, {} as Columns);
    if (totalColumns.id === undefined && !excludeIdColumn) {
      totalColumns.id = {
          title: { [currentLanguage]: 'id' },
          path: 'id',
          width: '2.2rem',
          priority: -1,
          show: true,
          sortable: false,
          filterable: false,
          resizeable: false,
        };
    }
    if (totalColumns.title === undefined) {
      totalColumns.title = {
          title: { [currentLanguage]: t('column_title') },
          path: 'title',
          width: '',
          priority: 0,
          show: true,
          sortable: true,
          filterable: true,
          resizeable: true,
        };
    }
    if (totalColumns.assignee === undefined) {
      totalColumns.assignee = {
          path: 'assignee',
          show: true,
          sortable: false,
          filterable: false,
          title: { [currentLanguage]: t('column_assignee') },
          width: '10rem',
          resizeable: true,
          priority: 1,
        };
    }
    if (totalColumns.candidateUsers === undefined) {
      totalColumns.candidateUsers = {
        path: 'candidateUsers',
        show: true,
        sortable: false,
        filterable: false,
        title: { [currentLanguage]: t('column_candidateUsers') },
        width: '3rem',
        resizeable: false,
        priority: 2,
      };
    }
    const existingColumnsSignature = columnsOfTasks === undefined
        ? ' ' // initial state is different then updates
        : columnsOfTasks.map(c => c.path).join('|');
    const columnsToShow = (Object
        .values(totalColumns) as Array<Column>)
        .sort((a, b) => a.priority - b.priority);
    const newColumnsSignature = columnsToShow.map(c => c.path).join('|');
    if (existingColumnsSignature === newColumnsSignature) {
      return;
    }
    setColumnsOfTasks(columnsToShow);
  }, [ modules, definitionsOfTasks, columnsOfTasks, setColumnsOfTasks, refreshIndicator ]);

  const [ allSelected, setAllSelected ] = useState(false);
  const [ anySelected, setAnySelected ] = useState(false);
  const [ refreshNecessary, setRefreshNecessary ] = useState(false);
  const [ effectiveSort, _setSort ] = useState<string | undefined>(defaultSort);
  const sort = effectiveSort?.startsWith("title.") ? "title" : effectiveSort;
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

  const [ columnWidthAdjustments, setColumnWidthAdjustments ] = useState<ColumnWidthAdjustments>({});
  const getColumnSize = (column: Column) => {
    return !column.resizeable
        ? column.width
        : column.width !== ''
        ? `max(4rem, calc(${column.width} + ${columnWidthAdjustments[column.path] ? columnWidthAdjustments[column.path] : 0}px))`
        : columnWidthAdjustments[column.path]
        ? `${columnWidthAdjustments[column.path]}px`
        : undefined;
  };
  const setColumnWidthAdjustment = (column: Column, adjustment: number) => {
    if (column.width === '') {
      column.width = `${adjustment}px`;
      return;
    }
    const current = columnWidthAdjustments[column.path];
    if (current === adjustment) return;
    setColumnWidthAdjustments({ ...columnWidthAdjustments, [column.path]: adjustment })
  };

  const selectAll = (select: boolean) => {
    (refreshItemRef.current!)(
        userTasks.current!
            .reduce((allItemIds, item) => {
              item.selected = select;
              allItemIds.push(item.id);
              return allItemIds;
            }, new Array<string>())
    );
    setAllSelected(select);
    if (anySelected !== select) {
      setAnySelected(select);
    }
  };
  const selectItem = (item: ListItem<BcUserTask>, select: boolean) => {
    item.selected = select;
    (refreshItemRef.current!)([ item.id ]);
    const currentlyAllSelected = userTasks.current!
        .reduce((allSelected, userTask) => allSelected && userTask.selected, true);
    if (currentlyAllSelected !== allSelected) {
      setAllSelected(currentlyAllSelected);
    }
    const currentlyAnySelected = userTasks.current!
        .reduce((anySelected, userTask) => anySelected || userTask.selected, false);
    if (anySelected !== currentlyAnySelected) {
      setAnySelected(currentlyAnySelected);
    }
  };

  // @ts-ignore
  const columnsOfList: ColumnConfig<ListItem<BcUserTask>>[] = columnsOfTasks === undefined
      ? []
      : columnsOfTasks!
          .filter(column => column.show)
          .filter(column => (columns === undefined) || columns.includes(column.path))
          .map(column => ({
            property: column.path,
            size: getColumnSize(column),
            plain: true,
            verticalAlign: "top",
            header: <ListColumnHeader
                t={ t }
                currentLanguage={ currentLanguage }
                nameOfList={ name }
                columnHeader={ columnHeader }
                hasColumnWidthAdjustment={ columnWidthAdjustments[column.path] !== undefined }
                setColumnWidthAdjustment={ setColumnWidthAdjustment }
                sort={ sort === column.path }
                setSort={ setSort }
                sortAscending={ sortAscending }
                setSortAscending={ setSortAscending }
                column={ column }
                allSelected={ allSelected }
                selectAll={ selectAll } />,
            render: (item: ListItem<BcUserTask>) => <ListCell
                modulesAvailable={ modules! }
                column={ column }
                currentLanguage={ currentLanguage }
                nameOfList={ name }
                typeOfItem={ TypeOfItem.TaskList }
                showUnreadAsBold={ true }
                t={ t }
                // @ts-ignore
                item={ item }
                // @ts-ignore
                selectItem={ selectItem }
                // @ts-ignore
                defaultListCell={ UserTaskDefaultListCell } />
          }));

  const mapToBcUserTask = (userTask: UserTask): BcUserTask => {
      return {
          ...userTask,
          open: () => openTask(userTask),
          navigateToWorkflow: () => navigateToWorkflow(userTask),
          unassign: userId => unassign(userTask.id, userId),
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

  const defaultFooter = () =>
      <DefaultFooter
          isPhone={ isPhone }
          isTablet={ isTablet }
          numberOfTasks={ numberOfTasks }
          t={ t } />;
  const defaultHeader = () =>
      <DefaultHeader
          tasklistApi={ tasklistApi }
          t={ t }
          refresh={ refreshList }
          refreshDisabled={ !refreshNecessary }
          markAsRead={ () => markAsRead(false) }
          markAsUnread={ () => markAsRead(true) }
          markAsReadDisabled={ !anySelected }
          claimTasks={ () => claim(false) }
          unclaimTasks={ () => claim(true) }
          claimTasksDisabled={ !anySelected }
          assignTasks={ assign }
          assignDisabled={ !anySelected } />;
  const themeContext = useTheme();

  return (
      <Grommet
          plain
          style={ { height: '100%', minHeight: '100%', maxHeight: '100%' } }
          theme={ themeContext }>
        <Grid
            key="grid"
            rows={ [ headerHeight, 'auto', footerHeight ] }
            fill>
          {
            children !== undefined
                ? children(isPhone, isTablet, numberOfTasks, columnsOfTasks, sort, setSort, sortAscending, setSortAscending, selectAll,
                    allSelected, refreshList, !refreshNecessary, () => markAsRead(false), () => markAsRead(true), !anySelected,
                    () => claim(false), () => claim(true), !anySelected, assign, !anySelected)
                : defaultHeader()
          }
          {
            (columnsOfTasks === undefined)
                ? <Box key="list"></Box>
                : <Box key="list">
                    <SearchableAndSortableUpdatingList
                        rowSeparator={ rowSeparator }
                        applyBackgroundColor={ applyBackgroundColor }
                        showLoadingIndicator={ showLoadingIndicator }
                        minWidthOfAutoColumn={ minWidthOfTitleColumn }
                        columns={ columnsOfList }
                        itemsRef={ userTasks }
                        showColumnHeaders={ showColumnHeaders }
                        columnHeaderBackground={ columnHeaderBackground }
                        columnHeaderSeparator={ columnHeaderSeparator }
                        updateListRef={ updateListRef }
                        refreshItemRef={ refreshItemRef }
                        refreshNecessaryCallback={ () => setRefreshNecessary(true) }
                        retrieveItems={ async (pageNumber, pageSize, initialTimestamp) =>
  // @ts-ignore
                            await loadUserTasks(
                                tasklistApi,
                                setNumberOfTasks,
                                pageSize,
                                pageNumber,
                                initialTimestamp,
                                effectiveSort,
                                sortAscending,
                                mapToBcUserTask) }
                        reloadItems={ async (numberOfItems, updatedItemsIds, initialTimestamp) =>
  // @ts-ignore
                            await reloadUserTasks(
                                tasklistApi,
                                setNumberOfTasks,
                                modulesOfTasks,
                                setModulesOfTasks,
                                definitionsOfTasks,
                                setDefinitionsOfTasks,
                                numberOfItems,
                                updatedItemsIds,
                                initialTimestamp,
                                effectiveSort,
                                sortAscending,
                                mapToBcUserTask) }
                      />
                    </Box>
          }
          {
            footer !== undefined
                ? footer(isPhone, isTablet, numberOfTasks, columnsOfTasks, sort, setSort, sortAscending, setSortAscending, selectAll,
                    allSelected, refreshList, !refreshNecessary, () => markAsRead(false), () => markAsRead(true), !anySelected,
                    () => claim(false), () => claim(true), !anySelected, assign, !anySelected)
                : defaultFooter()
          }
        </Grid>
      </Grommet>);
      
};

export { ListOfTasks };
