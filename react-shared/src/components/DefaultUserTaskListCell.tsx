import React, { FC } from 'react';
import { Box, Text, TextExtendedProps } from 'grommet';
import { DefaultUserTaskListCellProps } from '../types/index.js';
import { getObjectProperty, toLocalDateString } from '../utils/index.js';

interface UserTaskListTextCellWrapperProps extends TextExtendedProps {
  value?: string;
}

const UserTaskListTextCellWrapper: React.FC<UserTaskListTextCellWrapperProps> = ({
  value = '',
  ...props
}) => (
    <Box
        fill
        align="center"
        direction='row'
        justify='start'
        pad="xxsmall"
        gap="xsmall">
      <Text
          truncate="tip"
          { ...props }>
        { value }
      </Text>
    </Box>);

const DefaultUserTaskListCell: FC<DefaultUserTaskListCellProps> = ({
    item,
    column
}) => {
  if (column.path === 'dueDate') {
    return <UserTaskListTextCellWrapper
        value={ toLocalDateString(item.data.dueDate) } />;
  };
  
  return <UserTaskListTextCellWrapper
      value={ getObjectProperty(item.data, column.path) } />;
}

export { DefaultUserTaskListCell, UserTaskListTextCellWrapper };
