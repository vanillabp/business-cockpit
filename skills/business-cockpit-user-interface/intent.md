# Wozu ein Business-Cockpit-UI da ist

Diese Datei ist das Gerüst, das Stephan ausfüllt. Sie ist bis zum Abschluss deutsch, so wie der
Rest dieser Beschreibung, und wird danach übersetzt.

Sie ist der erste Teil, den ein Agent liest, und der wichtigste. Die anderen Dateien sagen, was
das mitgelieferte UI TUT; sie sind aus dem Code abgeleitet und beweisbar. Diese Datei sagt, WOZU
es da ist und was davon Absicht ist. Das steht in keinem Code, und wo sie leer bleibt, rät ein
Agent.

## Wie du sie ausfüllst

Jeder Abschnitt hat fünf Felder, immer dieselben. Kurz antworten ist besser als ausführlich.

- **Wozu**: ein oder zwei Sätze. Welches Problem eines Benutzers löst das, und was wäre ohne es
  schlimmer.
- **Was der Benutzer sieht und tun kann**: in seinen Worten, nicht in Feldnamen.
- **Regeln, die nicht gebrochen werden dürfen**: was ein selbstgebautes UI falsch machen würde.
  Hier sind die Sätze am wertvollsten, die mit "nie" oder "erst wenn" anfangen.
- **Was ein Kunde hier anders will**: woran in der Praxis geschraubt wird. Das sagt dem Agenten,
  wo er Freiheit hat und wo nicht.
- **Offen**: was du selbst nicht entschieden hast. Eine offene Frage hier ist besser als eine
  erfundene Antwort.

Ein Feld, zu dem dir nichts einfällt, lass leer. Leer heißt "nichts Besonderes", und das ist auch
eine Aussage. Wo du einen Abschnitt für überflüssig hältst, streich ihn mit einem Satz, warum.

Was du NICHT schreiben musst: Endpunkte, Feldnamen, Typen, Pfade, Anmeldung, Module Federation.
Das steht schon in `references/` und ist gegen den Code belegt.

## Das Ganze

Wozu:

Was der Benutzer sieht und tun kann:

Regeln, die nicht gebrochen werden dürfen:

Was ein Kunde hier anders will:

Offen:

## Die Aufgabenlisten

Das mitgelieferte UI hat drei: alles was der Benutzer bearbeiten darf, was ihm persönlich
zugewiesen ist, und was seine Gruppen übernehmen dürfen. Dass es genau diese drei sind, ist eine
Entscheidung der mitgelieferten Anwendung und kein Vertrag.

Wozu:

Was der Benutzer sieht und tun kann:

Regeln, die nicht gebrochen werden dürfen:

Was ein Kunde hier anders will:

Offen:

## Eine Aufgabe öffnen und bearbeiten

Das Formular kommt aus dem Workflow-Modul, nicht aus dem Cockpit.

Wozu:

Was der Benutzer sieht und tun kann:

Regeln, die nicht gebrochen werden dürfen:

Was ein Kunde hier anders will:

Offen:

## Die Aktionen an einer Aufgabe

Übernehmen, zurückgeben, einer Person zuweisen, als gelesen markieren, Wiedervorlage setzen.

Wozu:

Was der Benutzer sieht und tun kann:

Regeln, die nicht gebrochen werden dürfen:

Was ein Kunde hier anders will:

Offen:

## Die Liste der Geschäftsfälle

Wozu:

Was der Benutzer sieht und tun kann:

Regeln, die nicht gebrochen werden dürfen:

Was ein Kunde hier anders will:

Offen:

## Die Seite eines Falls und seine Aufgaben

Wozu:

Was der Benutzer sieht und tun kann:

Regeln, die nicht gebrochen werden dürfen:

Was ein Kunde hier anders will:

Offen:

## Suchen, filtern, sortieren, blättern

Wozu:

Was der Benutzer sieht und tun kann:

Regeln, die nicht gebrochen werden dürfen:

