import React, { FC } from 'react';
import { Box, Text, TextExtendedProps } from 'grommet';
import { DefaultUserTaskListCellProps } from '../types/index.js';
import { getObjectProperty, toLocalDateString, toLocaleTimeStringWithoutSeconds } from '../utils/index.js';

const DATE_REGEXP = /^(\d{4})-(\d{2})-(\d{2})/;

type Alignment = 'left' | 'center' | 'right';

interface UserTaskListTextCellWrapperProps extends TextExtendedProps {
  value?: string | String;
  align?: Alignment;
  tip?: string;
}

const UserTaskListTextCellWrapper: React.FC<UserTaskListTextCellWrapperProps> = ({
  value = '',
  align = 'left',
  tip,
  ...props
}) => {
  return (
    <Box
        fill
        align="center"
        direction='row'
        justify={ align === 'left'
            ? 'start'
            : align === 'right'
            ? 'end'
            : 'center' }
        pad="xxsmall"
        gap="xsmall">
      {
        tip === undefined
            ? <Text
                  truncate="tip"
                  { ...props }>
                { value }
              </Text>
            : <Text
                  truncate
                  tip={ { content: tip } }
                  { ...props }>
                { value }
              </Text>
      }
    </Box>);
}

const DefaultUserTaskListCell: FC<DefaultUserTaskListCellProps> = ({
    item,
    column
}) => {
  const propertyValue = getObjectProperty(item.data, column.path);
  let value;
  let tip;
  let align: Alignment = 'left';
  if (propertyValue === undefined) {
    value = '';
  } else if (propertyValue instanceof Date) {
    if (column.path === 'dueDate') {
      value = toLocalDateString(item.data.dueDate);
      tip = `${toLocalDateString(item.data.dueDate)} ${toLocaleTimeStringWithoutSeconds(item.data.dueDate!)}`
    } else {
      value = toLocaleTimeStringWithoutSeconds(propertyValue);
    }
  } else if (typeof propertyValue === 'number') {
    value = (propertyValue as Number).toLocaleString();
    align = 'right';
  } else if (typeof propertyValue === 'string') {
    const dateMatch = DATE_REGEXP.exec(propertyValue as string);
    if (dateMatch) {
      if (propertyValue.length === 10) {
        value = toLocalDateString(new Date(Date.parse(propertyValue as string)));
      } else if (column.path.endsWith('.dueDate')) {
        const tmpDate = new Date(Date.parse(propertyValue as string));
        value = toLocalDateString(tmpDate);
        tip = `${toLocalDateString(tmpDate)} ${toLocaleTimeStringWithoutSeconds(tmpDate)}`
      } else {
        const tmpDate = new Date(Date.parse(propertyValue as string));
        value = `${toLocalDateString(tmpDate)} ${toLocaleTimeStringWithoutSeconds(tmpDate)}`;
      }
    } else {
      value = propertyValue;
    }
  }
  return <UserTaskListTextCellWrapper
      value={ value }
      tip={ tip }
      align={ align } />;
}

export { DefaultUserTaskListCell, UserTaskListTextCellWrapper };
