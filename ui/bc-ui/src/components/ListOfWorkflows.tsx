import { ReactNode, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { SearchQuery, Workflow, WorkflowEvent } from '@vanillabp/bc-official-gui-client';
import { Box, CheckBox, ColumnConfig, Grid, Text, TextInput, Tip } from 'grommet';
import {
  BcUserTask,
  BcWorkflow,
  colorForEndedItemsOrUndefined,
  Column,
  debounce,
  EventMessage,
  EventSourceMessage,
  GetUserTasksFunction,
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
  TypeOfItem,
  useFederationModules,
  WorkflowlistApi,
  WorkflowlistApiHook
} from '../index.js';
import { TranslationFunction } from "../types/translate";
import { ListColumnHeader } from "./ListColumnHeader.js";
import { Clear, Search } from "grommet-icons";

const minWidthOfTitleColumn = '20rem';

interface Suggestion {
  label: ReactNode;
  value: string | undefined;
}

interface ColumnWidthAdjustments {
  [key: string]: number
}

const loadWorkflows = async (
  workflowlistApi: WorkflowlistApi,
  setNumberOfWorkflows: (number: number) => void,
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

const FulltextSearch = ({
  t,
  initialQuery = '',
  workflowlistApi,
  kwic,
  searchQueries,
  focus = false,
}: {
  t: TranslationFunction,
  initialQuery?: string,
  workflowlistApi: WorkflowlistApi,
  kwic: (query?: string) => void,
  searchQueries: Array<SearchQuery>,
  focus?: boolean,
}) => {
  const { isPhone } = useResponsiveScreen();
  const textFieldRef = useRef<HTMLInputElement>(null);
  const [ query, setQuery ] = useState(initialQuery)
  const currentQuery = useRef<string>(initialQuery);
  const [ suggestions, setSuggestions ] =
      useState<Array<Suggestion> | undefined>(undefined);
  const ignoreKeyEnter = useRef(false);

  useEffect(() => {
    if (!focus) return;
    if (!textFieldRef.current) return;
    textFieldRef.current.focus();
  }, [textFieldRef, focus]);

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
    kwic(value);
  };
  const loadResult = async () => {
    const result = await workflowlistApi.kwicWorkflows(
        currentQuery.current,
        undefined,
        searchQueries);
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
  }
  const kwicDebounced = useMemo(() => debounce(loadResult, 300), [ workflowlistApi, currentQuery ]);
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
    kwic(undefined);
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

interface DefinitionOfWorkflow {
  [key: string]: Workflow;
}

const ListOfWorkflows = ({
  showLoadingIndicator,
  useGuiSse,
  useWorkflowlistApi,
  openTask,
  navigateToWorkflow,
  currentLanguage,
  defaultSort,
  defaultSortAscending,
  columns,
  t,
}: {
  showLoadingIndicator: ShowLoadingIndicatorFunction,
  useGuiSse: GuiSseHook,
  useWorkflowlistApi: WorkflowlistApiHook,
  openTask: OpenTaskFunction,
  navigateToWorkflow: NavigateToWorkflowFunction,
  currentLanguage: string,
  defaultSort?: string,
  defaultSortAscending?: boolean,
  columns?: string[];
  t: TranslationFunction,
}) => {

  const { isNotPhone } = useResponsiveScreen();

  const wakeupSseCallback = useRef<WakeupSseCallback>(undefined);
  const workflowlistApi = useWorkflowlistApi(wakeupSseCallback);
  const [ refreshIndicator, setRefreshIndicator ] = useState<Date>(new Date());
  const [ searchQueries, setSearchQueries ] = useState<Array<SearchQuery>>([]);
  const kwicInProgress = useRef(false);

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

  const workflows = useRef<Array<ListItem<Workflow>> | undefined>(undefined);
  const [ numberOfWorkflows, setNumberOfWorkflows ] = useState<number>(-1);
  const [ modulesOfWorkflows, setModulesOfWorkflows ] = useState<Workflow[] | undefined>(undefined);
  const [ languagesOfTitles, setLanguagesOfTitles ] = useState<Array<string>>([currentLanguage]);
  const [ definitionsOfWorkflows, setDefinitionsOfWorkflows ] = useState<DefinitionOfWorkflow | undefined>(undefined);
  useEffect(() => {
      const loadMetaInformation = async () => {
        const result = await workflowlistApi
            .getWorkflows(new Date().getTime().toString(), 0, 100, undefined, true, searchQueries);
        const moduleDefinitions = result
            .workflows
            .reduce((moduleDefinitions, workflow) => moduleDefinitions.includes(workflow)
                ? moduleDefinitions : moduleDefinitions.concat(workflow), new Array<Workflow>());
        setModulesOfWorkflows(moduleDefinitions);
        const workflowDefinitions: DefinitionOfWorkflow = {};
        const titleLanguages = result
            .workflows
            .map(workflow => workflowDefinitions[`${workflow.workflowModule}#${workflow.bpmnProcessId}`] = workflow)
            .flatMap(workflow => Object.keys(workflow.title))
            .reduce((allLanguages, titleLanguage) => {
              if (!allLanguages.includes(titleLanguage)) {
                allLanguages.push(titleLanguage);
              }
              return allLanguages;
            }, new Array<string>(currentLanguage));
        setDefinitionsOfWorkflows(workflowDefinitions);
        setLanguagesOfTitles(titleLanguages);
      };
      if (workflows.current === undefined) {
        showLoadingIndicator(true);
        loadMetaInformation();
      }
    },
    // workflowlistApi is not part of dependency because it changes one time but this is irrelevant to the
    // purpose of preloading modules used by workflows
    [ workflows, setNumberOfWorkflows, setModulesOfWorkflows, setDefinitionsOfWorkflows, showLoadingIndicator, refreshIndicator, setLanguagesOfTitles ]);

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
                .filter(m => m.workflowModule === definition.workflowModule)
                .filter(m => m.workflowListColumns !== undefined)
                .map(module => module.workflowListColumns!(definition));
            if (columnsOfWorkflow.length === 0) return undefined;
            return columnsOfWorkflow[0];
          })
        .filter(columnsOfWorkflow => columnsOfWorkflow !== undefined)
        .reduce((totalColumns, columnsOfWorkflow) => {
            columnsOfWorkflow!.forEach(column => {
                // @ts-ignore
                totalColumns[column.path] = column });
            return totalColumns;
          }, {});
    const existingColumnsSignature = columnsOfWorkflows === undefined
        ? ' ' // initial state is different then updates
        : columnsOfWorkflows.map(c => c.path).join('|');
    const columnsToShow = (Object
        .values(totalColumns) as Array<Column>)
        .filter(column => (columns === undefined) || columns.includes(column.path))
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
  const [ sort, _setSort ] = useState<string | undefined>(defaultSort);
  const [ sortAscending, _setSortAscending ] = useState(defaultSortAscending === undefined ? true : defaultSortAscending);

  const refreshList = () => {
    workflows.current = undefined;
    setRefreshNecessary(false);
    setColumnsOfWorkflows(undefined);
    setRefreshIndicator(new Date());
  }

  const kwic = (value?: string) => {
    const newSearchQueries = searchQueries
        .filter(query => query.path !== undefined);
    if ((value !== undefined)
        && (value.trim().length > 0)) {
      newSearchQueries.push({ path: undefined, query: value });
    }
    setSearchQueries(newSearchQueries);
    kwicInProgress.current = true;
    refreshList();
  };

  useLayoutEffect(() => {
    kwicInProgress.current = false;
  }, [ kwicInProgress ]);

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

  const openWorkflow = async (workflow: Workflow) => {
    navigateToWorkflow(workflow);
  };

  const [ dropIdentifier, setDropIdentifier ] = useState<string | undefined>(undefined);
  const [ columnWidthAdjustments, setColumnWidthAdjustments ] = useState<ColumnWidthAdjustments>({});
  const getColumnSize = (column: string, width: string) => `max(4rem, calc(${width} + ${columnWidthAdjustments[column] ? columnWidthAdjustments[column] : 0}px))`;
  const setColumnWidthAdjustment = (column: string, adjustment: number) => {
    const current = columnWidthAdjustments[column];
    if (current === adjustment) return;
    setColumnWidthAdjustments({ ...columnWidthAdjustments, [column]: adjustment })
  };

  const columnsOfList: ColumnConfig<ListItem<BcWorkflow>>[] =
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
                                workflows
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
            render: (item: ListItem<BcWorkflow>) => (
                <Box
                    align="center">
                  <CheckBox
                      checked={ item.selected }
                      onChange={ event => {
                        item.selected = event.currentTarget.checked;
                        (refreshItemRef.current!)([ item.id ]);
                        const currentlyAllSelected = workflows
                            .current!
                            .reduce((allSelected, workflow) => allSelected && workflow.selected, true);
                        if (currentlyAllSelected !== allSelected) {
                          setAllSelected(currentlyAllSelected);
                        }
                        const currentlyAnySelected = workflows
                            .current!
                            .reduce((anySelected, workflow) => anySelected || workflow.selected, false);
                        if (anySelected !== currentlyAnySelected) {
                          setAnySelected(currentlyAnySelected);
                        }
                      } } />
                </Box>)
          },
          { property: 'name',
            header: <ListColumnHeader
                t={ t }
                currentLanguage={ currentLanguage }
                column={ {
                  path: 'title',
                  show: true,
                  sortable: true,
                  filterable: true,
                  title: { [currentLanguage]: t('column_title') },
                  width: '',
                  priority: -1,
                  resizeable: true,
                }}
                setColumnWidthAdjustment={ setColumnWidthAdjustment }
                sort={ sort?.startsWith('title.') } // like 'title.de,title.en'
                setSort={ setSort }
                sortAscending={ sortAscending }
                selectAll={ (select: boolean) => {} }
                allSelected={ false }
                setSortAscending={ setSortAscending} />,
            plain: true,
            render: (item: ListItem<BcWorkflow>) => {
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
                          truncate="tip">
                        {
                          item.status === ListItemStatus.ENDED
                              ? <>{ title }</>
                              : <Link
                                    // @ts-ignore
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
                    header: <ListColumnHeader
                        t={ t }
                        currentLanguage={ currentLanguage }
                        setColumnWidthAdjustment={ setColumnWidthAdjustment }
                        sort={ sort === column.path }
                        setSort={ setSort }
                        sortAscending={ sortAscending }
                        setSortAscending={ setSortAscending }
                        column={ column }
                        selectAll={ (select: boolean) => {} }
                        allSelected={ false } />,
                    size: getColumnSize(column.path, column.width),
                    plain: true,
                    render: (item: ListItem<BcWorkflow>) => <ListCell
                                                              modulesAvailable={ modules! }
                                                              column={ column }
                                                              currentLanguage={ currentLanguage }
                                                              t={ t }
                                                              typeOfItem={ TypeOfItem.WorkflowList }
                                                              // @ts-ignore
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
          getUserTasks: getUserTasksFunction,
        };
    };

  const initialKwicQuery = searchQueries
      .filter(query => query.path === undefined)
      .map(query => query.query);

  return (
      <Grid
          rows={ [ '3rem', 'auto', '2rem' ] }
          fill>
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
              workflowlistApi={ workflowlistApi }
              initialQuery={ initialKwicQuery.length === 0 ? undefined : initialKwicQuery[0] }
              kwic={ kwic }
              focus={ kwicInProgress.current }
              searchQueries={ searchQueries } />
        </Box>
        {
          (columnsOfWorkflows === undefined)
              ? <Box key="list"></Box>
              : <Box key="list">
                  <SearchableAndSortableUpdatingList
                      t={ t }
                      applyBackgroundColor
                      showLoadingIndicator={ showLoadingIndicator }
                      minWidthOfAutoColumn={ getColumnSize('title', minWidthOfTitleColumn) }
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
                              pageSize,
                              pageNumber,
                              initialTimestamp,
                              searchQueries,
                              sort,
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
                              sort,
                              sortAscending,
                              mapToBcWorkflow) }
                    />
                </Box>
        }
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
