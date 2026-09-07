package sk.uniza.fri.cp.BreadboardSim.Board;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Socket.PowerSocket;
import sk.uniza.fri.cp.Bus.Bus;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simulačné jadro.
 * O beh vlákna so simuláciou sa stara Service.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class BoardSimulator {

    private static final int ASYNCH_TIMEOUT_MS = 5000;

    private LinkedBlockingQueue<BoardEvent> eventsQueue; //naplánované udalosti
    private List<PowerSocket> powerSockets; //sokety, ktoré sa maju po spusteni napojit
    private BooleanProperty running; //priznak behu simulacie

    public AtomicLong tick = new AtomicLong(0);
    public static final Logger QUEUELOGGER = LogManager.getLogger("QueueLogger");

    /**
     * Simulačná slučka nového vlákna
     */
    private Service simulatorService = new Service() {
		@Override
		protected Task createTask() {
			return new Task() {
				@Override
				protected Object call() throws Exception {
                    Thread.currentThread().setName("SimulationThread");

                    boolean steadyState = false;

                    //pripojenie napajania
                    powerSockets.forEach(PowerSocket::powerUp);

                    running.setValue(true);

                    Bus.getBus().setEventsQueue(eventsQueue);
                    Bus.getBus().simulationIsRunning(true);
                    Bus.getBus().dataIsChanging();

                    HashSet<Device> devicesToUpdate = new HashSet<>();
                    BoardEvent event;

                    long lastSteadyStateTime = System.currentTimeMillis();

                    //obsluha eventov
					while(!isCancelled()){
						try {
                            if (System.currentTimeMillis() - lastSteadyStateTime > ASYNCH_TIMEOUT_MS) {
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(
                                            Alert.AlertType.ERROR,
                                            "Chyba simulácie. Príliš dlhá odozva.",
                                            ButtonType.CLOSE);
                                    alert.show();
                                });
                                this.cancel();
                            }

                            event = eventsQueue.poll();
                            if (event == null) {
                                if (eventsQueue.size() > 0) {
                                    event = eventsQueue.take();
                                } else {
                                    Bus.getBus().dataInSteadyState();
                                    steadyState = true;
                                    event = eventsQueue.take();
                                    lastSteadyStateTime = System.currentTimeMillis();
                                }
                            }

                            if (steadyState) {
                                Bus.getBus().dataIsChanging();
                                steadyState = false;
                            }

                            event.process(devicesToUpdate);

                            devicesToUpdate.forEach((Device::simulate));

							devicesToUpdate.clear();

                            //tick.incrementAndGet();
                            //QUEUELOGGER.debug("TICK: " + tick.incrementAndGet());
                        } catch (InterruptedException e){
							if(isCancelled()) break;
						} catch (NullPointerException e){
							e.printStackTrace(System.err);
                            devicesToUpdate.clear();
                        }
                    }

                    Bus.getBus().dataIsChanging();

					powerSockets.forEach(PowerSocket::powerDown);

                    while (eventsQueue.size() > 0) {
                        eventsQueue.take().process(devicesToUpdate);
                        devicesToUpdate.forEach((Device::simulate));
                        devicesToUpdate.clear();
                    }
                    running.setValue(false);
                    Bus.getBus().simulationIsRunning(false);

                    return null;
                }
            };
		}
	};

    /**
     * Objekt jadra simulátora.
     */
    BoardSimulator() {
        this.eventsQueue = new LinkedBlockingQueue<>();
        this.running = new SimpleBooleanProperty(false);

        simulatorService.setOnFailed(event -> {
            System.out.println(event);
            simulatorService.getException().printStackTrace(System.err);
        });
    }

    /**
     * Spustenie simulačného vlákna.
     *
     * @param powerSockets Najájacie sokety, ktoré sa majú simulovať ako prvé.
     */
    public void start(List<PowerSocket> powerSockets){
		this.powerSockets = powerSockets;

		simulatorService.restart();
    }

    /**
     * Zastavenie simulačného vlákna.
     */
    public void stop(){
        if (Platform.isFxApplicationThread())
            simulatorService.cancel();
        else
            Platform.runLater(() -> simulatorService.cancel());
    }

	/**
     * Pridanie novej udalosti do fronty.
     *
     * @param event Nová udalosť simulácie.
     */
	public void addEvent(BoardEvent event){
		try {
            //vyriesene asynchronne zapojenie -> odstranenie eventu na sokete ktory uz nie je aktualny
            //existuje efektivnejsi sposob?
            eventsQueue.removeIf((boardEvent -> boardEvent.getSocket() != null && boardEvent.getSocket().equals(event.getSocket())));
            eventsQueue.put(event);
        } catch (InterruptedException e) {
//            e.printStackTrace();
        }
	}

    /**
     * Proterty behu simululácie.
     *
     * @return Property - true, ak sa simulácia vykonáva, false inak.
     */
    public BooleanProperty runningProperty(){
        return this.running;
    }

}