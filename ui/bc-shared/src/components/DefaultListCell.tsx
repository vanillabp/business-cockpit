import { Box, BoxExtendedProps, Text, TextExtendedProps } from 'grommet';
import { ColorType } from 'grommet/utils/index.js';
import { TFunction } from 'i18next';
import React, { FC, PropsWithChildren } from 'react';
import { useTranslation } from "react-i18next";
import { Column, ListItem, ListItemStatus } from '../types/index.js';
import {
  getObjectProperty,
  i18n,
  toLocaleDateString,
  toLocaleStringWithoutSeconds,
  toLocaleTimeStringWithoutSeconds,
} from '../utils/index.js';
import { BackgroundType } from "grommet/utils";

i18n.addResources('en', 'default-list-cell', {
  "boolean-true": 'Yes',
  "boolean-false": 'No',
});
i18n.addResources('en', 'default-list-cell', {
  "boolean-true": 'Ja',
  "boolean-false": 'Nein',
});

const DATE_REGEXP = /^(\d{4})-(\d{2})-(\d{2})/;

export const ENDED_FONT_COLOR = 'light-4';
export const ENDED_FONT_COLOR_ODD = '#b4b4b4';

export type Alignment = 'left' | 'center' | 'right';

const colorForEndedItemsOrUndefined = (item: ListItem<any>): ColorType => {
  return item.status !== ListItemStatus.ENDED
      ? undefined
      : ENDED_FONT_COLOR;
};

interface ListCellProps extends BoxExtendedProps {
  background?: BackgroundType;
  align?: Alignment;
}

const ListCell: React.FC<PropsWithChildren<ListCellProps>> = ({
  background,
  align,
  children,
  ...props
}) => (
    <Box
        fill
        align="center"
        direction='row'
        justify={ align === 'left'
            ? 'start'
            : align === 'right'
                ? 'end'
                : 'center' }
        background={ background }
        pad="xxsmall"
        gap="xsmall"
        { ...props }>
      {
        children
      }
    </Box>);

interface TextListCellProps extends TextExtendedProps {
  item: ListItem<any>;
  value?: string | String;
  tip?: string;
  showUnreadAsBold?: boolean;
  background?: BackgroundType;
  align?: Alignment;
}

const TextListCell: React.FC<TextListCellProps> = ({
  item,
  value = '',
  align = 'left',
  tip,
  showUnreadAsBold = false,
  background,
  ...props
}) => {
  const color = colorForEndedItemsOrUndefined(item);
  return (
    <ListCell
        background={ background }
        align={ align }>
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
    </ListCell>);
}

export interface DefaultListCellProps<D> {
  item: ListItem<D>;
  column: Column;
  showUnreadAsBold?: boolean;
  defaultLanguage?: string;
  currentLanguage: string;
  selectItem: (select: boolean) => void,
}

export interface DefaultListCellAwareProps<T> extends DefaultListCellProps<T> {
  defaultCell: FC<DefaultListCellProps<T>>;
}

type RenderResult = {
  value: string | undefined;
  align: Alignment;
}
const render = (
    t: TFunction,
    propertyValue: any,
    currentLanguage: string,
    defaultLanguage?: string): RenderResult => {
  let align: Alignment = 'left';
  let value: string | undefined;
  let tip;
  if (typeof propertyValue === 'object') { // check for { "de": "whatever" }
    let langValue = propertyValue[currentLanguage];
    if (langValue !== undefined) {
      propertyValue = langValue;
    } else if (defaultLanguage !== undefined) {
      langValue = propertyValue[defaultLanguage];
      if (langValue !== undefined) {
        propertyValue = langValue;
      }
    }
  }
  if (propertyValue === undefined) {
    value = '';
  } else if (propertyValue instanceof Date) {
    value = toLocaleTimeStringWithoutSeconds(propertyValue);
  } else if (typeof propertyValue === 'number') {
    value = (propertyValue as Number).toLocaleString(window.navigator.language);
    align = 'right';
  } else if (typeof propertyValue === "boolean") {
    if (propertyValue) {
      value = t('boolean-true');
    } else {
      value = t('boolean-false');
    }
  } else if (typeof propertyValue === 'string') {
    const dateMatch = DATE_REGEXP.exec(propertyValue as string);
    if (dateMatch) {
      if (propertyValue.length === 10) {
        value = toLocaleDateString(new Date(Date.parse(propertyValue as string)));
        align = 'right';
      } else {
        const tmpDate = new Date(Date.parse(propertyValue as string));
        value = toLocaleStringWithoutSeconds(tmpDate);
      }
    } else {
      value = propertyValue;
    }
  }
  return { value, align };
}

