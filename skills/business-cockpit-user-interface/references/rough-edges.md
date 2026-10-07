# Die harten Kanten

*Teil der Beschreibung in [`../SKILL.md`](../SKILL.md). Bis zum Review deutsch.*

Hier steht, was ein selbstgebautes UI zu Fall bringt, wenn es niemand erwähnt. Nichts davon ist eine
Vermutung. Jede Zeile kommt aus dem Code des Cockpits, und das mitgelieferte UI weicht genau an
diesen Stellen vom Naheliegenden ab.

Lies das vor dem ersten Bildschirm und nicht erst, wenn etwas nicht geht.

## Zuweisen heißt Kandidat machen

`PATCH /usertask/{id}/assign` setzt nicht den Bearbeiter. Es fügt die Person der Liste der Kandidaten
hinzu. Mit `unassign=true` wird sie wieder entfernt.

Den Bearbeiter setzt `claim`. Ein UI, das eine Schaltfläche "zuweisen" baut und einen Bearbeiter
erwartet, baut etwas anderes, als der Benutzer liest. Nenne die Aktion also so, wie sie wirkt.

## Übernehmen überschreibt

`claim` setzt den Bearbeiter, auch wenn schon jemand anderes draufsteht. Der Server fragt nicht nach
und gibt keinen Konflikt zurück. Ein UI, das eine Übernahme ohne Rückfrage anbietet, erlaubt damit,
eine Aufgabe aus den Händen eines Kollegen zu nehmen.

Eine Aufgabe nochmal zu übernehmen, die man schon hat, ändert nichts und antwortet 200.

## Zurückgeben einer fremden Aufgabe antwortet 200

`claim?unclaim=true` entfernt den Bearbeiter nur, wenn es die angegebene Person ist. Trifft es
niemanden, ändert sich nichts, und die Antwort ist trotzdem 200. Dein UI erfährt also nicht, ob es
gewirkt hat. Lies danach nach oder verlasse dich auf das Ereignis aus dem Strom.

Besonderheit beim Zurückgeben: der Server liest die Aufgabe danach ohne Sichtbarkeitsprüfung nochmal
und gibt sie zurück. Das ist Absicht, denn eine zurückgegebene Aufgabe verschwindet oft aus genau der
Liste, aus der heraus sie zurückgegeben wurde, und das UI braucht sie noch, um die Zeile umzuzeichnen.

## Abgeschlossene Aufgaben lassen sich noch übernehmen

Nur die Wiedervorlage prüft, ob eine Aufgabe schon beendet ist, und antwortet dann mit 409.
Übernehmen, Zurückgeben, Zuweisen, Entziehen und als gelesen markieren prüfen das nicht. Eine
abgeschlossene Aufgabe, die der Benutzer noch sehen darf, nimmt diese Aktionen an und antwortet 200.

Dein UI muss die Schaltflächen also selbst abschalten, wenn `endedAt` gesetzt ist.

## Die Mehrfachaktionen verschweigen, was sie übersprungen haben

Die Form mit einer Liste von Kennungen antwortet immer mit 200. Kennungen, die dieser Benutzer nicht
sehen darf, werden still weggelassen. Nach einer Mehrfachauswahl weißt du also nicht, wie viele
Zeilen tatsächlich geändert wurden.

Die Einzelform antwortet dagegen mit 404, und zwar sowohl für eine Aufgabe, die es nicht gibt, als
auch für eine, die dieser Benutzer nicht sehen darf. Das ist Absicht: die Antwort soll nicht
verraten, dass es die Aufgabe gibt. Schreibe in deine Meldung also nichts, was das doch verrät.

## `current-user` antwortet 200 statt 404

Die OpenAPI-Beschreibung sagt für `GET /gui/api/v1/app/current-user` einen Status 404, wenn niemand
angemeldet ist. Der Code antwortet mit 200 und einem leeren Körper. Prüfe also auf einen leeren
Körper und nicht auf einen Status.

## Der Refresh-Token hängt in der Luft

Das Kopffeld `x-refresh-token` steht in der Beschreibung des Endpunkts, und der Controller nimmt es
als Parameter an. Benutzt wird es im Körper der Methode nicht, und nichts im Cockpit setzt dieses
Kopffeld in einer Antwort.

Im mitgelieferten UI ist der ganze Weg deshalb tot: es würde den Wert speichern und nach einer 401
nochmal probieren, bekommt aber nie einen. Das ist ein Haken für ein eigenes Cockpit mit einem
eigenen Identity-Provider und kein Verhalten, auf das du bauen kannst. Was ein Server mit dem
Kopffeld tun soll, steht nirgends.

