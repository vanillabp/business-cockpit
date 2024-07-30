import { getObjectProperty, TextListCell, UserTaskListCell } from '@vanillabp/bc-shared';

const TestForm2ListCell: UserTaskListCell = ({
  item,
  column,
  defaultCell,
  ...props
}) => {
  const DefaultCell = defaultCell;
  return column.path.startsWith('details.test1')
      ? <TextListCell item={ item } weight="bold" value={ getObjectProperty(item.data, column.path) } />
      : <DefaultCell item={ item } column={ column } { ...props } />;
}

export default TestForm2ListCell;