const colorRowAccordingToUpdateStatus = <T extends ListItem<any>, >(item: T): BackgroundType | undefined => (
    item.status === ListItemStatus.NEW
        ? { color: 'accent-3', opacity: 0.1 }
        : item.status === ListItemStatus.UPDATED
        ? { color: 'accent-1', opacity: 0.15 }
        : item.status === ListItemStatus.ENDED
        ? { color: 'light-2', opacity: 0.5 }
        : item.status === ListItemStatus.REMOVED_FROM_LIST
        ? { color: 'light-2', opacity: 0.5 }
        : undefined);

const DefaultListCell: FC<DefaultListCellProps<any>> = ({
    item,
    column,
    showUnreadAsBold,
    currentLanguage,
    defaultLanguage,
    selectItem,
}) => {
  const { t } = useTranslation("default-list-cell");
  let propertyValue = getObjectProperty(item.data, column.path);
  let value;
  let tip;
  let align: Alignment = 'left';
  if (typeof propertyValue === 'object') { // check for { "de": "whatever" }
    let langValue = propertyValue[currentLanguage];
    if (langValue !== undefined) {
      propertyValue = langValue;
    } else if (defaultLanguage !== undefined) {
      langValue = propertyValue[defaultLanguage];
      if (langValue !== undefined) {
        propertyValue = langValue;
      }
    }
  }
  if (propertyValue === undefined) {
    value = '';
  } else if (propertyValue instanceof Date) {
    if (column.path === 'dueDate') {
      value = toLocaleDateString(item.data.dueDate);
      tip = toLocaleStringWithoutSeconds(item.data.dueDate);
      align = 'right';
    } else {
      value = toLocaleTimeStringWithoutSeconds(propertyValue);
    }
  } else if (typeof propertyValue === 'number') {
    value = (propertyValue as Number).toLocaleString(window.navigator.language);
    align = 'right';
  } else if (typeof propertyValue === "boolean") {
    if (propertyValue) {
      value = t('boolean-true');
    } else {
      value = t('boolean-false');
    }
  } else if (typeof propertyValue === 'string') {
    const dateMatch = DATE_REGEXP.exec(propertyValue as string);
    if (dateMatch) {
      if (propertyValue.length === 10) {
        value = toLocaleDateString(new Date(Date.parse(propertyValue as string)));
        align = 'right';
      } else if (column.path.endsWith('.dueDate')) {
        const tmpDate = new Date(Date.parse(propertyValue as string));
        value = toLocaleDateString(tmpDate);
        tip = toLocaleStringWithoutSeconds(tmpDate);
        align = 'right';
      } else {
        const tmpDate = new Date(Date.parse(propertyValue as string));
        value = toLocaleStringWithoutSeconds(tmpDate);
      }
    } else {
      value = propertyValue;
    }
  }
  const background = colorRowAccordingToUpdateStatus(item);
  return <TextListCell
      item={ item }
      value={ value }
      tip={ tip }
      showUnreadAsBold={ showUnreadAsBold }
      background={ background }
      align={ align } />;
}

export { DefaultListCell, TextListCell, ListCell, colorForEndedItemsOrUndefined, colorRowAccordingToUpdateStatus };
