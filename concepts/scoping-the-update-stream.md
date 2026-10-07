# Wer den Update-Strom angeht

Dieser Text ist bis zum Review deutsch, weil Stephan ihn selbst liest. Nach dem Review wird
daraus die englische Beschreibung in der Wiki-Seite `Security`.

Das Review ist am 2026-10-06 erfolgt, siehe [Stephans Review](#stephans-review-2026-10-06). Nach
der Messung hat Stephan am selben Tag Variante b) gewählt, siehe
[Entscheidung: Variante b)](#entscheidung-variante-b). Die englische Beschreibung steht seitdem in
der Wiki-Seite `Security`, Abschnitt "Who gets which live update". Dieser Text bleibt deutsch, als
Weg zur Entscheidung.

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
- [Was ein Ereignis später noch tragen muss](#was-ein-ereignis-später-noch-tragen-muss)
- [Was aus den Fragen des Prompts folgt](#was-aus-den-fragen-des-prompts-folgt)
- [Offene Fragen mit Default](#offene-fragen-mit-default)
- [Stephans Review, 2026-10-06](#stephans-review-2026-10-06)
- [Variante a) und b)](#variante-a-und-b)
- [Messung Variante a)](#messung-variante-a)
- [Entscheidung: Variante b)](#entscheidung-variante-b)
- [Messung Variante b)](#messung-variante-b)

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
   Änderung, und keine Zielgruppen. Die Art der Änderung ist dabei mehr als Beiwerk, siehe
   [Was ein Ereignis später noch tragen muss](#was-ein-ereignis-später-noch-tragen-muss). `GuiEvent.targetGroups` und `GuiEvent.matchesTargetGroups`
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

   Nachtrag beim Umsetzen: dazu kommt jede Sicht, mit der eine Ansicht dieser Person tatsächlich
   geantwortet hat. Der Grund ist die Workflow-Seite. `AbstractWorkflowListGuiApiController` sagt
   selbst, dass ein Cockpit für ein Support-Team jeden Workflow in einer Liste zeigen darf, also mit
   `WorkflowVisibility.everyWorkflow()`. Mit `workflowsAddressedTo` allein bekäme dieses Team für
   neue Workflows nie einen Wecker, und seine Liste sähe aus wie hängengeblieben. Die Sicht kommt
   an derselben Stelle zum Strom wie die Kennungen für das Gedächtnis, also ohne neue Regel: die
   Ansicht hat sie ohnehin gerade benutzt.
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

Die erste Fassung hat hier gesagt, der Strom sei einer je Anmeldung. Das stimmt nicht. Jedes
Abonnieren bekommt seinen eigenen `SseEmitter` (`gui/api/v1/LoginApiController.java`, Zeile 63),
und jeder Tab abonniert für sich. Es gibt also einen Strom je Tab. Bei 250 bis 300 gleichzeitigen
Benutzern sind das rund 450 Ströme.

Damit stellt sich die Frage, wem das Gedächtnis gehört: dem Strom oder der Person. Entschieden ist:
das Gedächtnis liegt am Strom, und es wird für alle Ströme derselben Person gefüttert.

- Am Strom, weil es dann mit dem Strom stirbt und keine eigene Aufräumregel braucht. Die
  Obergrenze gilt je Strom, also je Tab, und damit ist der Speicher je Tab begrenzt. Ein
  Gedächtnis je Person bräuchte eine Lebensdauer über den letzten Strom hinaus, sonst verliert
  es beim Wiederverbinden alles.
- Für alle Ströme der Person gefüttert, weil die Anfrage einer Liste nicht sagt, aus welchem Tab
  sie kommt. Das zu ändern hieße, die Kennung des Stroms in jede Anfrage der Oberfläche zu legen,
  also API und Oberfläche anzufassen. Der Preis: ein Tab merkt sich auch, was ein anderer Tab
  derselben Person zeigt, und bekommt dafür einen Entzug mit. Das verrät nichts, die Person hat die
  Task ja gesehen. Es kostet ein Nachladen in einem Tab, das nichts ändert.

Ein neuer Strom beginnt mit leerem Gedächtnis. Das passiert beim Laden der Seite, und dort kann die
erste Liste schon beantwortet sein, bevor der Strom steht. Es passiert auch beim Wiederverbinden,
und dann hält der Browser eine ganze Liste, von der der neue Strom nichts weiß. Deshalb schickt der
Server direkt nach dem Abonnieren je Art ein Ereignis zum Neuladen, siehe Frage 1. Die Liste lädt
nach, und dieses Nachladen füttert das Gedächtnis des neuen Stroms. Nebenbei holt es auch die
Änderungen nach, die zwischen Abbruch und Wiederverbinden niemand zugestellt hat.

Das Gedächtnis wird vom Nachladen auch dann gefüttert, wenn das Nachladen aus einem anderen Grund
lief, etwa weil der Benutzer gesucht oder sortiert hat.

Was bleibt, ist ein Fall, den dieser Weg nicht erreicht: ein Browser, dessen Liste aus einem
Stand kommt, den ihm niemand mehr berichtigt, weil zwischen seinem letzten Nachladen und dem
Entzug kein Takt lief, in dem er etwas Gesammeltes bekam. Praktisch heißt das, der Entzug wirkt
bei seinem nächsten Nachladen, und nachgeladen wird bei jedem Wecker, beim Suchen, beim Sortieren
und beim Neuladen der Seite. Wer eine Zusage ohne dieses "praktisch" will, braucht das Vorbild aus
der Datenbank, und was das kostet, steht oben.

Damit sind die drei Fälle aus der Abnahme der Story abgedeckt, ohne Vorbild aus der Datenbank und
ohne eine zweite Sichtbarkeitsregel.

## Was ein Ereignis später noch tragen muss

Stephan hat beim Review dieses Konzepts einen Punkt nachgetragen, der über den Filter hinausgeht
und eine eigene Zeile bekommen hat, `1410`. Er gehört hierher, weil er den Schnitt des Ereignisses
betrifft.

Ein Wecker sagt, dass sich etwas geändert hat. Er sagt nicht, WAS passiert ist, und eine
Oberfläche braucht genau das: wer sich eine Aufgabe zuweist und damit jemand anderen als
Bearbeiter abmeldet, soll bei dem anderen eine Meldung auslösen, in der Art "'yyyy' hat die
Aufgabe 'abc' von Ihnen übernommen", mit der Aufgabe als Link. Je Komponente soll auf verschiedene
Ereignisse verschieden reagiert werden können.

Aus den Daten allein ist das oft nicht zu holen. Dass ein Bearbeiter von A auf B gewechselt ist,
steht nach der Änderung nicht mehr im Dokument, und ein Vorbild aus der Datenbank gibt es nicht,
siehe oben. Stephans Entwurf dafür: eine eigene Sammlung führt die Änderungshistorie, ein Eintrag
nennt Objekt, Kennung und Aktion, das Objekt verweist auf den Eintrag, und die Aktion übergibt
kein fertiges Satzstück, sondern ein Objekt als Template-Kontext. Gerendert wird erst beim
Zustellen, in der Sprache dessen, der den Strom hält.

Für dieses Konzept folgt daraus nur eines, und das ist der Grund, warum es hier steht: der
Schnitt des Ereignisses bleibt offen. Die Art der Änderung gehört ins Ereignis, nicht nur die
Kennung, und neben ihr bleibt der Platz für den Verweis auf einen Eintrag der Historie frei. Beides
kommt additiv dazu, bricht also keine Oberfläche, und die Beschreibung aus `1238` soll sagen, dass
dieser Platz frei ist, statt ihn zu verschweigen. Dass ein Strom die Sprache seines Benutzers
kennt, fällt dabei mit ab: er trägt seit Punkt 2 die `UserDetails`, und `User.locale` steht darin.

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
   Default: die weiteste der angemeldeten Person. Der Strom ist einer je Tab, nicht einer je
   Liste, und in einem Tab können mehrere Listen hängen. Die Ansicht filtert beim Nachladen ohnehin
   selbst. Ein Wecker zu viel kostet ein Nachladen, ein Wecker zu wenig lässt eine Liste veralten.
   (Korrigiert nach dem Review: die erste Fassung sagte "einer je Anmeldung".)
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

## Stephans Review, 2026-10-06

Stephan hat die offenen Fragen am 2026-10-06 beantwortet. Was hier steht, gilt vor den Defaults
oben.

### Frage 1: Obergrenze des Gedächtnisses

2.000 Kennungen je Strom, konfigurierbar. Wer darüber kommt, bekommt ein Ereignis zum
vollständigen Neuladen. Der Grund für die kleinere Zahl ist der Strom je Tab: bei 250 bis 300
gleichzeitigen Benutzern sind es rund 450 Ströme. 10.000 Kennungen wären im schlimmsten Fall rund
540 MB, 2.000 rund 110 MB, der Normalfall rund 25 bis 30 MB. Das waren Schätzungen; die Messung
unten setzt Zahlen dagegen.

Umgesetzt heißt das:

- Der Schlüssel ist `business-cockpit.gui-sse.max-known-ids-per-stream`, Vorgabe 2.000. Er zählt
  Task- und Workflow-Kennungen zusammen, weil beide im selben Strom hängen.
- Das Ereignis zum Neuladen ist ein gewöhnlicher Wecker der Art (`UserTask` oder `Workflow`) mit
  dem Typ `RELOAD` und einer leeren Kennung. Die Oberfläche braucht dafür nichts Neues. Jeder
  Wecker lässt die Liste die ganze sichtbare Strecke nachladen
  (`ui/bc-ui/src/components/SearchableAndSortableUpdatingList.tsx`, `reloadData`), und eine
  leere Kennung nimmt keine Task davon aus.
- Beim Überlauf behält der Strom die Kennungen der Antwort, die ihn ausgelöst hat, und vergisst
  den Rest. Neu geladen wird nur, wenn dabei etwas verloren ging. Ohne diese Regel würde eine
  einzelne Antwort über der Grenze ein Neuladen auslösen, dessen Antwort wieder über der Grenze
  liegt, und so fort.

Wie viele Kennungen hat ein Benutzer realistisch? Grundlage ist der Kunde, auf den die Zahlen
zurückgehen: 400 Benutzer am Tag, 250 bis 300 gleichzeitig, rund 5.000 User-Tasks am Tag. Die
Oberfläche lädt 30 Einträge auf einmal und beim Nachladen die gezeigte Strecke plus 60. Ein Tab,
in dem niemand scrollt, hält also 30 bis 90 Kennungen je Liste. Das Gedächtnis wächst darüber
hinaus, weil es jede Kennung behält, die eine Antwort je genannt hat. Nach oben begrenzt ist es durch
die Tasks, die eine Person überhaupt sehen darf. Verteilt sich die Arbeit über Gruppen so, dass
eine Person ein bis zehn Prozent der Tasks sieht, sind das 50 bis 500 neue Kennungen am Tag.
2.000 erreicht damit ein Tab, der mehrere Tage offen bleibt, oder eine Person, die fast alles sieht
und den ganzen Tag lädt. Für sie kostet das zwei bis drei Mal am Tag ein Nachladen. Das ist eine
Schätzung aus den Zahlen des Kunden, nicht gemessen.

### Frage 2: Vertretungen

Die Kunden lösen Vertretungen auf zwei Arten: die Person bekommt mehr Rechte dazu, oder sie
wechselt per JWT in die Rolle der vertretenen Person. Beide wirken beim Auflösen der Rechte, also
dort, wo die `UserDetails` entstehen. Der Strom braucht dafür nichts Eigenes.

Er braucht nur ein Ende. Ein Strom endet, wenn das Token abläuft, mit dem er abonniert wurde.
Bisher lief er mit `NO_SSE_TIMEOUT` so lange wie der Tab, also mit den Rechten vom Zeitpunkt des
Abonnierens. Der Browser verbindet neu und bekommt mit dem neuen Token die frisch aufgelösten
Rechte. Was dabei im Code steht und was nicht, steht im Abschnitt
[Variante a) und b)](#variante-a-und-b) unter "Ende mit dem Token".

### Frage 3 und 5: Kosten der Abfrage, Vorbild des Change-Streams

Abgefragt wird nur, wenn der Change-Stream im Takt etwas gemeldet hat. Das Vorbild bleibt aus.
Zuerst wird Variante a) gebaut und mit 450 Strömen gemessen. Stephan ist unwohl mit einer Last,
die linear mit den Benutzern wächst, und bevorzugt Variante b). Entschieden wird nach der Messung.

### Fragen 4, 6 und 7

Es gelten die Defaults.

## Variante a) und b)

Beide Varianten filtern im selben Takt und mit demselben Gedächtnis. Sie unterscheiden sich nur in
der Frage, welche der gesammelten Kennungen ein Strom sehen darf.

Variante a) stellt eine Abfrage je Strom und Art, wenn im Takt etwas gesammelt wurde. Die Abfrage
nimmt die Sichten des Stroms, die gesammelten Kennungen als `_id in (…)` und liest nur `_id`.
Die Regel ist die der Liste, `UserTaskService.buildUserTasksCriteria` und
`WorkflowlistService.buildWorkflowlistCriteria`. Es gibt keine zweite. Die Last wächst mit der
Zahl der Ströme: 450 Ströme sind 450 Abfragen je Art in jedem Takt, in dem etwas geschah.

Variante b) stellt eine Abfrage je Takt und Art für alle Ströme. Sie liest die gesammelten
Dokumente mit den Feldern, von denen die Sichtbarkeit abhängt. Je Strom wird dann im Speicher
entschieden. Die Last auf MongoDB hängt nicht mehr an der Zahl der Ströme. Der Preis ist eine
zweite Fassung der Regel in Java neben der Abfrage, und ein Test, der beide gegeneinander prüft.

