---
name: business-cockpit-user-interface
description: Build a user interface for the VanillaBP Business Cockpit in any web framework, and the dev shell that goes with it. Describes the GUI API with its paging, sorting, filtering over reported business data and server-sent events, the module federation contract with the four parts UserTaskForm, WorkflowPage, UserTaskList and WorkflowList, the types of @vanillabp/bc-types and @vanillabp/bc-shared, the behaviour a user expects, and what a cockpit user interface never does. Use whenever somebody wants their own Business Cockpit front end, whenever the delivered React application is to be replaced or ported to Angular, Vue or anything else, and whenever a user task form or a workflow page has to be loaded into a cockpit.
license: Apache-2.0
metadata:
  author: vanillabp
  homepage: https://www.vanillabp.io
---

# Ein Business-Cockpit-UI selbst bauen

> Dieser Text ist bis zum Review auf Deutsch und wird danach ins Englische übersetzt. Alles andere
> im Repository bleibt englisch. Wer hier etwas ergänzt, schreibt es bis dahin ebenfalls deutsch.

Das Business Cockpit liefert eine Oberfläche in React mit. Sie ist ein Beispiel und kein Produkt. In
der Praxis bringt jede Organisation ihr eigenes Framework mit, ihre eigene Version davon, ihre
eigene Komponentenbibliothek und ihr eigenes Design. Die mitgelieferte Anwendung trifft das nie
ganz, also wird sie kopiert und umgebaut, und die Kopie läuft dann von dem weg, was das Cockpit
erwartet.

Dieser Text ist der Ausweg. Er beschreibt, was eine Cockpit-Oberfläche leisten muss, genau genug, um
eine zu bauen, ohne den Quelltext der React-Anwendung zu lesen.

Er liegt im Repository `business-cockpit`, weil er dessen eigene Schnittstellen beschreibt. Läge er
woanders, würde er getrennt vom Code veralten. Wo er und der Code sich widersprechen, hat der Code
recht, und der Text ist der Fehler.

## Was du bauen wirst

Zwei Dinge, und beide stehen hier:

Ein Cockpit-UI. Es zeigt die Listen, lädt die Formulare der Workflow-Module in seine Seiten und
hält sich an den Ereignisstrom. Es ersetzt die mitgelieferte React-Anwendung.

Eine Dev Shell. Sie ist die Werkbank, auf der jemand ein Formular für ein Workflow-Modul schreibt,
ohne ein Cockpit zu starten. Auch sie gibt es heute dreimal sprachspezifisch, und auch sie braucht
eine Beschreibung statt dreier Vorlagen.

## Die Teile der Beschreibung

Lies sie in dieser Reihenfolge. Jede baut auf der vorigen auf.

1. [references/gui-api.md](references/gui-api.md)<br>
   Was der Server kann: Anmelden, die Endpunkte, Blättern, Sortieren, Filtern über gemeldete
   Geschäftsdaten, Vorschläge für ein Suchfeld, die Modi einer Liste, der Zeitstempel, der eine
   Liste stabil hält, und die Live-Aktualisierung über Server-Sent-Events.
2. [references/module-federation.md](references/module-federation.md)<br>
   Wie ein Teil eines Workflow-Moduls in deine Seite kommt: die vier Teile, der Ablauf beim Laden,
   was geteilt wird, und woran du erkennst, dass ein Teil fertig ist.
3. [references/types.md](references/types.md)<br>
   Die Typen, und welche davon Vertrag sind und welche nur Bequemlichkeit.
4. [references/behaviour.md](references/behaviour.md)<br>
   Was der Benutzer erwartet: die Bildschirme, die Spalten, die Zellen, das Verhalten einer Liste,
   die Aktionen auf einer Aufgabe, das Öffnen, die Sprache.
5. [references/dev-shell.md](references/dev-shell.md)<br>
   Die Werkbank für ein Workflow-Modul und der Simulator dahinter.
6. [references/rough-edges.md](references/rough-edges.md)<br>
   Die harten Kanten. Lies sie, bevor du den ersten Bildschirm baust, nicht hinterher.

