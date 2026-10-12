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

Mit "individuell" ist in Folge normalerweise gemeint, dass die Entscheidung beim Kunden
liegt, wie die konkrete Ausprägung einer Funktion oder Erscheinung gewünscht ist und kann
von Kunde zu Kunde variieren.

## Das Ganze

Wozu:

Das Business Cockpit dient als zentrale UI eines Unternehmens, in der einerseits Aufgaben
aus der Geschäftsprozess-Digitalisierung den MitarbeiterInnen zur Abarbeitung vorgelegt werden und
andererseits bietet es MitarbeiterInnen die Möglichkeit, den aktuellen Status eines Geschäftsprozesses
einzusehen und ggfls. abseits der Aufgaben zu beeinflussen. Aus der Sicht des Unternehmens handelt
es sich bei einem Geschäftsprozess typischerweise um einen Geschäftsfall - egal ob ein externer
Geschäftsfall (z.B. bei einer Bank "Konto eröffnen") oder um einen internen (z.B. Freigabe eines
Urlaubsantrags einer MitarbeiterIn). Daher wird in der Oberfläche selten das Wort Prozess- oder Workflow
verwendet, sondern oft Vorgang bzw. Vorgänge oder Fall bzw. Fälle. 

Da diese Anwendung potentiell
flächendeckend im Unternehmen genutzt wird, kann sie auch als Basis für flächendeckende Funktionen
genutzt werden, die womöglich nur indirekt oder gar nichts mit Prozessen zu tun haben und sonst eine
eigene Web-Anwendung sein müssten. Beispiel: Vertretungsregelung bei Abwesenheit.

Was der Benutzer sieht und tun kann:

Typischerweise wird der BenutzerIn beim Einstieg ein 
Dashboard präsentiert. Was dieses Dashboard visualisiert ist kundenindividuell und zu erfragen.
In der Beispiel-UI könnte dies ein Torten-Diagramm der Aufgaben sein, aufgeteilt nach den
Aufgabenarten aller offenen Aufgaben. Zusätzlich ein zweites Tortendiagramm aller offenen 
Vorgänge, aufgeteilt nach den Arten. Über das Anklicken des Torten-diagrams kann
man in die Liste der Aufgaben gelangen (mit entsprechender Einschränkung der Menge)
oder eben in die Liste der Vorgänge. Gleiches geht auch über zwei Buttons, die im Header
links platziert sind, dann aber ohne Einschränkung der jeweiligen Liste. 

Regeln, die nicht gebrochen werden dürfen:

Die Web-Anwendung darf keine UI-Anteile von Geschäftsprozessen direkt beinhalten. Diese
werden dynamisch nachgeladen. Dies ist ganz wichtig, um den Lebenszyklus der Business Cockpit UI
von denen der einzelnen Geschäftsprozesse zu entkoppeln. Im Umkehrschluss müsste sonst für
jede UI-Änderung durch einen Prozess eine neue Release vom Business Cockpit gemacht werden,
was ab den ersten 10 Prozessen organisatorisch zum Problem wird und viele Unterbrechungen
für den End-Benutzer bedeutet.

Jegliche Daten zur Business-cockpit-Funktionalität steht über die REST-API "official-gui-api"
zur Verfügung. Normalerweise werden über OpenAPI-Generator passende Klassen und Interfaces
generiert, damit diese Dinge nicht ins Git des Kunden-Repo müssen.

Was ein Kunde hier anders will:

Oft kommt das Thema Module Federation als Punkt zur Sprache, weil dies nicht
den altbekannten Weboberflächen entsprechen und dann besondere Herangehensweisen
beim Entwickeln gewählt werden müssen. z.B. Development über DevShell, weil HMR nicht unterstützt wird.

Offen:

Welches UI-Framework (Angular, React, etc.), welches Design-Framework (PrimeNG, tailwind, etc.)
und welche Versionen davon. Jeder Kunde hat irgendetwas anderes und Vorlieben.

