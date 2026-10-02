# Die Typen und was davon Vertrag ist

*Teil der Beschreibung in [`../SKILL.md`](../SKILL.md). Bis zum Review deutsch.*

Vier NPM-Pakete liegen zwischen dem Cockpit und einem Workflow-Modul. Wer ein UI in TypeScript und
React baut, nimmt sie wie sie sind. Wer ein UI in einem anderen Framework baut, braucht nur einen
Teil davon, denn der Rest ist React.

| Paket | Was drin ist |
|---|---|
| `@vanillabp/bc-official-gui-client` | der aus der OpenAPI-Beschreibung erzeugte Client der GUI-API |
| `@vanillabp/bc-types` | die TypeScript-Typen, die Cockpit und Workflow-Modul gemeinsam haben |
| `@vanillabp/bc-shared` | die Formen der vier Teile, dazu Komponenten und Hilfsmittel für React |
| `@vanillabp/bc-ui` | die Listen und Seiten des mitgelieferten UIs |

`@vanillabp/bc-ui` ist das mitgelieferte UI selbst. Es ist das Beispiel und kein Vertrag. Ein UI in
einem anderen Framework kann es nicht benutzen, denn es sind React-Komponenten.

## Was Vertrag ist

Vertrag ist, was über eine Grenze geht: über die HTTP-Grenze zur GUI-API oder über die Grenze zu
einem geladenen Teil eines Workflow-Moduls. Alles andere ist Bequemlichkeit.

### Über die HTTP-Grenze

Die Wahrheit über diese Typen steht in `apis/official-gui-api/openapi/v1.yaml` und nicht in einem
TypeScript-Paket. Ein UI, das nicht TypeScript spricht, erzeugt seinen eigenen Client daraus.
`UserTask`, `Workflow`, `Page`, `SearchQuery`, `UserTaskRetrieveMode`, `WorkflowRetrieveMode`,
`KwicResult`, `Person`, `Group`, `WorkflowModule` und `UiUriType` kommen alle von dort.

Eine Falle liegt hier: `@vanillabp/bc-types` hat ein eigenes `Person`, das dem `Person` der GUI-API
nachgebaut ist. Welches von beiden du vor dir hast, siehst du am Import und nicht an den Feldern.

### Über die Grenze zum Workflow-Modul

Diese Typen sind der Vertrag, der nirgends in einer OpenAPI-Beschreibung steht. Ein UI in einem
anderen Framework muss sie trotzdem einhalten, denn das Modul ist gegen sie gebaut.

| Typ | Paket | Richtung | Was er bedeutet |
|---|---|---|---|
| `Column` | `bc-types` | Modul an UI | eine Spalte einer Liste |
| `ColumnsOfUserTaskFunction`, `ColumnsOfWorkflowFunction` | `bc-types` | Modul an UI | die Form der beiden Spaltenfunktionen |
| `ListItem<T>`, `ListItemStatus` | `bc-types` | UI an Modul | eine Zeile und ihr Zustand |
| `Translatable` | `bc-types` | beide | ein Text je Sprache |
| `BcUserTask` | `bc-types` | UI an Modul | die Aufgabe plus die Funktionen darauf |
| `BcWorkflow` | `bc-types` | UI an Modul | der Fall plus die Funktionen darauf |
| `BcUserTasksProvider` | `bc-types` | UI an Modul | die Funktion, mit der eine Fallseite ihre Aufgaben holt |
| `UserTaskForm`, `WorkflowPage` | `bc-shared` | Modul an UI | die Form der beiden Seitenkomponenten |
| `UserTaskListCell`, `WorkflowListCell` | `bc-shared` | Modul an UI | die Form der beiden Zellenkomponenten |
| `DefaultListCellProps`, `DefaultListCellAwareProps` | `bc-shared` | UI an Modul | was eine Zelle bekommt |
| `WorkflowModuleComponent` | `bc-shared` | Modul an UI | die Form eines eigenen Teils |

`ColumnsOfUserTaskFunction` und `ColumnsOfWorkflowFunction` stehen zweimal da, in `bc-types` und
nochmal in `bc-shared`. Die beiden Erklärungen sind heute gleich, aber es sind zwei. Lies die aus
`bc-types`, denn `bc-shared` baut darauf auf.

## Column

```typescript
interface Column {
  title: Translatable;
  path: string;
  type?: 'value' | 'i18n' | 'person' | 'date' | 'date-time' | 'time';
  priority: number;
  width: string;
  show: boolean;
  sortable: boolean;
  filterable: boolean;
  resizeable: boolean;
}
```

`path` ist der Schlüssel. Er ist zugleich

* der Pfad, an dem der Wert in der Zeile steht, aufgelöst mit Punkten als Trennern,
* der Pfad, den dein UI als `sort` an den Server schickt,
* der Pfad, den dein UI als `path` einer `SearchQuery` schickt,
* und der Schlüssel, unter dem die Spalten verschiedener Workflow-Module zusammengelegt werden.

