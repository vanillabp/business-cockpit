import { Column } from "@vanillabp/bc-shared";

const sortWithoutColumnTypeSpecificAttributes = (currentLanguage: string, effectiveSort?: string) =>
  effectiveSort?.endsWith(`.${currentLanguage}`) // column type 'i18n'
    ? effectiveSort?.substring(0, effectiveSort?.length - currentLanguage.length - 1)
    : effectiveSort?.indexOf('.sort,') !== -1 // column type 'person'
    ? effectiveSort?.substring(0, effectiveSort?.indexOf('.sort,'))
    : effectiveSort;

const sortWithColumnTypeSpecificAttributes = (currentLanguage: string, column?: Column) =>
  column === undefined
    ? undefined
    : column.type === 'i18n'
    ? `${column.path}.${currentLanguage ?? window.navigator.language.replace(/* exclude country */ /-.*$/, '')}`
    : column.type === 'person'
    ? `${column.path}.sort,${column.path}.id`
    : column.path;

export {
  sortWithColumnTypeSpecificAttributes,
  sortWithoutColumnTypeSpecificAttributes,
}