import { getObjectProperty, TextListCell, UserTaskListCell } from '@vanillabp/bc-shared';

const TestForm1ListCell: UserTaskListCell = ({
  item,
  column,
  defaultCell,
  ...props
}) => {
  const DefaultCell = defaultCell;
  return column.path.startsWith('details')
      ? <TextListCell item={ item } value={ getObjectProperty(item.data, column.path) } { ...props } />
      : <DefaultCell item={ item } column={ column } { ...props } />;
}

export default TestForm1ListCell;
