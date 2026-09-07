package sk.uniza.fri.cp.SchematicSim.Sheet;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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

                    // počiatočné vyhodnotenie - nahrádza powerSockets.forEach(PowerSocket::powerUp)
                    allGates.forEach(GateSymbol::reset);
                    allGates.forEach(GateSymbol::simulate);

                    HashSet<GateSymbol> gatesToUpdate = new HashSet<>();
                    SheetEvent event;
                    long lastActivityTime = System.currentTimeMillis();

                    while (!isCancelled()) {
                        try {
                            event = eventsQueue.poll();
                            if (event == null) {
                                event = eventsQueue.take();
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

                            event.process(gatesToUpdate);
                            gatesToUpdate.forEach(GateSymbol::simulate);
                            gatesToUpdate.clear();

                            lastActivityTime = System.currentTimeMillis();
                        } catch (InterruptedException e) {
                            if (isCancelled()) break;
                        } catch (NullPointerException e) {
                            e.printStackTrace(System.err);
                            gatesToUpdate.clear();
                        }
                    }

                    while (!eventsQueue.isEmpty()) {
                        eventsQueue.take().process(gatesToUpdate);
                        gatesToUpdate.forEach(GateSymbol::simulate);
                        gatesToUpdate.clear();
                    }

                    allGates.forEach(GateSymbol::reset);
                    running.setValue(false);
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
