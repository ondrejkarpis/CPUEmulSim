package sk.uniza.fri.cp.SchematicSim.Sheet;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import sk.uniza.fri.cp.SchematicSim.Buses.BusSymbol;
import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;
import sk.uniza.fri.cp.SchematicSim.Gates.SevenSegmentDisplay;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Simulačné jadro. Nahrádza {@code BoardSimulator} z BreadboardSim - narozdiel od neho
 * pri spustení nemá čo "napájať" (žiadne PowerSocket/VCC ako pri reálnych 74xx čipoch),
 * namiesto toho pri štarte jednoducho vyhodnotí všetky súčiastky raz od nuly, aby sa
 * ustálili počiatočné hodnoty výstupov, a ďalej beží identická event-driven slučka
 * ako v origináli.
 *
 * @author Tomáš Hianik (pôvodný autor BoardSimulator), adaptácia pre SchematicSim
 */
public class SchematicSimulator {

    private static final int ASYNCH_TIMEOUT_MS = 5000;
    /** Ako často sa vyhodnocujú všetky hradlá, keď nie sú žiadne udalosti (neustála simulácia). */
    private static final int IDLE_TICK_MS = 5;

    private final LinkedBlockingQueue<SheetEvent> eventsQueue;
    private List<GateSymbol> allGates;
    private final BooleanProperty running;

