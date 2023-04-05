import React, { useState, useRef, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import i18n from '../i18n';
import { ListItem, ListItems, ReloadCallbackFunction, SearchableAndSortableUpdatingList } from '../components/SearchableAndSortableUpdatingList';
import { useTasklistApi } from './TasklistAppContext';
import { TasklistApi, UserTask, UserTaskEvent } from '../client/gui';
import { useGuiSse } from '../client/guiClient';
import { Grid, Box } from 'grommet';
import useResponsiveScreen from '../utils/responsiveUtils';
import { EventSourceMessage, WakeupSseCallback } from '../components/SseProvider';

i18n.addResources('en', 'tasklist/list', {
      "total": "Total:",
    });
i18n.addResources('de', 'tasklist/list', {
      "total": "Anzahl:",
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
  
  return (
      <Grid
          rows={ [ 'auto', '2rem' ] }
          fill>
        <Box>
          <SearchableAndSortableUpdatingList
              t={ t }
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
              gap='small'
              pad='xsmall'
              align="center">
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
      </Grid>);
      
};

export { ListOfTasks };