Zuerst gebaut und gemessen war Variante a). Gebaut ist heute Variante b), siehe
[Entscheidung: Variante b)](#entscheidung-variante-b). Die Entscheidung "welche Kennungen darf
dieser Strom sehen" liegt hinter einer kleinen Schnittstelle, `UpdateStreamAudience`. Sie bekommt
alle Ströme und die gesammelten Kennungen eines Takts auf einmal und antwortet je Strom. Der Takt,
das Gedächtnis und die Obergrenze sind in beiden Varianten dieselben.

Der Takt des Filters hat ein eigenes Intervall, `business-cockpit.gui-sse.filtering-interval`.
Die Vorgabe war bei Variante a) 1.000 Millisekunden und ist seit Variante b) 250 Millisekunden,
siehe [Messung Variante b)](#messung-variante-b). Der bisherige Takt, `collecting-interval`, stellt weiter zu, was
gefiltert ist. Gefiltert wird je Takt nur einmal, nicht für jeden der vier Takte des Zustellens.

### Ende mit dem Token

Die Ablaufzeit liest der Strom beim Abonnieren aus der Anmeldung. Das Token des Cockpits ist ein
`JwtAuthenticationToken`, das ein `Jwt` trägt; ein Spring-Security-Token für OAuth trägt seine
Ablaufzeit ebenso. Wo keine Ablaufzeit zu finden ist, lebt der Strom wie bisher so lange wie der
Tab. Die Lebensdauer des Cockpit-Tokens ist `business-cockpit.jwt.cookie.expires-duration`,
Vorgabe zwölf Stunden. Das Cookie läuft zur selben Zeit ab wie das Token.

Was der Browser dann tut, steht in `ui/bc-shared/src/components/SseProvider.tsx`. Schließt der
Server den Strom, wirft `onclose`, und der Browser verbindet nach 15 Sekunden neu. Ein Aufruf der
REST-API in dieser Zeit verbindet sofort neu (`wakeupSseCallback`). Das neue Abonnieren ist ein
gewöhnlicher `fetch` an dieselbe Adresse, und der Browser legt das Cookie bei, das er in diesem
Moment hat. Ein neues Token geht also mit, sobald es im Cookie steht. Wer es dort hineinschreibt,
entscheidet die Anwendung:

- Im Cockpit selbst gibt es keine Verlängerung. Das Cookie entsteht bei der Anmeldung per Basic
  Auth (`WebSecurityConfiguration`, `httpBasic` mit `JwtSecurityContextRepository`), und nach
  zwölf Stunden ist es weg. Der Browser verbindet dann ohne Cookie neu. Hat er die Zugangsdaten
  der Basic Auth noch, schickt er sie mit, die Antwort trägt ein neues Cookie, und der nächste
  Versuch gelingt. Sonst ist die ganze Oberfläche abgemeldet, nicht nur der Strom.
  Nachtrag: seit Story `1450` verlängert das Cockpit das Token, solange jemand arbeitet, bis
  höchstens `business-cockpit.jwt.cookie.max-login-duration` nach der Anmeldung. Der Strom selbst
  verlängert nicht.
- Eine abgeleitete Anwendung, die das Token per OAuth oder per Wechsel der Rolle erneuert, legt
  das neue Token ins Cookie. Der nächste Versuch trägt es.

Eine Lücke bleibt: die bis zu 15 Sekunden zwischen Ende und neuem Strom. Was in dieser Zeit
geschieht, holt das Neuladen nach, das jeder neue Strom zuerst schickt.

## Messung Variante a)

Gemessen am 2026-10-06 im DevContainer (16 GB, 12 Kerne, Docker auf demselben Rechner) mit
`UpdateStreamFilterMeasurement` (`business-cockpit/src/test/java/io/vanillabp/cockpit/gui/api/v1`).
Die Messung läuft nicht im normalen Bau. Aufruf:

```bash
mvn -Pvanillabp-snapshots,java-install -pl business-cockpit -am test \
    -Dsurefire.failIfNoSpecifiedTests=false -Dtest=UpdateStreamFilterMeasurement \
    -Dmeasure.update-streams=true -Djacoco.skip=true
```

Das Ergebnis steht danach in `business-cockpit/target/update-stream-measurement.md`.
Bestand, Raten und Takte lassen sich per Systemeigenschaft ändern (`measure.datasets`,
`measure.open-tasks`, `measure.rates`, `measure.intervals`, `measure.seconds-per-run`).

### Aufbau

- MongoDB 8.2 (`ContainerImages.MONGODB`) mit 4 GB Speichergrenze, mit den Indizes, die die
  Changesets des Cockpits an der Sammlung `usertask` anlegen.
- 300 Personen in je zwei bis vier von 30 Gruppen, 450 Ströme (jede zweite Person hat zwei
  Tabs). Jede Liste hat beim Start 90 offene Tasks geladen, und das Gedächtnis kennt sie.
- Tasks mit ein oder zwei Kandidatengruppen, ein Drittel zugewiesen, rund 1,5 KB Details. 4.000
  davon offen, der Rest beendet: einmal 20.000 Tasks, einmal 500.000 Tasks. 500.000 entsprechen
  gut drei Monaten beim Kunden mit 5.000 Tasks am Tag, nicht einem Jahr (1,8 Mio.). Mehr hat
  der Lauf nicht gebraucht, siehe unten.
- Änderungen als Poisson-Prozess an zufälligen offenen Tasks, mit 0,3/s (Schnitt beim Kunden),
  3/s (Spitze, etwa das Zehnfache) und 5, 20, 50/s als Belastungsprobe.
- Der Filtertakt läuft mit `fixedDelay` wie im Cockpit. Jede Zeile lief 30 Sekunden, die mit
  0,3/s 120 Sekunden.

Gemessen sind die Abfragen an MongoDB (Zähler am Treiber), die Dauer eines Takts mit
gesammelten Änderungen, die CPU-Zeit des Test-Prozesses und die CPU-Zeit des MongoDB-Containers
(aus dessen cgroup), je als Anteil eines Kerns.

### Gedächtnis

| Kennungen je Strom | Ströme | Heap     | Bytes je Kennung |
|--------------------|--------|----------|------------------|
| 500                | 450    | 27,2 MB  | 127              |
| 2.000              | 450    | 104,6 MB | 122              |

Kennungen sind UUIDs mit 36 Zeichen. Die Schätzung im Review (110 MB bei 2.000) trifft.

### Filtertakt

| Tasks   | Rate/s | Takt    | Takte mit Änderung | Abfragen/s | Takt p50 | p95    | max    | Server-CPU | MongoDB-CPU |
|---------|--------|---------|--------------------|------------|----------|--------|--------|------------|-------------|
| 20.000  | 0,3    | 250 ms  | 6 %                | 101        | 109 ms   | 200 ms | 340 ms | 6 %        | 2 %         |
| 20.000  | 3      | 250 ms  | 49 %               | 735        | 92 ms    | 105 ms | 107 ms | 13 %       | 8 %         |
| 20.000  | 5      | 250 ms  | 78 %               | 1.080      | 91 ms    | 123 ms | 217 ms | 15 %       | 11 %        |
| 20.000  | 20     | 250 ms  | 100 %              | 1.305      | 91 ms    | 107 ms | 115 ms | 18 %       | 13 %        |
| 20.000  | 50     | 250 ms  | 100 %              | 1.245      | 108 ms   | 125 ms | 176 ms | 20 %       | 16 %        |
| 20.000  | 0,3    | 1000 ms | 22 %               | 97         | 87 ms    | 104 ms | 104 ms | 2 %        | 2 %         |
| 20.000  | 3      | 1000 ms | 93 %               | 375        | 99 ms    | 114 ms | 168 ms | 7 %        | 5 %         |
| 20.000  | 5      | 1000 ms | 100 %              | 405        | 105 ms   | 114 ms | 115 ms | 7 %        | 5 %         |
| 20.000  | 20     | 1000 ms | 100 %              | 404        | 115 ms   | 127 ms | 131 ms | 7 %        | 6 %         |
| 20.000  | 50     | 1000 ms | 100 %              | 390        | 151 ms   | 168 ms | 185 ms | 8 %        | 9 %         |
| 500.000 | 0,3    | 250 ms  | 6 %                | 101        | 94 ms    | 108 ms | 110 ms | 2 %        | 2 %         |
| 500.000 | 3      | 250 ms  | 50 %               | 765        | 85 ms    | 102 ms | 102 ms | 10 %       | 7 %         |
| 500.000 | 5      | 250 ms  | 78 %               | 1.080      | 95 ms    | 107 ms | 111 ms | 15 %       | 11 %        |
| 500.000 | 20     | 250 ms  | 100 %              | 1.288      | 98 ms    | 111 ms | 114 ms | 18 %       | 14 %        |
| 500.000 | 50     | 250 ms  | 100 %              | 1.273      | 102 ms   | 116 ms | 121 ms | 18 %       | 16 %        |
| 500.000 | 0,3    | 1000 ms | 22 %               | 97         | 90 ms    | 105 ms | 106 ms | 2 %        | 2 %         |
| 500.000 | 3      | 1000 ms | 93 %               | 375        | 102 ms   | 122 ms | 235 ms | 6 %        | 5 %         |
| 500.000 | 5      | 1000 ms | 100 %              | 405        | 100 ms   | 112 ms | 115 ms | 7 %        | 5 %         |
| 500.000 | 20     | 1000 ms | 100 %              | 390        | 123 ms   | 136 ms | 138 ms | 7 %        | 7 %         |
| 500.000 | 50     | 1000 ms | 100 %              | 375        | 161 ms   | 187 ms | 202 ms | 7 %        | 9 %         |

Was die Zahlen sagen:

- Ein Takt mit Änderung kostet 450 Abfragen und rund 100 ms, also gut 0,2 ms je Abfrage. Die
  Abfragen laufen nacheinander auf einem Faden.
- Die Größe des Bestands spielt keine Rolle. Die Abfrage beginnt mit `_id in (…)`, MongoDB nimmt
  den Index auf `_id` und prüft die Sichtbarkeit nur an den wenigen gefundenen Dokumenten. 20.000
  und 500.000 Tasks liefern dieselben Zahlen. Das gilt deshalb auch für 1,8 Mio.
- Die Last hängt an der Zahl der Takte mit Änderung, nicht an der Zahl der Änderungen. Ab etwa
  3/s hat beim 1-s-Takt fast jeder Takt etwas, und dann bleibt es bei rund 400 Abfragen/s, ob 5
  oder 50 Änderungen je Sekunde kommen. Beim 250-ms-Takt sind es bis zu 1.300 Abfragen/s.
- Beim Schnitt des Kunden (0,3/s) hat nur jeder fünfte 1-s-Takt etwas, das sind rund 100
  Abfragen/s und 2 % eines Kerns auf beiden Seiten. In der Spitze (3/s) sind es 375 Abfragen/s,
  rund 6 % eines Kerns für das Cockpit und 5 % für MongoDB.
- Der 250-ms-Takt wird nicht eingehalten. Mit `fixedDelay` und 100 ms je Takt kommt er alle
  rund 340 ms. Er kostet das Drei- bis Vierfache des 1-s-Takts und bringt eine Antwort, die
  höchstens 0,7 Sekunden früher kommt.
- Die Dauer eines Takts wächst linear mit den Strömen. Bei 450 Strömen sind das 100 ms. Ein
  Cockpit mit dreimal so vielen Tabs käme in die Nähe eines 1-s-Takts.

### Einschränkungen

- Cockpit und MongoDB liefen auf demselben Rechner, ohne Netz dazwischen. Im Betrieb kommt je
  Abfrage die Laufzeit über das Netz dazu. Bei 0,5 ms je Weg wären das 450 × 1 ms, also ein
  Takt von rund einer halben Sekunde statt 100 ms, und der Faden des Takts wäre den größten Teil
  der Sekunde belegt. Die Last auf MongoDB bliebe gleich.
- Die Server-CPU ist die CPU-Zeit des ganzen Test-Prozesses, also auch des Erzeugers der
  Änderungen und des Abholens der Ereignisse. Sie liegt damit eher zu hoch.
- Keine Browser, kein Schreiben in echte Ströme, keine Last durch das Laden von Listen und durch
  die Schreibzugriffe des Cockpits neben der Messung.
- Die Verteilung auf Gruppen ist ausgedacht: jede Task hat ein oder zwei von 30 Gruppen, jede
  Person zwei bis vier. Ob eine Abfrage etwas findet, ändert an ihrer Dauer kaum etwas, weil der
  Index auf `_id` sie trägt.
- Eine einzige Messung je Zeile, ohne Wiederholung. Ausreißer wie die 340 ms im ersten Lauf
  (Aufwärmen der JVM) sind nicht herausgerechnet.

### Schätzung für Variante b)

Diese Schätzung entstand, bevor Variante b) gebaut war. Gemessen ist sie inzwischen unter
[Messung Variante b)](#messung-variante-b); die Schätzung bleibt als Vergleich stehen.

Je Takt mit Änderung gibt es eine Abfrage statt 450: `_id in (…)` mit den Feldern, von denen die
Sichtbarkeit abhängt. Die Messung hat diese Abfrage nach jeder Zeile 20-mal gestellt, mit so
vielen Kennungen, wie ein Takt der Zeile sammelt. Sie dauerte im Mittel 0,1 bis 0,4 ms, höchstens
1,8 ms. Danach entscheidet das Cockpit im Speicher, für 450 Ströme und eine Handvoll Dokumente, im
Bereich von Mikrosekunden je Paar.

| Rate/s | Takt    | Abfragen/s a) | Abfragen/s b), geschätzt | Takt a) p50 | Takt b), geschätzt |
|--------|---------|---------------|--------------------------|-------------|--------------------|
| 0,3    | 1000 ms | 97            | 0,2                      | 87 ms       | unter 5 ms         |
| 3      | 1000 ms | 375           | 0,9                      | 99 ms       | unter 5 ms         |
| 50     | 1000 ms | 390           | 1                        | 151 ms      | unter 10 ms        |
| 50     | 250 ms  | 1.245         | 3                        | 108 ms      | unter 10 ms        |

