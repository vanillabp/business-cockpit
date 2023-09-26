import React, { MutableRefObject, useCallback, useEffect, useRef, useState } from 'react';
import { Box, ColumnConfig } from 'grommet';
import { SnapScrollingDataTable } from './SnapScrollingDataTable.js';
import { ListItemStatus, ShowLoadingIndicatorFunction } from '@vanillabp/bc-shared';

const itemsBatchSize = 30;

export type ListItemData = {
  id: string,
  version?: number,
  createdAt: Date,
  updatedAt: Date,
  endedAt?: Date,
};

export interface ListItem<T extends ListItemData> {
  id: string;
  number: number;
  data: T;
  status: ListItemStatus;
  selected: boolean;
};

export interface ListItems<T extends ListItemData> {
  serverTimestamp: Date;
  items: Array<T>;
};

type RetrieveItemsFunction = <T extends ListItemData>(pageNumber: number, pageSize: number, initialTimestamp: Date | undefined) => Promise<ListItems<T>>;

type ReloadItemsFunction = <T extends ListItemData>(numberOfItems: number, knownItemsIds: Array<string>, initialTimestamp: Date | undefined) => Promise<ListItems<T>>;

export type ReloadCallbackFunction = (updatedItemsIds: Array<string>) => Promise<void>;

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
) => {

  const prefill = 2; // estimate that not more than 60 items will be created at once
  const size = (items!.length === 0
      ? prefill
      : Math.floor(items!.length / itemsBatchSize) + prefill) * itemsBatchSize;

  const result = await reloadItems(
      size,
      items!
          .filter(item => !updatedItemsIds.includes(item.id))
          .map(item => item.id),
      initialTimestamp.current);
  
  const itemsById = new Map(items!.map(item => [ item.id, item ]));
  const mergedItems = result
      .items
      .map((item, index) => {
        const oldItem = itemsById.get(item.id)!;
        const itemNotInUpdateResponse = item.version === 0;

        const status = itemNotInUpdateResponse
            ? oldItem.status
            : Boolean(item.endedAt) && item.endedAt!.getTime() > initialTimestamp.current!.getTime()
            ? ListItemStatus.ENDED
            : item.createdAt.getTime() > initialTimestamp.current!.getTime()
            ? ListItemStatus.NEW
            : item.updatedAt.getTime() > initialTimestamp.current!.getTime()
            ? ListItemStatus.UPDATED
            : ListItemStatus.INITIAL;

        const newItem = (itemNotInUpdateResponse
            ? {
                id: item.id,
                data: oldItem?.data,
                number: 1 + index,
                selected: oldItem?.selected,
                status: oldItem?.status,
              }
            : {
                id: item.id,
                data: item,
                number: 1 + index,
                selected: oldItem?.selected,
                status,
              }
            ) as ListItem<T>;
        return newItem;
      });
   setItems(mergedItems);
   
};

const SearchableAndSortableUpdatingList = <T extends ListItemData>({
  itemsRef,
  updateListRef,
  retrieveItems,
  reloadItems,
  columns,
  showLoadingIndicator,
}: {
  itemsRef: MutableRefObject<Array<ListItem<T>> | undefined>,
  updateListRef: MutableRefObject<ReloadCallbackFunction | undefined>,
  retrieveItems: RetrieveItemsFunction,
  reloadItems: ReloadItemsFunction,
  columns: ColumnConfig<any>[],
  showLoadingIndicator: ShowLoadingIndicatorFunction,
}) => {
  const [ items, _setItems ] = useState<Array<ListItem<T>> | undefined>(undefined);
  const initialTimestamp = useRef<Date | undefined>(undefined);
  const setItems = useCallback((newItems: Array<ListItem<T>>) => {
      itemsRef.current = newItems;
      _setItems(newItems);
    }, [ _setItems, itemsRef ]);
    
  useEffect(() => {
      updateListRef.current = (updatedItemsIds) => reloadData(
          reloadItems,
          setItems,
          items,
          updatedItemsIds,
          initialTimestamp);
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
  const phoneMargin = '0';
  
  const colorRowAccordingToUpdateStatus = items?.reduce((props, item) => {
      if (item.status !== ListItemStatus.INITIAL) {
        return {
            ...props,
            [ item.id ]: { background:
                item.status === ListItemStatus.NEW
                    ? { color: 'accent-3', opacity: 0.1 }
                    : item.status === ListItemStatus.UPDATED
                    ? { color: 'accent-1', opacity: 0.15 }
                    : item.status === ListItemStatus.ENDED
                    ? { color: 'light-2', opacity: 0.5 }
                    : undefined
              }
          };
      }
      return props;
    }, {});
   
  return (<Box
              fill>
            <SnapScrollingDataTable
                fill
                pin
                border={ { body: { side: 'bottom', color: 'light-3' } } }
                rowProps={ colorRowAccordingToUpdateStatus }
                size='100%'
                columns={ columns }
                step={ itemsBatchSize }
                headerHeight={ headerHeight }
                phoneMargin={ phoneMargin }
                onMore={ () => loadItems(retrieveItems, setItems, items, initialTimestamp) }
                data={ items }
                replace />
          </Box>);

};

export { SearchableAndSortableUpdatingList };