## Dev-Shell

Wozu:

Simuliert das Business-Cockpit zur Optimierung der Developer-Experience bei der Entwicklung von UI-Komponenten,
die das Workflow-Modul zur Verfügung stellt. Diese Komponenten werden über module federeation ins
Business Cockpit eingebetten, wodurch HMR nicht funktioniert und daher kein incrementeller Build und automatische
UI-Updates genutzt werden können. Die UI des Dev-Shell nutzt die Komponenten nicht über die module-federation
Wrapper (z.B. webcomponent), sondern ist in der UI-Technologie des moduls entwickelt und bindet die 
Komponenten direkt ein. Das Backend simuliert ebenfalls das Business-Cockpit und empfängt User-Tasks und Workflows
über die REST-API (bpms-api). Dadurch kann das Workflow-Modul unverändert zusammen mit der DevShell genutzt werden.

Regeln, die nicht gebrochen werden dürfen:

Für die Komponenten des Workflow-Moduls darf kein Unterschied zum Business-Cockpit sein.

Was ein Kunde hier anders will:

Kunden wollen direkt im Business Cockpit entwickeln. Das hat Vor- und Nachteile. Größter Vorteil ist, dass die UI manchmal
durch globale CSS-Styles verändert werden, was man in der DevShell nicht sieht, wenn man diese Stile dort nicht
ebenso verwendet. Die größten zwei Nachteile sind, dass das Business Cockpit vollständig aufgesetzt werden muss
(MongoDb; die Dev-Shell nutzt eine in-memory H2 DB) und dass durch die Module-Federation nachgeladene UI-Komponenten
nicht aktualisiert werden (ein Produktions npm-Build ist nötig und ein Relead im Browser).

## Die Aufgabenlisten

Es gibt mindestens eine Aufgabenliste, die alle Aufgaben anzeigt, die der Benutzer sehen darf. Der Umfang der Aufgaben
ergibt sich aus "(candidateGroups + candidateUsers + assignee) - excludedCandidateUsers + admittedUsers", wird
aber serverseitig bestimmt. Dann übernimmt ein Benutzer eine Aufgabe aktiv (claim), wodurch er als Assignee dieser
Aufgabe geführt wird. Die Übernahme ist keine technische Hürde, die serverseitig geprüft wird. Manche Kunden wollen
dies nur informell und andere hart (Bearbeitung nur möglich, nach vorigem claim). Es gibt verschiedene
Sichten auf diese Liste. Da die Methode UserTaskService#getUserTasks hier verschiedene Filterungen zulässt, ist die Wahl
der zur Verfügung gestellten Listen immer eine individuelle.

Manchmal werden Sublisten
daraus direkt zur Verfügung gestellt (z.B. eingeschränkt auf bestimmte Rollen oder bestimmte Aufgabentypen).
Ein Beispiel: Ein Unternehmen hat eine Gesamtliste an Gremien, in denen ein Nutzer Mitglied sein
kann und hat auf der Startseite für jedes Gremium (= Rolle) eine Card platziert mit der Anzahl der für den Nutzer
sichtbaren Aufgaben und der Anzahl vom Nutzer übernommen Aufgaben dieses Gremiums. Wenn man die Rolle nicht hatte,
war es zwangsläufig 0/0. 

Darüber hinaus hat jede Liste (egal ob Gesamt- oder Subliste) folgende Primärfilter: OpenTasks,
OpenTasksWithoutFollowUp (default), OpenTasksWithFollowUp, OpenTaskOnlyFollowUp, ClosedTasksOnly. Manche Kunden
wünschen nicht alle dieser Zustände zur Verfügung zu stellen.

Wozu:

Um die Aufgaben fristgerecht zu erledigen.

Geschlossene Aufgaben dienen der Dokumentation. Man soll sehen können, mit welchen
Eingaben der Task damals abgeschlossen wurde.

