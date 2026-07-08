import {
  emptyConfiguration,
  globalMediumEnabled,
  globalNoneChecked,
  setGlobalMedium,
  setGlobalNone,
  setWorkflowMedium,
  setWorkflowNone,
  translate,
  workflowKey,
  workflowMediumEnabled,
  workflowNoneChecked,
} from './notificationConfig';

const MEDIA = ['email', 'sms'];
const KEY = 'moduleA#procX';

describe('notificationConfig', () => {

  test('translate prefers the current language and falls back to the first key', () => {
    expect(translate({ de: 'Hallo', en: 'Hi' }, 'de')).toBe('Hallo');
    expect(translate({ en: 'Hi' }, 'de')).toBe('Hi');
    expect(translate(undefined, 'de')).toBe('');
  });

  test('workflowKey', () => {
    expect(workflowKey({ workflowModuleId: 'moduleA', bpmnProcessId: 'procX' })).toBe(KEY);
  });

  test('global none is checked when no medium is enabled', () => {
    expect(globalNoneChecked(emptyConfiguration(), MEDIA)).toBe(true);
    const withEmail = setGlobalMedium(emptyConfiguration(), 'email', true);
    expect(globalNoneChecked(withEmail, MEDIA)).toBe(false);
    expect(globalMediumEnabled(withEmail, 'email')).toBe(true);
    expect(globalMediumEnabled(withEmail, 'sms')).toBe(false);
  });

  test('setGlobalNone clears all media (AC func 4e)', () => {
    let config = setGlobalMedium(emptyConfiguration(), 'email', true);
    config = setGlobalMedium(config, 'sms', true);
    config = setGlobalNone(config);
    expect(globalNoneChecked(config, MEDIA)).toBe(true);
    expect(globalMediumEnabled(config, 'email')).toBe(false);
  });

  test('a workflow without override inherits the global setting (AC func 4d)', () => {
    const config = setGlobalMedium(emptyConfiguration(), 'email', true);
    expect(workflowMediumEnabled(config, KEY, 'email')).toBe(true);
    expect(workflowMediumEnabled(config, KEY, 'sms')).toBe(false);
    expect(workflowNoneChecked(config, KEY, MEDIA)).toBe(false);
  });

  test('setWorkflowMedium overrides one medium and preserves the others', () => {
    const global = setGlobalMedium(emptyConfiguration(), 'email', true); // email inherited on
    const config = setWorkflowMedium(global, KEY, 'sms', true, MEDIA);    // enable sms for this workflow
    expect(workflowMediumEnabled(config, KEY, 'sms')).toBe(true);
    expect(workflowMediumEnabled(config, KEY, 'email')).toBe(true);       // inherited email preserved
  });

  test('excluding a medium for a workflow while global has it enabled', () => {
    const global = setGlobalMedium(emptyConfiguration(), 'email', true);
    const config = setWorkflowMedium(global, KEY, 'email', false, MEDIA);
    expect(workflowMediumEnabled(config, KEY, 'email')).toBe(false);
    expect(globalMediumEnabled(config, 'email')).toBe(true); // global untouched
  });

  test('setWorkflowNone clears the workflow media (AC func 4e)', () => {
    const global = setGlobalMedium(emptyConfiguration(), 'email', true);
    const config = setWorkflowNone(global, KEY);
    expect(workflowNoneChecked(config, KEY, MEDIA)).toBe(true);
    expect(workflowMediumEnabled(config, KEY, 'email')).toBe(false);
  });

  test('workflow none is checked when all its media are effectively off', () => {
    const config = emptyConfiguration(); // global none, no override
    expect(workflowNoneChecked(config, KEY, MEDIA)).toBe(true);
  });

});
