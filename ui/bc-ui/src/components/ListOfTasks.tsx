import React, { FC, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Person, SearchQuery, UserTask, UserTaskEvent } from '@vanillabp/bc-official-gui-client';
import { Box, CheckBox, ColumnConfig, Drop, Grid, Grommet, Text, TextInput, Tip } from 'grommet';
import {
  backgroundColorAccordingToStatus,
  BcUserTask,
  Column,
  debounce,
  DefaultListCell,
  DefaultListCellProps,
  DefaultListHeaderAwareProps,
  EventMessage,
  EventSourceMessage,
  GuiSseHook,
  Link,
  ListCell as StyledListCell,
  ShowLoadingIndicatorFunction,
  textColorAccordingToStatus,
  TranslationFunction,
  useOnClickOutside,
  UserDetailsBox,
  useResponsiveScreen,
  WakeupSseCallback,
} from "@vanillabp/bc-shared";
import {
  FulltextSearchInput,
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
  sortWithColumnTypeSpecificAttributes,
  sortWithoutColumnTypeSpecificAttributes,
  TasklistApi,
  TasklistApiHook,
  TypeOfItem,
  useFederationModules
} from '../index.js';
import { Blank, ContactInfo, FormTrash, FormView, Hide, Refresh, User as UserIcon } from "grommet-icons";
import { User } from "./User.js";
import { AUTO_SIZE_COLUMN, ListColumnHeader } from "./ListColumnHeader.js";
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

const updateModuleDefinitions = (
  userTasks: UserTask[],
  existingModuleDefinitions: UserTask[] | undefined,
  setModulesOfTasks: (modules: UserTask[] | undefined) => void,
  existingUserTaskDefinitions: DefinitionOfUserTask | undefined,
  setDefinitionsOfTasks: (definitions: DefinitionOfUserTask | undefined) => void
) => {
  const newModuleDefinitions = userTasks
      .filter(userTask => userTask.workflowModuleId !== undefined)
      .reduce((moduleDefinitions, userTask) => moduleDefinitions.includes(userTask)
          ? moduleDefinitions : moduleDefinitions.concat(userTask), existingModuleDefinitions || []);
  if (existingModuleDefinitions?.length !== newModuleDefinitions.length) {
    setModulesOfTasks(newModuleDefinitions);
    const newUserTaskDefinitions: DefinitionOfUserTask = { ...existingUserTaskDefinitions };
    userTasks
        .filter(userTask => userTask.taskDefinition !== undefined)
        .forEach(userTask => newUserTaskDefinitions[`${userTask.workflowModuleId}#${userTask.taskDefinition}`] = userTask);
    if ((existingUserTaskDefinitions === undefined)
        || Object.keys(existingUserTaskDefinitions).length !== Object.keys(newUserTaskDefinitions).length) {
      setDefinitionsOfTasks(newUserTaskDefinitions);
    }
  }
}

const loadUserTasks = async (
  tasklistApi: TasklistApi,
  numberOfUserTask: number,
  setNumberOfUserTasks: (number: number) => void,
  existingModuleDefinitions: UserTask[] | undefined,
  setModulesOfTasks: (modules: UserTask[] | undefined) => void,
  existingUserTaskDefinitions: DefinitionOfUserTask | undefined,
  setDefinitionsOfTasks: (definitions: DefinitionOfUserTask | undefined) => void,
  pageSize: number,
  pageNumber: number,
  initialTimestamp: Date | undefined,
  searchQueries: Array<SearchQuery>,
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
      searchQueries,
      initialTimestamp);

  if (numberOfUserTask !== result!.page.totalElements) {
    setNumberOfUserTasks(result!.page.totalElements);
  }
  updateModuleDefinitions(result!.userTasks, existingModuleDefinitions, setModulesOfTasks, existingUserTaskDefinitions, setDefinitionsOfTasks);

  return {
      serverTimestamp: result.serverTimestamp,
      items: result.userTasks.map(userTask => mapToBcUserTask(userTask)),
  	};
};

