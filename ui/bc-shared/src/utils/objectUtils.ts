
const getObjectProperty = (object: any, path: string, defaultValue?: string): any | undefined => {
    if (!Boolean(object)) { // null or undefined
      return defaultValue;
    }
    const parts = path.split('.');
    return parts.reduce((object, key) => object?.[key], object);
};

// credits to https://wsvincent.com/javascript-merge-two-sorted-arrays/
const mergeTwoSortedArrays = <T>(
    array1: Array<T>,
    array2: Array<T>,
    compare: (first: T, second: T) => boolean = (f, s) => f < s): Array<T> => {

  let merged: Array<T> = [];
  let index1 = 0;
  let index2 = 0;
  let current = 0;

  while (current < (array1.length + array2.length)) {

    let isArr1Depleted = index1 >= array1.length;
    let isArr2Depleted = index2 >= array2.length;

    if (!isArr1Depleted && (isArr2Depleted || compare(array1[index1], array2[index2]))) {
      merged[current] = array1[index1];
      index1++;
    } else {
      merged[current] = array2[index2];
      index2++;
    }

    current++;
  }

  return merged;
}

const keepOldItemsInArray = <T>(
    newArray: Array<T>,
    oldArray: Array<T>,
    getId: (item: T) => any = item => item,
    map: (newItem: T | undefined, oldItem: T | undefined, index: number) => T = (i, oi, idx) => (i || oi)!,
    isFirstBeforeSecond: (first: T, second: T) => boolean = (f, s) => f < s,
): Array<T> => {

  let merged: Array<T> = [];
  let indexNew = 0;
  let indexOld = 0;

  // process all items of both arrays
  while ((indexNew < newArray.length) || (indexOld < oldArray.length)) {

    if (indexNew < newArray.length) {

      // check if new item is part of old array
      let idOfNewItem = getId(newArray[indexNew]);
      let toBeMerged = indexOld;
      while ((toBeMerged < oldArray.length)
          && (getId(oldArray[toBeMerged]) !== idOfNewItem)) {
        ++toBeMerged;
      }
      if (toBeMerged < oldArray.length) {
        // if new id found in old list, then push old items between
        while (indexOld < toBeMerged) {
          merged.push(map(undefined, oldArray[indexOld], merged.length));
          ++indexOld;
        }
        merged.push(map(newArray[indexNew], oldArray[indexOld], merged.length));
        ++indexNew;
        ++indexOld;
      } else {
        // if new id not found in old list then check if next new items are found in old list
        let lookAheadIndexNew = indexNew + 1;
        toBeMerged = indexOld;
        while (lookAheadIndexNew < newArray.length) {
          idOfNewItem = getId(newArray[lookAheadIndexNew]);
          while ((toBeMerged < oldArray.length)
              && (getId(oldArray[toBeMerged]) !== idOfNewItem)) {
            ++toBeMerged;
          }
          if (toBeMerged != oldArray.length) {
            lookAheadIndexNew = newArray.length;
          } else {
            toBeMerged = indexOld;
          }
          ++lookAheadIndexNew;
        }
        // add old items
        while (indexOld < toBeMerged) {
          if (isFirstBeforeSecond(oldArray[indexOld], newArray[indexNew])) {
            merged.push(map(undefined, oldArray[indexOld], merged.length));
            ++indexOld;
          } else {
            toBeMerged = indexOld;
          }
        }
        // add new item
        if (isFirstBeforeSecond(oldArray[indexOld], newArray[indexNew])) {
          merged.push(map(undefined, oldArray[indexOld], merged.length));
          ++indexOld;
        } else {
          merged.push(map(newArray[indexNew], undefined, merged.length));
          ++indexNew;
        }
      }

    } else {

      // if now new items left then push the rest of the old array
      merged.push(map(undefined, oldArray[indexOld], merged.length));
      ++indexOld;

    }
  }

  return merged;
}

export { getObjectProperty, mergeTwoSortedArrays, keepOldItemsInArray };
