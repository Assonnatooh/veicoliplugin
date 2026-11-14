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
Il supporto di guida è invisibile e serve solo per movimento e controllo. La carrozzeria usa `LEATHER_HORSE_ARMOR` con CustomModelData **20**. Le ruote sono esattamente **due** entità `ItemDisplay`, ciascuna con una bardatura di cuoio (`LEATHER_HORSE_ARMOR`) e CustomModelData **240**, una per lato del veicolo.

Il resource pack deve fornire i modelli corrispondenti ai CMD 20 e 240. La distanza laterale delle ruote si regola in `config.yml` con `vehicles.amg-cross.wheel-side-offset`.
