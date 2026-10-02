# Die Dev Shell

*Teil der Beschreibung in [`../SKILL.md`](../SKILL.md). Bis zum Review deutsch.*

Wer ein Formular für ein Workflow-Modul schreibt, braucht etwas, worin es läuft. Das echte Cockpit
ist dafür schlecht geeignet: Module Federation und der Austausch von Code im laufenden Browser
kommen nicht miteinander aus, also bedeutet jede Änderung einen neuen Bau des Bundles und ein
Neuladen. Dazu müssen ein Cockpit, eine MongoDB und ein BPMS laufen.

Die Dev Shell ist der Ausweg. Sie ist eine kleine Webanwendung, die die Teile des Moduls ganz normal
importiert, sodass der Dev-Server sie beim Tippen austauschen kann. Hinter ihr steht der Dev Shell
Simulator, eine Java-Anwendung, die die Fragen beantwortet, die sonst das Cockpit beantwortet.

Es gibt sie heute dreimal, in `development/dev-shell-react`, `development/dev-shell-vue` und
`development/dev-shell-angular`. Diese drei sind Belege und Beispiele. Der Vertrag ist, was unten
steht, und wo die drei auseinandergehen, steht es dabei.

1. [Was eine Dev Shell leisten muss](#was-eine-dev-shell-leisten-muss)
1. [Der Einstiegspunkt](#der-einstiegspunkt)
1. [Was sie anzeigt und wie man umschaltet](#was-sie-anzeigt-und-wie-man-umschaltet)
1. [Woher die Daten kommen](#woher-die-daten-kommen)
1. [Der Dev Shell Simulator](#der-dev-shell-simulator)
1. [Der Proxy des Dev-Servers](#der-proxy-des-dev-servers)
1. [Wo sie absichtlich anders ist als das Cockpit](#wo-sie-absichtlich-anders-ist-als-das-cockpit)
1. [Wo die drei vorhandenen Shells auseinandergehen](#wo-die-drei-vorhandenen-shells-auseinandergehen)

## Was eine Dev Shell leisten muss

Sie tritt an die Stelle der Cockpit-Seiten, die Teile eines Moduls laden: die Seite einer Aufgabe,
die Seite eines Falls, die Zelle einer Listenzeile und die Stelle, an der ein Cockpit einen eigenen
Teil eines Moduls unterbringt.

Fünf Dinge muss sie dafür können:

1. Die Teile des Moduls direkt importieren, nicht über Module Federation. Nur so kann der Dev-Server
   sie austauschen, während der Entwickler schreibt.
2. Echte Daten vorsetzen. Die Aufgabe und der Fall kommen aus der GUI-API, nicht aus einer Datei mit
   Beispieldaten. Nur so stimmt die Form der Daten mit der des Cockpits überein.
3. Die Daten in `BcUserTask` und `BcWorkflow` verpacken, also die Funktionen ergänzen, die ein Teil
   erwartet. Siehe [types.md](types.md#bcusertask-und-bcworkflow).
4. Zwischen den Teilen umschaltbar sein und zwischen den Aufgaben und Fällen, die es gibt.
5. Den angemeldeten Benutzer wechselbar machen, denn ein Formular zeigt je Benutzer anderes.

Sie braucht keine Listen, keine Live-Aktualisierung und keine Anmeldung. Das sind Aufgaben des
Cockpits und nicht der Werkbank.

## Der Einstiegspunkt

Der Entwickler übergibt seine Teile und eine Adresse, und die Shell baut daraus die Anwendung. Was
übergeben wird, ist das eigentliche Stück Vertrag:

| Was | Warum die Shell es braucht |
|---|---|
| die Adresse der GUI-API | daraus baut sie ihre Clients, im Beispiel `/official-api/v1` |
| `UserTaskForm` | die Seite einer Aufgabe |
| `WorkflowPage` | die Seite eines Falls |
| weitere Teile unter ihrem Namen | für die eigenen Teile eines Moduls, siehe [module-federation.md](module-federation.md#eigene-teile) |
| die Spaltenfunktionen und die Zellen | nur nötig, wenn die Shell eine Liste nachstellt |
| das Workflow-Modul und das Thema | nur nötig, wenn die Shell einen Teil wie das Cockpit umrahmt |

Die React-Shell nimmt alles davon, in dieser Reihenfolge, und setzt die Anwendung in ein Element mit
der angegebenen Kennung:

```ts
bootstrapDevShell(
    elementId, workflowModule, theme, officialGuiApiUrl,
    userTaskForm, userTaskListColumns, userTaskListCell,
    workflowListColumns, workflowListCell, workflowPage,
    additionalComponents?)
```

Die Vue-Shell ist eine Komponente `DevShellApp` mit vier Eigenschaften, die Angular-Shell liefert
eine `ApplicationConfig` über `appConfig(...)` und dazu ein `bootstrapDevShell`. Beide nehmen nur die
Adresse, das Formular, die Fallseite und die weiteren Teile.

Die Form ist also nicht gleich, und das muss sie auch nicht sein. Nimm die Form, die in deinem
Framework natürlich ist. Gleich ist, was übergeben wird.

Ein Hinweis zum Beispiel im Repository: `development/simulator/src/main/webapp-react/test/index.tsx`
ruft `bootstrapDevShell` noch mit der alten Reihenfolge auf, ohne `workflowModule` und ohne `theme`.
Nimm die Form aus `development/dev-shell-react/src/dev-shell-react/index.tsx` und nicht aus dem
Beispiel.

## Was sie anzeigt und wie man umschaltet

Drei Oberflächen genügen:

Eine Startseite mit einem Weg zu jedem Teil und mit der Wahl des Benutzers.

Eine Seite je Teil, mit der Kennung der Aufgabe oder des Falls in der Adresse. Die drei vorhandenen
Shells benutzen dafür `/task/<id>` und `/workflow/<id>`, in der React-Shell übersetzt, also auch
`/aufgabe/<id>` und `/vorgang/<id>`.

Ein Kopfbereich mit einer Auswahlliste der vorhandenen Aufgaben beziehungsweise Fälle, nach Namen
durchsuchbar und blätterbar, dazu ein Schalter für alle, nur offene oder nur abgeschlossene. Die
Auswahlliste holt ihre Einträge in Blöcken von 20 und sortiert nach `createdAt` absteigend.

Die React-Shell hat zusätzlich eine Ansicht, die eine Listenzeile nachstellt, und eine
Platzhalterseite für ein Symbol. Die anderen zwei haben beides nicht.

## Woher die Daten kommen

Aus der GUI-API, über denselben erzeugten Client, den auch ein Cockpit benutzt. Die Shell bekommt
dafür nur die Basisadresse. Es gibt in keiner der drei Shells eine Datei mit Beispieldaten.

Das ist der Grund, warum eine Dev Shell ohne den Simulator nichts zeigt, und es ist auch der Grund,
warum sie überhaupt etwas wert ist: ein Formular, das hier läuft, läuft gegen dieselbe Datenform wie
im Cockpit.

Die Funktionen, die die Shell an `BcUserTask` und `BcWorkflow` hängt, darf sie nachstellen, und die
drei tun das auf drei Weisen. Was du dabei entscheidest:

| Funktion | Was die Shell damit tun kann |
|---|---|
| `open`, `navigateToWorkflow` | zur eigenen Seite dieser Aufgabe oder dieses Falls springen |
| `assign`, `unassign`, `claim`, `unclaim` | wirklich die GUI-API rufen, oder nichts tun und es sagen |
| `getUserTasks` | `POST /workflow/{id}/usertasks` rufen und die Aufgaben wieder verpacken |

Nichts tun ist eine zulässige Entscheidung, solange der Entwickler es merkt. Die Angular-Shell zeigt
dafür einen Hinweis im Browser, die React-Shell tut bei vier dieser Funktionen schlicht nichts, und
die Vue-Shell ruft die echte API. Eine Funktion, die stillschweigend nichts tut, ist die schlechteste
der drei Varianten, denn der Entwickler sucht den Fehler dann in seinem Formular.

`getUserTasks` sollte echt sein. Eine Fallseite ohne ihre Aufgaben ist keine Fallseite.

## Der Dev Shell Simulator

Eine Spring-Boot-Anwendung in `development/dev-shell-simulator`, als lauffähiges Jar gebaut und einem
Release beigelegt. Sie läuft auf Port 8079 und hält alles in einer H2-Datenbank im Speicher. Weder
MongoDB noch BPMS noch Cockpit sind nötig.

Sie hat zwei Seiten:

Nach vorne, zur Dev Shell, spricht sie die offizielle GUI-API unter `/official-api/v1`. Sie
beantwortet davon fünf Aufrufe: eine Aufgabe, die Aufgabenliste, einen Fall, die Fallliste und die
Aufgaben eines Falls. Seitenweise lesen und Sortieren macht sie im Speicher, die Seitengröße ist
standardmäßig 20, und die Standardsortierung ist `createdAt` absteigend.

Nach hinten, zum Workflow-Modul, spricht sie die BPMS-API unter `/bpms/api/v1` und `/bpms/api/v1_1`.
Dort meldet sich ein Workflow-Modul an, und dort melden seine Berichte über Aufgaben und Fälle
herein, genau wie beim Cockpit.

Umgehängt wird das Modul mit einem Schlüssel: `vanillabp.cockpit.rest.base-url` zeigt auf
`http://localhost:8079` statt auf das Cockpit. Die Adresse ist der Rumpf, der Transport hängt
`/bpms/api/v1_1` selbst an, und genau diesen Pfad beantwortet der Simulator. Anmeldedaten braucht
es dazu nicht, denn der Simulator lässt seine Datenpfade offen.
Wo der Schlüssel sonst noch stehen darf, steht im Wiki unter
[Connecting a workflow module](https://github.com/vanillabp/business-cockpit/wiki/Connecting-a-workflow-module).

Dazu kommt eine eigene kleine Schnittstelle für die Shell, unter `/dev-shell/user`:

| Aufruf | Für wen | Was er tut |
|---|---|---|
| `GET /dev-shell/user/all/id` | Shell | die Kennungen aller Benutzer |
| `GET /dev-shell/user/all` | Shell | alle Benutzer mit Details |
| `GET /dev-shell/user/` | Shell | die Kennung des gerade gewählten Benutzers, als Text |
| `POST /dev-shell/user/{userId}` | Shell | den Benutzer wechseln, `---` meldet ab |
| `GET /dev-shell/user/details` | Anwendung | der gerade gewählte Benutzer, aus dem Cookie |
| `GET /dev-shell/user/{userId}` | Anwendung | ein Benutzer nach Kennung |

Die vorgetäuschten Benutzer stehen in `development/dev-shell-simulator/users.yaml`, unter dem
Schlüssel `dev-shell-simulator.users`. Je Benutzer `id`, `email`, `first-name`, `last-name`, `groups`
als kommagetrennte Liste und `attributes` als freie Abbildung. Mitgeliefert sind `john`, `jane` und
`joe` mit den Gruppen `ADMIN` und `RISK_ASSESSMENT`.

Diese Benutzer sind drei Dinge auf einmal: die Einträge der Auswahlliste in der Shell, die
Benutzerverwaltung des Simulators, und die Gruppen werden zu den Berechtigungen im Token. Der
Simulator löst Gruppen dabei absichtlich nicht auf. Eine Gruppenhierarchie ist Sache der
Geschäftsanwendung.

Angemeldet wird nicht. Der Simulator schreibt denselben JWT in dasselbe Cookie, das auch das Cockpit
benutzt, mit demselben Satz Klassen aus `commons`, und der Wechsel des Benutzers ist ein `POST`. Das
Geheimnis zum Signieren steht im Klartext in seiner Konfiguration, denn es soll keine Sicherheit
geben, sondern ein echtes Token in der richtigen Form. Die Datenpfade `/official-api/**` und
`/bpms/api/**` sind für jeden offen.

Der Simulator ist absichtlich eine Anwendung und keine Attrappe aus Testdaten. Was er antwortet, muss
antworten, was das Cockpit antwortet, sonst läuft ein Formular hier und dort nicht.

Was er nicht beantwortet, ist der Rest der GUI-API: Übernehmen, Zuweisen, Als-gelesen-markieren,
Wiedervorlage, die Aktualisierungsform der Listen, die Benutzersuche, die Vorschläge für das
Suchfeld, die Benachrichtigungen und den Ereignisstrom. Eine Shell, die eine dieser Funktionen echt
ruft, ruft ins Leere. Rechne damit, wenn du die Funktionen von `BcUserTask` echt bauen willst.

## Der Proxy des Dev-Servers

Die Shell läuft auf dem Dev-Server des Frameworks, der Simulator auf Port 8079. Damit das Cookie
mitreist und der Browser keine fremde Herkunft sieht, leitet der Dev-Server weiter:

| Pfad | Ziel |
|---|---|
| `/official-api/**` | `http://localhost:8079` |
| `/dev-shell/**` | `http://localhost:8079` |

Dazu der Rückfall auf die Startseite für unbekannte Pfade, denn die Shell ist eine
Single-Page-Anwendung.

Im Repository steht der erste Pfad in der Konfiguration der React- und der Angular-Anwendung. Der
zweite fehlt in beiden, obwohl alle drei Shells ihn aufrufen. Nimm beide.

## Wo sie absichtlich anders ist als das Cockpit

Eine Dev Shell soll nicht das Cockpit sein. Diese Abweichungen sind gewollt, und ein Entwickler muss
sie kennen, damit er seinem Formular nicht etwas nachweist, was die Shell gar nicht prüft:

Die Liste ist gestellt. Die React-Shell zeichnet zwei Zeilen aus derselben einen Aufgabe, eine davon
mit erfundener Fälligkeit und eine als ungelesen markiert. Die Spalten gibt der Entwickler als
kommagetrennte Liste in ein Textfeld ein, und die Shell merkt sie sich in der Sitzung. Ob die
Oberfläche ein Telefon oder ein Rechner ist, wird über Radioknöpfe gewählt und nicht am Fenster
gemessen. Eine Liste mit mehreren Modulen, mit Sortierung und mit Filterung kommt hier nicht vor.

Es gibt keine Live-Aktualisierung. Die React-Shell hängt zwar einen Ereignisstrom ein, aber der
Simulator liefert diesen Pfad nicht, und der Dev-Server leitet ihn auch nicht weiter.

Es gibt keine Anmeldung. Der Benutzer wird gewählt, nicht angemeldet, und die Datenpfade prüfen
nichts.

Die Sichtbarkeitsregeln des Cockpits werden nicht nachgestellt. Was die Shell zeigt, zeigt sie allen.

Die Übersetzung in der Zelle ist die Identität. Die React-Shell gibt der Zelle eine
Übersetzungsfunktion, die den Schlüssel zurückgibt, und nagelt die Sprache auf `de` fest. Ein
Texteintrag, der im Cockpit übersetzt wird, steht hier als Schlüssel da.

## Wo die drei vorhandenen Shells auseinandergehen

Das sind Unterschiede der Beispiele und keine Regeln. Wer eine vierte Shell baut, entscheidet selbst,
welcher Seite er folgt.

| Punkt | React | Vue | Angular |
|---|---|---|---|
| Art des Einstiegs | Funktion, die in ein Element rendert | Komponente mit Eigenschaften | Konfigurationsfunktion plus Bootstrap |
| Workflow-Modul und Thema als Parameter | ja | nein | nein |
| Spalten und Zellen als Parameter | ja | nein | nein |
| Listenansicht | gestellt, zwei Zeilen | keine | keine |
| Symbolansicht | Platzhalter | keine | keine |
| Eigene Teile bekommen `toast` | ja | nein | nein |
| Übersetzung | eigene i18next-Instanz, `de` mit Rückfall `en` | keine | keine |
| Thema | das vom Aufrufer übergebene Grommet-Thema | PrimeVue `Aura`, fest | keines |
| `assign` und `claim` | tun nichts | rufen die echte API | zeigen einen Hinweis |
| Beispiel im Repository | ja, aber veraltet | keines | ja, zwei |
| README | ja | nein | ja, nennt aber das falsche Paket |
| Proxy-Konfiguration im Repository | ja, ohne `/dev-shell` | keine | ja, ohne `/dev-shell` |

Eine Eigenart von Angular ist dabei keine Entscheidung, sondern eine Einschränkung des Werkzeugs:
`npm link` funktioniert für Angular-Bibliotheken nicht, also muss jede Änderung in die lokale
NPM-Registry veröffentlicht und von der benutzenden Anwendung neu geholt werden. Wer für Angular
baut, plant diesen Schritt ein.
