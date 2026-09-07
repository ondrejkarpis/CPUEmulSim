# SchematicSim - schematický editor namiesto breadboardu

Samostatný balík `sk.uniza.fri.cp.SchematicSim`, ktorý nezasahuje do existujúceho
`sk.uniza.fri.cp.BreadboardSim` - dá sa najprv vyskúšať vedľa neho a integrovať do UI (FXML)
až keď budete spokojní s modelom.

## Mapovanie tried (staré -> nové)

| BreadboardSim                     | SchematicSim                        | Poznámka |
|------------------------------------|--------------------------------------|----------|
| `Board`                            | `Sheet.SchematicSheet`               | bez počiatočnej vývojovej dosky, pridaná `GridOccupancy` |
| `BoardLayersManager`               | `Sheet.SheetLayersManager`           | 3 vrstvy namiesto 4 (bez `components`) |
| `BoardSimulator`                   | `Sheet.SchematicSimulator`           | bez PowerSocket/Bus - hradlá nemajú napájanie |
| `BoardEvent` / `BoardChangeEvent`  | `Sheet.SheetEvent` / `SheetChangeEvent` | Socket -> Pin |
| `Socket` + `Devices.Pin.Pin`       | `Pin.Pin` (+`InputPin`/`OutputPin`)  | zlúčené do jednej triedy, žiadna kolízna detekcia |
| `Socket.SocketType`                | `Electrical.PinType`                 | rovnaké hodnoty |
| `Socket.Potential`                 | `Electrical.Potential`               | logika stromu spojení 1:1, iba Socket->Connectable |
| `Devices.Device` (+`Chip`)         | `Gates.GateSymbol`                   | žiadne reálne 74xx, žiadne VCC/GND |
| `Components.Component`, `SchoolBreadboard` | **odstránené** | nahradené voľným umiestňovaním + `GridOccupancy` |
| `Wire.Wire` / `Joint` / `WireEnd`  | `Wire.*` (rovnaké názvy)             | Socket->Pin, bez BusInterface hacku |
| `Wire.WireSegment` (Line)          | `Wire.WireSegment` (Polyline)        | ortogonálne vedenie cez nový `OrthogonalRouter` |
| `Item`, `Movable`, `Selectable`, `HighlightGroup` | rovnaké názvy | takmer bezo zmeny, iba `Board`->`SchematicSheet` |
| `ItemPicker`                       | `ItemPicker`                         | iba jedna paletka (žiadne "Komponenty") |
| `DescriptionPane`                  | `DescriptionPane`                    | zjednodušené (bez zámku) |

Nové triedy bez predlohy: `Connectable`, `Side`, `GridOccupancy`, `Wire.OrthogonalRouter`,
`Gates.AndGate`/`OrGate`/`NotGate` (príklady).

## Čo funguje

- Umiestňovanie súčiastok z paletky ťahaním, so snapovaním na mriežku a kontrolou voľných
  buniek (bez kolíznej detekcie so soketmi).
- Ťahanie vodiča priamo z pinu na pin, s ortogonálnym (Manhattan) vedením.
- Elektrická simulácia - strom potenciálov, detekcia skratu na výstupoch - funguje rovnako
  ako v origináli, iba naviazaná na `Pin` namiesto `Socket`.
- Manuálne pridávanie/presúvanie zlomov na vodiči (Ctrl+ťah / dvojklik), presun celých
  súčiastok aj s pripojenými vodičmi.
- Výber, zvýraznenie, mazanie objektov, zoom a pan plochy.

## Čo zámerne zostalo zjednodušené / mimo rozsahu

1. **`OrthogonalRouter` je heuristický** (max. 3 zalomenia), nie obstacle-avoiding pathfinder -
   nerieši prekrývanie trasy s telami iných súčiastok. V praxi si to používateľ dolaďuje
   manuálnym pridaním zlomu, presne ako doteraz.
2. **Grafické značky hradiel** (`AndGate`/`OrGate`/`NotGate.drawBody()`) sú zámerne jednoduché
   (obdĺžnik/trojuholník + popisok) - dajú sa nahradiť poriadnymi IEC/ANSI schematickými
   značkami bez zásahu do zvyšku architektúry.
3. **Ikony v paletke** - `Item.getImage()` zobrazí len názov triedy textom; pre pekné ikony
   treba jednotlivým `GateSymbol` potomkom pridať vlastné prepísanie `getImage()`.
4. **Uloženie/načítanie (`SchemeLoader`)** - zatiaľ nie je prerobené. Bude potrebný nový formát
   bez breadboard-pozícií (zoznam súčiastok s ich typom/pozíciou/rotáciou + zoznam vodičov
   s koncovými pinmi a zoznamom zlomov).
5. **FXML/App integrácia** - tieto triedy sa dajú vložiť do existujúceho FXML layoutu presne
   tak, ako `Board`/`ItemPicker` teraz - `SchematicSheet` aj `ItemPicker` sú bežné JavaFX
   uzly (`ScrollPane`/`VBox`).
6. Chýbajú zatiaľ ďalšie bežné hradlá (NAND, NOR, XOR, XNOR, prípadne viac-vstupové varianty) -
   pridávajú sa rovnako ako `AndGate`/`OrGate`/`NotGate`.

## Ako pridať nové hradlo

Stačí implementovať `GateSymbol`: `createPins()`, `drawBody()`, `simulate()`, `reset()`,
`getGridWidth()/getGridHeight()`, `getName()/getShortDescription()`, plus bezparametrický
konštruktor (pre paletku) a konštruktor s `SchematicSheet` (pre umiestnenie na plochu) -
pozri `AndGate`/`OrGate`/`NotGate` ako predlohu.
