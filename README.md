# Veicoli plugin (Minecraft)

Plugin **Paper per Minecraft 1.21.8**, compilato con Java 21.

## Build e installazione
1. Esegui `mvn package` con Java 21 e Maven 3.9+.
2. Copia `target/veicoliplugin-1.0.0.jar` in `plugins/`.
3. Avvia il server. La configurazione viene creata in `plugins/VeicoliPlugin/config.yml`.
4. Permesso staff: `veicoliplugin.admin` (default: OP).

## Comandi
- `/adminveicoli give <nick> <nome veicolo>` — assegna un mezzo.
- `/adminveicoli remove <nick> <nome veicolo>` — lo rimuove dal garage.
- `/garage` — apre il garage personale; clic sinistro su un veicolo per spawnarlo.
- `/garage <nick>` — lo staff apre il garage del giocatore; clic destro rimuove il mezzo.

## AMG Cross e resource pack
Il veicolo è un cavallo sellato con una bardatura di cuoio personalizzata. L'AMG Cross usa `LEATHER_HORSE_ARMOR` con CustomModelData **22** per la carrozzeria. Le **due ruote** sono due entità `ItemDisplay` separate, ciascuna con `LEATHER_HORSE_ARMOR` e CustomModelData **240**; il plugin le mantiene ai lati del veicolo mentre si muove.

Il resource pack deve fornire i modelli corrispondenti ai CMD 22 e 240. La distanza laterale delle ruote si regola in `config.yml` con `vehicles.amg-cross.wheel-side-offset` (default `0.42` blocchi); la posizione verticale può richiedere regolazione in base al modello del resource pack.
