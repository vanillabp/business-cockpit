# Was der Benutzer erwartet

*Teil der Beschreibung in [`../SKILL.md`](../SKILL.md). Bis zum Review deutsch.*

Hier steht, was ein Cockpit-UI tut, nicht wie es aussieht. Das Aussehen ist deine Sache. Was unten
steht, ist das Verhalten, das die Benutzer des mitgelieferten UIs kennen und das ein Workflow-Modul
voraussetzt.

1. [Die Bildschirme](#die-bildschirme)
1. [Wie die Spalten einer Liste entstehen](#wie-die-spalten-einer-liste-entstehen)
1. [Eine Zelle zeichnen](#eine-zelle-zeichnen)
1. [Was eine Liste live tut](#was-eine-liste-live-tut)
1. [Blättern](#blättern)
1. [Sortieren und suchen](#sortieren-und-suchen)
1. [Die Aktionen auf einer Aufgabe](#die-aktionen-auf-einer-aufgabe)
1. [Eine Aufgabe und einen Fall öffnen](#eine-aufgabe-und-einen-fall-öffnen)
1. [Sprache](#sprache)

## Die Bildschirme

Das mitgelieferte UI hat vier Listen. Drei davon sind Aufgabenlisten und unterscheiden sich nur im
Basispfad, den sie rufen, siehe [gui-api.md](gui-api.md#die-endpunkte). Die vierte ist die Liste der
Geschäftsfälle.

| Bildschirm | Pfad im mitgelieferten UI | Was er zeigt |
|---|---|---|
| Aufgabenliste | `/tasks` | alles, was der Benutzer bearbeiten darf |
| meine Aufgaben | `/tasks/mine` | was ihm persönlich zugewiesen ist |
| Aufgaben meiner Rollen | `/tasks/for-my-roles` | was seine Gruppen übernehmen dürfen |
| Fallliste | `/workflows` | die Geschäftsfälle |
| Seite eines Falls | `/workflows/<id>` | die Seite aus dem Workflow-Modul |
| Seite einer Aufgabe | `/task/<id>` | das Formular aus dem Workflow-Modul, in eigenem Fenster |
| Benachrichtigungen | `/notifications` | was der Benutzer wie benachrichtigt bekommen will |

Die Pfade sind übersetzt. Auf Deutsch heißen sie `/aufgaben`, `/vorgaenge`, `/aufgabe/<id>` und
`/benachrichtigungen`. Das ist eine Entscheidung des mitgelieferten UIs und kein Vertrag.

Die Seite einer Aufgabe steht dabei bewusst außerhalb des geschützten Bereichs der Anwendung. Ein
Link aus einer Benachrichtigungsmail zeigt dorthin, und er soll ohne Umweg über die Startseite
funktionieren.

## Wie die Spalten einer Liste entstehen

Eine Liste zeigt Aufgaben mehrerer Workflow-Module gleichzeitig, und jedes Modul sagt, welche
Spalten es will. Der Ablauf:

1. Die Liste holt eine erste Seite von 20 Zeilen, nur um zu erfahren, welche Module und welche
   Definitionen überhaupt vorkommen. Die Zeilen selbst werden danach weggeworfen.
2. Eine Definition ist bei Aufgaben das Paar aus Workflow-Modul und `taskDefinition`, bei Fällen das
   Paar aus Workflow-Modul und `bpmnProcessId`.
3. Für jede Definition wird der Teil `UserTaskList` beziehungsweise `WorkflowList` des Moduls
   geladen und seine Spaltenfunktion mit einer Zeile dieser Definition gerufen.
4. Die Antworten werden in eine Abbildung über `path` gelegt. Zwei Definitionen mit demselben `path`
   bedeuten also: die zuletzt gesehene Spalte gewinnt, mit ihrem ganzen Inhalt. Es wird nichts
   zusammengerechnet.
5. Was danach fehlt, ergänzt das UI selbst.
6. Sortiert wird nach `priority`, die kleinste Zahl zuerst.

Was das mitgelieferte UI in einer Aufgabenliste selbst ergänzt, jeweils nur wenn kein Modul diesen
`path` schon belegt hat:

| `path` | `priority` | `type` | Breite | Was drin steht |
|---|---|---|---|---|
| `id` | -1 | | 2.2rem | das Auswahlkästchen der Zeile |
| `title` | 0 | `i18n` | der Rest | der Titel, als Link, der die Aufgabe öffnet |
| `assignee` | 1 | `person` | 10rem | der Bearbeiter, mit Übernehmen und Zurückgeben beim Überfahren |
| `candidateUsers` | 2 | | 3rem | die Kandidaten, aufklappbar, mit Entziehen je Person |

In einer Fallliste ergänzt es nur `id` und `title`, und `title` öffnet dort die Seite des Falls.

Die Spalte `id` trägt das Auswahlkästchen. Wer sie weglässt, hat keine Mehrfachauswahl mehr. Die
Fallliste des mitgelieferten UIs lässt sie deshalb weg, denn sie hat keine Mehrfachaktionen.

Eine Spalte mit `show: false` wird nicht gezeichnet. Eine Oberfläche, in der der Benutzer sie
einschalten könnte, gibt es nicht.

Wenn ein Modul noch nicht geladen ist, hat die Liste keine Spalten, und dann zeigt sie nichts. Ein
Modul, dessen Teil nicht lädt, hält also die ganze Liste leer. Rechne damit und zeige in diesem Fall
etwas anderes als eine leere Fläche.

## Eine Zelle zeichnen

Für jede Zelle entscheidet das UI zuerst, wer sie zeichnet:

1. Gehört die Spalte dem Cockpit, also `id`, `title`, `assignee` oder `candidateUsers`, zeichnet das
   UI sie selbst.
2. Sonst, wenn das Modul der Zeile unbekannt ist, steht eine Warnung in der Zelle.
3. Sonst, wenn das Laden des Moduls gescheitert ist, steht ein Hinweis in der Zelle.
4. Sonst, wenn das Modul eine Zellenkomponente hat, zeichnet die.
5. Sonst zeichnet die Standardzelle, und eine Zeile im Protokoll des Browsers sagt, dass das Modul
   keine hat.

Die Zellenkomponente des Moduls bekommt immer `defaultCell` mit und gibt alles, was sie nicht selbst
behandeln will, dorthin weiter. Sie ist in einem `Suspense` gezeichnet, darf also nachladen.

Der Wert einer Zelle ist `column.path`, aufgelöst gegen die Zeile, mit Punkten als Trennern. Also
`details.customer.name` greift in die gemeldeten Geschäftsdaten.

Was die Standardzelle je `type` daraus macht:

| `type` | Was gezeichnet wird |
|---|---|
| `i18n` | der Pfad wird um `.<Sprache>` verlängert; fehlt die Sprache, bleibt die Zelle leer |
| `date` | das Datum in der Sprache des Benutzers, rechtsbündig, mit Datum und Zeit im Tooltip |
| `time` | die Uhrzeit, rechtsbündig |
| `date-time` | Datum und Uhrzeit, rechtsbündig |
| `person` | Kurzname, sonst E-Mail, sonst Kennung, mit den Details im Tooltip |
| `value` oder nichts | nach dem Typ des Werts, siehe unten |

Ohne `type` entscheidet der Wert: eine Zahl wird in der Sprache des Benutzers formatiert und
rechtsbündig gesetzt, ein Wahrheitswert wird über die Texte `boolean-true` und `boolean-false`
übersetzt, eine Zeichenkette, die wie ein Datum anfängt, wird als Datum formatiert, und ein Objekt
wird zu einem Hinweis, dass hier eine eigene Zelle hingehört.

Jede Zelle hängt an einem Zustand der Zeile, siehe [types.md](types.md#listitem-und-sein-zustand).
Die Schrift wird fett, solange der Benutzer die Zeile nicht gelesen hat, und sie hat je Zustand eine
andere Farbe.

## Was eine Liste live tut

Dies ist der aufwendigste Teil eines Cockpit-UIs, und er ist der Grund, warum das Cockpit überhaupt
einen Ereignisstrom hat.

So macht es das mitgelieferte UI:

1. Beim Aufbau der Liste wird der Zeitpunkt festgehalten, den der Server zurückgegeben hat. Das ist
   der `initialTimestamp` aus [gui-api.md](gui-api.md#der-zeitstempel-der-eine-liste-stabil-hält).
2. Es hört auf dem Ereignisstrom auf genau einen Namen, `UserTask` beziehungsweise `Workflow`, und
   nimmt aus jedem Ereignis nur die Kennung. Die Art des Ereignisses wird nicht gelesen. Jede Art
   führt zu derselben Handlung.
3. Es fragt mit `PUT` nach so vielen Zeilen, wie es geladen hat, plus einer Reserve von zwei Blöcken,
   und lässt die Kennungen der geänderten Zeilen weg. Die kommen dadurch vollständig zurück.
4. Es setzt je zurückgelieferter Zeile den Zustand:
   * beendet nach dem Startzeitpunkt, also `ENDED`,
   * sonst entstanden nach dem Startzeitpunkt, also `NEW`,
   * sonst geändert nach dem Startzeitpunkt, also `UPDATED`,
   * sonst `INITIAL`.
5. Eine Zeile, die der Server nicht mehr liefert, bleibt stehen und wird `REMOVED_FROM_LIST`.
6. Die Auswahl des Benutzers bleibt dabei erhalten, und die Zeilen werden neu durchnummeriert.
7. Hat sich die Zusammensetzung der Liste geändert, wird die Schaltfläche "Liste neu laden" aktiv
   und hervorgehoben. Vorher ist sie abgeschaltet.

Die letzten zwei Punkte sind die eigentliche Idee: eine Zeile verschwindet dem Benutzer nie unter den
Händen weg. Sie wird umgefärbt und bleibt stehen, bis er selbst neu lädt. Ein Neuladen wirft die
Zeilen und die Spalten weg und nimmt einen neuen Startzeitpunkt.

Neu geladen wird auch beim Wechsel der Sortierung und beim Setzen einer Suche. Dabei fängt die
Einfärbung ebenfalls neu an.

Ein Ereignis der Art `FOLLOWUP` ist der Grund, warum eine Liste nicht nur die Zeilen nachfragen
darf, die sie kennt. Der Server schickt es, wenn eine Wiedervorlage fällig geworden ist, und die
betroffene Aufgabe war bis dahin aus der Liste gefiltert. Sie taucht also durch dieses Ereignis
überhaupt erst auf.

## Blättern

Das mitgelieferte UI blättert nicht, es lädt nach. Blöcke von 30 Zeilen, und der nächste Block
kommt, wenn der Benutzer am Ende der gezeichneten Zeilen ankommt. Die Seitennummer rechnet es aus
der Anzahl der geladenen Zeilen aus.

Die Gesamtzahl steht in der Fußzeile und kommt aus `page.totalElements`.

Beim ersten Laden wird der Ladeanzeiger gesetzt, beim Nachladen nicht.

## Sortieren und suchen

Ein Klick auf einen Spaltenkopf macht diese Spalte zur Sortierspalte. Ein weiterer Klick dreht die
Richtung, und ein dritter geht zur Standardsortierung der Liste zurück. Das Sortierzeichen erscheint
nur bei einer Spalte mit `sortable`.

Was dabei als `sort` an den Server geht, hängt am `type` der Spalte, und das ist eine der
unauffälligsten Stellen des ganzen Vertrags:

| `type` der Spalte | Was als `sort` geschickt wird |
|---|---|
| `i18n` | `<path>.<Sprache>` |
| `person` | `<path>.sort,<path>.id` |
| alles andere | `<path>` |

Der Grund für den zweiten Fall: eine Person liegt im gespeicherten Dokument mit den Feldern `id`,
`fulltext` und `sort`, und `sort` ist der Wert, nach dem sortiert werden soll. Die Anwendung baut ihn
selbst, zum Beispiel aus Nachnamen und Vornamen.

Daraus folgt die allgemeine Regel, die auch fürs Filtern gilt: ein Pfad greift in das Dokument, das
der Server speichert, und nicht in das JSON, das du bekommen hast. Bei `details.*` ist beides gleich.
Bei `assignee` ist es nicht gleich: die API liefert `display`, `displayShort`, `email` und `avatar`,
gespeichert sind `id`, `fulltext` und `sort`.

Die Volltextsuche des mitgelieferten UIs ist ein Feld mit Vorschlägen. Es fragt ab drei Zeichen, mit
300 Millisekunden Verzögerung, zeigt höchstens 20 Vorschläge und bei mehr einen Hinweis, dass es
mehr als zwanzig Treffer gibt. Übernommen wird ein Vorschlag mit einem Klick oder die Eingabe mit der
Eingabetaste. Gesucht wird immer ohne Rücksicht auf Groß- und Kleinschreibung.

Ein Filter je Spalte ist vorbereitet und nicht gebaut. Die Funktionen nehmen einen Spaltenpfad an,
und kein Bedienelement ruft sie damit. Wer einen baut, baut ihn ohne Vorbild, und
[rough-edges.md](rough-edges.md#zwei-filter-auf-einem-pfad-sind-ein-fehler) sagt, worauf er achten
muss.

## Die Aktionen auf einer Aufgabe

Für eine Zeile oder für die Auswahl mehrerer Zeilen:

| Aktion | Was sie bedeutet |
|---|---|
| Übernehmen | der Benutzer wird Bearbeiter |
| Zurückgeben | der Bearbeiter wird entfernt |
| Zuweisen | eine gesuchte Person wird Kandidat |
| Entziehen | eine Person wird aus den Kandidaten entfernt |
| Als gelesen markieren | setzt `read`, der Titel wird nicht mehr fett |
| Als ungelesen markieren | nimmt `read` zurück |

Beim Zuweisen sucht der Benutzer in einem Feld nach Personen, und das UI ruft dafür `POST /user` mit
einer Höchstzahl von Treffern.

Was diese Aktionen ändern, bleibt. Wer eine Aufgabe sieht, legt die Meldung fest, mit der das Cockpit
die Aufgabe anlegt: Bearbeiter, Kandidaten, Kandidatengruppen und ausgeschlossene Kandidaten. Danach
ändert keine Meldung des Workflow-Moduls diese Felder mehr. Nur Übernehmen, Zurückgeben, Zuweisen und
Entziehen im Cockpit ändern Bearbeiter und Kandidaten. Eine Aufgabe verschwindet also nicht aus der
Liste eines Benutzers, weil das Workflow-Modul sie jemand anderem gibt. Ausnahme ist eine Aufgabe,
die niemanden nennt und deshalb allen angezeigt wird: sie übernimmt die Namen aus der ersten
späteren Meldung, die welche hat. Die zugelassenen Benutzer
(`admittedUsers`) sind die Ausnahme: jede Meldung darf sie setzen.

Das mitgelieferte UI geht bei "als gelesen markieren" optimistisch vor: es setzt die Markierung
sofort in der Zeile und ruft danach die API. Bei Übernehmen, Zurückgeben und Zuweisen tut es das
nicht, sondern wartet auf das Ereignis aus dem Strom. Deshalb braucht es den Strom, damit diese
Aktionen überhaupt sichtbar werden. Ein UI ohne Live-Aktualisierung muss die Zeile nach jeder Aktion
selbst nachladen.

Die Wiedervorlage hat einen Endpunkt und kein Bedienelement. Das mitgelieferte UI ruft
`PATCH /usertask/{id}/follow-up-date` nirgends. Im UI hat es diese Aktion also noch nie gegeben; die
Modi der Liste, die sie braucht, gibt es schon.

Nicht vergessen: eine Aufgabe, die schon beendet ist, nimmt diese Aktionen trotzdem an, siehe
[rough-edges.md](rough-edges.md#abgeschlossene-aufgaben-lassen-sich-noch-übernehmen).

## Eine Aufgabe und einen Fall öffnen

Eine Aufgabe wird in einem eigenen Fenster geöffnet, und das UI merkt sich dieses Fenster. Ein
zweiter Klick auf dieselbe Aufgabe holt das offene Fenster nach vorne, statt ein zweites zu öffnen.
Beim Typ `EXTERNAL` wird in diesem Fenster direkt die gemeldete Adresse geöffnet.

Ein Fall wird im selben Fenster geöffnet, mit einer eigenen Adresse, die man weitergeben kann. Beim
Typ `EXTERNAL` wird auch er in einem eigenen Fenster geöffnet.

Von einer Aufgabe zum Fall führt immer die eigene Fallseite des Cockpits und nicht die Adresse der
Aufgabe. Die Aufgabe sagt, wo die Aufgabe gezeigt wird, und das ist nicht, wo der Fall gezeigt wird.

Die Seite einer Aufgabe tut vier Dinge:

1. Sie lädt die Aufgabe und markiert sie dabei als gelesen.
2. Sie lädt den Teil `UserTaskForm` des Moduls.
3. Sie zeigt, solange eines von beidem läuft, einen Ladezustand, und bei einem Fehler einen Hinweis
   mit einer Schaltfläche, die es nochmal versucht. Zwei Fehler sind zu unterscheiden: die Aufgabe
   ließ sich nicht laden, oder der Teil des Moduls ließ sich nicht laden.
4. Sie setzt das `BcUserTask` zusammen und gibt es dem Formular.

Die Seite eines Falls tut dasselbe mit `WorkflowPage` und ergänzt `getUserTasks`.

Was die Funktionen auf diesen Objekten tun müssen, steht in
[types.md](types.md#bcusertask-und-bcworkflow). Baue sie alle. Eine Funktion, die nichts tut, ist
für den Autor eines Formulars nicht von einem Fehler zu unterscheiden.

## Sprache

Das mitgelieferte UI nagelt die Sprache beim Start auf `de` fest, mit `en` als Rückfall, und hat
keine Sprachwahl. Deine Oberfläche entscheidet das selbst.

Was in jedem Fall gilt:

Die gewählte Sprache muss überall hinreichen, wo ein Text je Sprache gezeichnet wird: in den
Spaltenkopf, in die Zelle, in den Titel einer Zeile und in jede Zellenkomponente eines Moduls.

Entscheide einmal, was bei einer fehlenden Sprache passiert, und halte dich daran. Das mitgelieferte
UI tut hier drei verschiedene Dinge: der Spaltenkopf bleibt leer, der Titel einer Zeile nimmt die
erste vorhandene Sprache, und eine Spalte vom Typ `i18n` bleibt leer. Eine davon ist richtig, und
eine Mischung ist es nicht.

Ohne `window.i18n` fehlen die Texte der Pakete `bc-shared` und `bc-ui` ohne eine Fehlermeldung, siehe
[types.md](types.md#was-nur-bequemlichkeit-ist).