## Ein Suchmuster kommt ungefiltert in die Datenbank

`query` einer `SearchQuery` wird als regulärer Ausdruck verwendet, ohne Maskierung und ohne
Verankerung. Eine Eingabe mit `.` oder `*` sucht etwas anderes, als der Benutzer meint, und ein
aufwendiges Muster kostet den Server Zeit. Maskiere, was du aus einem Eingabefeld weitergibst.

## Zwei Filter auf einem Pfad sind ein Fehler

Die Bedingungen einer Abfrage werden mit UND verknüpft, aber zwei Bedingungen auf demselben Feld
nimmt die Abfrage nicht an. Setze beim Filtern also immer den alten Eintrag für diesen Pfad zurück,
bevor du den neuen hinzufügst. So macht es das mitgelieferte UI.

## `businessIds` verschwindet bei der Aktualisierung

`businessIds` wirkt nur auf `POST /workflow`. `WorkflowsUpdateRequest` hat das Feld nicht, und die
Aktualisierung übergibt an dieser Stelle nichts. Eine Liste, die nach Geschäftskennungen gefiltert
ist, bekommt bei der Aktualisierung also auch Fälle, die nicht dazugehören. Filtere danach selbst
nach, wenn du diesen Filter benutzt.

## `OpenTasksWithFollowUp` filtert nichts

Von den sechs Modi einer Aufgabenliste setzt dieser keine Bedingung auf die Wiedervorlage. Er wirkt
heute wie `OpenTasks`. Der Name verspricht mehr, als der Code tut.

Wer offene Aufgaben mit einer Wiedervorlage in der Zukunft will, nimmt `OpenTaskOnlyFollowUp`. Wer
die ohne Wiedervorlage oder mit einer fälligen will, nimmt `OpenTasksWithoutFollowUp`.

Lücke in dieser Beschreibung: ob der Modus so gemeint ist, sagt niemand. Er steht in der
OpenAPI-Beschreibung, im Enum des Servers und in keinem einzigen `if`, und kein Kommentar nennt die
Bedingung, die gemeint war. Nimm ihn also nicht, bevor das entschieden ist.

## Die Vorschläge sind nicht die häufigsten

Die Antwort auf eine KWIC-Abfrage hat höchstens 21 Einträge, und die Grenze greift in der Datenbank,
bevor sortiert wird. Es sind also die ersten 21, die gefunden werden, und nicht die 21 häufigsten.
Sortiert werden sie danach aufsteigend nach Anzahl, das seltenste Wort zuerst.

Das mitgelieferte UI zeigt bei 21 Einträgen deshalb einen Hinweis "mehr als zwanzig Treffer" statt
der Liste. Tu etwas Ähnliches, sonst zeigst du dem Benutzer eine beliebige Auswahl als wäre sie die
beste.

Die Anzahl zählt Vorkommen eines Wortes, nicht Zeilen, und sie zählt nur innerhalb der Zeilen, die
den übrigen Bedingungen entsprechen.

## Der Ereignisstrom geht an alle

Jeder verbundene Browser bekommt jedes Ereignis, auch zu einer Aufgabe, die dieser Benutzer nicht
sehen darf. Der Filter auf die Gruppen ist im Code vorgesehen und abgeschaltet.

Zwei Folgen: eine Kennung aus dem Strom beweist nicht, dass es die Zeile für diesen Benutzer gibt,
und ein Ereignis ist kein Geheimnis. Zeige nie etwas direkt aus einem Ereignis an. Frage nach, und
nimm eine leere Antwort als Antwort.

## `filterable` wird nicht gelesen

Das Feld steht in jeder Spalte und wird überall gesetzt. Kein Code liest es. Ob eine Spalte filterbar
ist, entscheidet dein UI, und wenn du das Feld auswertest, bist du der erste.

## Die Sprache ist festgenagelt

Das mitgelieferte UI setzt `de` als Sprache und `en` als Rückfall, beim Start, in einer Zeile Code.
Eine Sprachwahl für den Benutzer gibt es nicht, und der Server hat zwar einen Schlüssel
`business-cockpit.default-locale`, der aber die Sprache des UIs nicht setzt.

Für dein UI heißt das: du entscheidest, wie die Sprache gewählt wird, und du musst sie überall
hinreichen, wo ein Text je Sprache gezeichnet wird. Es gibt dafür heute kein Vorbild, dem du folgen
könntest.

Lücke in dieser Beschreibung: ob eine Sprachwahl geplant ist, steht nirgends. Baust du eine, ist
sie deine Sache allein, und eine spätere Sprachwahl des Cockpits muss nicht so aussehen wie deine.
