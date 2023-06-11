const taskDefinition = 'TestForm2';

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
  }
];

export { taskListColumns, taskDefinition };
