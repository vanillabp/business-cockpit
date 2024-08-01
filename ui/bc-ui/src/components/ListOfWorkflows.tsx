import { FC, ReactNode, useEffect, useMemo, useRef, useState } from 'react';
import { SearchQuery, Workflow, WorkflowEvent } from '@vanillabp/bc-official-gui-client';
import { Box, CheckBox, ColumnConfig, Grid, Text, TextInput, Tip } from 'grommet';
import {
  BcUserTask,
  BcWorkflow,
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
  GetUserTasksFunction,
  GuiSseHook,
  Link,
  ListCell as StyledListCell,
  ShowLoadingIndicatorFunction,
  TranslationFunction,
  useResponsiveScreen,
  WakeupSseCallback,
} from "@vanillabp/bc-shared";
import {
  ListCell,
  ListItem,
  ListItems,
  ListOfWorkflowsHeaderFooterFunction,
  ModuleDefinition,
  NavigateToWorkflowFunction,
  OpenTaskFunction,
  RefreshItemCallbackFunction,
  ReloadCallbackFunction,
  SearchableAndSortableUpdatingList,
  sortWithColumnTypeSpecificAttributes,
  sortWithoutColumnTypeSpecificAttributes,
  TypeOfItem,
  useFederationModules,
  WorkflowlistApi,
  WorkflowlistApiHook
} from '../index.js';
import { ListColumnHeader } from "./ListColumnHeader.js";
import { Clear, Refresh, Search } from "grommet-icons";
import { BackgroundType, ColorType } from "grommet/utils";

const minWidthOfTitleColumn = '20rem';

interface Columns {
  [key: string]: Column;
}

interface Suggestion {
  label: ReactNode;
  value: string | undefined;
}

interface DefinitionOfWorkflow {
  [key: string]: Workflow;
}

interface ColumnWidthAdjustments {
  [key: string]: number
}

const updateModuleDefinitions = (
    workflows: Workflow[],
    existingModuleDefinitions: Workflow[] | undefined,
    setModulesOfWorkflows: (modules: Workflow[] | undefined) => void,
    existingWorkflowDefinitions: DefinitionOfWorkflow | undefined,
    setDefinitionsOfWorkflows: (definitions: DefinitionOfWorkflow | undefined) => void) => {

  const newModuleDefinitions = workflows
      .filter(workflow => workflow.workflowModuleId !== undefined)
      .reduce((moduleDefinitions, workflow) => moduleDefinitions.includes(workflow)
          ? moduleDefinitions : moduleDefinitions.concat(workflow), existingModuleDefinitions || []);
  if (existingModuleDefinitions?.length !== newModuleDefinitions.length) {
    setModulesOfWorkflows(newModuleDefinitions);
    const newWorkflowDefinitions: DefinitionOfWorkflow = { ...existingWorkflowDefinitions };
    workflows
        .forEach(workflow => newWorkflowDefinitions[`${workflow.workflowModuleId}#${workflow.bpmnProcessId}`] = workflow);
    if ((existingWorkflowDefinitions === undefined)
        || Object.keys(existingWorkflowDefinitions).length !== Object.keys(newWorkflowDefinitions).length) {
      setDefinitionsOfWorkflows(newWorkflowDefinitions);
    }
  }

}

const loadWorkflows = async (
  workflowlistApi: WorkflowlistApi,
  setNumberOfWorkflows: (number: number) => void,
  existingModuleDefinitions: Workflow[] | undefined,
  setModulesOfWorkflows: (modules: Workflow[] | undefined) => void,
  existingWorkflowDefinitions: DefinitionOfWorkflow | undefined,
  setDefinitionsOfWorkflows: (definitions: DefinitionOfWorkflow | undefined) => void,
  pageSize: number,
  pageNumber: number,
  initialTimestamp: Date | undefined,
  searchQueries: Array<SearchQuery>,
  sort: string | undefined,
  sortAscending: boolean,
  mapToBcWorkflow: (workflow: Workflow) => BcWorkflow,
): Promise<ListItems<Workflow>> => {

  const result = await workflowlistApi
        .getWorkflows(new Date().getTime().toString(), pageNumber, pageSize, sort, sortAscending, searchQueries, initialTimestamp);

  setNumberOfWorkflows(result!.page.totalElements);
  updateModuleDefinitions(result!.workflows, existingModuleDefinitions, setModulesOfWorkflows, existingWorkflowDefinitions, setDefinitionsOfWorkflows)

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
  searchQueries: Array<SearchQuery>,
  sort: string | undefined,
  sortAscending: boolean,
  mapToBcWorkflow: (workflow: Workflow) => BcWorkflow,
): Promise<ListItems<Workflow>> => {

  const result = await workflowlistApi.getWorkflowsUpdate(
      new Date().getTime().toString(),
      numberOfItems,
      knownItemsIds,
      sort,
      sortAscending,
      searchQueries,
      initialTimestamp);

  setNumberOfWorkflows(result!.page.totalElements);
  updateModuleDefinitions(result!.workflows, existingModuleDefinitions, setModulesOfWorkflows, existingWorkflowDefinitions, setDefinitionsOfWorkflows)
  
  return {
      serverTimestamp: result.serverTimestamp,
      items: result.workflows.map(workflow => mapToBcWorkflow(workflow))
    };

};