Was der Benutzer sieht und tun kann:

Jede Aufgabe hat eine Zeile. Die Spalten sind gemäß der Vorgaben zur Aufgabe vorhanden, gefüllt, sortierbar und filterbar.
Die Liste aktualisiert sich fortwährend. Neue Aufgabe kommen dazu und sind farblich hervorgehoben.
Änderungen werden sofort angezeigt und andersfarbig hervorgehoben. Erledigte Aufgabe werden in Grautönen
dargestellt. Im Idealfall erfolgen Aktualisierung ausschließlich durch den SSE-Stream. Da dies jedoch manchmal
mit einer merkbaren zeitlichen Verzögerung einhergeht, wird die Aktualisierung teilweise durch die UI vorweggenommen.

Die erste Spalte (ID) dient mit Checkboxen zu Auswahl für Bulk-Aktionen. Die Aufgaben sollen mit den
Kontext-Informationen (Spalten) angezeigt werden,
die für die jeweilige Aufgabe notwendig sind, woraus folgt, dass die angezeigten Spalten dynamisch,
abhängig vom Inhalt der Liste sind. In der Regel sehen Benutzer aber nicht alle gesamt möglichen Spalten,
weil der individuelle Tätigkeitsbereich die Vielzahl an möglichen Aufgaben-typen einschränkt. Darüber hinaus,
werden manchmal über vordefinierte Liste nur jene Aufgaben angezeigt die gleiche oder ähnliche Spalten haben.

Die ausgewählten Aufgaben können zur Bearbeitung übernommen werden (assignee) oder anderen zur Bearbeitung
vorgeschlagen werden (candidateUsers). Aufgaben können mit Notizen versehen werden. Aufgaben können auf Wiedervorlage
gesetzt werden. Aufgaben können als ungelesen markiert werden (gesamte Zeile fett geschrieben) oder als gelesen. 

Es gibt fixe Spalten, die alle Aufgaben haben. Diese werden individuell gewählt. z.B. ID, Fallart, Aufgabentitel,
Bearbeiter (assignee), Kandidaten (candidateUsers), Erstellt (createdAt), Fälligkeit (dueDate),
Wiedervorlage (followup), Abgeschlossen (endedAt) - Reihenfolge ebenso individuell.
Die Spalte Wiedervorlage wird nur bei OpenTasksWithFollowUp, OpenTaskOnlyFollowUp angezeigt.
Die Spalte Abgeschlossen wird nur bei ClosedTasksOnly angezeigt.

Wenn die Notizenspalte angezeigt wird, können Notizen in der Liste bearbeitet werden oder gesammelt für alle gewählten
über den Header-Bereich.

Regeln, die nicht gebrochen werden dürfen:

-

Was ein Kunde hier anders will:

Manche wollen keine Wiedervorlagefunktionalität oder keine geschlossenen Aufgaben anzeigen.

Manche wollen nur fixe Spalten und keine dynamischen, weil das Business Cockpit nur in einem Bereich des Unternehmens
eingesetzt wird, wo es nur bestimmte Werte in der Liste geben soll.

Offen:

In React war es möglich über Modul-Federation auch einzelne Spalten-UI-Komponenten zu definieren. Dadurch konnten einzelne
Aufgaben individuelle Darstellungen für Zelleninhalte anbieten (z.B. Ampel für Risiko-Einstufung) und sogar
in-place-Erledigung der Aufgaben umsetzen (Eine Zelle zeigt Buttons). Ebenso wurde es genutzt, um Detaildaten in der
Liste nachzuladen (on-mouse-over). Über Angular haben wir das nicht geschafft,
damals hatten wir aber noch nicht die native-module-federation genutzt. Abseits dieser Zellen wird einerseits nur
plain Javascript über module-federation zur Verfügung gestellt und andererseits immer eine Webcomponent diese verwendet,
damit die Frameworks und die Frameworkversionen des Cockpits und des Workflow-Moduls nicht zwingend gleich bzw.
kompatibel sein müssen. In den Listenzellen ist das schwierig, weil hier die Komponenten sehr oft gerendert werden und
dann für jede Zelle innerhalb der Webcomponent das Framework erneut instanziert würde. Hier gibt es noch keine gute Lösung.
Daher ist das Rendern von Zellen durch Komponenten des Workflow-Module nicht zwingend nötig, aber natürlich
wünschenswert weil es ein tolles Feature ist.

