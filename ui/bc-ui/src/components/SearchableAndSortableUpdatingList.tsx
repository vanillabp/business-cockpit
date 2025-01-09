import React, { MutableRefObject, ReactNode, useCallback, useEffect, useRef, useState } from 'react';
import { Box, ColumnConfig } from 'grommet';
import { SnapScrollingDataTable } from './SnapScrollingDataTable.js';
import { keepOldItemsInArray, ListItemStatus, ShowLoadingIndicatorFunction } from '@vanillabp/bc-shared';
import { BackgroundType, ColorType } from "grommet/utils";

const itemsBatchSize = 30;

export type ListItemData = {
  id: string,
  version?: number,
  createdAt: Date,
  updatedAt: Date,
  endedAt?: Date,
  read?: Date;
};

export interface ListItem<T extends ListItemData> {
  id: string;
  number: number;
  data: T;
  status: ListItemStatus;
  selected: boolean;
  read?: Date;
};

export interface ListItems<T extends ListItemData> {
  serverTimestamp: Date;
  items: Array<T>;
};

type RetrieveItemsFunction = <T extends ListItemData>(pageNumber: number, pageSize: number, initialTimestamp: Date | undefined) => Promise<ListItems<T>>;

type ReloadItemsFunction = <T extends ListItemData>(numberOfItems: number, knownItemsIds: Array<string>, initialTimestamp: Date | undefined) => Promise<ListItems<T>>;

type RefreshNecessaryFunction = () => void;

export type ReloadCallbackFunction = (updatedItemsIds: Array<string>) => Promise<void>;

export type RefreshItemCallbackFunction = (itemIds: Array<string>) => void;

const loadItems = async <T extends ListItemData>(
  retrieveItems: RetrieveItemsFunction,
  setItems: (items: Array<ListItem<T>>) => void,
  items: Array<ListItem<T>> | undefined,
  initialTimestamp: MutableRefObject<Date | undefined>,
): Promise<Date> => {

  const result = await retrieveItems(
      items === undefined
          ? 0
          : items.length % itemsBatchSize === 0
          ? Math.floor(items.length / itemsBatchSize)
          : Math.floor(items.length / itemsBatchSize) + 1,
      itemsBatchSize,
      initialTimestamp.current,
    );
  
  const currentNumberOfItems = items === undefined
      ? 1
      : items.length + 1;
  
  const newItems = result
      .items
      .map((item, index) => ({
          id: item.id,
          data: item,
          number: currentNumberOfItems + index,
          selected: false,
          status: item.endedAt === undefined ? ListItemStatus.INITIAL : ListItemStatus.ENDED,
          read: item.read,
        } as ListItem<T>));
  setItems(
      items === undefined
          ? newItems
          : items.concat(newItems));
  
  return result.serverTimestamp;
  
};

const reloadData = async <T extends ListItemData>(
  reloadItems: ReloadItemsFunction,
  setItems: (items: Array<ListItem<T>>) => void,
  items: Array<ListItem<T>> | undefined,
  updatedItemsIds: Array<string>,
  initialTimestamp: MutableRefObject<Date | undefined>,
  refreshNecessaryCallback?: RefreshNecessaryFunction,
) => {

  const prefill = 2; // estimate that not more than 60 items will be created at once
  const size = (items!.length === 0
      ? prefill
      : Math.floor(items!.length / itemsBatchSize) + prefill) * itemsBatchSize;

  const result = await reloadItems(
      size,
      items!  // only request items updated or unknown
          .filter(item => !updatedItemsIds.includes(item.id))
          .map(item => item.id),
      initialTimestamp.current);
  
  const mappedResult = result
      .items
      .map((item, index) => ({
          id: item.id,
          data: item,
          number: 1 + index,
          selected: false,
          status: item.version === 0 // item in update response,
              ? undefined
              : Boolean(item.endedAt) && item.endedAt!.getTime() > initialTimestamp.current!.getTime()
              ? ListItemStatus.ENDED
              : item.createdAt.getTime() > initialTimestamp.current!.getTime()
              ? ListItemStatus.NEW
              : item.updatedAt.getTime() > initialTimestamp.current!.getTime()
              ? ListItemStatus.UPDATED
              : ListItemStatus.INITIAL,
          read: item.read,
        }) as ListItem<T>);
  let anyUpdate = false;
  const mergedItems = keepOldItemsInArray(
      mappedResult,
      items!,
      item => item.id,
      (newItem, oldItem, index) => {
          const itemInUpdateResponse = newItem?.status !== undefined;
          const result = itemInUpdateResponse ? newItem! : oldItem!;
          if (newItem === undefined) {
            if (result !== undefined) {
              result.status = ListItemStatus.REMOVED_FROM_LIST;
              result.number = index + 1;
            }
            anyUpdate = true;
          } else if (itemInUpdateResponse) {
            result.number = index + 1;
            anyUpdate = true;
          }
          if ((newItem !== undefined) && oldItem?.selected) {
            newItem!.selected = oldItem?.selected;
          }
          return result;
        },
      (first, second) => {
          if (first?.id === undefined) {
            return second?.id === undefined;
          }
          if (second?.id === undefined) {
            return true;
          }
          return first.number < second.number;
        }
      );

   setItems(mergedItems);
   if (anyUpdate && refreshNecessaryCallback) {
     refreshNecessaryCallback();
   }
   
};

