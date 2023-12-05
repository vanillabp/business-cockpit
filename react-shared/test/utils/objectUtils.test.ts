import { keepOldItemsInArray } from "../../src/utils/objectUtils";


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
});
