import { Column } from "../types";
import { FC } from "react";
import { Box, CheckBox, Text, Tip } from "grommet";
import { Ascend, ContactInfo, Descend, Unsorted } from "grommet-icons";

export interface DefaultListHeaderProps<D> {
  column: Column;
  currentLanguage: string;
  nameOfList?: string;
  isPhone: boolean;
  isTablet: boolean;
  selectAll: (select: boolean) => void;
  allSelected: boolean;
  sort?: boolean,
  sortAscending?: boolean,
  setSort: (column?: Column) => void,
  setSortAscending: (ascending: boolean) => void,
}

export interface DefaultListHeaderAwareProps<T> extends DefaultListHeaderProps<T> {
  defaultHeader: FC<DefaultListHeaderProps<T>>;
}

const DefaultListHeader: FC<DefaultListHeaderProps<any>> = ({
    column,
    currentLanguage,
    sort,
    sortAscending,
    setSort,
    setSortAscending,
    allSelected,
    selectAll
}) => {
  if (column.path === 'id') {
    return (
        <Box
            align="center">
          <CheckBox
              checked={ allSelected }
              onChange={ event => selectAll(event.currentTarget.checked) } />
        </Box>);
  }
  if (column.path === 'candidateUsers') {
    return (
        <Box
            fill
            align="center">
          <Tip
              content={ column.title[currentLanguage] || column.title['en'] }>
            <ContactInfo />
          </Tip>
        </Box>);
  }
  return (
      <>
        <Text
            truncate="tip">
          { column.title[currentLanguage] || column.title['en'] }
        </Text>
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
      </>);
}

export { DefaultListHeader };
