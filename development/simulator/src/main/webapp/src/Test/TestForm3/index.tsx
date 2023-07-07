const taskDefinition = 'TestForm3';

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
    id: 'ID2',
    title: {
      'de': 'ID 2',
      'en': 'id 2'
    },
    path: 'details.test1.testId2',
    showAsColumn: true,
    sortable: true,
    filterable: true
  }
];

export { taskListColumns, taskDefinition };
