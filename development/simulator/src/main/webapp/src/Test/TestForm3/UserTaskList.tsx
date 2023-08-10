import React from 'react';
import { getObjectProperty, UserTaskListCell, TextListCell } from '@vanillabp/bc-shared';

const TestForm3ListCell: UserTaskListCell = ({
  item,
  column,
  defaultCell
}) => {
  const DefaultCell = defaultCell;
  return column.path.startsWith('details.test1')
      ? <TextListCell item={ item } weight="bold" value={ getObjectProperty(item.data, column.path) } />
      : <DefaultCell item={ item } column={ column } />;
}

export default TestForm3ListCell;
