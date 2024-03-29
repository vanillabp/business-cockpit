import { getObjectProperty, TextListCell, UserTaskListCell } from '@vanillabp/bc-shared';

const TestForm1ListCell: UserTaskListCell = ({
  item,
  column,
  defaultCell
}) => {
  const DefaultCell = defaultCell;
  return column.path.startsWith('details')
      ? <TextListCell item={ item } value={ getObjectProperty(item.data, column.path) } />
      : <DefaultCell item={ item } column={ column } />;
}

export default TestForm1ListCell;
