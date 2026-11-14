# veicoliplugin

Plugin FiveM per gestione veicoli, garage player e HUD in auto.

Funzioni principali:
- `/adminveicoli give [nick] [nome veicolo]`
- `/adminveicoli remove [nick] [nome veicolo]`
- `/garage` per aprire il garage personale
- `/garage [nick]` per aprire il garage di un altro player come staff
- HUD automatico in auto con:
  - Velocità
  - Carburante
  - Marcia
- Cambio marcia con `F`
- Limiti di marcia:
  - I: 0-15 km/h
  - II: 16-30 km/h
  - III: 31-55 km/h
  - IV: 56-75 km/h
  - V: 76-110 km/h

## Installazione
1. Copia la cartella `veicoliplugin` dentro `resources/`
2. Avvia il resource nel server.cfg:
   `ensure veicoliplugin`
3. Riapri il server.

## Comandi
- `adminveicoli give [nick] [veicolo]`
- `adminveicoli remove [nick] [veicolo]`
- `garage`

## Note
Il plugin usa `SetResourceKvp` per salvare i veicoli del garage in modo persistente e non dipende da framework esterni.
