# Veicoli plugin (Minecraft)

Plugin **Paper/Spigot Java** per server Minecraft. I file Lua/HTML/FiveM presenti nella cronologia del repository sono un tentativo precedente errato e non fanno parte del plugin Minecraft: il server carica il JAR creato da Maven.

## Requisiti
- Minecraft/Paper 1.21.4
- Java 21
- Maven 3.9+
- Resource pack con il modello dell'AMG Cross associato all'oggetto `LEATHER_HORSE_ARMOR` con CustomModelData `22`.

## Compilazione e installazione
1. Compila con `mvn package`.
2. Copia `target/veicoliplugin-1.0.0.jar` nella cartella `plugins/` del server.
3. Avvia il server. Verrà generato `plugins/VeicoliPlugin/config.yml`.
4. Assegna agli staff il permesso `veicoliplugin.admin` (è `op` per default).

## Comandi
- `/adminveicoli give <nick> <nome veicolo>` — assegna un veicolo, per esempio `/adminveicoli give Steve AMG Cross`.
- `/adminveicoli remove <nick> <nome veicolo>` — rimuove quel veicolo dal garage.
- `/garage` — apre il tuo garage.
- `/garage <nick>` — lo staff apre il garage di un giocatore; clic sinistro visualizza, clic destro rimuove.

Nel garage personale, clic sinistro su un veicolo lo fa apparire e ci fa salire. La marcia cambia con **F** (evento cambio mano), da 1 a 5. L'HUD è mostrato nella action bar con velocità, carburante e marcia. I limiti delle marce sono 15, 30, 55, 75 e 110 km/h.

Il veicolo è implementato come cavallo sellato con bardatura personalizzata: il resource pack è necessario per renderlo come l'AMG Cross. Le ruote e il loro modello possono essere aggiunti quando sarà disponibile lo screenshot promesso.