const reloadUserTasks = async (
  tasklistApi: TasklistApi,
  numberOfUserTask: number,
  setNumberOfUserTasks: (number: number) => void,
  existingModuleDefinitions: UserTask[] | undefined,
  setModulesOfTasks: (modules: UserTask[] | undefined) => void,
  existingUserTaskDefinitions: DefinitionOfUserTask | undefined,
  setDefinitionsOfTasks: (definitions: DefinitionOfUserTask | undefined) => void,
  numberOfItems: number,
  knownItemsIds: Array<string>,
  initialTimestamp: Date | undefined,
  searchQueries: Array<SearchQuery>,
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
      searchQueries,
      initialTimestamp);

  if (numberOfUserTask !== result!.page.totalElements) {
    setNumberOfUserTasks(result!.page.totalElements);
  }
  updateModuleDefinitions(result!.userTasks, existingModuleDefinitions, setModulesOfTasks, existingUserTaskDefinitions, setDefinitionsOfTasks);

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
              flex={ false }
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
                <Box
                    pad={ { vertical: '0.25rem' } }
                    width="1.5rem"
                    height="2rem">
                  <UserIcon
                      color={ textColor } />
                </Box>
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
                  height="2rem"
                  pad={ { horizontal: '0.4rem' } }
                  round={ { size: '0.4rem', corner: 'right' } }
                  align="center"
                  justify="center"
                  border={ { color, side: 'left' } }>
                <Box
                    style={ { position: 'relative' } }
                    pad={ { vertical: '0.25rem' } }
                    width="1.5rem"
                    height="2rem">
                  <UserIcon
                      color={ textColor } />
                  <Blank
                      style={ { position: 'absolute' } }
                      color={ textColor }>
                    <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                      <line x1="24" y1="4" x2="4" y2="24" strokeWidth="2" />
                    </svg>
                  </Blank>
                </Box>
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
  const [ users, setUsers ] = useState<Array<Person> | undefined>(undefined);
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

  const { isPhone } = useResponsiveScreen();
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
            width={ isPhone ? "10rem" : "15rem" }
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
                                t={ t }
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
              hoverIndicator={ disabled ? 'light-2' : "list-refresh" }
              focusIndicator={ false }
              onClick={ refresh }
              direction="row"
              elevation="small"
              background={ !disabled ? { color: "list-refresh", opacity: "strong" } : undefined }
              round={ { size: '0.4rem' } }
              border={ { color } }>
            <Tip
                content={ t('refresh_tasks') }>
              <Box
                  height="2rem"
                  width="2rem"
                  pad={ { horizontal: '0.4rem' } }
                  align="center"
                  direction="row"
                  justify="center">
                <Refresh
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
                  background="list-new">
                <Text size="xsmall" color='list-text-new'>T</Text>
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
                  background="list-updated">
                <Text size="xsmall" color='list-text-updated'>T</Text>
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
                  background="list-ended">
                <Text size="xsmall" color='list-text-ended'>T</Text>
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
                  background="list-removed_from_list">
                <Text size="xsmall" color='list-text-removed_from_list'>T</Text>
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
  assignDisabled,
  initialKwicQuery,
  limitListToKwic,
  kwic
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
  initialKwicQuery: (columnPath?: string) => string,
  limitListToKwic: (columnPath: string | undefined, query?: string) => void,
  kwic: (columnPath: string | undefined, query: string) => Promise<Array<{ item: string, count: number }>>,
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
        <FulltextSearchInput
            t={ t }
            initialQuery={ initialKwicQuery }
            limitListToKwic={ limitListToKwic }
            kwic={ kwic } />
      </Box>);

}

const CandidateUsersListCell: FC<DefaultListCellProps<BcUserTask>> = ({
  t,
  item,
}) => {

  const targetRef = useRef<HTMLDivElement>(null);
  const [ dropIdentifier, setDropIdentifier ] = useState<string | undefined>(undefined);
  const background = backgroundColorAccordingToStatus(item);

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
                                stretch
                                round="xsmall"
                                onMouseLeave={ () => setDropIdentifier(undefined) }
                                target={ targetRef }>
                              <Grid columns={['auto', 'auto']} gap="xsmall" margin="xsmall">
                                {
                                  item.data.candidateUsers?.map(user => <>
                                      <Text
                                          style={ { whiteSpace: "nowrap" } }
                                          tip={ { content: <UserDetailsBox user={ user } t={ t } /> } }>
                                        {
                                          user.displayShort ?? user.email ?? user.id
                                        }
                                      </Text>
                                      <FormTrash onClick={ () => item.data.unassign(user.id) } />
                                  </>)
                                }
                              </Grid>
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
  const background = backgroundColorAccordingToStatus(item);
  return (
      <StyledListCell
          background={ background }
          align="center">
        <CheckBox
            id={ item.id }
            checked={ item.selected }
            onChange={ event => selectItem(event.currentTarget.checked) } />
      </StyledListCell>);
}