## Eine Aufgabe öffnen und bearbeiten

Das Formular kommt aus dem Workflow-Modul, nicht aus dem Cockpit. Dennoch muss um das Formular herum ein Rahmen vom
Cockpit angezeigt werden, der allgemeine Funktionalität zur Verfügung stellt: Kommentar, Wiedervorlage, Übernehmen.

Wozu:

Der Anwender bedient die Aufgabe, um sie im fachlichen Sinne abzuschließen.

Was der Benutzer sieht und tun kann:

Meistens gibt es im Header Funktionen wie Kommentar, Wiedervorlage und Übernehmen. Zusätzlich gibt es die Möglichkeit
die Aufgabe als eigenes Fenster zu öffnen. Was dann eigentlich passiert ist, dass der Anwendungsheader ausgeblendet wird
(Achtung Anwendungheader != Aufgaben-Header) und die Liste (!) in einem neuen Tab geöffnet wird. Damit soll ermöglicht
werden, dass eine Formulareingabe nicht geschlossen werden muss und womöglich Daten verloren gehen, wenn der Benutzer
in einen anderen Bereich wechseln möchte (Use-Case: Kunde ruft an und will über seinen Fall informiert werden).
Der Aufgabenheader kann auch als Footer ausgeprägt sein.

Überlange Formulare scrollen im eigenen Bereich, der Header bleibt stehen. Der Formular-Bereich ist ohne
Übernahme nicht bearbeitbar, meisten hergestellt über ein halbtransparentes Div darüber. Diese Barriere ist
Kunden-individuell.

Das Formular wird vom Workflow Modul zur Verfügung gestellt. Alle Abfragen, die nötig sind um Daten anzuzeigen
oder das Formular abzuschließen werden über den Proxy /wm/ direkt vom Workflow-Modul bedient. Das Business Cockpit
bekommt davon nichts mit.

Meistens gibt es die Regel, dass am rechten Rand des Formulars eine Spalte angezeigt wird, die oben Stamm-Infos
zur Aufgabe anzeigen (z.B. Fallnummer, Kunde, etc.) und rechts unten in der Spalte die möglichen Abschlußaktionen
meist als Buttons z.B. "Annehmen", "Abweisen" angezeigt werden. Die Spalte füllt den gesamten sichtbaren Bereich,
scroll aber mit überlangen Formularen nicht mit, damit die Aktions-Buttons immer sichtbar sind. Ist der Bereich über
den Abschlußaktionen zu klein, dann wird in diesem Bereich gescrollt.

Manche Formular können zwischengespeichert werden und bieten zusätzlich eine Save-Aktionsbutton an. Ob Formulare
automatisch zwischenspeichern, manuell oder gar nicht bestimmt das Formular selbst.

Änderungen am Formular durch andere werden sofort mitgeteilt: z.b. "Benutzer x hat die Aufgabe übernommen. Bitte
Rücksprache vor dem Speichern oder Abschließen halten, um Datenverlust zu vermeiden". Die Meldung erfolgt über
einen Toastr der zur Bestätigung weggeklickt werden muss.

Regeln, die nicht gebrochen werden dürfen:

Alle Möglichkeiten der Aufgabe sind Sache des Workflow-Moduls. Nur die Dinge des Headers kommen vom Cockpit und 
der notwendige Rahmen, damit die Sicht durch die Inhalte der Aufgaben nicht zerstört werden kann (scrolling).

