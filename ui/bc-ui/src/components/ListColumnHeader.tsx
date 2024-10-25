import {
  Column,
  DefaultListHeader,
  DefaultListHeaderAwareProps,
  TranslationFunction,
  useResponsiveScreen
} from "@vanillabp/bc-shared";
import { FC, MouseEvent as ReactMouseEvent, useCallback, useEffect, useRef, useState } from "react";
import { Box } from "grommet";

const AUTO_SIZE_COLUMN = '';

const ListColumnHeader = ({
  t,
  currentLanguage,
  column,
  columnIndex,
  numberOfAllColumns,
  nameOfList,
  columnHeader,
  columnWidthAdjustment,
  setAutoColumnWidth,
  setColumnWidthAdjustment,
  sort,
  isDefaultSort,
  sortAscending,
  defaultSortAscending,
  setSort,
  setSortAscending,
  allSelected,
  selectAll
}: {
  t: TranslationFunction,
  currentLanguage: string,
  column: Column,
  columnIndex: number,
  numberOfAllColumns: number,
  nameOfList?: string,
  columnHeader?: FC<DefaultListHeaderAwareProps<any>>,
  columnWidthAdjustment?: number,
  setAutoColumnWidth: (column: Column, width: number) => void,
  setColumnWidthAdjustment: (column: Column, adjustment: number) => void,
  sort?: boolean,
  isDefaultSort: boolean,
  sortAscending?: boolean,
  defaultSortAscending: boolean,
  setSort: (column?: Column) => void,
  setSortAscending: (ascending: boolean) => void,
  allSelected: boolean;
  selectAll: (select: boolean) => void;
}) => {
  const { isPhone, isTablet } = useResponsiveScreen();

  const resize = useRef(-1);
  const [ widthAdjustment, setWidthAdjustment ] = useState(columnWidthAdjustment ?? 0);
  const startResize = (event: ReactMouseEvent) => {
    event.stopPropagation();
    event.preventDefault();
    resize.current = event.clientX;
    document.body.style.cursor = 'col-resize';
    moveHandler(event.nativeEvent);
  };
  const moveHandler = useCallback((ev: MouseEvent) => {
    if (resize.current == -1) return;
    const adjustment = widthAdjustment + (ev.clientX - resize.current);
    setColumnWidthAdjustment(column, adjustment);
  }, [ resize, setColumnWidthAdjustment, widthAdjustment, column.path ]);
  const upHandler = useCallback((ev: MouseEvent) => {
    if (resize.current == -1) return;
    document.body.style.cursor = 'default';
    const adjustment = widthAdjustment + (ev.clientX - resize.current);
    resize.current = -1;
    setWidthAdjustment(adjustment);
  }, [ resize, setWidthAdjustment, widthAdjustment ]);
  useEffect(() => {
    window.addEventListener("mousemove", moveHandler);
    window.addEventListener("mouseup", upHandler);
    return () => {
      window.removeEventListener("mousemove", moveHandler);
      window.removeEventListener("mouseup", upHandler);
    }
  }, [ setColumnWidthAdjustment, widthAdjustment, setWidthAdjustment ]);
  const Header = columnHeader!;
  return (
      <Box
          style={ {
            position: "relative",
            overflow: (columnIndex + 1) === numberOfAllColumns ? "hidden" : undefined
          } }
          ref={ element => {
            if (element === null) return;
            if (columnWidthAdjustment !== undefined) return;
            if (column.width !== AUTO_SIZE_COLUMN) return;
            setAutoColumnWidth(column, element!.getBoundingClientRect().width);
          } }>
        <Box
            direction="row"
            justify="between"
            align="center"
            overflow="hidden"
            style={ { position: "relative" } }>
          {
            columnHeader !== undefined
                ? <Header
                      column={ column }
                      defaultHeader={ DefaultListHeader }
                      currentLanguage={ currentLanguage }
                      nameOfList={ nameOfList }
                      isPhone={ isPhone }
                      isTablet={ isTablet }
                      selectAll={ selectAll }
                      allSelected={ allSelected }
                      sort={ sort }
                      isDefaultSort={ isDefaultSort }
                      sortAscending={ sortAscending }
                      defaultSortAscending={ defaultSortAscending }
                      setSort={ setSort }
                      setSortAscending={ setSortAscending } />
                : <DefaultListHeader
                    column={ column }
                    currentLanguage={ currentLanguage }
                    nameOfList={ nameOfList }
                    isPhone={ isPhone }
                    isTablet={ isTablet }
                    selectAll={ selectAll }
                    allSelected={ allSelected }
                    sort={ sort }
                    isDefaultSort={ isDefaultSort }
                    sortAscending={ sortAscending }
                    defaultSortAscending={ defaultSortAscending }
                    setSort={ setSort }
                    setSortAscending={ setSortAscending } />
          }
        </Box>
        {
          column.resizeable
              ? <Box
                    align="center"
                    onMouseDown={ startResize }
                    style={ { cursor: 'col-resize', position: "absolute", top: '-0.5rem', bottom: '-0.5rem', right: '-0.2rem', zIndex: 1000 } }>
                  &nbsp;
                </Box>
              : <Box
                    align="center"
                    style={ { cursor: 'not-allowed', position: "absolute", top: '-0.5rem', bottom: '-0.5rem', right: '-0.2rem', zIndex: 1000 } }>
                  &nbsp;
                </Box>
        }
      </Box>);
};

export { ListColumnHeader, AUTO_SIZE_COLUMN }