const SearchableAndSortableUpdatingList = <T extends ListItemData>({
  itemsRef,
  updateListRef,
  refreshItemRef,
  retrieveItems,
  reloadItems,
  refreshNecessaryCallback,
  columns,
  showLoadingIndicator,
  additionalHeader,
  minWidthOfAutoColumn,
  rowSeparator,
  applyBackgroundColor = true,
  showColumnHeaders = true,
  columnHeaderBackground = 'dark-3',
  columnHeaderSeparator,
}: {
  itemsRef: MutableRefObject<Array<ListItem<T>> | undefined>,
  updateListRef: MutableRefObject<ReloadCallbackFunction | undefined>,
  refreshItemRef?: MutableRefObject<RefreshItemCallbackFunction | undefined>,
  retrieveItems: RetrieveItemsFunction,
  reloadItems: ReloadItemsFunction,
  refreshNecessaryCallback?: RefreshNecessaryFunction,
  columns: ColumnConfig<any>[],
  showLoadingIndicator: ShowLoadingIndicatorFunction,
  additionalHeader?: ReactNode,
  minWidthOfAutoColumn?: string,
  rowSeparator?: boolean | string,
  applyBackgroundColor?: boolean,
  showColumnHeaders?: boolean,
  columnHeaderBackground?: BackgroundType,
  columnHeaderSeparator?: ColorType | null,
}) => {
  const [ items, _setItems ] = useState<Array<ListItem<T>> | undefined>(undefined);
  const initialTimestamp = useRef<Date | undefined>(undefined);
  const setItems = useCallback((newItems: Array<ListItem<T>>) => {
      itemsRef.current = newItems;
      _setItems(newItems);
    }, [ _setItems, itemsRef ]);
  const setSelected = useCallback((itemId: string, selected: boolean) => {
        _setItems(items?.map(item => {
          if (item.id === itemId) {
            item.selected = selected;
          }
          return item;
        }));
      },
      [ _setItems, items ]);
    
  useEffect(() => {
      if (refreshItemRef) {
        refreshItemRef.current = (itemIds) => setItems(items!.map(item => itemIds.includes(item.id) ? { ...item } : item));
      }
      updateListRef.current = async (updatedItemsIds) => await reloadData(
          reloadItems,
          setItems,
          items,
          updatedItemsIds,
          initialTimestamp,
          refreshNecessaryCallback);
      if (initialTimestamp.current) {
        return;
      }
      initialTimestamp.current = new Date();
      const initList = async () => {
          showLoadingIndicator(true);
          const result = await loadItems(
              retrieveItems,
              setItems,
              items,
              initialTimestamp);
          initialTimestamp.current = result;
          showLoadingIndicator(false);
        };
      initList();
    }, [ showLoadingIndicator, items, retrieveItems, reloadItems, setItems, initialTimestamp, updateListRef ]);
  
  const headerHeight = 'auto';

  const colorRowAccordingToUpdateStatus = items?.reduce((props, item) => {
      if (item === undefined) {
        return props;
      }
      if (item.status === undefined) {
        return {
          ...props,
          [ item.id ]: {
            background: "list-ended"
          }
        }
      }
      if (item.status !== ListItemStatus.INITIAL) {
        return {
            ...props,
            [ item.id ]: { background:
                item.status === ListItemStatus.NEW
                    ? "list-new"
                    : item.status === ListItemStatus.UPDATED
                    ? "list-updated"
                    : item.status === ListItemStatus.ENDED
                    ? "list-ended"
                    : item.status === ListItemStatus.REMOVED_FROM_LIST
                    ? "list-removed_from_list"
                    : undefined
              }
          };
      }
      return props;
    }, {});

  return (<Box
              fill
              background={ columnHeaderSeparator as BackgroundType }>
            <SnapScrollingDataTable
                pin
                minWidthOfAutoColumn={ minWidthOfAutoColumn }
                additionalHeader={ additionalHeader }
                border={ rowSeparator
                    ? { body: { side: 'bottom', color: typeof rowSeparator === 'string' ? rowSeparator : 'light-3' } }
                    : undefined }
                rowProps={ applyBackgroundColor ? colorRowAccordingToUpdateStatus : undefined }
                size='100%'
                columns={ columns }
                step={ itemsBatchSize }
                headerHeight={ headerHeight }
                showColumnHeaders={ showColumnHeaders }
                columnHeaderBackground={ columnHeaderBackground }
                columnHeaderSeparator={ columnHeaderSeparator }
                onMore={ () => loadItems(retrieveItems, setItems, items, initialTimestamp) }
                data={ items }
                replace />
          </Box>);

};

export { SearchableAndSortableUpdatingList };
