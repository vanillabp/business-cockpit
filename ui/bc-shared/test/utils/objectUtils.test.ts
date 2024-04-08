import { keepOldItemsInArray } from "../../src/utils/objectUtils";

/*
enum ListItemStatus {
    INITIAL,
    REMOVED_FROM_LIST
}

interface ListItem<T> {
    id: number | undefined;
    number: number;
    status: ListItemStatus;
    data: T;
}

const sample1_old = JSON.parse('[{"id":"796157de-ec08-11ee-86b5-32e212678d62","data":{"id":"796157de-ec08-11ee-86b5-32e212678d62","version":2,"createdAt":"2024-03-27T07:06:03.619Z","updatedAt":"2024-04-05T05:38:39.766Z","workflowModuleId":"din-en","bpmnProcessId":"DinEn-Draft-PreparationTranslation","bpmnProcessVersion":"1","workflowTitle":{"de":"DIN EN: Draft: Preparation Translation"},"workflowId":"5157ae27-e527-11ee-9770-ea7d36e77ad8","businessId":"-1710766752541","title":{"de":"Entscheidung deutsche Sprachfassung"},"bpmnTaskId":"GermanVersion_51336","taskDefinition":"dinEn-draft-germanVersion","taskDefinitionTitle":{"de":"Entscheidung deutsche Sprachfassung"},"uiUri":"/wm/din-en/remoteEntry.js","uiUriType":"WEBPACK_MF_REACT","workflowModuleUri":"/wm/din-en","candidateUsers":[],"candidateGroups":[],"dueDate":"2024-04-03T07:06:03.608Z","details":{"workflow":{},"project":{"projectPk":-1710766752541},"isStandardDueDate":true},"detailsFulltextSearch":"Entscheidung deutsche Sprachfassung","read":"2024-04-05T05:38:39.766Z"},"number":1,"selected":false,"status":0,"read":"2024-04-05T05:38:39.766Z"},{"id":"ab262539-eb87-11ee-9cc5-32e212678d62","data":{"id":"ab262539-eb87-11ee-9cc5-32e212678d62","version":3,"createdAt":"2024-03-26T15:44:01.997Z","updatedAt":"2024-04-05T09:38:43.511Z","workflowModuleId":"din-en-iec-type-a","bpmnProcessId":"DinEnIecTypeA-StandardizationInitiation-ProposalHandling","bpmnProcessVersion":"2","workflowTitle":{"de":"DIN IEC"},"workflowId":"ab08d921-eb87-11ee-9cc5-32e212678d62","businessId":"7043410","title":{"de":"NP Prüfung und Verteilung"},"bpmnTaskId":"NpContentAndDistributionCheck_42961","taskDefinition":"npContentAndDistributionCheck","taskDefinitionTitle":{"de":"Prüfung Verteilung Dokument / inhaltliche Prüfung"},"uiUri":"/wm/din-en-iec-type-a/remoteEntry.js","uiUriType":"WEBPACK_MF_REACT","workflowModuleUri":"/wm/din-en-iec-type-a","assignee":"test@dke.com","candidateUsers":[],"candidateGroups":["MEMBER_TECHNOLOGY_G_REFSEK_2001343","MEMBER_TECHNOLOGY_G_REF_2001343"],"dueDate":"2016-06-14T00:00:00.000Z","details":{"workflow":{"phase":"NP","imageCount":"0","batch":false},"document":{"imageCount":0,"provisionDate":"2016-06-14T15:33:16.873Z","numberOfPages":5,"documentPk":7078874,"documentName":"8/1427A/NP:2016-06","craftsmanSubscription":false},"prioTask":false,"project":{"referent":{"forename":"Sebastian","dkePersonPk":32909,"surname":"NN 32909"},"responsibleCommittee":"DKE/K 261","assistant":{"forename":"Margot","dkePersonPk":13614,"surname":"NN 13614"},"projectName":"IEC TS 62898-3-1:2020 ED1","department":"Energy","projectPk":7043410},"isStandardDueDate":true},"detailsFulltextSearch":"Prüfung Verteilung Dokument / inhaltliche Prüfung","read":"2024-03-26T15:45:58.814Z"},"number":2,"selected":false,"status":0,"read":"2024-03-26T15:45:58.814Z"},{"id":"341bf549-ec20-11ee-86b5-32e212678d62","data":{"id":"341bf549-ec20-11ee-86b5-32e212678d62","version":37,"createdAt":"2024-03-27T09:55:55.258Z","updatedAt":"2024-04-08T09:28:48.533Z","workflowModuleId":"din-en-iec-type-a","bpmnProcessId":"DinEnIecTypeA-StandardizationInitiation-ProposalHandling","bpmnProcessVersion":"1","workflowTitle":{"de":"DIN IEC"},"workflowId":"d1439385-e13f-11ee-aa78-ea7d36e77ad8","businessId":"7042536","title":{"de":"NP Prüfung und Verteilung"},"bpmnTaskId":"NpContentAndDistributionCheck_42961","taskDefinition":"npContentAndDistributionCheck","taskDefinitionTitle":{"de":"Prüfung Verteilung Dokument / inhaltliche Prüfung"},"uiUri":"/wm/din-en-iec-type-a/remoteEntry.js","uiUriType":"WEBPACK_MF_REACT","workflowModuleUri":"/wm/din-en-iec-type-a","assignee":"test@dke.com","candidateUsers":[],"candidateGroups":["MEMBER_TECHNOLOGY_G_REFSEK_2000102","MEMBER_TECHNOLOGY_G_REF_2000102"],"dueDate":"2016-08-02T00:00:00.000Z","details":{"workflow":{"phase":"NP","imageCount":"0","batch":false},"document":{"imageCount":0,"provisionDate":"2016-08-08T15:44:21.313Z","numberOfPages":0,"documentPk":7080787,"documentName":"9/2199/NP:2016-07","craftsmanSubscription":false},"prioTask":false,"project":{"referent":{"forename":"Deniz","dkePersonPk":2045463,"surname":"NN 2045463"},"responsibleCommittee":"DKE/UK 351.1","assistant":{"forename":"Jessica Naomi","dkePersonPk":31809,"surname":"NN 31809"},"projectName":"IEC 62973-2:2020 ED1","projectPk":7042536},"isStandardDueDate":true},"detailsFulltextSearch":"Prüfung Verteilung Dokument / inhaltliche Prüfung","read":"2024-04-08T09:28:48.533Z"},"number":3,"selected":false,"status":0},{"id":"e20240ca-f347-11ee-9308-52a7c324827b","data":{"id":"e20240ca-f347-11ee-9308-52a7c324827b","version":3,"createdAt":"2024-04-05T12:27:35.582Z","updatedAt":"2024-04-08T06:33:11.041Z","workflowModuleId":"din-en-iec-type-a","bpmnProcessId":"DinEnIecTypeA-StandardizationInitiation-ProposalHandling","bpmnProcessVersion":"1","workflowTitle":{"de":"DIN IEC"},"workflowId":"4e34d8e2-f291-11ee-b3a7-52a7c324827b","businessId":"7047235","title":{"de":"NP Prüfung und Verteilung"},"bpmnTaskId":"NpContentAndDistributionCheck_42961","taskDefinition":"npContentAndDistributionCheck","taskDefinitionTitle":{"de":"Prüfung Verteilung Dokument / inhaltliche Prüfung"},"uiUri":"/wm/din-en-iec-type-a/remoteEntry.js","uiUriType":"WEBPACK_MF_REACT","workflowModuleUri":"/wm/din-en-iec-type-a","candidateUsers":[],"candidateGroups":["MEMBER_TECHNOLOGY_G_REFSEK_2001443","MEMBER_TECHNOLOGY_G_REF_2001443"],"dueDate":"2017-01-31T00:00:00.000Z","details":{"workflow":{"phase":"NP","imageCount":"0","batch":false},"document":{"imageCount":0,"provisionDate":"2017-01-31T17:48:18.547Z","numberOfPages":20,"documentPk":7088672,"documentName":"113/352/NP:2017-01","craftsmanSubscription":false},"prioTask":false,"project":{"referent":{"forename":"Elena","dkePersonPk":2038375,"surname":"NN 2038375"},"responsibleCommittee":"DKE/K 141","assistant":{"forename":"Elena","dkePersonPk":2038375,"surname":"NN 2038375"},"projectName":"IEC TS 62607-5-3:2020 ED1","department":"Components & Technologies","projectPk":7047235},"isStandardDueDate":true},"detailsFulltextSearch":"Prüfung Verteilung Dokument / inhaltliche Prüfung"},"number":4,"selected":false,"status":0},{"id":"085d04ba-eb87-11ee-9cc5-32e212678d62","data":{"id":"085d04ba-eb87-11ee-9cc5-32e212678d62","version":3,"createdAt":"2024-03-26T15:39:28.913Z","updatedAt":"2024-04-08T07:05:34.013Z","workflowModuleId":"din-en-iec-type-a","bpmnProcessId":"DinEnIecTypeA-StandardizationInitiation-ProposalHandling","bpmnProcessVersion":"2","workflowTitle":{"de":"DIN IEC"},"workflowId":"5e37876f-eb5c-11ee-9487-32e212678d62","businessId":"1711449242339","title":{"de":"NP Prüfung und Verteilung"},"bpmnTaskId":"NpContentAndDistributionCheck_42961","taskDefinition":"npContentAndDistributionCheck","taskDefinitionTitle":{"de":"Prüfung Verteilung Dokument / inhaltliche Prüfung"},"uiUri":"/wm/din-en-iec-type-a/remoteEntry.js","uiUriType":"WEBPACK_MF_REACT","workflowModuleUri":"/wm/din-en-iec-type-a","candidateUsers":[],"candidateGroups":[],"dueDate":"2024-04-02T15:39:28.858Z","details":{"workflow":{"phase":"NP","imageCount":"0","batch":false},"document":{"imageCount":0},"prioTask":false,"project":{},"isStandardDueDate":true},"detailsFulltextSearch":"Prüfung Verteilung Dokument / inhaltliche Prüfung"},"number":5,"selected":false,"status":0}]') as Array<ListItem<any>>;
const sample1_new = JSON.parse('[{"id":"ab262539-eb87-11ee-9cc5-32e212678d62","data":{"id":"ab262539-eb87-11ee-9cc5-32e212678d62","version":0,"createdAt":null,"updatedAt":null,"title":{}},"number":1,"selected":false},{"id":"341bf549-ec20-11ee-86b5-32e212678d62","data":{"id":"341bf549-ec20-11ee-86b5-32e212678d62","version":38,"createdAt":"2024-03-27T09:55:55.258Z","updatedAt":"2024-04-08T09:29:43.156Z","workflowModuleId":"din-en-iec-type-a","bpmnProcessId":"DinEnIecTypeA-StandardizationInitiation-ProposalHandling","bpmnProcessVersion":"1","workflowTitle":{"de":"DIN IEC"},"workflowId":"d1439385-e13f-11ee-aa78-ea7d36e77ad8","businessId":"7042536","title":{"de":"NP Prüfung und Verteilung"},"bpmnTaskId":"NpContentAndDistributionCheck_42961","taskDefinition":"npContentAndDistributionCheck","taskDefinitionTitle":{"de":"Prüfung Verteilung Dokument / inhaltliche Prüfung"},"uiUri":"/wm/din-en-iec-type-a/remoteEntry.js","uiUriType":"WEBPACK_MF_REACT","workflowModuleUri":"/wm/din-en-iec-type-a","assignee":"test@dke.com","candidateUsers":[],"candidateGroups":["MEMBER_TECHNOLOGY_G_REFSEK_2000102","MEMBER_TECHNOLOGY_G_REF_2000102"],"dueDate":"2016-08-02T00:00:00.000Z","details":{"workflow":{"phase":"NP","imageCount":"0","batch":false},"document":{"imageCount":0,"provisionDate":"2016-08-08T15:44:21.313Z","numberOfPages":0,"documentPk":7080787,"documentName":"9/2199/NP:2016-07","craftsmanSubscription":false},"prioTask":false,"project":{"referent":{"forename":"Deniz","dkePersonPk":2045463,"surname":"NN 2045463"},"responsibleCommittee":"DKE/UK 351.1","assistant":{"forename":"Jessica Naomi","dkePersonPk":31809,"surname":"NN 31809"},"projectName":"IEC 62973-2:2020 ED1","projectPk":7042536},"isStandardDueDate":true},"detailsFulltextSearch":"Prüfung Verteilung Dokument / inhaltliche Prüfung"},"number":2,"selected":false,"status":2},{"id":"e20240ca-f347-11ee-9308-52a7c324827b","data":{"id":"e20240ca-f347-11ee-9308-52a7c324827b","version":0,"createdAt":null,"updatedAt":null,"title":{}},"number":3,"selected":false},{"id":"085d04ba-eb87-11ee-9cc5-32e212678d62","data":{"id":"085d04ba-eb87-11ee-9cc5-32e212678d62","version":0,"createdAt":null,"updatedAt":null,"title":{}},"number":4,"selected":false},{"id":"796157de-ec08-11ee-86b5-32e212678d62","data":{"id":"796157de-ec08-11ee-86b5-32e212678d62","version":0,"createdAt":null,"updatedAt":null,"title":{}},"number":5,"selected":false}]') as Array<ListItem<any>>;
*/

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
    /*
    test('test8', () => {
        let anyUpdate = false;
        expect(keepOldItemsInArray(sample1_new, sample1_old,
            e => e.id,
            (newItem, oldItem, index) => {
                const itemInUpdateResponse = newItem?.status !== undefined;
                const result = itemInUpdateResponse ? newItem! : oldItem!;
                if (newItem === undefined) {
                    if (result !== undefined) {
                        result.status = ListItemStatus.REMOVED_FROM_LIST;
                        result.number = index + 1;
                    }
                    anyUpdate = true;
                } else if (itemInUpdateResponse) {
                    result.number = index + 1;
                    anyUpdate = true;
                }
                return result;
            },
            (first, second) => {
                if (first?.id === undefined) {
                    return second?.id === undefined;
                }
                if (second?.id === undefined) {
                    return true;
                }
                return first.number < second.number;
            })).toStrictEqual(sample1_old);
    });
     */
});
