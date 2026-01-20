/**
 * A map of language codes to their corresponding translated strings.
 * The keys are language identifiers (e.g., 'en', 'de'),
 * and the values are the translated versions of the same content.
 *
 * @example
 *
 * const title: Translatable = {
 * en: "Hello",
 * de: "Hallo",
 * };
 *
 */
export interface Translatable {
  [languageKey: string]: string;
}