Daneben steht das Wiki des Repositories, und es beschreibt dieselben Dinge für den Leser, der kein
UI baut:
[Architecture](https://github.com/vanillabp/business-cockpit/wiki/Architecture),
[Security](https://github.com/vanillabp/business-cockpit/wiki/Security),
[Configuration](https://github.com/vanillabp/business-cockpit/wiki/Configuration),
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit),
[User task forms and status sites](https://github.com/vanillabp/business-cockpit/wiki/User-task-forms-and-status-sites)
und
[Customizing the user interface](https://github.com/vanillabp/business-cockpit/wiki/Customizing-the-user-interface).

## Die Reihenfolge beim Bauen

Jeder Schritt ist für sich vorzeigbar. Wer die Reihenfolge umstellt, baut lange, ohne etwas zu
sehen.

1. Anmelden und den Rahmen zeigen. `GET /app/info` und `GET /app/current-user` genügen für eine
   Anwendung, die weiß, wer da ist.
2. Eine Liste ohne Spalten der Module. Nimm die Spalten, die das Cockpit selbst ergänzt, und zeige
   `POST /usertask`. Damit steht das Blättern.
3. Sortieren und Suchen. Danach sind die Pfade und ihre Fallen geklärt.
4. Die Teile der Module laden. Erst die Spalten der Liste, dann das Formular einer Aufgabe.
5. Der Ereignisstrom. Zuletzt, weil er am meisten vom Rest voraussetzt.
6. Die Dev Shell, falls du für dein Framework auch die brauchst.

## Was ein Cockpit-UI nicht tut

Diese vier Grenzen sind keine Sparsamkeit, sondern der Entwurf. Wer sie überschreitet, baut ein
zweites System neben den Workflow-Modulen.

Es enthält keine Geschäftslogik. Was mit einer Aufgabe fachlich geschieht, entscheidet das
Workflow-Modul. Das Cockpit kennt keinen Fall, es kennt nur, was gemeldet wurde.

Es schließt keine Aufgabe ab und startet keinen Fall. Ein Formular ruft dafür die eigene API seines
Workflow-Moduls. Dass die Aufgabe fertig ist, erfährt das Cockpit auf demselben Weg wie alles
andere, als Bericht.

Es greift nicht selbst auf ein Workflow-Modul zu. Jede Anfrage an ein Modul geht über den Proxy des
Cockpits unter `/wm/<workflowModuleId>/`, und sie geht vom Formular aus, nicht vom Rahmen.

Es hat keine eigene Anmeldung. Es gibt kein Anmeldeformular, keine Benutzerverwaltung und keine
Rechteprüfung im Browser. Wer was sehen darf, entscheidet der Server, und was er nicht liefert, gibt
es für dein UI nicht.

## Wie dein UI in das Cockpit kommt

Ein Cockpit ist eine Spring-Boot-Anwendung, und das UI liegt in ihr. Die Bibliothek
`io.vanillabp.businesscockpit:business-cockpit` baut die mitgelieferte React-Anwendung nach
`target/classes/static`, und von dort liefert jede Cockpit-Anwendung sie aus, auch eine eigene, die
die Bibliothek nur als Abhängigkeit hat. Wie so eine eigene Anwendung entsteht, steht im Wiki unter
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit).

Dein UI tritt an diese Stelle. Was du dafür wissen musst:

Die gebauten Dateien gehören unter `/static` oder `/assets`. Nur diese zwei Präfixe sind ohne
Anmeldung erreichbar. Liegt dein Bundle woanders, verlangt der Server dafür eine Anmeldung, und der
Browser lädt es nicht.

Die Hülle benennt `application.spa-default-file`, standardmäßig `classpath:/static/index.html`.
Jeder Pfad, den kein Controller und keine Datei beantwortet, bekommt diese Datei statt eines
Fehlers. Das ist es, was einen Link mitten in deine Anwendung funktionieren lässt.

Die Hülle selbst ist nicht offen. Ein Aufruf ohne Anmeldung antwortet mit 401 und der Aufforderung
zu Basic Auth; das mitgelieferte UI lässt dafür schlicht den Anmeldedialog des Browsers erscheinen.
Die Antwort auf diese Anfrage setzt das Cookie, mit dem alles Weitere läuft. Eine eigene
Anmeldemaske baut dein UI nicht, siehe
[Security](https://github.com/vanillabp/business-cockpit/wiki/Security).

## Woran du merkst, dass es fertig ist

Gegen ein laufendes Cockpit mit mindestens einem Workflow-Modul:

1. Die Aufgabenliste und die Fallliste zeigen Zeilen, mit den Spalten, die das Modul beisteuert.
2. Das Formular einer Aufgabe aus dem Modul lädt und zeigt die Daten der Aufgabe.
3. Eine Änderung an einer Aufgabe, die jemand anderes auslöst, erscheint ohne Neuladen.
4. Übernehmen und Zurückgeben wirken, und die Zeile zeigt es danach.
5. Blättern, Sortieren und Suchen liefern, was sie sollen, auch über gemeldete Geschäftsdaten.
6. Eine Aufgabe eines Moduls vom Typ `EXTERNAL` öffnet die fremde Seite und hält die Liste heil.

Ein Test, der ein erzeugtes UI gegen die GUI-API fährt, ist der einzige Weg, ein Auseinanderlaufen
zu bemerken. Den gibt es heute nicht.
