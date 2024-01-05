import { Box, ColumnConfig, DataTable, DataTableExtendedProps, Grid, Text } from 'grommet';
import { SnapAlignBox, SnapScrollingGrid } from './SnapScrolling.js';
import { useResponsiveScreen } from "@vanillabp/bc-shared";
import React, { forwardRef, PropsWithChildren, ReactElement, ReactNode, UIEventHandler } from 'react';

interface SnapScrollingDataTableProps<TRowType = any> extends PropsWithChildren<Omit<DataTableExtendedProps<TRowType>, 'columns'>> {
  additionalHeader?: ReactNode | undefined;
  headerHeight: string;
  phoneMargin: string;
  onScroll?: UIEventHandler<any> | undefined;
  columns: ColumnConfig<TRowType>[];
  minWidthOfAutoColumn?: string;
};

const calculateColumWidth = (minWidthOfAutoColumn: string | undefined, width: string, column: any) => {
  const columnSize = column.size ? column.size : minWidthOfAutoColumn;
  return width !== '' ? width + ' + ' + columnSize : columnSize;
}

const SnapScrollingDataTable = forwardRef(({
    phoneMargin,
    headerHeight,
    additionalHeader,
    columns,
    onScroll,
    children,
    minWidthOfAutoColumn,
    ...props
  }: SnapScrollingDataTableProps, ref) => {

  const { isPhone, isNotPhone } = useResponsiveScreen();

  const columnsWidth = columns.reduce((width, column) => calculateColumWidth(minWidthOfAutoColumn, width, column), '');
  const tableWidth = `max(calc(${columnsWidth}), 100%)`;

  const dataTableColumns = columns.map(column => ({ ...column, header: undefined }));
  return (columns
    ? <SnapScrollingGrid
          fill
          rows={ [ 'max-content', 'auto' ]}
          style={ { overflow: 'auto' } }
          snapDirection='horizontal'
          onScroll={ onScroll }>
        <Box
            width={ '100%' }
            style={ {
              position: 'sticky',
              top: '0',
              zIndex: 2,
              minWidth: 'auto'
              } }
            background='dark-3'
            align="center">
          {
            additionalHeader
          }
          <Grid
              fill
              columns={ columns.map(column => column.size ? column.size : 'auto') }
              style={
                isNotPhone
                    ? {
                        maxHeight: headerHeight,
                        minHeight: headerHeight,
                        marginLeft: 'auto',
                        marginRight: 'auto',
                        zIndex: '2',
                      }
                    : {
                        maxHeight: headerHeight,
                        minHeight: headerHeight,
                    } }
              pad={
                isPhone
                    ? { horizontal: phoneMargin }
                    : undefined }>{
            columns.map((column, index) =>
              <SnapAlignBox
                  style={ { minWidth: column.size === undefined ? minWidthOfAutoColumn : column.size } }
                  key={ `column${index}` }
                  align="left"
                  justify='center'
                  height="100%"
                  border={
                      index === 0
                          ? undefined
                          : { side: 'left' }
                  }
                  pad='xsmall'
                  snapAlign='center'>
                {
                  column.header instanceof String
                      ? <Text
                            color='light-2'
                            truncate='tip'>{
                          column.header
                        }</Text>
                      : column.header as ReactElement
                }
              </SnapAlignBox>)
            }</Grid>
        </Box>
        <DataTable
            ref={ ref as any }
            fill={ isNotPhone }
            pad='none'
            style={ {
              minWidth: tableWidth,
              marginLeft: 'auto',
              marginRight: 'auto'
            } }
            columns={ dataTableColumns }
            { ...props } />
        { children }
      </SnapScrollingGrid>
    : <></>);
  
});

export { SnapScrollingDataTable };
