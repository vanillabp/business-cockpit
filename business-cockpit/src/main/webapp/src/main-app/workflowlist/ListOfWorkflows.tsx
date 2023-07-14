import React, { useState, useRef, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import i18n from '../../i18n';
import { ListItem, ListItems, ReloadCallbackFunction, SearchableAndSortableUpdatingList } from '../../components/SearchableAndSortableUpdatingList';
import { useWorkflowlistApi } from "./WorkflowlistAppContext";
import { useGuiSse } from '../../client/guiClient';
import { Grid, Box, CheckBox, ColumnConfig } from 'grommet';
import { useResponsiveScreen } from "@vanillabp/bc-shared";
import { EventSourceMessage, WakeupSseCallback } from '@vanillabp/bc-shared';
import { Link, toLocalDateString, toLocaleTimeStringWithoutSeconds } from '@vanillabp/bc-shared';
import { useAppContext } from "../../AppContext";
import { WorkflowlistApi, Workflow, WorkflowEvent } from "../../client/gui";

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
): Promise<ListItems<Workflow>> => {
  
  const result = await workflowlistApi
        .getWorkflows({ pageNumber, pageSize });

  setNumberOfWorkflows(result!.page.totalElements);

  return {
      serverTimestamp: result.serverTimestamp,
      items: result.workflows
  };
};

const reloadWorkflows = async (
  workflowlistApi: WorkflowlistApi,
  setNumberOfWorkflows: (number: number) => void,
  numberOfItems: number,
  knownItemsIds: Array<string>,
): Promise<ListItems<Workflow>> => {

  const result = await workflowlistApi.getWorkflowsUpdate({
      workflowsUpdate: {
          size: numberOfItems,
          knownWorkflowIds: knownItemsIds
        }
    })

  setNumberOfWorkflows(result!.page.totalElements);
  
  return {
      serverTimestamp: result.serverTimestamp,
      items: result.workflows
  };
};


const ListOfWorkflows = () => {

  const { isNotPhone } = useResponsiveScreen();
  const { t } = useTranslation('workflowlist/list');
  const { t: tApp } = useTranslation('app');
  const { toast } = useAppContext();
  
  const wakeupSseCallback = useRef<WakeupSseCallback>(undefined);
  const workflowlistApi = useWorkflowlistApi(wakeupSseCallback);

  const updateListRef = useRef<ReloadCallbackFunction | undefined>(undefined);
  const updateList = useMemo(() => async (ev: EventSourceMessage<WorkflowEvent>) => {
      if (!updateListRef.current) return;
      const listOfUpdatedWorkflows = ev.data.map(workflowEvent => workflowEvent.event.id);
      updateListRef.current(listOfUpdatedWorkflows);
    }, [ updateListRef ]);
  wakeupSseCallback.current = useGuiSse<WorkflowEvent>(
      updateList,
      /^Workflow$/
  );

  const workflows = useRef<Array<ListItem<Workflow>> | undefined>(undefined);
  const [ numberOfWorkflows, setNumberOfWorkflows ] = useState<number>(-1);

  const openWorkflow = async (workflow: Workflow) => { console.log('TODO: open workflow')};
  /*const openTask = async (userTask: UserTask) => {
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
    };*/
    
  const columns: ColumnConfig<ListItem<Workflow>>[] =
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
            render: (_item: ListItem<Workflow>) => (
                <Box
                    align="center">
                  <CheckBox />
                </Box>)
          },
          { property: 'number',
            header: t('no'),
            size: '3rem'
          },
          { property: 'name',
            header: t('name'),
            size: 'calc(100% - 2.2rem - 3rem - 15rem - 15rem)',
            render: (item: ListItem<Workflow>) => (
                <Box
                    fill
                    pad="xsmall">
                  <Link
                      onClick={ () => openWorkflow(item.data) }
                      truncate="tip">
                    { item.data['title'].de }
                  </Link>
                </Box>)
          },
          { property: 'project',
              header: t('project'),
              size: '15rem',
              render: (item: ListItem<Workflow>) => (
                  <Box
                      fill
                      pad="xsmall">
                    { item?.data['details']?.project?.name || "-" }
                  </Box>)
          },
          { property: 'gremium',
              header: t('gremium'),
              size: '15rem',
              render: (item: ListItem<Workflow>) => (
                  <Box
                      fill
                      pad="xsmall">
                    { item?.data['details']?.gremium?.name || "-" }
                  </Box>)
          },

      ];
  
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
              retrieveItems={ (pageNumber, pageSize) => 
// @ts-ignore
                  loadWorkflows(
                      workflowlistApi,
                      setNumberOfWorkflows,
                      pageSize,
                      pageNumber) }
              reloadItems={ (numberOfItems, updatedItemsIds) =>
// @ts-ignore
                  reloadWorkflows(
                      workflowlistApi,
                      setNumberOfWorkflows,
                      numberOfItems,
                      updatedItemsIds) }
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

export { ListOfWorkflows };
