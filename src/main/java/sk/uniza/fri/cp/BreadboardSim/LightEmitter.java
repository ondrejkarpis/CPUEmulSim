package sk.uniza.fri.cp.BreadboardSim;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Objekt "vyžarujúci svetlo".
 *
 * Je možné ho riadiť iba pomocou jednej simulácie z jedného boardu!
 * Potrebné mazanie cez delete!
 *
 * @author Tomáš Hianik
 * @created 16.4.2017
 */
public class LightEmitter {

    //Board podla ktoreho sa spusta/zastavuje simulacia
    private static Board board;
    //"skrtiace" vlakno RunLater poziadaviek
    private static Thread thread;
    //pocitadlo pre pristup k RunLater
    private AtomicLong counter = new AtomicLong(-1);
    //vytvorene instancie emittorov
    private static final ConcurrentLinkedQueue<LightEmitter> instances = new ConcurrentLinkedQueue<>();

    private final Shape shape;
    private final Color col_turnedOn;
    private final Color col_turnedOff;
    private final AtomicInteger turnedOn = new AtomicInteger(0); //kolko krat bol zapnuty od poslednej aktualizacie
    private final AtomicBoolean state;
    private final int minUpdateDelayMs;
    private long lastUpdate = 0;

    private int history = 100;
    private TimeWeight avgLight = new TimeWeight(history);

    /**
     * Vytvorenie nového emitora a pridanie medzi ostatné vytovrené.
     *
     * @param paBoard          Plocha simulátora podľa ktorej simulácie sa riadia všetky vytvorené emitory.
     * @param shape            Tvar emitora.
     * @param colorTurnedOn    Farba zapnutého emitora.
     * @param colorTurnedOff   Farba vypnutého emitora.
     * @param minUpdateDelayMs Minimálny interval obnovy.
     */
    public LightEmitter(Board paBoard, Shape shape, Color colorTurnedOn, Color colorTurnedOff, int minUpdateDelayMs) {
        this.shape = shape;
        this.col_turnedOn = colorTurnedOn;
        this.col_turnedOff = colorTurnedOff;
        this.minUpdateDelayMs = minUpdateDelayMs;
        this.state = new AtomicBoolean(false);

        instances.add(this);

        if (board == null) {
            board = paBoard;
            board.simRunningProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    counter.set(-1);

                    thread = new Thread(() -> {
                        long count = 0;
                        while (!Thread.currentThread().isInterrupted()) {
                            count++;
                            if (counter.getAndSet(count) == -1) {
                                updateUI(counter);
                            }
                        }

                        counter.set(-1); //oznam o ukonceni
                        updateUI(counter); //posledny update -> vypnutie
                    });

                    thread.setDaemon(true);
                    thread.start();
                } else {
                    if (thread != null) thread.interrupt();
                }
            });
        }
    }

    /**
     * Vytvorenie nového emitora a pridanie medzi ostatné vytovrené.
     *
     * @param paBoard Plocha simulátora podľa ktorej simulácie sa riadia všetky vytvorené emitory.
     * @param shape Tvar emitora.
     * @param colorTurnedOn Farba zapnutého emitora.
     * @param colorTurnedOff Farba vypnutého emitora.
     */
    public LightEmitter(Board paBoard, Shape shape, Color colorTurnedOn, Color colorTurnedOff) {
        this(paBoard, shape, colorTurnedOn, colorTurnedOff, 0);
    }

    private long lastUpdateMs = 0;

    /**
     * Zapnutie emitora.
     */
    public void turnOn() {

//         this.avgLight.add(1, board.getSimulator().tick.get());

        turnedOn.getAndIncrement();
        state.set(true);
    }

    /**
     * Vypnutie emitora.
     */
    public void turnOff() {
//        this.avgLight.add(0, board.getSimulator().tick.get());
        state.set(false);
    }

    /**
     * Stav emitora.
     * @return Stav zapnutý / vypnutý.
     */
    public boolean getState() {
        return state.get();
    }

    /**
     * Zmazanie emitora a odstránenie tak zo zoznamu všetkých aktualizovateľných emitorov.
     */
    public void delete() {
        instances.remove(this);
        if (instances.size() == 0) board = null;
    }

    private static long lastTick = 0;
    private static void updateUI(final AtomicLong counter) {

        Platform.runLater(() -> {
            //pre kazdy emitter na ploche
            for (LightEmitter emitter :
                    instances) {

//                if(emitter.avgLight.getMean() > 0.01) {
//                    emitter.shape.setFill(emitter.col_turnedOn);
//                    emitter.shape.setOpacity(emitter.avgLight.getMean());
//                } else {
//                    emitter.shape.setFill(emitter.col_turnedOff);
//                }

                if ((System.currentTimeMillis() - emitter.lastUpdate > emitter.minUpdateDelayMs)) {
                    //ak je cas od posledneho update-u vacsi ako minimalny nastaveny

                    if (emitter.minUpdateDelayMs > 0 && emitter.turnedOn.get() > 2) {
                        //ak ma emitter nastaveny minimalny update a bol zopnuty viac ako dva krat za dany cas, zapni ho
                        emitter.shape.setFill(emitter.col_turnedOn);
                    } else {
                        //inak sa riad podla akutalne nastavenej hodnoty
                        if (emitter.state.get()) {
                            emitter.shape.setFill(emitter.col_turnedOn);
                        } else {
                            emitter.shape.setFill(emitter.col_turnedOff);
                        }
                    }

                    emitter.turnedOn.set(0);
                    emitter.lastUpdate = System.currentTimeMillis();
                } else if (counter.get() == -1) {
                    //prikaz na ukoncenie -> vypnutie
                    emitter.shape.setFill(emitter.col_turnedOff);
                }
            }

            counter.set(-1);
        });

    }


    private class TimeWeight {
        private int startIndex = 0, endIndex = -1, countOfHist;
        private int[] historyVal;
        private long[] historyTime;

        TimeWeight(int history) {
            this.countOfHist = history;
            this.historyVal = new int[countOfHist];
            this.historyTime = new long[countOfHist];
        }

        public void add(int value, long time) {
            endIndex = (endIndex + 1) % countOfHist;

            historyVal[endIndex] = value;
            historyTime[endIndex] = time;

            while (historyTime[startIndex] < historyTime[endIndex] - countOfHist) {
                startIndex++;
                if (startIndex + 1 == countOfHist) startIndex = 0;
            }
        }

        public double getMean() {
            if (endIndex == -1) return 0;
            if (startIndex == endIndex) return historyVal[startIndex];

            long startTime = historyTime[endIndex] - countOfHist;
            int beforeFirstIndex = startIndex - 1;
            if (beforeFirstIndex == -1) beforeFirstIndex = countOfHist - 1;
            long sum = (int) (historyTime[startIndex] - startTime) * historyVal[beforeFirstIndex];

            for (int i = startIndex; i < endIndex; i++) {
                sum += (historyTime[(i + 1) % countOfHist] - historyTime[i]) * historyVal[i];
            }

            //System.out.println("Mean: " + mean + " from: " + Arrays.toString(this.historyVal) + " / " + Arrays.toString(this.historyTime));
            return sum / (double) countOfHist;
        }
    }
}