Was ein Kunde hier anders will:

Offen:

## Was live passiert, während der Benutzer zusieht

Hier gehört auch hin, was eine Oberflaeche dem Benutzer SAGEN soll, wenn sich etwas geändert hat,
siehe Story `1410`.

Wozu:

Was der Benutzer sieht und tun kann:

Regeln, die nicht gebrochen werden dürfen:

Was ein Kunde hier anders will:

Offen:

## Die Spalten, die ein Workflow-Modul mitbringt

Wozu:

Was der Benutzer sieht und tun kann:

Regeln, die nicht gebrochen werden dürfen:

Was ein Kunde hier anders will:

Offen:

## Benachrichtigungen

Wozu:

Was der Benutzer sieht und tun kann:

Regeln, die nicht gebrochen werden dürfen:

Was ein Kunde hier anders will:

Offen:

## Sprache und Texte

Wozu:

Was der Benutzer sieht und tun kann:

Regeln, die nicht gebrochen werden dürfen:

Was ein Kunde hier anders will:

Offen:

## Wer was sehen darf

Wozu:

Was der Benutzer sieht und tun kann:

Regeln, die nicht gebrochen werden dürfen:

Was ein Kunde hier anders will:

Offen:

## Was ein Cockpit-UI nicht tut

`SKILL.md` nennt vier Dinge, die es nicht tut, abgeleitet aus dem Code. Hier gehört hin, was du
ausdrücklich nicht willst, auch wenn es technisch ginge.

## Die Endpunkte, und wer sie braucht

Das ist deine Vollständigkeitsprobe und der einzige Teil, der von der API ausgeht. Trag je
Endpunkt ein, welcher Abschnitt oben ihn braucht. Ein Endpunkt ohne Abschnitt ist entweder tot
oder eine Fähigkeit, die oben fehlt. Ein Abschnitt ohne Endpunkt ist entweder reine Oberfläche
oder etwas, das die API noch nicht kann; beides ist einen Satz wert.

| Aufruf | Wofür er da ist | Welcher Abschnitt braucht ihn |
|---|---|---|
| `POST /usertask` | eine Seite der Aufgabenliste | |
| `PUT /usertask` | dieselbe Abfrage als Aktualisierung | |
| `OPTIONS /usertask` | Vorschläge für ein Suchfeld über Aufgaben | |
| `GET /usertask/{id}` | eine Aufgabe, zugleich als gelesen markierbar | |
| `PATCH /usertask/claim` | Bearbeiter setzen oder zurückgeben | |
| `PATCH /usertask/assign` | eine Person als Kandidat hinzufügen oder entfernen | |
| `PATCH /usertask/mark-as-read` | als gelesen markieren oder zurücknehmen | |
| `PATCH /usertask/{id}/follow-up-date` | Wiedervorlage setzen oder löschen | |
| `POST /user` | Benutzer suchen, für die Zuweisung an eine Person | |
| `POST /workflow` | eine Seite der Fallliste | |
| `PUT /workflow` | dieselbe Abfrage als Aktualisierung | |
| `OPTIONS /workflow` | Vorschläge für ein Suchfeld über Fälle | |
| `GET /workflow/{id}` | ein Fall | |
| `POST /workflow/{id}/usertasks` | die Aufgaben eines Falls | |
| `GET /workflow-module` | die Module, die der Benutzer sehen darf | |
| `GET /notifications/*` | die Benachrichtigungen des Benutzers | |
| `GET /app/info`, `GET /app/current-user` | die Anwendung und der Benutzer | |
| `GET /updates` | der Ereignisstrom | |

## Was danach passiert

Sobald die Felder stehen, wird daraus kein zweiter Text: die Sätze wandern in die Abschnitte von
`SKILL.md` und `references/behaviour.md`, dorthin, wo heute nur das Verhalten steht. Diese Datei
bleibt als Begründung liegen, damit der nächste Leser sieht, was Absicht war. Erst danach wird
übersetzt, das ist Story `1408`.
