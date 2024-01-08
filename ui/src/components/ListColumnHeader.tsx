import { Column } from "@vanillabp/bc-shared";
import { MouseEvent as ReactMouseEvent, useCallback, useEffect, useRef, useState } from "react";
import { Box, Text } from "grommet";
import { Ascend, Descend, Unsorted } from "grommet-icons";

const ListColumnHeader = ({
  currentLanguage,
  column,
  minWidth,
  setColumnWidthAdjustment,
  sort,
  sortAscending,
  setSort,
  setSortAscending,
}: {
  currentLanguage: string,
  column: Column,
  minWidth?: string,
  setColumnWidthAdjustment: (column: string, adjustment: number) => void,
  sort?: boolean,
  sortAscending?: boolean,
  setSort: (column?: Column) => void,
  setSortAscending: (ascending: boolean) => void,
}) => {
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
    setColumnWidthAdjustment(column.path, adjustment);
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
  return (
      <Box
          style={ { minWidth, position: "relative" } }>
        <Box
            direction="row"
            justify="between"
            align="center"
            overflow="hidden"
            style={ { position: "relative" } }>
          <Text
              truncate="tip">{ column.title[currentLanguage] || column.title['en'] }</Text>
          <Box
              align="center"
              direction="row"
              justify="end"
              style={ { maxHeight: '1.5rem', minWidth: '2rem' } }>
            {
              !column.sortable
                  ? undefined
                  : !Boolean(sort)
                      ? <Box
                          overflow="hidden"
                          focusIndicator={ false }
                          width="1.6rem"
                          onClick={ event => setSort(column) }>
                        <Unsorted
                            size="32rem" />
                      </Box>
                      : sortAscending
                          ? <Box
                              focusIndicator={ false }
                              onClick={ event => setSortAscending(false) }
                              pad={ { right: '0.5rem' } }>
                            <Ascend size="16rem" />
                          </Box>
                          : <Box
                              focusIndicator={ false }
                              onClick={ event => setSort(undefined) }
                              pad={ { right: '0.5rem' } }>
                            <Descend size="16rem" />
                          </Box>
            }
            { /* <FormFilter /> */ }
          </Box>
        </Box>
        <Box
            align="center"
            onMouseDown={ startResize }
            style={ { cursor: 'col-resize', position: "absolute", top: '-0.5rem', bottom: '-0.5rem', right: '-0.35rem' } }>
          &nbsp;
        </Box>
      </Box>);
};

export { ListColumnHeader }