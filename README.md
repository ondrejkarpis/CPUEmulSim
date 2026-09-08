# CPUEmulSim

Emulátor 8-bitového CPU a schématický simulátor pre vývojovú dosku FRI UNIZA.

Projekt vychádza z bakalárskej práce Tomáša Hianika (2017). Pôvodný simulátor vývojovej
dosky (breadboard) bol v roku 2026 nahradený schématickým editorom a simulátorom
`SchematicSim`, ktorý umožňuje zapojiť CPU k I/O súčiastkam priamo vodičmi v schéme.

## Funkcie

- **Editor asembleru** (RichTextFX) so zvýrazňovaním syntaxe, parsovaním, breakpointmi,
  krokovým vykonávaním (F7) a zobrazením registrov a príznakov.
- **8-bitový CPU** — registry A/B/C/D, príznaky Z/CY/IE, zásobník, 16-bitová adresovacia
  zbernica (64 KB adresný priestor), prerušenia (`int`, `SCALL KEY/KPR/DSP`).
- **Schématický editor** — súčiastky z paletky, vozenie vodičmi s ortogonálnym
  preložením, ukladanie/načítanie schémy do súboru `.schx`.
- **Simulácia schémy** — spúšťa sa automaticky pri behu CPU (alebo manuálne prepínačom),
  logické stavy `1/0/Z` sa zobrazujú na vodičoch.

## I/O súčiastky a riadiace signály

Riadiace signály CPU sú aktívne na **nízkej úrovni** (`0` = aktívny signál).

| Súčiastka | Piny | Pripojenie |
|-----------|------|------------|
| `Ram256`  | `MR_`, `MW_`, `A7..A0` (vľavo), `D7..D0` (vpravo) | čítanie pri `MR_=0`, zápis pri `MW_=0`; adresa z `A7..A0`, dáta na `D7..D0` |
| `Led`     | `IN` (vľavo) | svieti pri logickej 1 na vstupe; pasívna, vhodná na zobrazenie zbernice aj riadiacich signálov |
| `Switches8` | `D7..D0`, `IR_` | smeruje stav 8 prepínačov na dátovú zbernicu pri `IR_=0` (inak high-impedance) |

Zbernice v schéme:
- **Adresná** — `A15..A0` (16-bit).
- **Dátová** — `D7..D0`.
- **Riadiace signály** — `MW_`, `MR_`, `IW_`, `IR_`, `IA_`, `RY`, `IT`.

Príklad I/O programu (odčítanie prepínačov a zobrazenie na LED):

```
start:
    INN A, 0xFF        ; načítaj stav prepínačov
    OUT 0xFF, A        ; zobraz na LED diódach
    JMP start
```

## Build a spustenie

Požiadavky:
- **JDK 8** (napr. Azul Zulu 8) — JavaFX je súčasťou JDK 8;
- **Maven 3**.

```
mvn clean package
java -jar target/CPUEmulSim-1.6.0-jar-with-dependencies.jar
```

## Klávesové skratky

| Kláves | Činnosť |
|--------|---------|
| `F4`   | Parsovať program |
| `F5`   | Spustiť / pokračovať |
| `F7`   | Krok |
| `F9`   | Pozastaviť |
| `F10`  | Zastaviť |
| `F12`  | Reset |
| `Ctrl+/` | Komentár / odkomentovanie bloku |
| `Ctrl+F9` | Zmenšiť font v editore |
| `Ctrl+F10` | Zväčšiť font v editore |
| `Ctrl+F12` | Prepínať konzolové logovanie (spúšťať program z konzoly) |
| Klávesnica | Vstup pre `SCALL KEY` / `SCALL KPR` počas behu programu |

## Inštrukčná sada

- Aritmetické a logické: `ADD ADC ADI SUB SUC SBI AND ANI ORR ORI XOR XRI INC INX DEC DCX CMP CMI`
- Posuny a rotácie: `SHL SHR SCR RTL RCL RTR RCR`
- Presun dát: `MOV MVI MXI MVX MMR LMI LMR SMI SMR STR LDR`
- I/O: `INN OUT PUS POP`
- Vetvenie: `JMP JZR JNZ JCY JNC` a podmienené skoky/podprogramy `JL JG JE JLE JGE JNE JNL JNG`, `CAL CZR CNZ CCY CNC CL CG CE CLE CGE CNE CNL CNG`, `RET`
- Špeciálne: `EIT DIT SCALL BYTE`

## Štruktúra projektu

- `sk.uniza.fri.cp.App` — JavaFX UI (okno CPU emulátora, okno schémy).
- `sk.uniza.fri.cp.CPUEmul` — emulátor CPU, parser, inštrukčná sada.
- `sk.uniza.fri.cp.Bus` — systémová zbernica a mapovanie riadiacich signálov na piny.
- `sk.uniza.fri.cp.SchematicSim` — schématický editor a simulátor: súčiastky,
  zbernice, vodiče, ukladanie `.schx` (`SchemeLoader`).