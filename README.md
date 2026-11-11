# OneDayTitan für Fabric 1.21.1

Portierung der mitgelieferten `onedaytitan-1.0.0-sources.jar` von Minecraft 1.18.2 auf 1.21.1. Die Originaldatei liegt unverändert unter `upstream/`; der bearbeitbare Code liegt unter `src/main/`.

## Bauen

- Java 21 verwenden.
- Unter Windows `.\gradlew.bat build` ausführen.
- Der Server-Mod liegt danach unter `build/libs/onedaytitan-1.0.0+1.21.1.jar`.

Für den Betrieb werden Fabric Loader und Fabric API für Minecraft 1.21.1 benötigt. Der Mod läuft serverseitig. Wenn `titanfabric:legend_sword` registriert ist, nutzt OneDayTitan dieses Item für Team-Einladungen. Andernfalls wird ein markiertes Eisenschwert verwendet.

Die gespeicherte Startausrüstung verwendet in dieser Version Minecrafts 1.21.1-Inventarformat in `config/startStuff.dat`. Eine Ausrüstung aus der alten `config/startStuff.json` muss mit `/setstartstuff` neu gespeichert werden.

## Stand der Prüfung

`.\gradlew.bat build` ist erfolgreich. `.\gradlew.bat runServer` hat Fabric, den Mod und eine neue Testwelt ohne Startfehler geladen; der Server meldete `Done`. Ein Test mit verbundenen Spielern und den Spielbefehlen steht noch aus. Der Testserver wurde danach beendet.
