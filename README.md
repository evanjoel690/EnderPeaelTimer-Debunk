# Ender Pearl Timer

Client-seitige Fabric-Mod fuer Minecraft 1.21.11, die einen Countdown
anzeigt, wann eine von dir geworfene Enderperle voraussichtlich landet.

Der Timer erscheint zentriert ueber der XP-Leiste (also im Bereich
zwischen Herzen/Hunger-Leiste und Hotbar) und zaehlt in Sekunden
herunter, bis die Perle einschlaegt.

## Funktionsweise

- Beim Werfen einer Enderperle erkennt der Mod ueber ein Fabric-API
  Entity-Load-Event, dass eine neue, dir gehoerende `EnderPearlEntity`
  in der Welt erscheint, und startet das Tracking.
- Jeden Client-Tick wird die Flugbahn der Perle mit den vanilla-typischen
  Bewegungswerten (Schwerkraft 0.03 Bloecke/Tick^2, Luftwiderstand 0.99)
  simuliert und per Raycast gegen die Blockkollision der Welt geprueft,
  wie viele Ticks bis zum Einschlag verbleiben.
- Da diese Simulation jeden Tick mit der echten, vom Server
  synchronisierten Position und Geschwindigkeit neu berechnet wird,
  gleicht sie sich laufend an und wird kurz vor dem Einschlag praktisch
  exakt.
- Sobald die Perle aus der Welt entfernt wird (Server hat den Teleport
  durchgefuehrt), verschwindet der Timer wieder.

## Bauen

Voraussetzungen: JDK 21, Internetzugriff auf die Fabric/Mojang-Maven-Server.

1. Projekt in IntelliJ IDEA (mit "Minecraft Development"-Plugin) oder
   in ein beliebiges Verzeichnis oeffnen.
2. Falls kein Gradle-Wrapper vorhanden ist:
   `gradle wrapper --gradle-version 8.11`
3. Bauen: `./gradlew build` (Linux/Mac) bzw. `gradlew.bat build` (Windows)
4. Die fertige Mod-Datei liegt danach unter `build/libs/enderpearltimer-1.0.0.jar`
5. Datei in den `mods`-Ordner einer Fabric-Loader-1.21.11-Installation
   legen (Fabric API muss ebenfalls installiert sein).

## Anpassungen

- Die vertikale Position des Timers laesst sich in
  `EnderPearlTimerClient.java` ueber die Konstante
  `Y_OFFSET_FROM_BOTTOM` verschieben.
- Farbe/Format des Textes kann in `renderTimer(...)` angepasst werden.

## Hinweis zu den Versionen in gradle.properties

Die Werte fuer `yarn_mappings`, `loader_version`, `loom_version` und
`fabric_version` sollten vor dem ersten Build auf
https://fabricmc.net/develop gegen die jeweils aktuellsten Releases
fuer 1.21.11 geprueft werden, falls der Build fehlschlaegt.