Deshalb muss er auf das Dokument passen, das der Server speichert. `details.customer.name` trifft
die Geschäftsdaten, die das Workflow-Modul gemeldet hat. Siehe
[gui-api.md](gui-api.md#sortieren) für die Folgen auf dem Server.

`title` ist ein Text je Sprache. Fehlt die aktuelle Sprache, steht kein Titel da, und dein UI
entscheidet, was es dann zeigt.

`type` entscheidet die Darstellung. Was das mitgelieferte UI daraus macht, steht in
[behaviour.md](behaviour.md#eine-zelle-zeichnen).

`priority` ordnet die Spalten, die kleinste Zahl zuerst. `width` ist eine CSS-Größe, und die leere
Zeichenkette heißt "nimm den Rest". `show` darf eine Spalte verstecken. `sortable` und `resizeable`
sagen, was der Benutzer mit der Spalte darf.

`filterable` ist überall gesetzt und wird von keinem Code gelesen. Es ist eine Absicht und keine
Zusicherung.

## ListItem und sein Zustand

```typescript
interface ListItem<T> {
  id: string;
  number: number;
  data: T;
  status: ListItemStatus;
  selected: boolean;
  read?: Date;
}

enum ListItemStatus { INITIAL, NEW, UPDATED, ENDED, REMOVED_FROM_LIST }
```

`data` ist die Aufgabe oder der Fall, so wie der Server sie geliefert hat. `number` ist die laufende
Nummer in der Liste und nichts, was vom Server kommt. `status` ist das Ergebnis des Vergleichs mit
dem Zeitpunkt, an dem der Benutzer angefangen hat zu schauen, siehe
[behaviour.md](behaviour.md#was-eine-liste-live-tut). `read` ist gesetzt, wenn der Benutzer die
Zeile gelesen hat.

Der Zustand ist für ein Modul sichtbar, weil seine Zelle ihn bekommt und die Farbe davon abhängt.
Wer eigene Farben nimmt, muss die fünf Zustände trotzdem unterscheidbar machen, sonst sieht der
Benutzer nicht, was neu ist.

## BcUserTask und BcWorkflow

Das sind die Objekte, die dein UI einem geladenen Teil vorsetzt. Sie sind die Antwort des Servers
plus Funktionen, die dein UI beisteuert:

```typescript
interface BcUserTask extends UserTask {
  open: () => void;
  navigateToWorkflow: () => void;
  assign: (userId: string) => void;
  unassign: (userId: string) => void;
  claim: () => void;
  unclaim: () => void;
}

interface BcWorkflow extends Workflow {
  navigateToWorkflow: () => void;
  getUserTasks: (activeOnly: boolean, limitListAccordingToCurrentUsersPermissions: boolean)
      => Promise<Array<BcUserTask>>;
}
```

Jede dieser Funktionen ist ein Versprechen deines UIs. Ein Formular darf `claim` aufrufen und
erwarten, dass die Aufgabe danach dem Benutzer gehört und die Liste es weiß. `getUserTasks` ruft
`POST /workflow/{workflowId}/usertasks` auf und gibt die Aufgaben als `BcUserTask` zurück, also
wieder mit diesen Funktionen.

`BcWorkflowModule` in `bc-types` ist nur die Teilmenge von Feldern, die zum Laden eines Teils
reicht: `workflowModuleId`, `uiUri` und `uiUriType`. Aufgabe und Fall erfüllen sie beide.

## Was eine Zelle bekommt

```typescript
interface DefaultListCellProps<D> {
  t: TranslationFunction;
  item: ListItem<D>;
  column: Column;
  showUnreadAsBold?: boolean;
  currentLanguage: string;
  currentUser?: Person;
  nameOfList?: string;
  selectItem: (select: boolean) => void;
  isPhone: boolean;
  isTablet: boolean;
}
```

Die Zelle eines Moduls bekommt dasselbe und zusätzlich `defaultCell`, die Standardzelle des
Cockpits. Sie behandelt die Spalten, die sie kennt, und gibt alles andere an `defaultCell` weiter.
Ein UI, das kein `defaultCell` mitgibt, bricht jede Zelle, die diesen Weg nimmt.

## Was nur Bequemlichkeit ist

Aus `@vanillabp/bc-shared`, alles React und alles auf Grommet gebaut: `Badge`, `CircleButton`,
`Link`, `ModalDialog`, `LoadingIndicator`, `Toast`, `CopyClipboard`, `UserDetailsBox`,
`UserTaskAppLayout`, `WarningListCell`, `SseProvider` und das Thema `theme`. Dazu die Hilfsmittel
`buildFetchApi`, `debounce`, `clickOutside`, `getObjectProperty`, die Zeitformatierer und der
`now`-Hook.

Nichts davon muss dein UI benutzen. Zwei Dinge daraus sind aber trotzdem Pflicht, wenn du Teile
eines Workflow-Moduls lädst, weil das Modul sie benutzt:

`window.i18n` muss eine `i18next`-Instanz sein, bevor ein Teil geladen wird. Die Komponenten von
`bc-shared` und `bc-ui` melden ihre Texte dort an. Ist es nicht gesetzt, melden sie nichts an, und
die Texte fehlen ohne Fehlermeldung.

Ein Grommet-Thema muss im Kontext stehen. Die Komponenten des Moduls zeichnen mit Grommet und
greifen auf Farbnamen zu, unter anderem `list-default`, `list-new`, `list-updated`, `list-ended`,
`list-removed_from_list` und die `list-text-`-Varianten davon. Ein Thema ohne diese Namen zeichnet
die Zeile ohne Farbe. Welche Namen das mitgelieferte Thema setzt, steht im Wiki unter
[Customizing the user interface](https://github.com/vanillabp/business-cockpit/wiki/Customizing-the-user-interface).
