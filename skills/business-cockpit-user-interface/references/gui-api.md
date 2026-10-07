# Die GUI-API

*Teil der Beschreibung in [`../SKILL.md`](../SKILL.md). Bis zum Review deutsch.*

Die GUI-API ist alles, was ein Cockpit-UI vom Server bekommt. Sie liegt unter `/gui/api/v1`, ist in
`apis/official-gui-api/openapi/v1.yaml` und `business-cockpit/src/main/resources/api/gui/v1.yaml`
beschrieben, und diese beiden Dateien sind die Wahrheit. Was hier steht, ist das Verhalten dahinter,
das in keiner der beiden Dateien steht.

Erzeuge dir deinen Client aus der OpenAPI-Beschreibung. Schreibe ihn nicht von Hand ab.

1. [Anmelden und angemeldet bleiben](#anmelden-und-angemeldet-bleiben)
1. [Die Endpunkte](#die-endpunkte)
1. [Seitenweise lesen](#seitenweise-lesen)
1. [Sortieren](#sortieren)
1. [Filtern über gemeldete Geschäftsdaten](#filtern-über-gemeldete-geschäftsdaten)
1. [Vorschläge für ein Suchfeld](#vorschläge-für-ein-suchfeld)
1. [Welche Zeilen überhaupt kommen](#welche-zeilen-überhaupt-kommen)
1. [Der Zeitstempel, der eine Liste stabil hält](#der-zeitstempel-der-eine-liste-stabil-hält)
1. [Die zweite Form derselben Abfrage](#die-zweite-form-derselben-abfrage)
1. [Live-Aktualisierung über Server-Sent-Events](#live-aktualisierung-über-server-sent-events)
1. [Was das Cockpit ausliefert](#was-das-cockpit-ausliefert)

## Anmelden und angemeldet bleiben

Das Cockpit hat keine eigene Anmeldeseite, und dein UI baut auch keine. Jede Anfrage an die GUI-API
verlangt eine Anmeldung, die Antwort auf die erste setzt ein Cookie, und von da an reist das Cookie
mit. Im Cookie steht ein JWT. Wie es signiert wird, wie lange es lebt und wie ein Identity-Provider
davorkommt, steht im Wiki unter
[Security](https://github.com/vanillabp/business-cockpit/wiki/Security).

Fünf Muster kommen ohne Anmeldung durch, und genau die braucht dein UI, um sich überhaupt zeigen zu
können: `/gui/api/v1/app/info`, `/gui/api/v1/app/current-user`, `/assets/**`, `/static/**` und der
Proxy auf die Workflow-Module unter `/wm/**`. Alles andere verlangt eine Anmeldung, auch die Hülle
deiner Anwendung unter `/`, siehe [`../SKILL.md`](../SKILL.md#wie-dein-ui-in-das-cockpit-kommt).

Daraus folgt der Ablauf beim Start:

1. `GET /gui/api/v1/app/info` holt Titel, Version und Bauzeitpunkt der Anwendung. Das geht ohne
   Benutzer, also kann der Rahmen schon stehen, bevor jemand angemeldet ist.
2. `GET /gui/api/v1/app/current-user` liefert den angemeldeten Benutzer mit seinen Gruppen. Ist
   niemand angemeldet, kommt eine Antwort ohne Körper. Dein UI darf daraus nicht auf einen Fehler
   schließen, sondern muss den Benutzer zur Anmeldung schicken.
3. Erst danach lohnt sich die erste Liste.

Der Grund für die leere Antwort ist die Anmeldung selbst: die Anfrage, die sich anmeldet, ist
authentifiziert, aber erst ihre Antwort trägt das Cookie. Das Cockpit liest den Benutzer aus dem
Cookie, also hat gerade diese Anfrage noch keinen, und der Client fragt mit dem Cookie nochmal.

Das Cookie verlängert der Server selbst. Hat das Token weniger als die Hälfte seiner Laufzeit übrig
(`business-cockpit.jwt.cookie.expires-duration`, standardmäßig zwölf Stunden), trägt die Antwort auf
eine gewöhnliche Anfrage ein neues Cookie. Der Browser übernimmt es von allein, dein UI tut dafür
nichts. Zwei Dinge musst du trotzdem einplanen:

- Der Ereignisstrom zählt nicht. Ein Tab, in dem niemand etwas tut, ist nach zwölf Stunden
  abgemeldet. Frag deshalb nichts im Hintergrund nach, nur um angemeldet zu bleiben. Genau das soll
  nicht passieren.
- Jede Anmeldung endet nach `business-cockpit.jwt.cookie.max-login-duration`, standardmäßig sieben
  Tage, auch mitten in der Arbeit. Danach antwortet jede Anfrage mit 401, und der Benutzer muss
  sich neu anmelden. Dein UI muss eine 401 also jederzeit verkraften, nicht nur beim Start.

`/logout` löscht das Cookie und schickt den Browser auf die Startseite.

Das mitgelieferte UI hat dazu noch ein zweites Verfahren, einen Refresh-Token. Es legt den Wert des
Kopffelds `x-refresh-token` im `localStorage` ab, schickt ihn mit, wenn eine Anfrage mit 401
abgelehnt wurde, und wiederholt sie dann. Die Anfragen laufen dafür durch ein Web-Lock namens
`bc-webapp`, eine Anfrage mit Refresh-Token exklusiv und jede andere geteilt, damit nicht zwei
Anfragen gleichzeitig erneuern. Das ist kein Vertrag der GUI-API. Ohne Identity-Provider, der so
etwas ausstellt, passiert hier nichts, und dein UI darf es weglassen. Siehe
[rough-edges.md](rough-edges.md#der-refresh-token-hängt-in-der-luft) für das, was daran hängt.

## Die Endpunkte

Die drei Aufgabenlisten der mitgelieferten Anwendung sind derselbe Endpunkt unter drei
Basispfaden. Welche Aufgaben jeder davon zeigt, entscheidet ein Controller der Anwendung und nicht
die API, siehe
[Building a custom Business Cockpit](https://github.com/vanillabp/business-cockpit/wiki/Building-a-custom-Business-Cockpit).

| Basispfad | Was die Liste zeigt |
|---|---|
| `/gui/api/v1` | alles, was der Benutzer bearbeiten darf |
| `/gui/api/v1/current-user` | nur was ihm persönlich zugewiesen ist |
| `/gui/api/v1/user-roles` | was seine Gruppen übernehmen dürfen und niemandem zugewiesen ist |

Dein UI darf diese drei Pfade nicht als gegeben nehmen. Sie sind die Entscheidung der
mitgelieferten Anwendung. Ein eigenes Cockpit darf andere Listen anbieten, und dann heißen die
Pfade anders.

Die Endpunkte selbst, alle relativ zum Basispfad:

| Aufruf | Wofür |
|---|---|
| `POST /usertask` | eine Seite der Aufgabenliste |
| `PUT /usertask` | dieselbe Abfrage als Aktualisierung, siehe unten |
| `OPTIONS /usertask` | Vorschläge für ein Suchfeld über Aufgaben |
| `GET /usertask/{id}` | eine Aufgabe, mit `markAsRead` zugleich als gelesen markieren |
| `PATCH /usertask/{id}/claim`, `PATCH /usertask/claim` | Bearbeiter setzen, mit `unclaim=true` zurückgeben |
| `PATCH /usertask/{id}/assign`, `PATCH /usertask/assign` | eine Person als Kandidat hinzufügen, mit `unassign=true` wieder entfernen |
| `PATCH /usertask/{id}/mark-as-read`, `PATCH /usertask/mark-as-read` | als gelesen markieren, mit `unread=true` zurücknehmen |
| `PATCH /usertask/{id}/follow-up-date` | Wiedervorlage setzen oder löschen |
| `POST /user` | Benutzer suchen, für die Zuweisung an eine Person |
| `POST /workflow` | eine Seite der Fallliste |
| `PUT /workflow` | dieselbe Abfrage als Aktualisierung |
| `OPTIONS /workflow` | Vorschläge für ein Suchfeld über Fälle |
| `GET /workflow/{id}` | ein Fall |
| `POST /workflow/{id}/usertasks` | die Aufgaben eines Falls |
| `GET /workflow-module`, `GET /workflow-module/{id}` | die Workflow-Module, die der Benutzer sehen darf |
| `GET /notifications/media`, `/config`, `/recipient-config`, `/workflows` | die Benachrichtigungen des Benutzers |
| `GET /app/info`, `GET /app/current-user` | die Anwendung und der Benutzer |
| `GET /updates` | der Ereignisstrom, siehe unten |

Die beiden Formen jeder Aktion, eine für eine Aufgabe und eine für eine Liste von Kennungen, machen
dasselbe. Die Mehrfachform ist da, damit eine Mehrfachauswahl in der Liste nicht zu zwanzig Anfragen
führt.

## Seitenweise lesen

`pageNumber` und `pageSize` in `POST /usertask` und `POST /workflow` gehen unverändert an die
Datenbank. Fehlt `pageNumber`, kommt die erste Seite, also `0`. Fehlt `sortAscending`, sortiert der
Server aufsteigend. Das gilt auch für die Aktualisierung mit `PUT`.

`pageSize` hat keinen Standardwert. Fehlt es, antwortet der Server mit 400 und nennt das Feld:
`The request is not valid: 'pageSize' is missing.` Eine Seitengröße unter 1 und eine Seite unter 0
lehnt er genauso mit 400 ab. Setze `pageSize` also immer.

Eine Obergrenze gibt es nicht. Wähle die Seitengröße selbst mit Bedacht. Das mitgelieferte UI lädt
in Blöcken von 30 Zeilen und hängt den nächsten Block an, wenn der Benutzer nach unten kommt. Der
Server hält dich von nichts ab, auch nicht von 100000 Zeilen in einer Anfrage.

Die Antwort trägt ein `page` mit `number`, `size`, `totalPages` und `totalElements`.
`totalElements` ist genau gezählt, in einer zweiten Abfrage über dieselben Bedingungen. Du kannst
also eine Gesamtzahl anzeigen, und sie kostet eine Zählung je Seite.

`POST /workflow/{id}/usertasks` ist die Ausnahme: dort ist die Seitengröße standardmäßig 100.

## Sortieren

`sort` ist eine Liste von Pfaden im gespeicherten Dokument, mit Komma getrennt. `sortAscending` gilt
für alle davon.

Es gibt keine erlaubte Liste. Was du schickst, wird sortiert, auch ein Pfad, den es nicht gibt. Der
Server legt zu jeder neuen Zeichenkette in `sort` einen Index in der Datenbank an und merkt sich,
dass er es getan hat. Darum gilt: schicke nur Pfade, die aus einer Spalte kommen, und baue kein
Suchfeld, aus dem ein Benutzer frei einen Sortierpfad eingeben kann.

An das, was du schickst, hängt der Server seine eigene Ordnung an, damit das Blättern stabil
bleibt. Bei Aufgaben ist das `dueDate`, `createdAt`, `id`, bei Fällen `createdAt`, `id`. Ein Feld,
das du selbst genannt hast, fällt aus dieser Ergänzung heraus. Weil `id` eindeutig ist und zuletzt
steht, ist die Ordnung vollständig, und dieselbe Abfrage liefert zweimal dieselbe Reihenfolge.

Ohne `sort` sortiert der Server Aufgaben nach `dueDate` und Fälle nach `createdAt`.

Eine Aufgabe ohne Fälligkeit ist dabei nicht ohne Wert. Der Server speichert dafür einen Zeitpunkt
in ferner Zukunft und liefert in der API `null`, damit so eine Aufgabe beim Sortieren nach
Fälligkeit hinten landet.

Bei jedem anderen Feld ist ein leerer Wert leer, und wo er landet, entscheidet die Datenbank. Der
Server bittet zwar darum, leere Werte hinten einzuordnen, aber die Bitte kommt nicht an: die
Abfrage trägt am Ende nur Feld und Richtung. Rechne also damit, dass Zeilen ohne Wert beim
aufsteigenden Sortieren vorne stehen.

## Filtern über gemeldete Geschäftsdaten

Jede `SearchQuery` hat drei Felder und wird zu einer Bedingung:

```json
{ "path": "details.customer.name", "query": "Meier", "caseInsensitive": true }
```

`path` ist der Pfad im Dokument, derselbe wie bei `sort` und derselbe wie `path` einer Spalte. Fehlt
er, sucht der Server im Feld `detailsFulltextSearch`. Das ist der Volltext, den das Workflow-Modul
selbst zusammengestellt hat, siehe
[Reporting workflows and user tasks](https://github.com/vanillabp/business-cockpit/wiki/Reporting-workflows-and-user-tasks).
Ein Volltextfeld und nichts darüber hinaus: wer nicht gemeldet hat, ist nicht zu finden.

`query` ist ein regulärer Ausdruck, kein Text. Er ist nicht verankert, trifft also auch mitten im
Wert. Ein `.` oder ein `*` aus der Eingabe des Benutzers wird als Sonderzeichen gelesen. Wenn dein
UI eine Eingabe durchreicht, maskiere sie.

`caseInsensitive` schaltet die Option `i` ein, sonst wird Groß- und Kleinschreibung unterschieden.

Mehrere `SearchQuery` werden mit UND verknüpft. Eine Grenze gibt es dabei: zwei Einträge mit
demselben `path` sind keine zwei Bedingungen auf einem Feld, sondern ein Fehlerfall. Das
mitgelieferte UI hält sich daran, indem es beim Setzen eines Filters den alten Eintrag für diesen
Pfad vorher entfernt. Mach es genauso.

Zwei Felder, die danach aussehen und es nicht sind:

`query` von `UserTasksRequest` liest der Server nicht. Es steht in der Beschreibung und in keinem
Code. Benutze `searchQueries`.

`businessIds` von `WorkflowsRequest` ist ein genauer Vergleich auf dem Feld `businessId` und kein
Suchmuster. Es wirkt nur auf `POST /workflow` und nicht auf die Aktualisierung mit `PUT`, siehe
[rough-edges.md](rough-edges.md#businessids-verschwindet-bei-der-aktualisierung).

## Vorschläge für ein Suchfeld

`OPTIONS /usertask` und `OPTIONS /workflow` liefern Wörter und ihre Anzahl, auf Englisch "keyword in
context", kurz KWIC. Ein UI benutzt sie für die Vorschläge unter einem Suchfeld.

Die Abfrage nimmt `query` und wahlweise `path` als Parameter und die bereits gesetzten
`searchQueries` im Körper. Sie antwortet mit ganzen Wörtern, die `query` enthalten, je Wort eine
Anzahl.

Was du dabei wissen musst:

Unter drei Zeichen kommt eine leere Antwort und kein Fehler. Lass das Feld also erst ab drei Zeichen
fragen.

Die Suche ist hier immer unabhängig von Groß- und Kleinschreibung, auch wenn eine `SearchQuery`
etwas anderes sagt.

Die Antwort hat höchstens 21 Einträge, und es sind nicht die 21 häufigsten, sondern die ersten 21,
die die Datenbank findet. Sortiert sind sie nach Anzahl aufsteigend, bei gleicher Anzahl nach Wort.
Das mitgelieferte UI zeigt bei 21 Einträgen einen Hinweis, dass es mehr als zwanzig Treffer gibt,
statt die Liste zu zeigen.

Die Anzahl zählt Vorkommen des Wortes, nicht Zeilen. Ein Wort, das in einer Aufgabe zweimal steht,
zählt zweimal.

Die Vorschläge halten sich an die Sichtbarkeit des Benutzers und an die gesetzten `searchQueries`,
aber nicht an den Modus der Liste. Sie schauen immer nur auf offene Aufgaben und aktive Fälle. Eine
Liste, die abgeschlossene Aufgaben zeigt, bekommt also Vorschläge, die dazu nicht passen.

## Welche Zeilen überhaupt kommen

Zwei Dinge entscheiden das: der Modus, den dein UI schickt, und die Sichtbarkeit, die der Server für
den Benutzer bestimmt.

Der Modus:

| `mode` einer Aufgabenliste | Was kommt |
|---|---|
| `All` | alles, offen und abgeschlossen |
| `OpenTasks` | was kein Ende hat |
| `OpenTasksWithoutFollowUp` | offen, ohne Wiedervorlage oder mit einer, die fällig ist |
| `OpenTaskOnlyFollowUp` | offen, mit einer Wiedervorlage in der Zukunft |
| `OpenTasksWithFollowUp` | heute dasselbe wie `OpenTasks`, siehe [rough-edges.md](rough-edges.md#opentaskswithfollowup-filtert-nichts) |
| `ClosedTasksOnly` | nur was ein Ende hat |

| `mode` einer Fallliste | Was kommt |
|---|---|
| `All` | alles |
| `Active` | was kein Ende hat |
| `Inactive` | was ein Ende hat |

Ohne `mode` nimmt der Server `All` bei `POST` und `OpenTasks` bzw. `Active` bei `PUT`. Das ist nicht
dasselbe, also schicke den Modus immer selbst, wenn dir die Antwort wichtig ist.

Ein unbekannter Wert ist ein Serverfehler und keine stille Ignoranz.

Die Sichtbarkeit ist die Sache des Servers. Dein UI fragt nicht nach ihr und kann sie nicht
umgehen. Welche Regel die mitgelieferte Anwendung anwendet, steht im Wiki unter
[Who sees which workflow](https://github.com/vanillabp/business-cockpit/wiki/Security#who-sees-which-workflow)
und [Who sees which user task](https://github.com/vanillabp/business-cockpit/wiki/Security#who-sees-which-user-task).
Dieselbe Regel gilt für die Liste, für eine einzelne Zeile, für die Vorschläge und für jede Aktion.

`POST /workflow/{id}/usertasks` hat dafür einen eigenen Schalter, den Parameter `llatcup`. Mit
`true` kommen die Aufgaben, die dieser Benutzer bearbeiten dürfte, mit `false` alle Aufgaben des
Falls. Eine Seite, die den Stand eines Falls zeigt, will meist `false`, eine Seite, die zu tun
gibt, `true`.

## Der Zeitstempel, der eine Liste stabil hält

`initialTimestamp` ist der Zeitpunkt, an dem der Benutzer angefangen hat, auf diese Liste zu
schauen. Er wirkt nur an einer Stelle, und die ist wichtig: in den Modi für offene Aufgaben und
aktive Fälle kommt eine Zeile auch dann noch mit, wenn sie nach diesem Zeitpunkt beendet wurde.

Ohne das würde sich die Liste unter dem Leser verschieben. Eine Aufgabe, die auf Seite eins endet,
würde verschwinden, und alles danach würde eine Zeile nach vorne rutschen, sodass der Benutzer beim
Blättern eine Zeile nie zu sehen bekommt.

Deshalb:

1. Die erste Anfrage einer Liste schickt keinen `initialTimestamp`. Der Server setzt dann die
   aktuelle Zeit ein.
2. Die Antwort trägt diesen Wert als `serverTimestamp` zurück. Das ist der eingesetzte Wert und kein
   neuer Blick auf die Uhr.
3. Dein UI behält ihn und schickt ihn bei jeder weiteren Anfrage derselben Liste als
   `initialTimestamp` mit. Auch beim Blättern, beim Sortieren, beim Filtern und bei den Vorschlägen.
4. Erst wenn der Benutzer die Liste neu lädt, fängt der Zeitstempel neu an.

Derselbe Zeitstempel ist die Grundlage für die Farbe einer Zeile, siehe
[behaviour.md](behaviour.md#was-eine-liste-live-tut).

`requestId` in `POST /workflow` und `PUT /workflow` gibt der Server unverändert zurück und wertet
ihn nicht aus. `UserTasks` hat kein solches Feld, und das mitgelieferte UI setzt `requestId`
nirgends. Lücke in dieser Beschreibung: wozu das Feld gedacht war, sagt weder der Code noch die
OpenAPI-Beschreibung. Eine Antwort einer Anfrage zuzuordnen wäre die naheliegende Verwendung, aber
niemand hat sie aufgeschrieben, also verlass dich nicht darauf, dass sie gemeint war.

## Die zweite Form derselben Abfrage

Neben `POST` hat jede Liste ein `PUT` mit `size` und den Kennungen, die der Client schon hat. Es
antwortet mit der ersten Seite von `size` Zeilen in derselben Reihenfolge wie die Liste, und es
liefert

* eine Zeile, die der Client schon kennt, nur als Hülle mit ihrer Kennung,
* eine Zeile, die er noch nicht kennt, vollständig.

Damit erfährt dein UI die ganze aktuelle Reihenfolge und lädt nur, was neu ist. Eine Hülle ist am
Feld `version` zu erkennen: sie trägt `0`, eine echte Zeile nie.

So benutzt es das mitgelieferte UI: nach einem Ereignis aus dem Ereignisstrom fragt es mit `PUT`
nach so vielen Zeilen, wie es geladen hat, plus einer Reserve, und lässt die Kennungen der Zeilen
weg, von denen es weiß, dass sie sich geändert haben. Genau die kommen dadurch vollständig zurück.

Zwei Dinge sind bei `PUT` anders als bei `POST`: `page.number` ist immer 0 und `page.size` ist die
Anzahl der gelieferten Zeilen, und `businessIds` gibt es nicht.

## Live-Aktualisierung über Server-Sent-Events

`GET /gui/api/v1/updates` ist ein `text/event-stream`. Er bleibt offen, bis das Token abläuft, mit
dem er geöffnet wurde. Dann schließt der Server ihn, und der Browser verbindet mit dem Cookie neu,
das er in diesem Moment hat. Hat der Benutzer inzwischen gearbeitet, ist das ein verlängertes, und
der neue Strom läuft weiter. Sonst antwortet der Server mit 401. Der Strom selbst verlängert die
Anmeldung nicht.

Was darüber kommt:

| Name des Ereignisses | Inhalt |
|---|---|
| `ping` | ein Objekt mit `id` und `name`, sonst nichts |
| `UserTask` | eine Liste von Ereignissen, jedes mit `id`, `name` und `type` |
| `Workflow` | dasselbe für Fälle |

`type` ist `INSERT`, `UPDATE`, `DELETE` oder `FOLLOWUP`. `id` ist die Kennung der Aufgabe oder des
Falls. Mehr steht nicht drin, also sagt ein Ereignis nur, dass sich etwas geändert hat, und nicht
was. Dein UI fragt danach neu.

Fünf Eigenschaften des Stroms musst du einplanen:

Die Ereignisse werden gesammelt und nicht einzeln geschickt. Der Sammler läuft alle 250
Millisekunden, und je Browser wird höchstens alle `business-cockpit.gui-sse.update-interval`
Millisekunden abgeliefert, standardmäßig alle 500, mit höchstens
`business-cockpit.gui-sse.max-items-per-update` Einträgen, standardmäßig 100. Was darüber liegt,
bleibt liegen und kommt beim nächsten Mal. Ein Schwung von Änderungen wird so zu einer
Aktualisierung statt zu hunderten.

Kurz nach dem Verbinden kommt ein `ping`, und alle 27 Sekunden wieder einer. Der erste ist nicht
Höflichkeit: solange der Browser noch nichts empfangen hat, gilt die Anfrage für ihn als laufend.
Das mitgelieferte UI schickt den Strom durch dasselbe Web-Lock wie jede andere Anfrage, und ohne
diesen ersten `ping` bliebe das Lock gehalten und die Oberfläche stünde. Wenn dein UI so ein Lock
nicht hat, brauchst du den ersten `ping` nicht. Die späteren brauchst du, denn ein Strom, über den
nichts läuft, wird unterwegs geschlossen.

Die Ereignisse sind heute nicht auf den Benutzer zugeschnitten. Jeder verbundene Browser bekommt
jedes Ereignis, auch zu einer Aufgabe, die dieser Benutzer nicht sehen darf. Verlass dich also nicht
darauf, dass eine Kennung aus dem Strom für diesen Benutzer existiert. Frag nach und nimm eine leere
Antwort als Antwort. Die Einschränkung ist vorgesehen und noch nicht gebaut.

Wenn die Verbindung abbricht, verbindet das mitgelieferte UI nach 15 Sekunden neu. In dieser Lücke
gehen Ereignisse verloren. Dafür gibt es den Weckruf: vor jeder normalen Anfrage wird eine
wartende Neuverbindung sofort ausgelöst, damit ein Ereignis, das diese Anfrage auslöst, nicht in die
Lücke fällt. Baue etwas in dieser Art, sonst verpasst dein UI genau die Änderungen, die der Benutzer
selbst angestoßen hat.

Beim Herunterfahren schließt der Server alle Ströme. Ein Strom, der sich von selbst schließt, ist
also normal und kein Fehler.

## Was das Cockpit ausliefert

Das Cockpit ist nicht nur eine API, es liefert auch aus, und zwei Pfade gehören zum Vertrag.

Unter `/wm/<workflowModuleId>/**` steht jedes registrierte Workflow-Modul. Das Cockpit leitet die
Anfrage an die Adresse weiter, die das Modul gemeldet hat. Darum laufen zwei Dinge über den eigenen
Ursprung des Cockpits: das Bundle eines Moduls und jede Anfrage, die ein Formular an sein eigenes
Backend stellt. Das Session-Cookie reist dabei mit. Die Routen ändern sich, während die Anwendung
läuft, denn ein Modul registriert sich im Betrieb.

Unter `/` liegt das UI selbst. Das mitgelieferte UI ist eine Single-Page-Anwendung, und ihre
statischen Dateien liegen im Jar der Bibliothek. Ein eigenes UI tritt an diese Stelle, siehe
[../SKILL.md](../SKILL.md#wie-dein-ui-in-das-cockpit-kommt).

In `uiUri` einer Aufgabe oder eines Falls steht der Pfad, den das Workflow-Modul gemeldet hat,
unverändert. Der Server baut daraus keine Adresse und liest auch `uiUriType` nicht. Die Adresse zu
bilden ist Sache deines UIs: beim Typ `WEBPACK_MF_REACT` gehört der Pfad unter
`/wm/<workflowModuleId>/`, weil das Cockpit das Modul dort selbst ausliefert, und beim Typ
`EXTERNAL` ist der gemeldete Wert schon die ganze Adresse, denn das Cockpit kennt diese Anwendung
nicht und leitet nichts dorthin weiter. In `workflowModuleUri` steht der Pfad zum Modul selbst, ohne
Datei. Er dient Anfragen eines Formulars an sein eigenes Backend und ist zugleich der Vorsatz, den
du vor einen gemeldeten Pfad setzt.