Die Last auf MongoDB fällt bei b) um den Faktor der Ströme, also 450, und hängt nicht mehr an
der Zahl der Benutzer. Was bleibt, ist die zweite Fassung der Regel in Java, siehe oben.

## Entscheidung: Variante b)

Stephan hat am 2026-10-06 nach der Messung Variante b) gewählt. Variante a) ist entfernt, einen
Schalter zwischen beiden gibt es nicht.

Die Gründe:

- Bei a) wächst die Last linear mit den offenen Tabs. 450 Ströme sind 450 Abfragen in jedem Takt
  mit Änderung. Ein Cockpit mit doppelt so vielen Benutzern hat doppelt so viel Last, ohne dass
  sich an der Arbeit etwas geändert hat.
- Ein Takt dauert bei a) schon ohne Netz rund 100 ms. Mit einem Netz zwischen Cockpit und MongoDB
  kommt je Abfrage die Laufzeit dazu, bei 1 ms hin und zurück also rund 0,5 Sekunden je Takt.
- b) fragt einmal je Takt und Art, egal wie viele Tabs offen sind. Der Preis ist die zweite
  Fassung der Regel in Java.

Die zweite Fassung steht neben der ersten. `UserTaskVisibility.letsThrough(TaskFacts)` und
`WorkflowVisibility.letsThrough(WorkflowFacts)` sind die Regeln in Java. `TaskFacts` und
`WorkflowFacts` sind genau die Felder, die die Regel liest, und genau die Felder, die die Abfrage
des Takts holt. `dangling` kommt dabei aus dem Dokument und wird nicht neu berechnet, weil die
Abfrage der Liste den gespeicherten Wert vergleicht. Das Zurückgeben einer Task ist ein `$unset`
und lässt den gespeicherten Wert so stehen, wie er war.