Die Usability der Haupt-Teile muss immer gleich sein: Funktionen und Informationen an der gleichen Stelle (Header
und rechte Spalte mit Infos- und Aktionsbuttons). Es wäre zwar wünschenswert, das über das Business-Cockpit zentral
sicherzustellen, jedoch ist das schwierig, weil dann eine Aufgabe in mehrere Aufgaben-Komponenten (Body, Infos,
Aktionsbuttons) zerlegt angeboten werden muss. Es ist allerdings durchaus in Ordnung, wenn die DevShell die gleiche
Darstellung anbietet.

Was ein Kunde hier anders will:

-

Offen:

Wie die Spalte neben dem Formular vereinheitlicht geregelt wird. In der Vergangenheit war das Sache der Workflow-Module
mit dem Ergebnis, dass es nicht vereinheitlicht war und eine schlechte Usability brachte. Besonders wenn
mehrere Teams an Modulen arbeiten kam unterschiedliches heraus.

Es könnten auch mehrere MF-Komponenten sein (Infos, Buttons, Formular), die vom 
Cockpit arrangiert werden. Nachteil dabei ist, dass ein Formular in Einzel-Komponenten aufgeteilt wird und dann 
nicht per Javascript kreuz- und quer zugegriffen werden kann. Die Aktionsbuttons müssten dann z.B. per
Messaging das Formular zum Abschicken auffordern. Wie das geht müsste wieder "nativ" anfühlen, entsprechend des gewählten
UI-Frameworks.

## Die Aktionen an einer Aufgabe

Wozu:

Basis Infos anzeigen (Aufgabenart, Vorgangsart, Vorgangsnummer). Aufgabe übernehmen, zurückgeben,
einer Person vorlegen, Wiedervorlage setzen.

Eine geöffnete Aufgabe gilt automatisch als gelesen. Sie kann aber als "ungelesen" markiert werden -
in der Liste und in der Aufgabenansicht.

Was der Benutzer sieht und tun kann:

Es wird als Header oder Footer in der Aufgabenansicht dargestellt. Es soll jedoch über Aufgaben-Typen hinweg
immer an der gleichen Stelle angezeigt werden, um dem Nutzer als optischer Anker zu dienen. Diese Darstellung
soll nicht durch Inhalte zerstört werden können (Überlange Inhalten werden mit "..." abgekürzt). Welche Infos
zur Aufgabe angezeigt werden, ist individuell zu bestimmen. 

Zu den Aktionen stehen die UI-Elemente immer an der gleichen Stelle.

Regeln, die nicht gebrochen werden dürfen:

Dieser Bereich wird vom Business Cockpit zur Verfügung gestellt.

Was ein Kunde hier anders will:

-

Offen:

-

## Die Liste der Geschäftsfälle

Technisch gesehen gilt grundsätzlich das Gleiche wie bei den Aufgabenlisten.

Die serverseitige Auswahl der angezeigten Vorgänge ergibt sich aus '(accessibleToUsers", "accessibleToGroups")'.

Primärfilter sind: All, Active, Inactive

Wozu:

Um Vorgänge zu finden, einen ersten Eindruck zu deren Status über die angezeigten Spalten zu gewinnen
und, wenn nötig, in die Detailansicht zu wechseln.

Was der Benutzer sieht und tun kann:

Jeder Vorgang hat eine Zeile. Es gibt eigene Spaltendefinitionen im Workflow-Modul für Vorgangslisten.
Es gibt eigene fixe Spalten, unabhängig von jenen der Aufgaben. Es gibt keine "gelesen" Darstellung und
-funktionalität wie beim den Aufgaben.

Regeln, die nicht gebrochen werden dürfen:

-

Was ein Kunde hier anders will:

Manche wollen nur fixe Spalten und keine dynamischen, weil das Business Cockpit nur in einem Bereich des Unternehmens
eingesetzt wird, wo es nur bestimmte Werte in der Liste geben soll.

Offen:

