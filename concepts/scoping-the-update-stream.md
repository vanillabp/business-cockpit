# Wer den Update-Strom angeht

Dieser Text ist bis zum Review deutsch, weil Stephan ihn selbst liest. Nach dem Review wird
daraus die englische Beschreibung in der Wiki-Seite `Security`.

Konzept zu Story 1251, Arbeitspaket 1. Es beantwortet die Fragen aus dem Prompt gegen den
gelesenen Code. Jede Aussage nennt Datei und Zeile, damit beim Umsetzen nachzulesen ist, worauf
sie sich stützt. Zeilennummern stehen für den Stand von `origin/main` am 2026-10-02.

## Inhalt

- [Was heute passiert](#was-heute-passiert)
- [Was ein Ereignis heute trägt](#was-ein-ereignis-heute-trägt)
- [Was der Browser mit einem Ereignis macht](#was-der-browser-mit-einem-ereignis-macht)
- [Woher der Zustand vor der Änderung kommt](#woher-der-zustand-vor-der-änderung-kommt)
- [Der Begriff aus Story 250](#der-begriff-aus-story-250)
- [Vorschlag](#vorschlag)
- [Was der Server dem Browser schon gegeben hat](#was-der-server-dem-browser-schon-gegeben-hat)
- [Was aus den Fragen des Prompts folgt](#was-aus-den-fragen-des-prompts-folgt)
- [Offene Fragen mit Default](#offene-fragen-mit-default)

## Was heute passiert

`LoginApiController.updateClients(GuiEvent)` legt jedes Ereignis in jeden offenen Strom
(`business-cockpit/src/main/java/io/vanillabp/cockpit/gui/api/v1/LoginApiController.java`,
Zeilen 131 bis 142). Der Filter steht in Zeile 139 als Kommentar.

Einschalten allein hilft nicht. `GuiEvent.matchesTargetGroups` vergleicht die Sammlung mit je
einer Gruppe (`gui/api/v1/GuiEvent.java`, Zeilen 40 bis 43), was nie wahr wird. Übrig bleibt der
Fall `targetGroups == null`, der alles durchlässt. Dieselbe Methode steht in
`util/events/NotificationEvent.java`, Zeilen 56 bis 59, dort richtig mit `targetGroup::equals`.
Es ist also eine Kopie, die auseinandergelaufen ist, und das ist der Grund, warum die kaputte
Fassung nie aufgefallen ist: niemand ruft sie auf.

## Was ein Ereignis heute trägt

Ein `GuiEvent` trägt drei Dinge (`gui/api/v1/GuiEvent.java`, Zeilen 10 bis 23): die Quelle als
Zeichenkette, die Zielgruppen und den Nutzinhalt.

Der Nutzinhalt ist schmal. Die Task-Seite setzt `name`, `id` und `type`
(`tasklist/api/GuiNotificationService.java`, Zeilen 21 bis 28), die Workflow-Seite dasselbe
(`workflowlist/api/GuiNotificationService.java`, Zeilen 21 bis 28). Mehr als die Kennung und die
Art der Änderung steht nicht drin.

Die Zielgruppen kommen aus dem Dokument. `UserTask.getTargetGroups` (`tasklist/model/UserTask.java`,
Zeilen 199 bis 210) ruft `CandidatesAware.getTargetGroups` (`util/candidates/CandidatesAware.java`,
Zeilen 15 bis 31) und legt den Zuständigen dazu. Herauskommt eine flache Menge aus
Kandidatengruppen, Kandidatenbenutzern mit dem Vorsatz `USER_` und dem Zuständigen mit demselben
Vorsatz. Eine leere Menge wird zu `null`, und `null` heißt "geht alle an".

Diese flache Menge ist weniger, als die Sichtbarkeit der Liste kennt. Sie enthält nicht:

- `admittedUsers` (`tasklist/model/UserTask.java`, Zeile 138). Wer eine Task aus einem Grund des
  Geschäfts sehen darf, steht in keiner Zielgruppe. Ein Filter auf Zielgruppen würde diese Person
  nie wecken, und ihre Liste würde stillschweigend aufhören, sich zu aktualisieren.
- `excludedCandidateUsers`, also das Vier-Augen-Prinzip. Ein Ausgeschlossener steckt als Kandidat
  weiter in den Zielgruppen und würde geweckt.
- Die Regel für Tasks, die niemanden nennen (`includeDanglingTasks`), und die Umkehrung
  `notInAssignees`, mit der die Liste der noch freien Arbeit gebaut wird.

Auf der Workflow-Seite trägt ein Ereignis gar keine Zielgruppen.
`WorkflowChangedNotification.build` übergibt `null` (`workflowlist/WorkflowChangedNotification.java`,
Zeilen 51 bis 56). Jedes Workflow-Ereignis ist also schon im Bau unbeschränkt, und die
`accessibleToUsers` und `accessibleToGroups` des Dokuments (`workflowlist/model/Workflow.java`,
Zeilen 97 bis 114) werden nie gelesen.

Am Strom hängt heute eine Liste von Gruppen, die beim Abonnieren eingefroren wird:
`UpdateEmitter.groups(user.getAuthorities())` (`gui/api/v1/LoginApiController.java`, Zeilen 64
bis 68). Die Kennung der Person ist darin schon enthalten, verkleidet als Gruppe: `JwtMapper`
legt `USER_` plus Subjekt in die Authorities (`commons/.../security/jwt/JwtMapper.java`, Zeile 57).

## Was der Browser mit einem Ereignis macht

Das ist für die Sicherheitsfrage der wichtigste Punkt, und er steht im Prompt nicht. Der Browser
benutzt ein Ereignis nur als Wecker. `ListOfTasks.tsx` nimmt die Kennungen aus dem Ereignis und
ruft damit das Nachladen (`ui/bc-ui/src/components/ListOfTasks.tsx`, Zeilen 1088 bis 1096). Das
Nachladen schickt die Kennungen, die der Browser schon hat, und übernimmt die Antwort
(`ui/bc-ui/src/components/SearchableAndSortableUpdatingList.tsx`, Zeilen 97 bis 143). Was in der
Antwort fehlt, bekommt `ListItemStatus.REMOVED_FROM_LIST`, Zeile 132.

Die Antwort selbst ist geprüft. `getUserTasksUpdate` fragt
`userTasksVisibleTo(currentUser)` (`tasklist/api/v1/AbstractUserTaskListGuiApiController.java`,
Zeilen 150 bis 173), und `UserTaskService.getUserTasksUpdated` baut daraus die Abfrage
(`tasklist/UserTaskService.java`, Zeilen 684 bis 735).

Daraus folgt zweierlei. Was heute durchsickert, ist nicht der Inhalt einer fremden Task, sondern
ihre Kennung und die Tatsache, dass es sie gibt. Und das Verschwinden einer Task aus einer Liste
ist schon gebaut: es braucht keine Meldung mit Inhalt, es braucht nur einen Wecker und eine
Antwort, in der die Task fehlt.

## Woher der Zustand vor der Änderung kommt

Heute nirgendwo her. Der Change-Stream wird ohne Vorbild abonniert:
`fullDocumentBeforeChangeLookup(FullDocumentBeforeChange.OFF)`
(`commons/src/main/java/io/vanillabp/cockpit/commons/mongo/changestreams/ChangeStreamUtils.java`,
Zeile 104). Das Vorbild einzuschalten kostet mehr als eine Zeile: MongoDB will
`changeStreamPreAndPostImages` an der Sammlung, und der Cosmos-Modus derselben Klasse (Zeilen 87
bis 95) kann es nicht.

Es ist aber auch nicht nötig. Der Vorzustand wird nur für eine einzige Frage gebraucht: "Wem muss
ich sagen, dass die Task weg ist?" Diese Frage beantwortet der Server aus dem, was er dieser
Person schon ausgeliefert hat, und das weiß er von jeder Antwort auf eine Liste. Das ist die
Antwort, die der Vorschlag unten benutzt, und sie trägt nur, wenn das Gedächtnis mehr kennt als
die Wecker des Stroms; der Abschnitt
[Was der Server dem Browser schon gegeben hat](#was-der-server-dem-browser-schon-gegeben-hat)
sagt, warum.

## Der Begriff aus Story 250

Story 250 (`prompts-businesscockpit/done/250-check-single-entity-access.md`, gemergt als
business-cockpit#151) hat den Begriff eingeführt, und er heißt Sichtbarkeit:

- `tasklist/UserTaskVisibility.java` als Record mit sieben Angaben und vier benannten Sichten,
  darunter `everythingTheUserMayWorkOn(UserDetails)`.
- `workflowlist/WorkflowVisibility.java` und `workflowmodules/WorkflowModuleVisibility.java`
  für die anderen beiden Listen.
- Die Basisklasse fragt eine Ansicht genau eine Sache:
  `userTasksVisibleTo(UserDetails)` (`tasklist/api/v1/AbstractUserTaskListGuiApiController.java`,
  Zeilen 70 bis 75).

Dieser Begriff entscheidet auch hier. `GuiEvent.targetGroups` ist die zweite Regel daneben, und
sie ist schwächer als die erste, siehe die drei Lücken oben. Sie fällt weg, statt repariert zu
werden.

## Vorschlag

1. Ein Ereignis ist ein Wecker. Es trägt die Art der Entität, ihre Kennung und die Art der
   Änderung, und keine Zielgruppen. `GuiEvent.targetGroups` und `GuiEvent.matchesTargetGroups`
   entfallen. `NotificationEvent.targetGroups` wird heute nur an zwei Stellen gelesen, in den
   beiden `GuiNotificationService` (`tasklist/api/GuiNotificationService.java` und
   `workflowlist/api/GuiNotificationService.java`, je Zeile 24), und beide geben es nur an das
   `GuiEvent` weiter. Mit dem Ereignis verliert das Feld also seinen letzten Leser. Ob es trotzdem
   stehen bleibt, entscheidet, wer die Benachrichtigungen anfasst; der Update-Strom braucht es
   nicht.
2. Ein Strom trägt die Sichtbarkeit seiner Person, nicht eine Liste von Gruppen. Er merkt sich
   die `UserDetails` und baut daraus dieselbe Sicht, die die weiteste Liste benutzt, also
   `UserTaskVisibility.everythingTheUserMayWorkOn` und
   `WorkflowVisibility.workflowsAddressedTo`.
3. Gefiltert wird im Sammeltakt, nicht je Ereignis. Der Takt hat die Kennungen aller Ereignisse
   seit dem letzten Mal. Eine Abfrage je Strom und Takt, die nur Kennungen liest, sagt, welche
   davon die Person sehen darf. Die Abfrage ist dieselbe wie in `getUserTasksUpdated`
   (`tasklist/UserTaskService.java`, Zeilen 702 bis 726): `buildUserTasksCriteria(visibility, …)`
   plus `_id in (…)` und `query.fields().include("_id")`.
4. Eine Kennung, die die Abfrage nicht hergibt, geht trotzdem durch, wenn der Browser dieser
   Person sie schon hat. Das ist der Entzug, und er verrät nichts: die Kennung ist dem Browser
   bekannt, und das Nachladen antwortet mit Abwesenheit. Alles andere fällt weg. Woher der Server
   weiß, was ein Browser hält, steht im Abschnitt
   [Was der Server dem Browser schon gegeben hat](#was-der-server-dem-browser-schon-gegeben-hat).
5. Der Zugang braucht keinen Sonderfall. Wird eine Task jemandem zugewiesen, nennt die neue
   Sichtbarkeit diese Person, die Abfrage gibt die Kennung her, und der Wecker geht raus.

## Was der Server dem Browser schon gegeben hat

Der Entzug hängt daran, dass der Server weiß, welche Kennungen ein Browser zeigt. Die erste
Fassung dieses Konzepts hat dafür gesagt: was der Strom geliefert hat. Das reicht nicht, und die
Lücke ist nicht klein.

Eine Liste wird nicht aus dem Strom gefüllt, sondern aus der ersten Abfrage. Wer eine Task beim
Laden der Seite bekommt und zu der nie ein Wecker nötig war, hat sie im Browser, ohne dass der
Strom je ihre Kennung ausgeliefert hätte. Wird diese Task der Person entzogen, sagt die Abfrage
des Takts "nicht sichtbar" und das Gedächtnis "nie geliefert". Dann geht kein Wecker raus, der
Browser lädt nicht nach, und die Task bleibt in der Liste stehen, obwohl die Person sie nicht mehr
sehen darf. Genau dieser Fall ist der Grund, warum man den Zustand vor der Änderung vermisst.

Der Ausweg braucht ihn nicht, weil der Server es ohnehin erfährt. Beide Wege, auf denen eine
Liste zu ihren Einträgen kommt, laufen durch denselben Controller und durch denselben
Aufruf von `mapper.toApi(userTasks, …)`:

- `getUserTasks` beim Laden der Seite
  (`tasklist/api/v1/AbstractUserTaskListGuiApiController.java`, Zeilen 101 bis 123). Der Server
  weiß, welche Kennungen er gerade ausgeliefert hat.
- `getUserTasksUpdate` bei jedem Nachladen (Zeilen 150 bis 172). Die Anfrage trägt zusätzlich
  `getKnownUserTasksIds()`, also die Kennungen, die der Browser hält, bevor die Antwort sie
  berichtigt.

Das Gedächtnis wird damit an einer Stelle gefüttert und nicht an zwei: der Strom merkt sich jede
Kennung, die der Server dieser Person in einer Liste beantwortet hat, und nicht nur die, die er
selbst geweckt hat. Der Strom der Workflow-Seite bekommt dasselbe über
`AbstractWorkflowListGuiApiController`.

Das Gedächtnis gehört der angemeldeten Person und nicht einem einzelnen Tab: eine Liste in einem zweiten Tab zeigt dieselben Tasks, und der Strom ist einer je
Anmeldung. Und es wird vom Nachladen auch dann gefüttert, wenn das Nachladen aus einem anderen
Grund lief, etwa weil der Benutzer gesucht oder sortiert hat.

Was bleibt, ist ein Fall, den dieser Weg nicht erreicht: ein Browser, dessen Liste aus einem
Stand kommt, den ihm niemand mehr berichtigt, weil zwischen seinem letzten Nachladen und dem
Entzug kein Takt lief, in dem er etwas Gesammeltes bekam. Praktisch heißt das, der Entzug wirkt
bei seinem nächsten Nachladen, und nachgeladen wird bei jedem Wecker, beim Suchen, beim Sortieren
und beim Neuladen der Seite. Wer eine Zusage ohne dieses "praktisch" will, braucht das Vorbild aus
der Datenbank, und was das kostet, steht oben.

Damit sind die drei Fälle aus der Abnahme der Story abgedeckt, ohne Vorbild aus der Datenbank und
ohne eine zweite Sichtbarkeitsregel.

## Was aus den Fragen des Prompts folgt

Was müsste ein Ereignis tragen? Weniger als heute, nicht mehr: Art, Kennung, Änderungsart. Die
Entscheidung, wen es angeht, gehört nicht ins Ereignis, weil sie je Strom anders ausfällt.

Woher kommt der Zustand vorher? Aus dem, was der Server dieser Person schon beantwortet hat, nicht
aus dem Change-Stream.

Reichen Gruppen, oder muss die Kennung der Person an den Emitter? Die Kennung ist schon da, als
`USER_<id>` in den Authorities (`JwtMapper`, Zeile 57). Darauf zu bauen heißt aber, eine
Zeichenkette mit Vorsatz als Datenmodell zu benutzen. Der Strom trägt die `UserDetails`, und die
Sichtbarkeit nimmt Kennung und Gruppen getrennt, so wie
`UserTaskVisibility.everythingTheUserMayWorkOn(String, Collection)` es schon tut.

Was ist die Antwort auf eine Änderung der Sichtbarkeit selbst? Zugang über die Abfrage, Entzug
über das Gedächtnis. Der Entzug nennt nur eine Kennung, die der Browser schon hat.

Wie verhält sich das zu Story 250? Es benutzt deren Begriff und stellt keinen zweiten daneben. Was
der Filter fragt, ist dieselbe Frage, die die Liste stellt.

Gilt dasselbe für den Strom der Workflow-Seite? Ja, mit `WorkflowVisibility`. Dort ist der Zustand
heute schlechter, weil das Ereignis schon im Bau keine Zielgruppen bekommt.

## Offene Fragen mit Default

1. Wie lange erinnert ein Strom die Kennungen, die er geliefert hat?
   Default: bis 10.000 Kennungen je Strom, danach schickt der Server ein Ereignis, das ein
   vollständiges Neuladen auslöst, und leert das Gedächtnis. Ein Strom lebt so lange wie ein
   offener Tab, also braucht die Menge eine Obergrenze.
2. Wie wirken Vertretungen?
   Default: im Code gibt es sie nicht. Eine Suche nach `substitut` und `mapping` findet nur
   Mapstruct und Konfiguration. Der TODO in Zeile 138 beschreibt also etwas Künftiges. Wenn es
   kommt, wird die Vertretung beim Auflösen der Sichtbarkeit berücksichtigt und nicht als zweite
   Liste am Strom gehalten. Weil der Strom seine Sichtbarkeit je Takt neu auflöst, wirkt eine
   Vertretung dann ab dem nächsten Takt und nicht erst beim nächsten Anmelden. Heute friert
   Zeile 66 die Gruppen beim Abonnieren ein, und auch eine Änderung an den Gruppen wirkt erst
   beim neuen Anmelden.
3. Eine Abfrage je Takt und Strom, ist das zu teuer?
   Default: ja, es wird gemessen, bevor es eingeschaltet wird, und nein, es wird nicht vorher
   optimiert. Hundert offene Tabs und ein Takt von 250 Millisekunden sind 400 Abfragen in der
   Sekunde, die nur Kennungen lesen und nur dann laufen, wenn für diesen Strom etwas gesammelt
   wurde. Ohne Änderungen läuft keine.
4. Welche Sichtbarkeit trägt ein Strom, wenn ein Browser mehrere Ansichten offen hat?
   Default: die weiteste der angemeldeten Person. Der Strom ist einer je Anmeldung, nicht einer je
   Liste, und die Ansicht filtert beim Nachladen ohnehin selbst. Ein Wecker zu viel kostet ein
   Nachladen, ein Wecker zu wenig lässt eine Liste veralten.
5. Soll das Vorbild des Change-Streams eingeschaltet werden?
   Default: nein. Der Entzug braucht es nicht, sobald das Gedächtnis des Stroms von den Antworten
   des Servers gefüttert wird, und der Cosmos-Modus kann es nicht. Dazu kommt, dass eine Änderung
   heute überhaupt kein Dokument mitbringt, siehe Zeile `1404`; wer das Vorbild einschaltet, löst
   zuerst diese Zeile und bezahlt dann an jeder Sammlung die Vor- und Nachbilder.
6. Was passiert mit Ereignissen, die zu keiner Entität gehören?
   Default: sie bleiben ungefiltert. Der Ping (`LoginApiController`, Zeilen 147 bis 177) und die
   Registrierung eines Workflow-Moduls sagen nichts über einen Fall.
7. Wird der Filter hinter einem Schalter eingeschaltet?
   Default: nein. Ein Schalter, der die alte Verteilung an alle erlaubt, ist ein Schalter, der
   Titel und Kennungen preisgibt, und niemand würde ihn umlegen. Die Story ist im Release-Gate
   genau deshalb geführt: nachträglich enger zu schneiden nimmt einer Anwendung etwas weg, das
   sie schon benutzt.