Zusammengehalten werden beide Fassungen von `TheVisibilityInMemoryAgreesWithTheQueryTest`:

- Er schreibt Tasks in allen Kombinationen von Zuständigem, Kandidatenbenutzern,
  Kandidatengruppen, ausgeschlossenen und zugelassenen Benutzern, je mit dem gespeicherten
  `dangling` wie berechnet und umgekehrt (384 Tasks), dazu 32 Workflows.
- Er fragt beide Fassungen für die benannten Sichten von drei Personen, für `everyUserTask` und
  `everyWorkflow`, und für eine feste Stichprobe von 1.500 bzw. 1.000 Sichten. Die Sichten baut er
  per Reflexion aus den Komponenten der Records. Eine neue Komponente wird damit ohne Zutun
  variiert; hat sie einen Typ, den der Test nicht kennt, wird er rot und sagt, was zu tun ist.
- Er prüft, dass die Abfrage keines Sichtbarkeits-Kriteriums ein Feld liest, das die Fakten nicht
  tragen. Ein neues Kriterium in `buildUserTasksCriteria` auf ein neues Feld fällt damit auf.
- Gegenprobe beim Bau: die Regel in Java ohne Ausschluss (Vier-Augen) macht den Test rot.

## Messung Variante b)

Gemessen am 2026-10-06 mit demselben Harness und derselben Matrix wie Variante a), auf demselben
Rechner. Die Tabelle für a) oben bleibt als Vergleich stehen. Neu ist die Spalte
"Allokation je Takt": was der Faden des Takts an Speicher belegt, gemessen mit
`ThreadMXBean.getCurrentThreadAllocatedBytes`, Median der Takte mit Änderung.

