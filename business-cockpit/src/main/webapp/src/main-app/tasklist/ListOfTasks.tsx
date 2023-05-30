import React, { useState, useRef, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import i18n from '../../i18n';
import { ListItem, ListItems, ReloadCallbackFunction, SearchableAndSortableUpdatingList } from '../../components/SearchableAndSortableUpdatingList';
import { useTasklistApi } from './TasklistAppContext';
import { TasklistApi, UserTask, UserTaskEvent } from '../../client/gui';
import { useGuiSse } from '../../client/guiClient';
import { Grid, Box, CheckBox } from 'grommet';
import { useResponsiveScreen } from "@vanillabp/bc-shared";
import { EventSourceMessage, WakeupSseCallback } from '@vanillabp/bc-shared';
import { Link } from '@vanillabp/bc-shared';
import { useAppContext } from "../../AppContext";

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

  const userTasks = useRef<Array<ListItem<UserTask>> | undefined>(undefined);
  const [ numberOfTasks, setNumberOfTasks ] = useState<number>(-1);
  
  const openTask = async (userTask: UserTask) => {
      if (userTask.uiUriType !== 'WEBPACK_REACT') {
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
            render: (_item: ListItem<T>) => (
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
            render: (item: ListItem<T>) => (
                <Box>
                  <Link
                      onClick={ () => openTask(item.data) }
                      truncate="tip">
                    { item.data['title'].de }
                  </Link>
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
