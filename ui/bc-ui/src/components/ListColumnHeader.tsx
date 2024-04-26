import {
  Column,
  DefaultListHeader,
  DefaultListHeaderAwareProps,
  TranslationFunction,
  useResponsiveScreen
} from "@vanillabp/bc-shared";
import { FC, MouseEvent as ReactMouseEvent, useCallback, useEffect, useRef, useState } from "react";
import { Box } from "grommet";

const ListColumnHeader = ({
  t,
  currentLanguage,
  column,
  columnIndex,
  numberOfAllColumns,
  nameOfList,
  columnHeader,
  hasColumnWidthAdjustment,
  setColumnWidthAdjustment,
  sort,
  sortAscending,
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
  hasColumnWidthAdjustment: boolean,
  setColumnWidthAdjustment: (column: Column, adjustment: number) => void,
  sort?: boolean,
  sortAscending?: boolean,
  setSort: (column?: Column) => void,
  setSortAscending: (ascending: boolean) => void,
  allSelected: boolean;
  selectAll: (select: boolean) => void;
}) => {
  const { isPhone, isTablet } = useResponsiveScreen();

  const resize = useRef(-1);
  const [ widthAdjustment, setWidthAdjustment ] = useState(0);
  const startResize = (event: ReactMouseEvent) => {
    event.stopPropagation();
    event.preventDefault();
    resize.current = event.clientX;
    document.body.style.cursor = 'col-resize';
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
            if (hasColumnWidthAdjustment) return;
            if (column.width !== '') return;
            setColumnWidthAdjustment(
                column,
                element!.getBoundingClientRect().width);
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
                      sortAscending={ sortAscending }
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
                    sortAscending={ sortAscending }
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
              : undefined
        }
      </Box>);
};

export { ListColumnHeader }