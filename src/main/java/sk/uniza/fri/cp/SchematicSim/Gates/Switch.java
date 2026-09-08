package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import sk.uniza.fri.cp.SchematicSim.Pin.OutputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.Arrays;
import java.util.List;

/**
 * Samostatný prepínač - farebný kruh s jedným prípojným bodom {@code O} (vľavo).
 * Kliknutím na kruh sa stav prepne (červený = zapnutý, bielosivý = vypnutý). Vývod
 * {@code O} trvalo generuje HIGH pre zapnutý prepínač a LOW pre vypnutý, bez ohľadu
 * na akékoľvek riadiace signály (popri prepínači nie je žiadny IR_ pin).
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class Switch extends GateSymbol {

    private static final int GRID_WIDTH = 2;
    private static final int GRID_HEIGHT = 2;

    private static final Color SWITCH_ON = Color.RED;
    private static final Color SWITCH_OFF = Color.WHITESMOKE;

    private Pin outPin;
    private Circle button;

    private volatile boolean on = false;

    /** Konštruktor pre paletku (ItemPicker). */
    public Switch() {
        super();
    }

    /** Konštruktor pre umiestnenie na plochu - volaný reflexiou zo SchematicSheet. */
    public Switch(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected List<Pin> createPins() {
        outPin = new OutputPin(this, "O", 0, 1, Side.LEFT);
        return Arrays.asList(outPin);
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();
        double w = getGridWidth() * cell;
        double h = getGridHeight() * cell;

        button = new Circle(w / 2.0, h / 2.0, cell * 0.55, SWITCH_OFF);
        button.setStroke(Color.BLACK);
        button.setStrokeWidth(1.5);
        button.setOnMouseClicked(event -> handleToggle());

        return new Pane(button);
    }

    @Override
    public void simulate() {
        // prepínač generuje výstup stále
        setPin(outPin, on ? Pin.PinState.HIGH : Pin.PinState.LOW);
    }

    @Override
    public void reset() {
        if (outPin != null) setPinForce(outPin, Pin.PinState.NOT_CONNECTED);
    }

    private void handleToggle() {
        setOn(!on);
    }

    /**
     * Priame nastavenie stavu prepínača (ekvivalent kliknutia).
     */
    public void setOn(boolean value) {
        on = value;
        refreshVisual();

        // ak beží simulácia, okamžite prepočítaj vývody, aby sa zmena rozšírila do vodičov a LED
        if (getSheet() != null && getSheet().isSimulationRunning()) {
            simulate();
        }
    }

    /**
     * Aktualizácia vizuálu vždy na FX vlákne (simulácia beží na separátnom vlákne).
     * Zmeny sú skoalescované do jednej čakajúcej úlohy, aby sa pri rýchlej simulácii
     * FX vlákno nezahltilo radom a aplikácia (klávesy F4/F5/F10...) ostala odozvá.
     */
    private volatile boolean visualUpdateScheduled;

    private void refreshVisual() {
        if (button == null || visualUpdateScheduled) return;
        visualUpdateScheduled = true;
        Platform.runLater(() -> {
            visualUpdateScheduled = false;
            button.setFill(on ? SWITCH_ON : SWITCH_OFF);
        });
    }

    public boolean isOn() {
        return on;
    }

    @Override
    public int getGridWidth() {
        return GRID_WIDTH;
    }

    @Override
    public int getGridHeight() {
        return GRID_HEIGHT;
    }

    @Override
    public String getName() {
        return "SW";
    }

    @Override
    public String getShortDescription() {
        return "Prepínač - klikom prepni, vývod O trvalo generuje HIGH/LOW";
    }
}