Siehe Aufgabenlisten.

## Die Seite eines Falls und seine Aufgaben

Wozu:

Der Anwender kann sich Details zum Vorgang ansehen. Darüber hinaus kann das Workflow-Modul
hier Aktionsmöglichkeiten abseits des regulären Prozessflusses zur Verfügung stellen: z.B.
"Vorgang Abbrechen" Funktionalität, die im BPMN-Prozess durch einen Event-based-subprocess
verarbeitet wird.

Was der Benutzer sieht und tun kann:

Unterschied zur Aufgabenansicht:
- Es gibt durch die UI Möglichkeit die Vorgangsdetailansicht in einem separaten Tab zu öffnen.
- Es gibt keine Aufgaben-Header, weil es keine übergreifende Funktionalität gibt wie bei den Aufgaben.
- Es gibt eine Spalte mit Info- und Aktionsbereich analog zur Aufgabenansicht.

Regeln, die nicht gebrochen werden dürfen:

Alle Möglichkeiten dieser Ansicht sind Sache des Workflow-Moduls. Überlange Inhalte dürften die
Darstellung der Anwendung nicht zerstören.

Was ein Kunde hier anders will:

-

Offen:

Siehe "Aufgabe öffnen und bearbeiten"

## Suchen, filtern, sortieren, blättern

Wozu:

Um mit der Menge an Einträge gut umgehen zu können.

Was der Benutzer sieht und tun kann:

Volltextsuche ist bei beiden Listen verfügbar. Es gibt einen x-Button in der Volltextsuche-Eingabe, um diese
zurückzusetzen.

Pro Spalte kann ein eigener Suchbegriff zum (serverseitigen) Filtern angegeben und wieder entfernt werden.

Es gibt einen Button, um die Suchen zurückzusetzen.

Pro Vorgang und Aufgabe kann geregelt werden, was mit der Spalte gemacht werden kann. Die Liste der
Spalten-Definitionen werden durch das Workflow-Modul über zwei Funktionen (Aufgaben, Vorgänge) definiert:

```
export type ColumnsOfWorkflowFunction = (
  workflow: Workflow,
) => Column[] | undefined;

export type ColumnsOfUserTaskFunction = (
  userTask: UserTask,
) => Column[] | undefined;

export interface Column {
  title: Translatable;
  path: string;      // path of the value within the UserTask or Workflow object (see official-gui-api)
  type?: 'value' | 'i18n' | 'person' | 'date' | 'date-time' | 'time';
  priority: number;
  width: string;
  show: boolean;
  sortable: boolean;
  filterable: boolean;
  resizeable: boolean;
}

export interface Person {
id: string;
display?: string;
displayShort?: string;
email?: string;
avatar?: number;
details?: { [key: string]: unknown };
}
```

Pro Typ wird unter "path" eine andere Datenstruktur erwartet:
- *value*:
  - Feld: Ein Wert, der
  - Darstellung: 
- *i18n*:
  - Feld: Eine Map, mit Sprachen (z.B. en, de) als Keys und Text in der jeweiligen Sprache als Values
  - Darstellung: Der Text passend zur Sprache der UI, mit Fallback auf die Defaultsprache
- *person*:
  - Feld: Eine Struktur von Person
  - Darstellung: Nachnahme + ", " + Erster Buchstabe des Vornamens. On-mouse-over:  
- *date*:
  - Feld: Ein String im Format 'YYYY-MM-DD'
  - Darstellung: Lokalisierte Form des Datums
- *date-time*:
  - Feld: Ein String im Format ISO
  - Darstellung: Lokalisierte Form des Datums, daneben lokalisierte Form von Stunde und Minute. On-mouse-over auch mit Sekunde.
- *time*:
  - Feld: Ein String im 'hh:mm[:ss]'
  - Darstellung: Lokalisierte Form von Stunde und Minute. on-mouse-over auch mit Sekunde, wenn enthalten.

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
