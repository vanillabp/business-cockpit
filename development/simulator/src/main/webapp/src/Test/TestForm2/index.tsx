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
    filterable: true,
    width: '10rem',
    priority: 99
  }
];

export { taskListColumns, taskDefinition };