const FulltextSearch = ({
  t,
  initialQuery,
  limitListToKwic,
  kwic,
  // focus = false,
}: {
  t: TranslationFunction,
  initialQuery: (columnPath?: string) => string,
  limitListToKwic: (columnPath: string | undefined, query?: string) => void,
  kwic: (columnPath: string | undefined, query: string) => Promise<Array<{ item: string, count: number }>>,
  // focus?: boolean,
}) => {
  const { isPhone } = useResponsiveScreen();
  const textFieldRef = useRef<HTMLInputElement>(null);
  const [ query, setQuery ] = useState(initialQuery)
  const currentQuery = useRef<string>(initialQuery(undefined));
  const [ suggestions, setSuggestions ] =
      useState<Array<Suggestion> | undefined>(undefined);
  const ignoreKeyEnter = useRef(false);
/*
  useEffect(() => {
    if (!focus) return;
    if (!textFieldRef.current) return;
    textFieldRef.current.focus();
  }, [textFieldRef, focus]);
*/
  const select = (value: string, suggestion: boolean) => {
    if (suggestion) {
      ignoreKeyEnter.current  = true;
    } else if (ignoreKeyEnter.current) {
      ignoreKeyEnter.current = false;
      return;
    }
    if (value === undefined) return;
    setSuggestions(undefined);
    setQuery(value);
    currentQuery.current = value;
    limitListToKwic(undefined, value);
  };
  const kwicDebounced = useMemo(() => debounce(async () => {
      const result = await kwic(undefined, currentQuery.current);
      const newSuggestions = result
          .map(r => ({
            value: r.item,
            label: <Box
                direction="row"
                justify="between"
                pad="xsmall">
              <Text
                  weight="bold"
                  truncate="tip">
                { r.item }
              </Text>
              <Box
                  align="right">
                { r.count }
              </Box>
            </Box> }));
      setSuggestions(
          newSuggestions.length > 20
              ? [ ...newSuggestions.slice(0, 20), { value: undefined, label: <Box pad="xsmall">{ t('kwic_to-many-hits') }</Box> } ]
              : newSuggestions);
    }, 300), [ currentQuery, setSuggestions ]);
  const updateResult = (newQuery: string) => {
    currentQuery.current = newQuery;
    setQuery(newQuery);
    if (newQuery.length < 3) {
      if (suggestions) {
        setSuggestions(undefined);
      }
      return;
    }
    kwicDebounced();
  };
  const clear = () => {
    currentQuery.current = '';
    setQuery('');
    setSuggestions(undefined);
    textFieldRef.current!.focus();
    limitListToKwic(undefined, undefined);
  };

  return (
      <Box
          width={ isPhone ? "100%" : "min(100%, 30rem)" }
          elevation="small"
          alignContent="center"
          hoverIndicator={ "white" }
          focusIndicator={ false }
          round={ { size: '0.4rem' } }
          border={ { color: "dark-4" } }>
        <Grid
            columns={ [ 'auto', '2rem' ] }
            fill>
          <Box
              pad={ { horizontal: '0.4rem', vertical: '0.25rem' } }
              direction="row"
              justify="center">
            <TextInput
                plain="full"
                ref={ textFieldRef }
                value={ query }
                placeholder={ t('kwic_placeholder') }
                onKeyDown={ event => event.key === 'Enter' ? select(query, false) : undefined }
                onChange={ event => updateResult(event.target.value) }
                suggestions={ suggestions === undefined
                    ? []
                    : suggestions.length === 0
                        ? [ { value: undefined, label: <Box pad="xsmall">{ t('kwic_no-hit') }</Box> } ]
                        : suggestions }
                onSuggestionSelect={ x => select(x.suggestion.value, true) }
                focusIndicator={ false }
                reverse />
          </Box>
          <Tip
              content={ t('kwic_tooltip') }>
            <Box
                align="center"
                justify="center">
              {
                Boolean(query)
                    ? <Clear
                        onMouseUp={ clear }
                        color="dark-4" />
                    : <Search
                        color="dark-4" />
              }
            </Box>
          </Tip>
        </Grid>
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
        content={ t('refresh_workflows') }>
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
  numberOfWorkflows
}: {
  t: TranslationFunction,
  isPhone: boolean,
  isTablet: boolean,
  numberOfWorkflows: number
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
                  background="list-updated">
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
                  background="list-ended">
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
                  background="list-ended">
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
    t,
    isPhone,
    isTablet,
    numberOfWorkflows,
    selectAll,
    allSelected,
    refresh,
    refreshDisabled,
    initialKwicQuery,
    limitListToKwic,
    kwic
}: {
    t: TranslationFunction,
    isPhone: boolean,
    isTablet: boolean,
    numberOfWorkflows: number,
    selectAll: (select: boolean) => void,
    allSelected: boolean,
    refresh: () => void,
    refreshDisabled: boolean,
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
        <FulltextSearch
            t={ t }
            initialQuery={ initialKwicQuery }
            limitListToKwic={ limitListToKwic }
            kwic={ kwic }
            // focus={ kwicInProgress.current }
        />
        <RefreshButton
            t={ t }
            refresh={ refresh }
            disabled={ refreshDisabled } />
      </Box>);

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
  let title: string;
  if (titleLanguages.includes(currentLanguage)) {
    title = item.data['title'][currentLanguage];
  } else {
    title = item.data['title'][titleLanguages[0]];
  }
  const background = colorRowAccordingToUpdateStatus(item);
  return useMemo(() => (
        <StyledListCell
            align="left"
            background={ background }>
          <Text
              color={ colorForEndedItemsOrUndefined(item) }
              weight={ item.read === undefined
                  ? 'bold'
                  : 'normal' }
              truncate="tip">
            <Link
                // @ts-ignore
                onClick={ item.data.navigateToWorkflow }>
              { title }
            </Link>
          </Text>
        </StyledListCell>), [ item.id, title ]);
}

