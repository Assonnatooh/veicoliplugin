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
Il cavallo è soltanto un supporto invisibile per guidare: a schermo non deve comparire. La carrozzeria usa `LEATHER_HORSE_ARMOR` con CustomModelData **20**; le ruote decorative usano il CMD **240** e vengono posizionate ai quattro angoli del mezzo. Il resource pack deve fornire i modelli corrispondenti.

Le posizioni si regolano in `config.yml` con `vehicles.amg-cross.wheel-side-offset` (larghezza) e `vehicles.amg-cross.wheel-axle-offset` (distanza tra asse anteriore e posteriore). Se il server ha già creato il file di configurazione, aggiorna manualmente il CMD carrozzeria da 22 a 20 e riavvia il server.