    private final Service<Void> simulatorService = new Service<Void>() {
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Thread.currentThread().setName("SchematicSimulationThread");

                    // celý beh simulácie je v try/finally - pri akejkoľvek chybe alebo rušení
                    // sa musí vždy dokončiť posledné vyhodnotenie hradiel
                    try {
                        setRunningQuietly(true);

                        // počiatočné vyhodnotenie - nahrádza powerSockets.forEach(PowerSocket::powerUp)
                        allGates.forEach(GateSymbol::reset);
                        allGates.forEach(GateSymbol::simulate);

                        // obnova hodnôt na zberniciach na vývody (pre prípad opätovného zapnutia simulácie)
                        for (GateSymbol gate : allGates) {
                            if (gate instanceof BusSymbol) ((BusSymbol) gate).syncFromBus();
                        }

                        HashSet<GateSymbol> gatesToUpdate = new HashSet<>();
                        SheetEvent event;
                        long lastActivityTime = System.currentTimeMillis();

                        // jednoduché meranie rýchlosti simulácie - počítadlá behu slučky
                        long gateSims = 0;
                        long sevenSegSims = 0;
                        long eventCount = 0;
                        long lastReportMs = System.currentTimeMillis();

                        while (!isCancelled()) {
                            try {
                                // raz za sekundu vypíš koľko simulácii hradiel prebehlo
                                long nowMs = System.currentTimeMillis();
                                if (nowMs - lastReportMs >= 1000) {
                                    System.out.println("[Sim] " + gateSims + " simulácii hradiel/s"
                                            + " (z toho 7-seg: " + sevenSegSims + ", spracovaných udalostí: " + eventCount + ")");
                                    gateSims = 0;
                                    sevenSegSims = 0;
                                    eventCount = 0;
                                    lastReportMs = nowMs;
                                }

                                event = eventsQueue.poll(IDLE_TICK_MS, TimeUnit.MILLISECONDS);
                                if (event == null) {
                                    if (eventsQueue.size() > 0) {
                                        event = eventsQueue.take();
                                    } else {
                                        // neustála simulácia - aj bez novej elektrickej udalosti sa
                                        // pravidelne vyhodnotia všetky hradlá (napr. 7-segmentové
                                        // hold-timeouty potrebujú periodický tick na zhasínanie)
                                        for (GateSymbol gate : allGates) {
                                            gate.simulate();
                                            gateSims++;
                                            if (gate instanceof SevenSegmentDisplay) sevenSegSims++;
                                        }
                                        lastActivityTime = System.currentTimeMillis();
                                        continue;
                                    }
                                }

                                if (System.currentTimeMillis() - lastActivityTime > ASYNCH_TIMEOUT_MS) {
                                    // kľud v simulácii (CPU načúva, nič sa nemení) je NORMÁLNY stav -
                                    // watchdog ruší simuláciu iba pri skutočnom zablokovaní (plný front),
                                    // inak by pri dlhšej pauze (krokovanie, zastavený program) zabil živú
                                    // simuláciu a CPU by začalo hlásiť chybu zbernice
                                    if (eventsQueue.size() > 0) {
                                        System.err.println("[Sim] RUŠÍM simuláciu - príliš dlhá odozva "
                                                + "(posledná aktivita pred " + (System.currentTimeMillis() - lastActivityTime)
                                                + " ms, front má " + eventsQueue.size() + " udalostí)");
                                        Platform.runLater(() -> {
                                            Alert alert = new Alert(Alert.AlertType.ERROR,
                                                    "Chyba simulácie. Príliš dlhá odozva.", ButtonType.CLOSE);
                                            alert.show();
                                        });
                                        this.cancel();
                                        break;
                                    } else {
                                        lastActivityTime = System.currentTimeMillis();
                                    }
                                }

                                event.process(gatesToUpdate);
                                eventCount++;
                                for (GateSymbol gate : gatesToUpdate) {
                                    gate.simulate();
                                    gateSims++;
                                    if (gate instanceof SevenSegmentDisplay) sevenSegSims++;
                                }
                                gatesToUpdate.clear();
                                lastActivityTime = System.currentTimeMillis();

                            } catch (InterruptedException e) {
                                if (isCancelled()) break;
                            } catch (RuntimeException e) {
                                // jeden chybny event nesmie zhltit celu simulaciu
                                e.printStackTrace(System.err);
                                gatesToUpdate.clear();
                            }
                        }

                        // zostatok udalosti po rušení - vyhodnot, aby piny nezostali v nekonzistentnom stave
                        while (!eventsQueue.isEmpty()) {
                            eventsQueue.take().process(gatesToUpdate);
                            gatesToUpdate.forEach(GateSymbol::simulate);
                            gatesToUpdate.clear();
                        }

                        allGates.forEach(GateSymbol::reset);
                    } catch (Throwable t) {
                        // zlyhanie simulácie sa nikdy nesmie prehltnúť - vždy je vidieť príčinu
                        System.err.println("[Sim] Chyba simulácie: " + t);
                        t.printStackTrace(System.err);
                    } finally {
                        System.err.println("[Sim] Koniec simulácie (isCancelled=" + isCancelled() + ")");
                        setRunningQuietly(false);
                    }
                    return null;
                }
            };
        }
    };

    SchematicSimulator() {
        this.eventsQueue = new LinkedBlockingQueue<>();
        this.running = new SimpleBooleanProperty(false);

        simulatorService.setOnFailed(event -> {
            if (simulatorService.getException() != null) simulatorService.getException().printStackTrace(System.err);
        });
    }

    /**
     * Spustenie simulácie.
     *
     * @param allGates Všetky súčiastky na ploche - vyhodnotia sa raz na začiatok simulácie.
     */
    public void start(List<GateSymbol> allGates) {
        this.allGates = allGates;
        simulatorService.restart();
    }

    public void stop() {
        if (Platform.isFxApplicationThread()) simulatorService.cancel();
        else Platform.runLater(simulatorService::cancel);
    }

    public void addEvent(SheetEvent event) {
        try {
            // asynchrónne zapojenie - odstránenie eventu na pine, ktorý už nie je aktuálny
            eventsQueue.removeIf(e -> e.getPin() != null && e.getPin().equals(event.getPin()));
            eventsQueue.put(event);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Nastavenie vlastnosti behu. Zmena vlastnosti vyvolá listenery - nesmú však zhodiť
     * cez vlastný RuntimeException celý simulačný task (vlastnosť sa mení na simulačnom vlákne).
     */
    private void setRunningQuietly(boolean value) {
        try {
            this.running.setValue(value);
        } catch (RuntimeException ignored) {
            ignored.printStackTrace(System.err);
        }
    }

    public BooleanProperty runningProperty() {
        return this.running;
    }
}