const TitleDefaultListCell: FC<DefaultListCellProps<BcUserTask>> = ({
  item,
  currentLanguage,
}) => {
  const titleLanguages = Object.keys(item.data['title']);
  let title: string;
  if (titleLanguages.includes(currentLanguage)) {
    title = item.data['title'][currentLanguage];
  } else {
    title = item.data['title'][titleLanguages[0]];
  }
  const background = backgroundColorAccordingToStatus(item);
  const text = textColorAccordingToStatus(item);
  return (
      <StyledListCell
          align="left"
          background={ background }>
        <Text
            weight={ item.read === undefined ? 'bold' : 'normal' }
            truncate="tip">
          <Link
              color={ text }
              // @ts-ignore
              onClick={ item.data.open }>
            { title }
          </Link>
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
    defaultSortAscending = true,
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
    columnHeaderSeparator = 'light-6',
    defaultSearchQueries = [],
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
    defaultSearchQueries?: Array<SearchQuery>,
}) => {

  const { isPhone, isTablet } = useResponsiveScreen();
  const wakeupSseCallback = useRef<WakeupSseCallback>(undefined);
  const tasklistApi = useTasklistApi(wakeupSseCallback);
  const [ refreshIndicator, setRefreshIndicator ] = useState<Date>(new Date());
  const [ searchQueries, setSearchQueries ] = useState<Array<SearchQuery>>(defaultSearchQueries);

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

  const mapToBcUserTask = (userTask: UserTask): BcUserTask => {
    return {
      ...userTask,
      open: () => openTask(userTask),
      navigateToWorkflow: () => navigateToWorkflow(userTask),
      assign: userId => assignFunction(userTask.id, userId, false),
      unassign: userId => assignFunction(userTask.id, userId, true),
    };
  };

  const userTasks = useRef<Array<ListItem<UserTask>> | undefined>(undefined);
  const [ numberOfTasks, setNumberOfTasks ] = useState<number>(0);
  const [ modulesOfTasks, setModulesOfTasks ] = useState<UserTask[] | undefined>(undefined);
  const [ definitionsOfTasks, setDefinitionsOfTasks ] = useState<DefinitionOfUserTask | undefined>(undefined);
  useEffect(() => {
      const loadMetaInformation = async () => {
        await loadUserTasks(tasklistApi, numberOfTasks, setNumberOfTasks, modulesOfTasks,
            setModulesOfTasks, definitionsOfTasks, setDefinitionsOfTasks, 20, 0, undefined,
            searchQueries, undefined, true, mapToBcUserTask);
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
  const autoColumnWidth = useRef<string>(AUTO_SIZE_COLUMN);
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
          type: 'i18n',
          path: 'title',
          width: autoColumnWidth.current,
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
          type: 'person',
          show: true,
          sortable: true,
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
  }, [ modules, definitionsOfTasks, columnsOfTasks, setColumnsOfTasks, refreshIndicator, autoColumnWidth ]);

  const [ allSelected, setAllSelected ] = useState(false);
  const [ anySelected, setAnySelected ] = useState(false);
  const [ refreshNecessary, setRefreshNecessary ] = useState(false);
  const [ effectiveSort, _setSort ] = useState<string | undefined>(defaultSort);
  const sort = sortWithoutColumnTypeSpecificAttributes(currentLanguage, effectiveSort);
  const [ sortAscending, _setSortAscending ] = useState(defaultSortAscending);

  const refreshList = () => {
    userTasks.current = undefined;
    setRefreshNecessary(false);
    setColumnsOfTasks(undefined);
    setRefreshIndicator(new Date());
  }

  const setSort = (column?: Column) => {
    let ascending: boolean;
    if (!column) {
      ascending = defaultSortAscending;
      column = columnsOfTasks?.filter(c => c.path === defaultSort).at(0);
    } else {
      ascending = true;
    }
    _setSort(sortWithColumnTypeSpecificAttributes(currentLanguage, column));
    _setSortAscending(ascending);
    refreshList();
  };
  const setSortAscending = (newSortAscending: boolean) => {
    _setSortAscending(newSortAscending);
    refreshList();
  }

  const assignFunction = (userTaskId: string, userId: string, unassign: boolean) => {
    tasklistApi.assignTask(userTaskId, userId, unassign);
  };

  const [ columnWidthAdjustments, setColumnWidthAdjustments ] = useState<ColumnWidthAdjustments>({});
  const getColumnSize = useCallback((column: Column) => !column.resizeable
        ? column.width
        : column.width !== AUTO_SIZE_COLUMN
        ? `max(4rem, calc(${column.width} + ${columnWidthAdjustments[column.path] ? columnWidthAdjustments[column.path] : 0}px))`
        : columnWidthAdjustments[column.path]
        ? `${columnWidthAdjustments[column.path]}px`
        : undefined
    , [ columnWidthAdjustments ]);
  const setColumnWidthAdjustment = useCallback((column: Column, adjustment: number) => {
      if (column.width === AUTO_SIZE_COLUMN) {
        column.width = `${adjustment}px`;
        autoColumnWidth.current = column.width;
        return;
      }
      const current = columnWidthAdjustments[column.path];
      if (current === adjustment) return;
      setColumnWidthAdjustments({ ...columnWidthAdjustments, [column.path]: adjustment })
    }, [ columnWidthAdjustments, setColumnWidthAdjustments, autoColumnWidth ]);

  const selectAll = useCallback((select: boolean) => {
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
    }, [ refreshItemRef, userTasks, allSelected, anySelected, setAnySelected, setAllSelected ]);
  const selectItem = useCallback((item: ListItem<BcUserTask>, select: boolean) => {
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
  }, [ refreshItemRef, userTasks, allSelected, anySelected, setAnySelected, setAllSelected ]);

  // @ts-ignore
  const columnsOfList: ColumnConfig<ListItem<BcUserTask>>[] = useMemo(() =>
      columnsOfTasks === undefined
        ? []
        : columnsOfTasks!
            .filter(column => column.show)
            .filter(column => (columns === undefined) || columns.includes(column.path))
            .map((column, columnIndex, allColumns) => ({
              property: column.path,
              size: getColumnSize(column),
              plain: true,
              verticalAlign: "top",
              header: <ListColumnHeader
                  t={ t }
                  currentLanguage={ currentLanguage }
                  nameOfList={ name }
                  columnHeader={ columnHeader }
                  columnWidthAdjustment={ columnWidthAdjustments[column.path] }
                  setColumnWidthAdjustment={ setColumnWidthAdjustment }
                  sort={ sort === column.path }
                  setSort={ setSort }
                  isDefaultSort={ column.path === defaultSort }
                  sortAscending={ sortAscending }
                  defaultSortAscending={ defaultSortAscending }
                  setSortAscending={ setSortAscending }
                  column={ column }
                  columnIndex={ columnIndex }
                  numberOfAllColumns={ allColumns.length }
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
            })),
      [ modules, currentLanguage, name, columnsOfTasks, setSort, setSortAscending, sortAscending,
              defaultSortAscending, selectItem, selectAll, allSelected, getColumnSize, columnWidthAdjustments,
              defaultSort, setColumnWidthAdjustment ]);

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

  const initialKwicQuery = (columnPath?: string) => {
    const query = searchQueries
        .filter(query => query.path === columnPath)
        .map(query => query.query);
    if (query.length === 0) return '';
    return query[0];
  }
  // const kwicInProgress = useRef(false);
  // useLayoutEffect(() => {
  //   kwicInProgress.current = false;
  // }, [ kwicInProgress ]);
  const setKwic = (columnPath: string | undefined, value?: string) => {
    const newSearchQueries = searchQueries
        .filter(query => query.path !== columnPath);
    if ((value !== undefined)
        && (value.trim().length > 0)) {
      newSearchQueries.push({ path: columnPath, query: value, caseInsensitive: true });
    }
    setSearchQueries(newSearchQueries);
    // kwicInProgress.current = true;
    refreshList();
  };
  const kwic = async (columnPath: string | undefined, query: string) => {
    return await tasklistApi.kwicUserTasks(
        query,
        columnPath,
        searchQueries);
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
          assignDisabled={ !anySelected }
          initialKwicQuery={ initialKwicQuery }
          limitListToKwic={ setKwic }
          kwic={ kwic } />;
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
                                numberOfTasks,
                                setNumberOfTasks,
                                modulesOfTasks,
                                setModulesOfTasks,
                                definitionsOfTasks,
                                setDefinitionsOfTasks,
                                pageSize,
                                pageNumber,
                                initialTimestamp,
                                searchQueries,
                                effectiveSort,
                                sortAscending,
                                mapToBcUserTask) }
                        reloadItems={ async (numberOfItems, updatedItemsIds, initialTimestamp) =>
  // @ts-ignore
                            await reloadUserTasks(
                                tasklistApi,
                                numberOfTasks,
                                setNumberOfTasks,
                                modulesOfTasks,
                                setModulesOfTasks,
                                definitionsOfTasks,
                                setDefinitionsOfTasks,
                                numberOfItems,
                                updatedItemsIds,
                                initialTimestamp,
                                searchQueries,
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
