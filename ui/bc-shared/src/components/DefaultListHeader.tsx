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
  isDefaultSort: boolean,
  sortAscending?: boolean,
  defaultSortAscending: boolean,
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
    isDefaultSort,
    sortAscending,
    defaultSortAscending,
    setSort,
    setSortAscending,
    allSelected,
    selectAll
}) => {
  const language = currentLanguage ?? window.navigator.language.replace(/* exclude country */ /-.*$/, '');
  const setNewSortAscending = () => {
    if (isDefaultSort) {
      if (sortAscending === defaultSortAscending) {
        setSortAscending(!defaultSortAscending);
      } else {
        setSort(undefined);
      }
    } else if (sortAscending) {
      setSortAscending(false);
    } else {
      setSort(undefined);
    }
  };
  if (column.path === 'id') {
    return (
        <Box
            fill
            pad="xsmall"
            align="center">
          <CheckBox
              checked={ allSelected }
              onChange={ event => selectAll(event.currentTarget.checked) } />
        </Box>);
  }
  if (column.path === 'candidateUsers') {
    return (
        <Box
            pad="xsmall"
            fill
            align="center">
          <Tip
              content={ column.title[language] }>
            <ContactInfo />
          </Tip>
        </Box>);
  }
  return (
      <Box
          fill
          direction="row"
          justify="between"
          pad="xsmall">
        <Text
            truncate="tip">
          { column.title[language] }
        </Text>
        <Box
            align="center"
            direction="row"
            justify="end"
            style={ { maxHeight: '1.5rem', minWidth: '2rem' } }>
          {
            !column.sortable
                ? undefined
                : <Box
                      style={ { position: 'relative' } }
                      width="1.6rem"
                      height="2rem"
                      align="center">
                    {
                      !Boolean(sort)
                      ? <Box
                            style={ { position: 'relative', top: '-1px' } }
                            focusIndicator={ false }
                            onClick={ event => setSort(column) }
                            direction="column"
                            justify="around">
                          <Unsorted size="34px" />
                        </Box>
                      : sortAscending
                          ? <Box
                                focusIndicator={ false }
                                style={ { marginRight: '-0.5rem' } }
                                width="1.2rem"
                                height="2rem"
                                direction="column"
                                justify="around"
                                onClick={ event => setNewSortAscending() }>
                              <Ascend size="18px" />
                            </Box>
                          : <Box
                                focusIndicator={ false }
                                style={ { marginRight: '-0.5rem' } }
                                width="1.2rem"
                                height="2rem"
                                direction="column"
                                justify="around"
                                onClick={ event => setNewSortAscending() }>
                              <Descend size="18px" />
                            </Box>
                    }
                  </Box>
          }
          { /* <FormFilter /> */ }
        </Box>
      </Box>);
}

export { DefaultListHeader };
