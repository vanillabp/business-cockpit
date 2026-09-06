import { NotificationConfiguration, NotificationWorkflow } from '@vanillabp/bc-official-gui-client';

/**
 * Pure helpers for editing the notification configuration on the config page (AC func 4).
 * Kept free of React/UI so the "none clears all media" rule (4e) and the inherit-from-global
 * defaults (4d) can be unit-tested in isolation.
 */

export type Translatable = { [language: string]: string };

export const translate = (map: Translatable | undefined | null, language: string): string => {
  if (!map) {
    return '';
  }
  if (map[language] !== undefined) {
    return map[language];
  }
  const keys = Object.keys(map);
  return keys.length > 0 ? map[keys[0]] : '';
};

export const workflowKey = (workflow: NotificationWorkflow): string =>
    `${workflow.workflowModuleId}#${workflow.bpmnProcessId}`;

const clone = (config: NotificationConfiguration): NotificationConfiguration => ({
  globalAllViaMedium: { ...(config.globalAllViaMedium ?? {}) },
  perWorkflow: Object.fromEntries(
      Object.entries(config.perWorkflow ?? {}).map(([k, v]) => [k, { none: v.none, allViaMedium: { ...(v.allViaMedium ?? {}) } }])),
});

// ---- global setting -------------------------------------------------------

export const globalMediumEnabled = (config: NotificationConfiguration, medium: string): boolean =>
    Boolean(config.globalAllViaMedium?.[medium]);

/** Global "none" is checked when no medium is enabled globally (AC func 4a/4d). */
export const globalNoneChecked = (config: NotificationConfiguration, media: string[]): boolean =>
    media.every(medium => !globalMediumEnabled(config, medium));

export const setGlobalMedium = (
    config: NotificationConfiguration,
    medium: string,
    checked: boolean): NotificationConfiguration => {
  const next = clone(config);
  next.globalAllViaMedium = { ...next.globalAllViaMedium, [medium]: checked };
  return next;
};

/** Selecting global "none" clears every "all via <medium>" flag (AC func 4e). */
export const setGlobalNone = (config: NotificationConfiguration): NotificationConfiguration => {
  const next = clone(config);
  next.globalAllViaMedium = {};
  return next;
};

// ---- per-workflow override ------------------------------------------------

export const workflowNoneChecked = (
    config: NotificationConfiguration,
    key: string,
    media: string[]): boolean => {
  const override = config.perWorkflow?.[key];
  if (override?.none) {
    return true;
  }
  return media.every(medium => !workflowMediumEnabled(config, key, medium));
};

/**
 * Effective "all via <medium>" state for a workflow: the explicit override if present, otherwise
 * inherited from the global setting (AC func 4d).
 */
export const workflowMediumEnabled = (
    config: NotificationConfiguration,
    key: string,
    medium: string): boolean => {
  const override = config.perWorkflow?.[key];
  if (override?.none) {
    return false;
  }
  if (override?.allViaMedium && override.allViaMedium[medium] !== undefined) {
    return Boolean(override.allViaMedium[medium]);
  }
  return globalMediumEnabled(config, medium);
};

export const setWorkflowMedium = (
    config: NotificationConfiguration,
    key: string,
    medium: string,
    checked: boolean,
    media: string[]): NotificationConfiguration => {
  const next = clone(config);
  // materialize the currently effective per-medium state so toggling one medium does not silently
  // change the others that were inherited from the global setting
  const allViaMedium: { [k: string]: boolean } = {};
  media.forEach(m => { allViaMedium[m] = workflowMediumEnabled(config, key, m); });
  allViaMedium[medium] = checked;
  next.perWorkflow = { ...next.perWorkflow, [key]: { none: false, allViaMedium } };
  return next;
};

/** Selecting a workflow's "none" clears all its media (AC func 4e). */
export const setWorkflowNone = (
    config: NotificationConfiguration,
    key: string): NotificationConfiguration => {
  const next = clone(config);
  next.perWorkflow = { ...next.perWorkflow, [key]: { none: true, allViaMedium: {} } };
  return next;
};

export const emptyConfiguration = (): NotificationConfiguration => ({
  globalAllViaMedium: {},
  perWorkflow: {},
});