| Tasks   | Rate/s | Takt    | Takte mit Änderung | Abfragen/s | Takt p50 | p95   | max   | Server-CPU | MongoDB-CPU | Allokation je Takt |
|---------|--------|---------|--------------------|------------|----------|-------|-------|------------|-------------|--------------------|
| 20.000  | 0,3    | 250 ms  | 6 %                | 0,2        | 8,5 ms   | 12 ms | 68 ms | 2 %        | 1 %         | 1,8 MB             |
| 20.000  | 3      | 250 ms  | 42 %               | 1,7        | 5,0 ms   | 11 ms | 12 ms | 3 %        | 1 %         | 1,8 MB             |
| 20.000  | 5      | 250 ms  | 71 %               | 2,8        | 7,9 ms   | 12 ms | 17 ms | 4 %        | 1 %         | 1,8 MB             |
| 20.000  | 20     | 250 ms  | 100 %              | 3,8        | 10 ms    | 15 ms | 19 ms | 6 %        | 1 %         | 5,0 MB             |
| 20.000  | 50     | 250 ms  | 100 %              | 3,8        | 11 ms    | 21 ms | 24 ms | 9 %        | 1 %         | 11,4 MB            |
| 20.000  | 0,3    | 1000 ms | 23 %               | 0,2        | 5,7 ms   | 10 ms | 10 ms | 0 %        | 1 %         | 1,8 MB             |
| 20.000  | 3      | 1000 ms | 90 %               | 0,9        | 6,6 ms   | 12 ms | 14 ms | 1 %        | 1 %         | 3,4 MB             |
| 20.000  | 5      | 1000 ms | 100 %              | 1,0        | 5,7 ms   | 10 ms | 13 ms | 1 %        | 1 %         | 5,0 MB             |
| 20.000  | 20     | 1000 ms | 100 %              | 1,0        | 15 ms    | 25 ms | 26 ms | 3 %        | 1 %         | 16,1 MB            |
| 20.000  | 50     | 1000 ms | 100 %              | 1,0        | 25 ms    | 34 ms | 38 ms | 5 %        | 1 %         | 41,3 MB            |
| 500.000 | 0,3    | 250 ms  | 6 %                | 0,2        | 5,2 ms   | 9 ms  | 13 ms | 1 %        | 1 %         | 1,8 MB             |
| 500.000 | 3      | 250 ms  | 45 %               | 1,8        | 3,6 ms   | 10 ms | 12 ms | 4 %        | 1 %         | 1,8 MB             |
| 500.000 | 5      | 250 ms  | 69 %               | 2,7        | 4,3 ms   | 11 ms | 13 ms | 3 %        | 1 %         | 1,8 MB             |
| 500.000 | 20     | 250 ms  | 100 %              | 3,9        | 4,5 ms   | 10 ms | 13 ms | 4 %        | 1 %         | 5,0 MB             |
| 500.000 | 50     | 250 ms  | 100 %              | 3,8        | 8,3 ms   | 17 ms | 21 ms | 7 %        | 1 %         | 10,6 MB            |
| 500.000 | 0,3    | 1000 ms | 23 %               | 0,2        | 4,4 ms   | 9 ms  | 11 ms | 0 %        | 1 %         | 1,8 MB             |
| 500.000 | 3      | 1000 ms | 90 %               | 0,9        | 6,8 ms   | 11 ms | 14 ms | 2 %        | 1 %         | 3,4 MB             |
| 500.000 | 5      | 1000 ms | 100 %              | 1,0        | 8,1 ms   | 14 ms | 17 ms | 1 %        | 1 %         | 4,2 MB             |
| 500.000 | 20     | 1000 ms | 100 %              | 1,0        | 14 ms    | 21 ms | 24 ms | 3 %        | 1 %         | 16,1 MB            |
| 500.000 | 50     | 1000 ms | 100 %              | 1,0        | 28 ms    | 39 ms | 44 ms | 6 %        | 1 %         | 40,5 MB            |

