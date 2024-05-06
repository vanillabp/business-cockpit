import { Box, BoxExtendedProps, Grid, Text, TextExtendedProps } from 'grommet';
import { ColorType } from 'grommet/utils/index.js';
import React, { FC, PropsWithChildren } from 'react';
import { Column, ListItem, ListItemStatus, Person, TranslationFunction } from '../types/index.js';
import {
  getObjectProperty,
  toLocaleDateString,
  toLocaleStringWithoutSeconds,
  toLocaleTimeStringWithoutSeconds,
} from '../utils/index.js';
import { BackgroundType } from "grommet/utils";

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
        pad="xsmall"
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

interface PersonListCellProps extends TextExtendedProps {
  t: TranslationFunction,
  item: ListItem<any>;
  value?: Person;
  showUnreadAsBold?: boolean;
  background?: BackgroundType;
}

const PersonListCell: React.FC<PersonListCellProps> = ({
  t,
  item,
  value,
  showUnreadAsBold = false,
  background,
  ...props
}) => {
  const color = colorForEndedItemsOrUndefined(item);
  return (
      <ListCell
          background={ background }
          align="left">
        <Text
            truncate
            color={ color }
            weight={ showUnreadAsBold && item.read === undefined ? 'bold' : 'normal' }
            tip={ { content: <Grid columns={['auto', 'auto']} gap="xsmall">
                                <Text>{ t('person-id') }:</Text>
                                <Text weight="bold">{ value?.id }</Text>
                                {
                                  value?.email !== null
                                      ? <>
                                          <Text>{ t('person-email') }:</Text>
                                          <Text weight="bold">{ value?.email }</Text>
                                        </>
                                      : undefined
                                }
                              </Grid> } }
            { ...props }>
          {
            value === undefined
                ? undefined
                : value.display
          }
        </Text>
      </ListCell>);
}

export interface DefaultListCellProps<D> {
  t: TranslationFunction;
  item: ListItem<D>;
  column: Column;
  showUnreadAsBold?: boolean;
  currentLanguage: string;
  nameOfList?: string;
  selectItem: (select: boolean) => void;
  isPhone: boolean;
  isTablet: boolean;
}

export interface DefaultListCellAwareProps<T> extends DefaultListCellProps<T> {
  defaultCell: FC<DefaultListCellProps<T>>;
}

type RenderResult = {
  value: string | undefined;
  align: Alignment;
}
const render = (
    t: TranslationFunction,
    propertyValue: any,
    currentLanguage: string): RenderResult => {
  let align: Alignment = 'left';
  let value: string | undefined;
  if (typeof propertyValue === 'object') {
    value = '[object - define custom cell to render content]';
  } else if (propertyValue === undefined) {
    value = '';
  } else if (propertyValue instanceof Date) {
    value = toLocaleTimeStringWithoutSeconds(propertyValue, currentLanguage);
  } else if (typeof propertyValue === 'number') {
    value = (propertyValue as Number).toLocaleString(currentLanguage ?? window.navigator.language);
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
        value = toLocaleDateString(new Date(Date.parse(propertyValue as string)), currentLanguage);
        align = 'right';
      } else {
        const tmpDate = new Date(Date.parse(propertyValue as string));
        value = toLocaleStringWithoutSeconds(tmpDate, currentLanguage);
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
    t,
    selectItem,
}) => {
  let align: Alignment = 'left';
  let value: string | undefined;
  let tip: string | undefined;
  let path = column.path;
  if (column.type === 'i18n') {
    path = `${path}.${currentLanguage}`;
  }
  const background = colorRowAccordingToUpdateStatus(item);
  let propertyValue = getObjectProperty(item.data, path);
  if (column.type === 'date') {
    value = toLocaleDateString(propertyValue as Date, currentLanguage);
    tip = toLocaleStringWithoutSeconds(propertyValue as Date, currentLanguage);
    align = 'right';
  } else if (column.type === 'date-time') {
    value = toLocaleTimeStringWithoutSeconds(propertyValue as Date, currentLanguage);
    tip = (propertyValue as Date).toLocaleTimeString(currentLanguage);
    align = 'right';
  } else if (column.type === 'person') {
    return <PersonListCell
        t={ t }
        item={ item }
        value={ propertyValue as Person }
        showUnreadAsBold={ showUnreadAsBold }
        background={ background } />;
  } else {
    const { value: v, align: a } = render(t, propertyValue, currentLanguage);
    value = v;
    align = a;
  }
  return <TextListCell
      item={ item }
      value={ value }
      tip={ tip }
      showUnreadAsBold={ showUnreadAsBold }
      background={ background }
      align={ align } />;
}

export { DefaultListCell, TextListCell, ListCell, colorForEndedItemsOrUndefined, colorRowAccordingToUpdateStatus };
