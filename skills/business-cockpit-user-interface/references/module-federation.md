# Teile eines Workflow-Moduls laden

*Teil der Beschreibung in [`../SKILL.md`](../SKILL.md). Bis zum Review deutsch.*

Das Cockpit zeigt Listen, und was ein Benutzer nach dem Klick auf eine Zeile sieht, kommt aus dem
Workflow-Modul. Das Modul liefert dafür ein Webpack-Bundle, und das Cockpit lädt Teile daraus in
seine eigenen Seiten. Der Mechanismus heißt Module Federation.

Die Seite [User task forms and status sites](https://github.com/vanillabp/business-cockpit/wiki/User-task-forms-and-status-sites)
des Wikis beschreibt diesen Vertrag von der Seite des Workflow-Moduls: was es exportiert und wie es
baut. Hier steht die andere Seite, nämlich was der Gastgeber tut. Die Beispiel-Implementierung war
`ui/bc-ui/src/utils/module-federation.ts` und steht seit Entscheidung 63 nur noch in der Git-Historie.

## Die vier Teile

Das Cockpit fragt ein Modul nach vier Namen, und nach keinem anderen von sich aus:

| Teil | Was das Cockpit daraus liest | Wo es gebraucht wird |
|---|---|---|
| `UserTaskForm` | `UserTaskForm`, `buildTimestamp` | die Seite einer Aufgabe |
| `WorkflowPage` | `WorkflowPage`, `buildTimestamp` | die Seite eines Geschäftsfalls |
| `UserTaskList` | `userTaskListColumns`, `UserTaskListCell`, `buildTimestamp` | die Aufgabenliste |
| `WorkflowList` | `workflowListColumns`, `WorkflowListCell`, `buildTimestamp` | die Fallliste |

Der Name des Teils ist der Name, unter dem das Modul ihn freigibt. Die Namen innerhalb des Teils
sind die Namen seiner Exporte. Ein Modul, das `UserTaskForm` freigibt, seine Komponente aber anders
nennt, wird geladen und zeigt nichts.

Ein Modul muss nicht alle vier haben. Fehlt einer, nimmt das Cockpit, was es selbst weiß.

## Welche Daten den Teil finden

Ein geladener Teil wird nicht nach einem Modul gefragt, sondern nach einer Kombination aus Modul und
Verwendung. Das Cockpit bildet dafür einen Schlüssel aus der Workflow-Modul-Kennung und dem Namen
des Teils und hält ihn in einer Tabelle, damit derselbe Teil nur einmal geladen wird.

Drei Felder jeder Aufgabe und jedes Falls aus der GUI-API sagen, was zu laden ist, und sie heißen in
beiden Objekten gleich:

| Feld | Bedeutung |
|---|---|
| `workflowModuleId` | das Workflow-Modul, aus dem die Zeile kommt |
| `uiUriType` | eine Zeichenkette; `WEBPACK_MF_REACT` für ein Bundle, `EXTERNAL` für eine fremde Seite |
| `uiUri` | der gemeldete Pfad, beim Typ `EXTERNAL` die Adresse der fremden Seite |

Beim Typ `WEBPACK_MF_REACT` steht in `uiUri` ein Pfad und keine Adresse. Das Cockpit liefert das
Modul unter `/wm/<workflowModuleId>/` selbst aus, also hängt dein UI diesen Vorsatz vor den
gemeldeten Pfad, siehe [gui-api.md](gui-api.md#was-das-cockpit-ausliefert). Der Server tut es nicht
für dich: er trägt den Wert und liest ihn nicht.

Beim Typ `EXTERNAL` gibt es nichts zu laden. Die Adresse steht so da, wie das Modul sie gemeldet
hat, und dein UI öffnet sie in einem eigenen Fenster. Ein Vorsatz davor würde den Browser auf eine
Route des Cockpits schicken, die es nicht gibt. Ein solches Modul liefert keine Spalten, keine
Zellen und keine Formulare.

`uiUriType` ist eine Zeichenkette, und das Cockpit prüft sie nicht. Ein dritter Wert ist deshalb
kein Fehler des Servers, sondern ein Modul, das für ein anderes UI gebaut wurde. Melde ihn dem
Benutzer und zeige die Zeile trotzdem.

## Der Ablauf beim Laden

Sieben Schritte, und alle sieben sind Webpack und nicht Cockpit:

1. Aus der Workflow-Modul-Kennung wird der Name des Webpack-Containers. Jeder Bindestrich wird zu
   einem Unterstrich, aus `taxi-ride` also `taxi_ride`.
2. Steht unter diesem Namen schon ein Objekt an `window`, ist das Bundle geladen. Sonst wird
   `uiUri` als `<script>` in den Kopf des Dokuments gehängt, asynchron, mit einer Kennung, an der es
   wiedergefunden wird.
3. Nach dem Laden des Skripts wird der geteilte Bereich vorbereitet. Das ist der Aufruf
   `__webpack_init_sharing__("default")`, eine Funktion, die der Webpack-Build des Gastgebers
   bereitstellt.
4. Der Container wird einmal mit diesem Bereich bekannt gemacht, über `container.init(...)` mit
   `__webpack_share_scopes__.default`. Ein zweites Mal darf das nicht passieren, also merkt sich der
   Gastgeber, dass es schon geschehen ist.
5. `container.get("<Name des Teils>")` liefert eine Factory, und die Factory liefert das Modul.
6. Aus dem Modul werden die Exporte der Tabelle oben gelesen.
7. Scheitert einer der Schritte, bleibt der Teil ungeladen und der Gastgeber merkt sich, dass er es
   nochmal versuchen kann.

Das heißt für ein UI in einem anderen Framework: der Build deines UIs muss ein Webpack-Build sein
oder zumindest dieselben zwei globalen Funktionen und einen geteilten Bereich bereitstellen. Die
Teile der Workflow-Module sind React-Komponenten. Dein UI muss React also mitbringen und teilen,
auch wenn es selbst in Angular oder Vue geschrieben ist, sonst gibt es nichts, worin ein Formular
laufen kann.

## Was geteilt wird

Das mitgelieferte UI teilt sechs Bibliotheken, jede als Singleton und `eager`. Die Versionen stehen
in der `package.json` neben der Webpack-Konfiguration, und das sind die Versionen, gegen die ein
Workflow-Modul heute baut:

| Bibliothek | Version im mitgelieferten UI |
|---|---|
| `react` | 18.2.0 |
| `react-dom` | 18.2.0 |
| `react-router-dom` | 6.3.0 |
| `grommet` | 2.33.2 |
| `i18next` | 22.0.3 |
| `react-i18next` | 12.0.0 |

Ein Modul erklärt `react`, `react-dom` und `react-router-dom` als geteilt und als Singleton. Was es
darüber hinaus braucht, holt es aus dem geteilten Bereich, wenn der Gastgeber es anbietet, und
bündelt es sonst selbst mit ein.

Drei Folgen für dein eigenes UI:

`react` und `react-dom` als Singleton sind Pflicht. Eine zweite Kopie von React im Browser bricht
jedes geladene Formular.

`grommet`, `i18next` und `react-i18next` sind Pflicht, solange du Formulare lädst, die gegen
`@vanillabp/bc-shared` gebaut sind. Die Komponenten dieses Pakets sind auf Grommet gebaut und
registrieren ihre Texte an `i18next`.

Eine andere Version einer dieser Bibliotheken ist keine Kleinigkeit. Sie ist eine Änderung an einem
veröffentlichten Vertrag, und jedes Workflow-Modul, das gegen die alte Version gebaut hat, muss neu
gebaut werden.

## Wann ein Teil fertig geladen ist

`buildTimestamp` ist das Zeichen. Ein Teil, dessen `buildTimestamp` gesetzt ist, gilt als fertig
geladen, und der Gastgeber fragt ihn nie wieder. Ein Teil ohne `buildTimestamp` gilt als noch
unterwegs, auch wenn seine Komponente schon da ist, und das UI zeigt weiter seinen Ladezustand.

`buildVersion` steht daneben und wird vom Cockpit nicht gelesen. Es ist für die eigenen Anzeigen
eines Moduls gedacht.

Das ist nicht derselbe Zeitstempel wie der aus `GET /gui/api/v1/app/info`. Jener beschreibt den
Server, dieser den geladenen Teil eines Moduls.

Vier Zustände muss dein UI auseinanderhalten, denn sie sehen für den Benutzer verschieden aus:

| Zustand | Woran du ihn erkennst | Was der Benutzer sieht |
|---|---|---|
| lädt noch | kein `buildTimestamp`, keine Wiederholung möglich | ein Ladezeichen |
| geladen | `buildTimestamp` gesetzt | der Teil des Moduls |
| gescheitert | eine Wiederholung ist möglich | ein Hinweis und eine Schaltfläche, die nochmal lädt |
| nicht föderiert | `uiUriType` war `EXTERNAL` | was das Cockpit selbst weiß |

Eine gescheiterte Ladung bleibt gescheitert, bis der Benutzer sie wiederholt. Der Gastgeber nimmt
dabei das alte `<script>` wieder aus dem Dokument, sonst lädt der Browser dieselbe Adresse nie
wieder.

## Was ein Teil bekommt

Die Typen stehen in [types.md](types.md). Kurz:

`UserTaskForm` bekommt eine Eigenschaft `userTask`, ein `BcUserTask`. Das ist die Aufgabe aus der
GUI-API, erweitert um die Funktionen, die dein UI für diese Aufgabe mitgibt.

`WorkflowPage` bekommt eine Eigenschaft `workflow`, ein `BcWorkflow`, ebenso erweitert.

`userTaskListColumns` und `workflowListColumns` sind Funktionen. Sie bekommen eine Aufgabe oder
einen Fall und liefern die Spalten dafür, oder `undefined`, wenn das Modul für diese Zeile keine
Spalten hat.

`UserTaskListCell` und `WorkflowListCell` sind Komponenten für eine einzelne Zelle. Sie bekommen die
Zeile, die Spalte und die Standardzelle des Cockpits.

## Eigene Teile

Ein Modul darf mehr als die vier Teile freigeben, und ein Cockpit darf nach einem davon fragen. Die
mitgelieferte Oberfläche hat dafür die Komponente `CustomWorkflowModuleComponent` in
`@vanillabp/bc-ui`, die einen Teil nach seinem Namen lädt. Sie bekommt das Workflow-Modul und eine
Funktion, mit der der Teil eine Meldung anzeigen kann.

So bringt ein Workflow-Modul etwas ein, das zu keinem einzelnen Fall gehört, etwa ein Formular, das
einen neuen Fall startet. Welche Namen es dafür gibt, ist zwischen dem Modul und dem Cockpit
abgesprochen und steht in keinem Vertrag.
