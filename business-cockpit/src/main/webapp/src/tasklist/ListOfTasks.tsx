import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import i18n from '../i18n';
import { SnapScrollingDataTable } from '../components/SnapScrollingDataTable';
import { useAppContext } from '../AppContext';
import { useTasklistApi } from './TasklistAppContext';
import { TasklistApi, UserTask } from '../client/gui';
import { Grid, Box, ColumnConfig, Text, CheckBox } from 'grommet';
import useResponsiveScreen from '../utils/responsiveUtils';

i18n.addResources('en', 'tasklist/list', {
      "total": "Total:",
    });
i18n.addResources('de', 'tasklist/list', {
      "total": "Anzahl:",
    });
    
const itemsBatchSize = 30;
 
const loadData = async (
  tasklistApi: TasklistApi,
  setNumberOfUserTasks: (number: number) => void,
  setUserTasks: (userTasks: Array<UserTask>) => void,
  userTasks: Array<UserTask> | undefined
) => {
  
  const result = await tasklistApi
        .getUserTasks({
            pageNumber: userTasks === undefined
                ? 0
                : Math.floor(userTasks.length / itemsBatchSize)
                  + (userTasks.length % itemsBatchSize),
            pageSize: itemsBatchSize
          });
    setNumberOfUserTasks(result!.page.totalElements);
    const currentNumberOfTasks = userTasks === undefined ? 1 : userTasks.length + 1;
    const numberedTasks = result
        .userTasks
        .map((task, index) => ({ ...task, number: currentNumberOfTasks + index }));
    setUserTasks(
        userTasks === undefined
        ? numberedTasks
        : userTasks.concat(numberedTasks));
  	
};

const ListOfTasks = () => {

  const { showLoadingIndicator } = useAppContext();
  const { isPhone } = useResponsiveScreen();
  const { t } = useTranslation('tasklist/list');
  const tasklistApi = useTasklistApi();
  
  const [ tasks, setTasks ] = useState<Array<UserTask> | undefined>(undefined);
  const [ numberOfTasks, setNumberOfTasks ] = useState<number>(-1);
  
  useEffect(() => {
      if (tasks !== undefined) {
        return;
      }
      const initList = async () => {
          showLoadingIndicator(true);
          await loadData(tasklistApi, setNumberOfTasks, setTasks, tasks);
          showLoadingIndicator(false);
        };
      initList();
    }, [ showLoadingIndicator, tasks, tasklistApi, setTasks, setNumberOfTasks ]);
  
  const columns: ColumnConfig<UserTask>[] =
      [
          { property: 'id',
            pin: true,
            size: '2.2rem',
            header: <Box
                    pad="xsmall">
                  <CheckBox />
                </Box>,
            render: (userTask: UserTask) => (
                <Box
                    pad="xsmall">
                  <CheckBox />
                </Box>)
          },
          { property: 'number',
            header: 'No',
            size: '3rem'
          },
          { property: 'name',
            header: t('name'),
            render: (userTask: UserTask) => (
                <Text truncate="tip">
                  { userTask.title.de }
                </Text>)
          },
      ];

  const headerHeight = 'auto';
  const phoneMargin = '10vw';
  
  return (
    <>
      <Grid
          rows={ [ 'xxsmall' ] }
          fill>
        <Box
            flex
            justify='between'
            direction='row'
            pad={ isPhone ? 'medium' : 'small' }>
          <Box
              justify='center'
              align="center">
            <Text>{ t('total') } { numberOfTasks }</Text>
          </Box>
          <Box
              direction='row'
              gap='medium'>
          </Box>
        </Box>
        <Box>
          <Box
              fill='horizontal'
              overflow={ { vertical: 'auto' }}>
            <SnapScrollingDataTable
                primaryKey={ false }
                fill
                pin
                size='100%'
                columns={ columns }
                step={ itemsBatchSize }
                headerHeight={ headerHeight }
                phoneMargin={ phoneMargin }
                onMore={ () => loadData(tasklistApi, setNumberOfTasks, setTasks, tasks) }
                data={ tasks }
                replace />
          </Box>
        </Box>
      </Grid>
    </>);
      
};

export { ListOfTasks };
