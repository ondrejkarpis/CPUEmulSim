package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import sk.uniza.fri.cp.SchematicSim.Pin.InputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.Arrays;
import java.util.List;

/**
 * Samostatná LED dióda - obyčajný farebný kruh s jedným prípojným bodom {@code IN} (vľavo).
 * Dióda svieti, keď je na vstupe logická 1 (HIGH), inak je zhasnutá. Je čisto pasívna -
 * nemá žiadne výstupné vývody, nikdy nehynie na zbernicu, a preto nemôže spôsobiť skrat.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class Led extends GateSymbol {

    private static final int GRID_WIDTH = 2;
    private static final int GRID_HEIGHT = 2;

    private static final Color LED_ON = Color.RED;
    private static final Color LED_OFF = Color.DARKGRAY;

    private Pin pinIn;
    private Circle light;

    private volatile boolean on = false;

    /** Konštruktor pre paletku (ItemPicker). */
    public Led() {
        super();
    }

    /** Konštruktor pre umiestnenie na plochu - volaný reflexiou zo SchematicSheet. */
    public Led(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected List<Pin> createPins() {
        pinIn = new InputPin(this, "IN", 0, 1, Side.LEFT);
        return Arrays.asList(pinIn);
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();
        double w = getGridWidth() * cell;
        double h = getGridHeight() * cell;

        light = new Circle(w / 2.0, h / 2.0, cell * 0.55, LED_OFF);
        light.setStroke(Color.BLACK);
        light.setStrokeWidth(1.5);

        return new Pane(light);
    }

    @Override
    public void simulate() {
        // LED svieti, keď je na vstupe logická 1
        boolean lit = isHigh(pinIn);
        if (lit != on) {
            on = lit;
            refreshVisual();
        }
    }

    @Override
    public void reset() {
        if (pinIn != null) setPinForce(pinIn, Pin.PinState.NOT_CONNECTED);
    }

    /**
     * Aktualizácia vizuálu vždy na FX vlákne (simulácia beží na separátnom vlákne).
     * Zmeny sú skoalescované do jednej čakajúcej úlohy, aby sa pri rýchlej simulácii
     * FX vlákno nezahltilo radom a aplikácia (klávesy F4/F5/F10...) ostala odozvá.
     */
    private volatile boolean visualUpdateScheduled;

    private void refreshVisual() {
        if (light == null || visualUpdateScheduled) return;
        visualUpdateScheduled = true;
        Platform.runLater(() -> {
            visualUpdateScheduled = false;
            light.setFill(on ? LED_ON : LED_OFF);
        });
    }

    public boolean isLit() {
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
        return "LED";
    }

    @Override
    public String getShortDescription() {
        return "LED dióda - svieti, keď je vstup IN v logickej 1";
    }
}