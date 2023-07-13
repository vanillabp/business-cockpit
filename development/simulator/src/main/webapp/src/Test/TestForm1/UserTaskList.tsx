import React from 'react';
import { getObjectProperty, UserTaskListCell, UserTaskListTextCellWrapper } from '@vanillabp/bc-shared';

const TestForm1ListCell: UserTaskListCell = ({
  item,
  column,
  defaultCell
}) => {
  const DefaultCell = defaultCell;
  return column.path.startsWith('details')
      ? <UserTaskListTextCellWrapper value={ getObjectProperty(item.data, column.path) } />
      : <DefaultCell item={ item } column={ column } />;
}

export default TestForm1ListCell;
