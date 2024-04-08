import { keepOldItemsInArray } from "../../src/utils/objectUtils";

interface Entity {
    id: number | undefined;
    number: number;
}

describe('testing keepOldItemsInArray', () => {
    test('test1', () => {
        const a = [] as number[];
        const b = [] as number[];
        expect(keepOldItemsInArray(a, b)).toStrictEqual([]);
    });
    test('test2', () => {
        const a = [1];
        const b = [] as number[];
        expect(keepOldItemsInArray(a, b)).toStrictEqual([1]);
    });
    test('test3', () => {
        const a = [] as number[];
        const b = [2];
        expect(keepOldItemsInArray(a, b)).toStrictEqual([2]);
    });
    test('test4', () => {
        const a = [1, 2, 10, 11, 12];
        const b = [1, 3, 4, 9, 11, 14];
        expect(keepOldItemsInArray(a, b)).toStrictEqual([1, 2, 3, 4, 9, 10, 11, 12, 14]);
    });
    test('test5', () => {
        const a = [1, 2, 3, 4];
        const b = [5, 6, 7, 8];
        expect(keepOldItemsInArray(a, b)).toStrictEqual([1, 2, 3, 4, 5, 6, 7, 8]);
    });
    test('test6', () => {
        const b = [1, 2, 3, 4];
        const a = [5, 6, 7, 8];
        expect(keepOldItemsInArray(a, b)).toStrictEqual([1, 2, 3, 4, 5, 6, 7, 8]);
    });
    test('test7', () => {
        const b = [2, 3, 4];
        const a = [1, 5, 6, 7, 8];
        expect(keepOldItemsInArray(a, b)).toStrictEqual([1, 2, 3, 4, 5, 6, 7, 8]);
    });
    test('test6', () => {
        const a = [{ id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }] as Entity[];
        const b = [{ id: undefined }, { id: undefined }, { id: 3 }, { id: undefined }] as Entity[];
        a.forEach((x, i) => x.number = i);
        b.forEach((x, i) => x.number = i);
        expect(keepOldItemsInArray(b, a,
                e => e.id,
                (i, oi, idx) => (i || oi)!,
                (first, second) => {
            if (first?.id === undefined) {
                return second?.id === undefined;
            }
            if (second?.id === undefined) {
                return true;
            }
            return first.number < second.number;
        })).toStrictEqual(a);
    });
    test('test7', () => {
        const a = [{ id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }] as Entity[];
        const b = [{ id: undefined }, { id: undefined }, { id: undefined }] as Entity[];
        a.forEach((x, i) => x.number = i);
        b.forEach((x, i) => x.number = i);
        expect(keepOldItemsInArray(b, a,
            e => e.id,
            (i, oi, idx) => (i || oi)!,
            (first, second) => {
                if (first?.id === undefined) {
                    return second?.id === undefined;
                }
                if (second?.id === undefined) {
                    return true;
                }
                return first.number < second.number;
            })).toStrictEqual(a);
    });
});
