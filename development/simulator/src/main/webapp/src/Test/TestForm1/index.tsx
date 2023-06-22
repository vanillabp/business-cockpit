const taskDefinition = 'TestForm1';

const taskListColumns = [
  {
    id: 'dueDate',
    title: {
      'de': 'Fällig',
      'en': 'due'
    },
    path: 'dueDate',
    showAsColumn: true,
    sortable: true,
    filterable: true
  },
  {
    id: 'ID1',
    title: {
      'de': 'ID 1',
      'en': 'id 1'
    },
    path: 'details.test1.testId1',
    showAsColumn: true,
    sortable: true,
    filterable: true
  }
];

export { taskListColumns, taskDefinition };