Das Gedächtnis ist unverändert: 27,2 MB bei 500 Kennungen je Strom, 104,5 MB bei 2.000, für 450
Ströme.

Was die Zahlen sagen:

- MongoDB sieht höchstens eine Abfrage je Takt, 0,2 bis 3,9 je Sekunde statt 97 bis 1.300. Seine
  CPU bleibt bei 1 %, was der Grundlast des leeren Containers entspricht.
- Ein Takt dauert 4 bis 28 ms statt rund 100 ms. Der größere Teil ist jetzt die Arbeit im
  Speicher: 450 Ströme mal die Sichten mal die geänderten Dokumente. Er wächst mit den Änderungen
  je Takt, nicht mit dem Bestand.
- Die Allokation liegt bei 1,8 MB je Takt mit wenigen Änderungen, rund 4 KB je Strom für Mengen
  und Listen, die sofort wieder frei sind. Bei 50 Änderungen je Takt sind es rund 40 MB. Das ist
  kurzlebiger Müll, der in der jungen Generation stirbt; bei 50 Änderungen pro Sekunde wären das
  40 MB je Sekunde, bei den 0,3/s des Kunden unter 1 MB je Sekunde.
- Wie bei a) spielt die Größe des Bestands keine Rolle.

### Vorgabe des Takts: 250 ms

Bei a) war der 250-ms-Takt das Drei- bis Vierfache teurer als der 1-s-Takt, deshalb 1.000 ms.
Bei b) kostet er in der Spitze des Kunden (3/s) 1,8 Abfragen je Sekunde und rund 4 % eines Kerns
im Cockpit, im Schnitt (0,3/s) 0,2 Abfragen je Sekunde und 1 bis 2 %. Dafür kommt eine Änderung
bis zu 750 ms früher im Browser an. Mit 250 ms läuft der Filter so oft wie das Zustellen
(`collecting-interval`, auch 250 ms), und die Antwortzeit ist die von vor dieser Story. Deshalb ist
die Vorgabe jetzt 250 ms. Wer Last sparen will, stellt 1.000 ms ein; die Tabelle sagt, was das
bringt.

Die Einschränkungen der Messung von a) gelten auch hier. Eine fällt bei b) weg: das Netz zwischen
Cockpit und MongoDB kostet je Takt eine Laufzeit und nicht 450.
