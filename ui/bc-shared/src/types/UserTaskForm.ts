import { FC } from 'react';
import { BcUserTask } from '@vanillabp/bc-types';

interface UserTaskFormProps {
  userTask: BcUserTask;
}

export type UserTaskForm = FC<UserTaskFormProps>;
