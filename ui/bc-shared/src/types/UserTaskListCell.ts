import { FC } from 'react';
import { UserTask } from '@vanillabp/bc-official-gui-client';
import { DefaultListCellAwareProps } from '../components';
import { BcUserTask, Column } from '@vanillabp/bc-types';

interface UserTaskListCellProps extends DefaultListCellAwareProps<BcUserTask> {
}

export type UserTaskListCell = FC<UserTaskListCellProps>;

export type ColumnsOfUserTaskFunction = (userTask: UserTask) => Column[]|undefined;
