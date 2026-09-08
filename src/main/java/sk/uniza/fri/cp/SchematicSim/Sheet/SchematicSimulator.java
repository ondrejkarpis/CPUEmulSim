package sk.uniza.fri.cp.SchematicSim.Sheet;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import sk.uniza.fri.cp.Bus.Bus;
import sk.uniza.fri.cp.SchematicSim.Buses.BusSymbol;
import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

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

                    running.setValue(true);

                    // pripojenie zbernice k simulátoru - CPU dostane znamenie, že nás môže počúvať
                    Bus.getBus().setEventsQueue(eventsQueue);
                    Bus.getBus().simulationIsRunning(true);
                    Bus.getBus().dataIsChanging();

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
                    boolean steadyState = false;

                    while (!isCancelled()) {
                        try {
                            event = eventsQueue.poll();
                            if (event == null) {
                                if (eventsQueue.size() > 0) {
                                    event = eventsQueue.take();
                                } else {
                                    // simulácia je ustálená - CPU môže bezpečne čítať dáta zo zbernice
                                    Bus.getBus().dataInSteadyState();
                                    steadyState = true;
                                    event = eventsQueue.take();
                                    lastActivityTime = System.currentTimeMillis();
                                }
                            }

                            if (System.currentTimeMillis() - lastActivityTime > ASYNCH_TIMEOUT_MS) {
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.ERROR,
                                            "Chyba simulácie. Príliš dlhá odozva.", ButtonType.CLOSE);
                                    alert.show();
                                });
                                this.cancel();
                                break;
                            }

                            if (steadyState) {
                                // podujatie sa našlo - dáta sa menia, CPU nesmie čítať
                                Bus.getBus().dataIsChanging();
                                steadyState = false;
                            }

                            event.process(gatesToUpdate);
                            gatesToUpdate.forEach(GateSymbol::simulate);
                            gatesToUpdate.clear();

                        } catch (InterruptedException e) {
                            if (isCancelled()) break;
                        } catch (NullPointerException e) {
                            e.printStackTrace(System.err);
                            gatesToUpdate.clear();
                        }
                    }

                    Bus.getBus().dataIsChanging();

                    while (!eventsQueue.isEmpty()) {
                        eventsQueue.take().process(gatesToUpdate);
                        gatesToUpdate.forEach(GateSymbol::simulate);
                        gatesToUpdate.clear();
                    }

                    allGates.forEach(GateSymbol::reset);
                    running.setValue(false);
                    Bus.getBus().simulationIsRunning(false);
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

    public BooleanProperty runningProperty() {
        return this.running;
    }
}
