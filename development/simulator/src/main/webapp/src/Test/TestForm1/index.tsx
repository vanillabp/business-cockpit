const taskDefinition = 'TestForm1';

const userTaskListColumns = [
  {
    id: 'dueDate',
    title: {
      'de': 'Fällig',
      'en': 'due'
    },
    path: 'dueDate',
    showAsColumn: true,
    sortable: true,
    filterable: true,
    width: '10rem',
    priority: 99
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
    filterable: true,
    width: '10rem',
    priority: 1
  }
];

export { userTaskListColumns, taskDefinition };
