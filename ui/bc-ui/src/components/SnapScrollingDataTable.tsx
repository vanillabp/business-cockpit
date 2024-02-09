import { Box, ColumnConfig, DataTable, DataTableExtendedProps, Grid, Text } from 'grommet';
import { SnapAlignBox, SnapScrollingGrid } from './SnapScrolling.js';
import React, { forwardRef, PropsWithChildren, ReactElement, ReactNode, UIEventHandler } from 'react';
import { BackgroundType, ColorType } from "grommet/utils";
import styled from "styled-components";

interface SnapScrollingDataTableProps<TRowType = any> extends PropsWithChildren<Omit<DataTableExtendedProps<TRowType>, 'columns' | 'ref'>> {
  additionalHeader?: ReactNode | undefined;
  headerHeight: string;
  onScroll?: UIEventHandler<any> | undefined;
  columns: ColumnConfig<TRowType>[];
  minWidthOfAutoColumn?: string;
  showColumnHeaders?: boolean;
  columnHeaderBackground?: BackgroundType,
  columnHeaderSeparator?: ColorType | null,
};

const calculateColumWidth = (minWidthOfAutoColumn: string | undefined, width: string, column: any) => {
  const columnSize = column.size ? column.size : minWidthOfAutoColumn;
  return width !== '' ? width + ' + ' + columnSize : columnSize;
}

const StyledDataTable = styled(DataTable)`
  tr {
    width: unset;
  }
`;

const SnapScrollingDataTable = forwardRef(({
    headerHeight,
    additionalHeader,
    columns,
    onScroll,
    children,
    minWidthOfAutoColumn,
    showColumnHeaders = true,
    columnHeaderBackground = 'dark-3',
    columnHeaderSeparator,
    ...props
  }: SnapScrollingDataTableProps, ref) => {

  const columnsWidth = columns.reduce((width, column) => calculateColumWidth(minWidthOfAutoColumn, width, column), '');
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
            background={ columnHeaderBackground }
            align="center">
          {
            additionalHeader
          }
          {
            showColumnHeaders
                ? <Grid
                      fill
                      columns={ columns.map(column => column.size ? column.size : 'auto') }
                      style={ {
                          maxHeight: headerHeight,
                          minHeight: headerHeight,
                          marginLeft: 'auto',
                          marginRight: 'auto',
                          zIndex: '2',
                        } }
                      >{
                    columns.map((column, index) =>
                        <SnapAlignBox
                            style={
                                  column.size === undefined
                                      ? { minWidth: columnHeaderSeparator === null ? minWidthOfAutoColumn : `calc(${minWidthOfAutoColumn} - 1px)` }
                                      : { width: columnHeaderSeparator === null ? column.size : `calc(${column.size} - 1px)` }
                            }
                            key={ `column${index}` }
                            align="left"
                            justify='center'
                            height="100%"
                            border={
                              (index === 0) || (columnHeaderSeparator === null)
                                  ? undefined
                                  : { side: 'left', color: columnHeaderSeparator }
                            }
                            snapAlign='center'>
                          {
                            column.header instanceof String
                                ? <Text
                                      color='light-2'
                                      truncate='tip'>
                                    {
                                      column.header
                                    }
                                  </Text>
                                : column.header as ReactElement
                          }
                        </SnapAlignBox>)
                  }</Grid>
                : undefined
          }
        </Box>
        <StyledDataTable
            pad='none'
            width={ columnsWidth }
            columns={ dataTableColumns }
            { ...props } />
      </SnapScrollingGrid>
    : <></>);
  
});

export { SnapScrollingDataTable };
