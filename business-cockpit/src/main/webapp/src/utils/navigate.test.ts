import { navigateToWorkflow, openTask } from './navigate';

/**
 * Opening a task or a case which is worked on in another application. The cockpit opens the
 * address the workflow module reported, in a window of its own, and a second click brings that
 * window to the front instead of opening a new one.
 */

const t = ((key: string) => key) as any;

const task = (id: string, uiUriType: string, uiUri?: string) => ({
  id,
  workflowId: `workflow-of-${id}`,
  uiUriType,
  uiUri,
}) as any;

const workflow = (id: string, uiUriType: string, uiUri?: string) => ({
  id,
  uiUriType,
  uiUri,
}) as any;

describe('navigate', () => {

  let opened: Array<{ url: string, name: string, focused: number }>;
  let toasted: Array<any>;
  let navigatedTo: Array<string>;

  const toast = (message: any) => { toasted.push(message) };
  const navigate = ((path: string) => { navigatedTo.push(path) }) as any;

  beforeEach(() => {
    opened = [];
    toasted = [];
    navigatedTo = [];
    window.open = jest.fn((url?: any, name?: any) => {
      const record = { url: url as string, name: name as string, focused: 0 };
      opened.push(record);
      return {
        closed: false,
        focus: () => { record.focused++ },
      } as any;
    });
  });

  test('a task of another application is opened at the address it reported', () => {
    openTask(task('task-1', 'EXTERNAL', 'https://tickets.example.com/ticket/4711'), toast, t);

    expect(opened).toHaveLength(1);
    expect(opened[0].url).toBe('https://tickets.example.com/ticket/4711');
    expect(toasted).toHaveLength(0);
  });

  test('a second click brings the window that is already open to the front', () => {
    const external = task('task-2', 'EXTERNAL', 'https://tickets.example.com/ticket/4712');
    openTask(external, toast, t);
    openTask(external, toast, t);

    expect(opened).toHaveLength(1);
    // once when it was opened, once when the second click brought it back
    expect(opened[0].focused).toBe(2);
  });

  test('a federated task is opened at the page of the cockpit', () => {
    openTask(task('task-3', 'WEBPACK_MF_REACT'), toast, t);

    expect(opened).toHaveLength(1);
    expect(opened[0].url).toBe('/url-usertask/task-3');
  });

  test('a task of another application which reports no address says so', () => {
    openTask(task('task-4', 'EXTERNAL'), toast, t);

    expect(opened).toHaveLength(0);
    expect(toasted).toHaveLength(1);
    expect(toasted[0].message).toBe('no-ui-uri_message');
  });

  test('a type nobody knows is still reported as unsupported', () => {
    openTask(task('task-5', 'SOMETHING_ELSE'), toast, t);

    expect(opened).toHaveLength(0);
    expect(toasted[0].message).toBe('unsupported-ui-uri-type_message');
  });

  test('a case of another application is opened at the address it reported', () => {
    navigateToWorkflow(
        workflow('workflow-1', 'EXTERNAL', 'https://orders.example.com/order/4711'),
        toast, t, navigate);

    expect(opened).toHaveLength(1);
    expect(opened[0].url).toBe('https://orders.example.com/order/4711');
    expect(navigatedTo).toHaveLength(0);
  });

  test('a federated case is shown by the cockpit itself', () => {
    navigateToWorkflow(workflow('workflow-2', 'WEBPACK_MF_REACT'), toast, t, navigate);

    expect(navigatedTo).toEqual([ '/url-workflowlist/workflow-2' ]);
    expect(opened).toHaveLength(0);
  });

  test('asked from a task the cockpit goes to its own page, because the task says nothing about the case', () => {
    navigateToWorkflow(
        task('task-6', 'EXTERNAL', 'https://tickets.example.com/ticket/4713'),
        toast, t, navigate);

    expect(navigatedTo).toEqual([ '/url-workflowlist/workflow-of-task-6' ]);
    expect(opened).toHaveLength(0);
    expect(toasted).toHaveLength(0);
  });

});