const WorkflowDefaultListCell: FC<DefaultListCellProps<BcUserTask>> = ({ column, ...props }) => {

  let Cell: FC<DefaultListCellProps<BcUserTask>>;
  if (column.path === 'id') {
     Cell = SelectDefaultListCell;
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

const ListOfWorkflows = ({
  showLoadingIndicator,
  useGuiSse,
  useWorkflowlistApi,
  openTask,
  navigateToWorkflow,
  currentLanguage,
  defaultSort,
  defaultSortAscending = true,
  name,
  columns,
  children,
  headerHeight = '3rem',
  footer,
  footerHeight = '2rem',
  t,
  rowSeparator,
  applyBackgroundColor = true,
  showColumnHeaders = true,
  excludeIdColumn = false,
  columnHeader,
  columnHeaderBackground = 'dark-3',
  columnHeaderSeparator,
  defaultSearchQueries = [],
}: {
  showLoadingIndicator: ShowLoadingIndicatorFunction,
  useGuiSse: GuiSseHook,
  useWorkflowlistApi: WorkflowlistApiHook,
  openTask: OpenTaskFunction,
  navigateToWorkflow: NavigateToWorkflowFunction,
  currentLanguage: string,
  defaultSort?: string,
  defaultSortAscending?: boolean,
  name?: string,
  columns?: string[];
  children?: ListOfWorkflowsHeaderFooterFunction,
  headerHeight?: string,
  footer?: ListOfWorkflowsHeaderFooterFunction,
  footerHeight?: string,
  rowSeparator?: boolean | string,
  applyBackgroundColor?: boolean,
  showColumnHeaders?: boolean,
  excludeIdColumn?: boolean,
  columnHeader?: FC<DefaultListHeaderAwareProps<any>>,
  columnHeaderBackground?: BackgroundType,
  columnHeaderSeparator?: ColorType | null,
  t: TranslationFunction,
  defaultSearchQueries?: Array<SearchQuery>,
}) => {

  const { isPhone, isTablet, isNotPhone } = useResponsiveScreen();

  const wakeupSseCallback = useRef<WakeupSseCallback>(undefined);
  const workflowlistApi = useWorkflowlistApi(wakeupSseCallback);
  const [ refreshIndicator, setRefreshIndicator ] = useState<Date>(new Date());
  const [ searchQueries, setSearchQueries ] = useState<Array<SearchQuery>>(defaultSearchQueries);

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
  const refreshItemRef = useRef<RefreshItemCallbackFunction | undefined>(undefined);

  const mapToBcWorkflow = (workflow: Workflow): BcWorkflow => {
    const getUserTasksFunction: GetUserTasksFunction = async (
        activeOnly,
        limitListAccordingToCurrentUsersPermissions
    ) => {
      return (await workflowlistApi
          .getUserTasksOfWorkflow(
              workflow.id,
              activeOnly,
              limitListAccordingToCurrentUsersPermissions))
          .map(userTask => ({
            ...userTask,
            open: () => openTask(userTask),
            navigateToWorkflow: () => {}, // don't change view because workflow is already shown
          } as BcUserTask));
    };
    return {
      ...workflow,
      navigateToWorkflow: () => navigateToWorkflow(workflow),
      getUserTasks: getUserTasksFunction,
    };
  };

  const workflows = useRef<Array<ListItem<Workflow>> | undefined>(undefined);
  const [ numberOfWorkflows, setNumberOfWorkflows ] = useState<number>(-1);
  const [ modulesOfWorkflows, setModulesOfWorkflows ] = useState<Workflow[] | undefined>(undefined);
  const [ definitionsOfWorkflows, setDefinitionsOfWorkflows ] = useState<DefinitionOfWorkflow | undefined>(undefined);
  useEffect(() => {
      const loadMetaInformation = async () => {
        await loadWorkflows(workflowlistApi, setNumberOfWorkflows, modulesOfWorkflows, setModulesOfWorkflows,
            definitionsOfWorkflows, setDefinitionsOfWorkflows,20, 0, undefined,
            searchQueries, undefined, true, mapToBcWorkflow);
      };
      if (workflows.current === undefined) {
        showLoadingIndicator(true);
        loadMetaInformation();
      }
    },
    // workflowlistApi is not part of dependency because it changes one time but this is irrelevant to the
    // purpose of preloading modules used by workflows
    [ workflows, setNumberOfWorkflows, setModulesOfWorkflows, setDefinitionsOfWorkflows, showLoadingIndicator, refreshIndicator ]);

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
        .map(definition => definitionsOfWorkflows[definition])
        .map(definition => {
            const columnsOfWorkflow = modules
                .filter(m => m !== undefined)
                .filter(m => m.workflowModuleId === definition.workflowModuleId)
                .filter(m => m.workflowListColumns !== undefined)
                .map(module => module.workflowListColumns!(definition));
            if (columnsOfWorkflow.length === 0) return undefined;
            return columnsOfWorkflow[0];
          })
        .filter(columnsOfWorkflow => columnsOfWorkflow !== undefined)
        .reduce((totalColumns, columnsOfWorkflow) => {
            columnsOfWorkflow!
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
        type: 'i18n',
        width: '',
        priority: 0,
        show: true,
        sortable: true,
        filterable: true,
        resizeable: true,
      };
    }
    const existingColumnsSignature = columnsOfWorkflows === undefined
        ? ' ' // initial state is different then updates
        : columnsOfWorkflows.map(c => c.path).join('|');
    const columnsToShow = (Object
        .values(totalColumns) as Array<Column>)
        .sort((a, b) => a.priority - b.priority);
    const newColumnsSignature = columnsToShow.map(c => c.path).join('|');
    if (existingColumnsSignature === newColumnsSignature) {
      return;
    }
    setColumnsOfWorkflows(columnsToShow);
  }, [ modules, definitionsOfWorkflows, columnsOfWorkflows, setColumnsOfWorkflows, refreshIndicator ]);

  const [ allSelected, setAllSelected ] = useState(false);
  const [ anySelected, setAnySelected ] = useState(false);
  const [ refreshNecessary, setRefreshNecessary ] = useState(false);
  const [ effectiveSort, _setSort ] = useState<string | undefined>(defaultSort);
  const sort = sortWithoutColumnTypeSpecificAttributes(currentLanguage, effectiveSort);
  const [ sortAscending, _setSortAscending ] = useState(defaultSortAscending);

  const refreshList = () => {
    workflows.current = undefined;
    setRefreshNecessary(false);
    setColumnsOfWorkflows(undefined);
    setRefreshIndicator(new Date());
  }

  const setSort = (column?: Column) => {
    let ascending: boolean;
    if (!column) {
      ascending = defaultSortAscending;
      column = columnsOfWorkflows?.filter(c => c.path === defaultSort).at(0);
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
        workflows.current!
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
  const selectItem = (item: ListItem<BcWorkflow>, select: boolean) => {
    item.selected = select;
    (refreshItemRef.current!)([ item.id ]);
    const currentlyAllSelected = workflows.current!
        .reduce((allSelected, userTask) => allSelected && userTask.selected, true);
    if (currentlyAllSelected !== allSelected) {
      setAllSelected(currentlyAllSelected);
    }
    const currentlyAnySelected = workflows.current!
        .reduce((anySelected, userTask) => anySelected || userTask.selected, false);
    if (anySelected !== currentlyAnySelected) {
      setAnySelected(currentlyAnySelected);
    }
  };

  const columnsOfList: ColumnConfig<ListItem<BcUserTask>>[] = columnsOfWorkflows === undefined
      ? []
      : columnsOfWorkflows!
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
                  hasColumnWidthAdjustment={ columnWidthAdjustments[column.path] !== undefined }
                  setColumnWidthAdjustment={ setColumnWidthAdjustment }
                  sort={ sort === column.path }
                  setSort={ setSort }
                  isDefaultSort={ column.path === defaultSort }
                  sortAscending={ sortAscending }
                  setSortAscending={ setSortAscending }
                  defaultSortAscending={ defaultSortAscending }
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
                  typeOfItem={ TypeOfItem.WorkflowList }
                  showUnreadAsBold={ false }
                  t={ t }
                  // @ts-ignore
                  item={ item }
                  // @ts-ignore
                  selectItem={ selectItem }
                  // @ts-ignore
                  defaultListCell={ WorkflowDefaultListCell } />
            }));

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
      newSearchQueries.push({ path: columnPath, query: value });
    }
    setSearchQueries(newSearchQueries);
    // kwicInProgress.current = true;
    refreshList();
  };
  const kwic = async (columnPath: string | undefined, query: string) => {
    return await workflowlistApi.kwicWorkflows(
        query,
        columnPath,
        searchQueries);
  };

  const defaultHeader = () =>
      <DefaultHeader
          t={ t }
          isPhone={ isPhone }
          isTablet={ isTablet }
          numberOfWorkflows={ numberOfWorkflows }
          selectAll={ select => {} }
          allSelected={ allSelected }
          refresh={ refreshList }
          refreshDisabled={ !refreshNecessary }
          initialKwicQuery={ initialKwicQuery }
          limitListToKwic={ setKwic }
          kwic={ kwic } />;
  const defaultFooter = () =>
      <DefaultFooter
          isPhone={ isPhone }
          isTablet={ isTablet }
          numberOfWorkflows={ numberOfWorkflows }
          t={ t } />;
  return (
      <Grid
          rows={ [ headerHeight, 'auto', footerHeight ] }
          fill>
        {
          children !== undefined
              ? children(isPhone, isTablet, numberOfWorkflows, columnsOfWorkflows, sort, setSort, sortAscending,
                    setSortAscending, selectAll, allSelected, refreshList, !refreshNecessary, initialKwicQuery, setKwic, kwic)
              : defaultHeader()
        }
        {
          (columnsOfWorkflows === undefined)
              ? <Box key="list"></Box>
              : <Box key="list">
                  <SearchableAndSortableUpdatingList
                      rowSeparator={ rowSeparator }
                      applyBackgroundColor={ applyBackgroundColor }
                      showLoadingIndicator={ showLoadingIndicator }
                      minWidthOfAutoColumn={ minWidthOfTitleColumn }
                      showColumnHeaders={ showColumnHeaders }
                      columnHeaderBackground={ columnHeaderBackground }
                      columnHeaderSeparator={ columnHeaderSeparator }
                      columns={ columnsOfList }
                      itemsRef={ workflows }
                      updateListRef= { updateListRef }
                      refreshItemRef={ refreshItemRef }
                      refreshNecessaryCallback={ () => setRefreshNecessary(true) }
                      retrieveItems={ (pageNumber, pageSize, initialTimestamp) =>
// @ts-ignore
                          loadWorkflows(
                              workflowlistApi,
                              setNumberOfWorkflows,
                              modulesOfWorkflows,
                              setModulesOfWorkflows,
                              definitionsOfWorkflows,
                              setDefinitionsOfWorkflows,
                              pageSize,
                              pageNumber,
                              initialTimestamp,
                              searchQueries,
                              effectiveSort,
                              sortAscending,
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
                              searchQueries,
                              effectiveSort,
                              sortAscending,
                              mapToBcWorkflow) }
                    />
                </Box>
        }
        {
          footer !== undefined
              ? footer(isPhone, isTablet, numberOfWorkflows, columnsOfWorkflows, sort, setSort, sortAscending, setSortAscending,
                    selectAll, allSelected, refreshList, !refreshNecessary, initialKwicQuery, setKwic, kwic)
              : defaultFooter()
        }
      </Grid>);
};

export { ListOfWorkflows };
