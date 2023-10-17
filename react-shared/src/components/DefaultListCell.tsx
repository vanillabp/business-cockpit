import React, { FC } from 'react';
import { Box, Text, TextExtendedProps } from 'grommet';
import { Column, ListItem, ListItemStatus } from '../types/index.js';
import {
  getObjectProperty,
  toLocaleDateString,
  toLocaleStringWithoutSeconds,
  toLocaleTimeStringWithoutSeconds
} from '../utils/index.js';
import { ColorType } from 'grommet/utils/index.js';

const DATE_REGEXP = /^(\d{4})-(\d{2})-(\d{2})/;

export const ENDED_FONT_COLOR = 'light-4';
export const ENDED_FONT_COLOR_ODD = '#b4b4b4';

export type Alignment = 'left' | 'center' | 'right';

const colorForEndedItemsOrUndefined = (item: ListItem<any>): ColorType => {
  return item.status !== ListItemStatus.ENDED
      ? undefined
      : ENDED_FONT_COLOR;
};

interface TextListCellProps extends TextExtendedProps {
  item: ListItem<any>;
  value?: string | String;
  align?: Alignment;
  tip?: string;
  showUnreadAsBold?: boolean;
}

const TextListCell: React.FC<TextListCellProps> = ({
  item,
  value = '',
  align = 'left',
  tip,
  showUnreadAsBold = false,
  ...props
}) => {
  const color = colorForEndedItemsOrUndefined(item);
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
                  color={ color }
                  weight={ showUnreadAsBold && item.read === undefined ? 'bold' : 'normal' }
                  { ...props }>
                { value }
              </Text>
            : <Text
                  truncate
                  color={ color }
                  weight={ showUnreadAsBold && item.read === undefined ? 'bold' : 'normal' }
                  tip={ { content: tip } }
                  { ...props }>
                { value }
              </Text>
      }
    </Box>);
}

export interface DefaultListCellProps<D> {
  item: ListItem<D>;
  column: Column;
  showUnreadAsBold?: boolean;
}

export interface DefaultListCellAwareProps<T> extends DefaultListCellProps<T> {
  defaultCell: FC<DefaultListCellProps<T>>;
}

const DefaultListCell: FC<DefaultListCellProps<any>> = ({
    item,
    column,
    showUnreadAsBold,
}) => {
  const propertyValue = getObjectProperty(item.data, column.path);
  let value;
  let tip;
  let align: Alignment = 'left';
  if (propertyValue === undefined) {
    value = '';
  } else if (propertyValue instanceof Date) {
    if (column.path === 'dueDate') {
      value = toLocaleDateString(item.data.dueDate);
      tip = toLocaleStringWithoutSeconds(item.data.dueDate);
    } else {
      value = toLocaleTimeStringWithoutSeconds(propertyValue);
    }
  } else if (typeof propertyValue === 'number') {
    value = (propertyValue as Number).toLocaleString(window.navigator.language);
    align = 'right';
  } else if (typeof propertyValue === 'string') {
    const dateMatch = DATE_REGEXP.exec(propertyValue as string);
    if (dateMatch) {
      if (propertyValue.length === 10) {
        value = toLocaleDateString(new Date(Date.parse(propertyValue as string)));
      } else if (column.path.endsWith('.dueDate')) {
        const tmpDate = new Date(Date.parse(propertyValue as string));
        value = toLocaleDateString(tmpDate);
        tip = toLocaleStringWithoutSeconds(tmpDate);
      } else {
        const tmpDate = new Date(Date.parse(propertyValue as string));
        value = toLocaleStringWithoutSeconds(tmpDate);
      }
    } else {
      value = propertyValue;
    }
  }
  return <TextListCell
      item={ item }
      value={ value }
      tip={ tip }
      showUnreadAsBold={ showUnreadAsBold }
      align={ align } />;
}

export { DefaultListCell, TextListCell, colorForEndedItemsOrUndefined };
