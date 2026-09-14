package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.SchematicSim.Electrical.PinType;
import sk.uniza.fri.cp.SchematicSim.Pin.InputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.OutputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.Arrays;
import java.util.List;

/**
 * Momentové tlačidlo - v kľude je výstup {@code O} (slabý push/pull so slabým pull-upom)
 * na logickej 1, po stlačení sa vstup {@code I} behaviorálne prepája na výstup: ak je vstup
 * v logickej 0, výstup sa pretiahne na 0, inak zostane 1. Vývod {@code O} je typu
 * {@link PinType#WEAK_OUT} - na spoločnej sieti ho vždy pretiahne silný (push-pull) výstup,
 * takže tlačidlo nemôže spôsobiť skrat.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class PushButton extends GateSymbol {

    private static final int GRID_WIDTH = 2;
    private static final int GRID_HEIGHT = 2;

    private Pin pinIn;
    private Pin pinOut;
    private Circle plunger;
    private Text valueText;

    private volatile boolean pressed = false;

    /** Konštruktor pre paletku (ItemPicker). */
    public PushButton() {
        super();
    }

    /** Konštruktor pre umiestnenie na plochu - volaný reflexiou zo SchematicSheet. */
    public PushButton(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected List<Pin> createPins() {
        pinIn = new InputPin(this, "I", 0, 1, Side.LEFT);
        pinOut = new OutputPin(this, "O", 1, 0, Side.TOP);
        // slabý výstup (pull-up): idle HIGH, silný vodič na sieti ho môže pretiahnuť
        pinOut.getOwnedPotential().setType(PinType.WEAK_OUT);
        return Arrays.asList(pinIn, pinOut);
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();
        double w = getGridWidth() * cell;
        double h = getGridHeight() * cell;

        Rectangle body = new Rectangle(w, h);
        body.setFill(Color.WHITESMOKE);
        body.setStroke(Color.GRAY);
        body.setStrokeWidth(1.5);

        // otlačené tlačidlo - stlačením mení farbu, pustenie ho vráti do kľudu
        plunger = new Circle(w / 2.0, h / 2.0, cell * 0.45, Color.WHITE);
        plunger.setStroke(Color.BLACK);
        plunger.setStrokeWidth(1.5);
        plunger.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                setPressedState(true);
            }
        });
        plunger.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                setPressedState(false);
            }
        });

        valueText = new Text("1");
        valueText.setTextOrigin(VPos.CENTER);
        valueText.setLayoutY(h / 2.0 + cell * 0.75);
        valueText.setFont(Font.font(cell * 0.4));
        valueText.setMouseTransparent(true);
        valueText.setLayoutX(w / 2.0 - valueText.getBoundsInLocal().getWidth() / 2.0);

        return new Pane(body, plunger, valueText);
    }

    @Override
    public void simulate() {
        // kľud: slabý pull-up drží výstup na 1; stlačenie prenáša vstup na výstup
        if (pressed && isLow(pinIn)) {
            setPin(pinOut, Pin.PinState.LOW);
        } else {
            setPin(pinOut, Pin.PinState.HIGH);
        }
    }

    @Override
    public void reset() {
        pressed = false;
        if (pinIn != null) setPinForce(pinIn, Pin.PinState.NOT_CONNECTED);
        if (pinOut != null) setPinForce(pinOut, Pin.PinState.NOT_CONNECTED);
    }

    /**
     * Priame nastavenie stavu stlačenia (ekvivalent kliknutia tlačidla).
     */
    public void setPressedState(boolean value) {
        pressed = value;
        refreshVisual();

        // ak beží simulácia, okamžite prepočítaj vývody, aby sa zmena rozšírila do vodičov a LED
        if (getSheet() != null && getSheet().isSimulationRunning()) {
            simulate();
        }
    }

    public boolean isPressedState() {
        return pressed;
    }

    /**
     * Aktualizácia vizuálu vždy na FX vlákne (simulácia beží na separátnom vlákne).
     * Zmeny sú skoalescované do jednej čakajúcej úlohy.
     */
    private volatile boolean visualUpdateScheduled;

    private void refreshVisual() {
        if (plunger == null || valueText == null || visualUpdateScheduled) return;
        visualUpdateScheduled = true;
        Platform.runLater(() -> {
            visualUpdateScheduled = false;
            plunger.setFill(pressed ? Color.DARKGRAY : Color.WHITE);
            String text = pinOut != null && pinOut.getState() == Pin.PinState.LOW ? "0" : "1";
            valueText.setText(text);
            valueText.setLayoutX(wCenter() - valueText.getBoundsInLocal().getWidth() / 2.0);
        });
    }

    private double wCenter() {
        return getSheet().getGrid().getSizeMin() * getGridWidth() / 2.0;
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
        return "BTN";
    }

    @Override
    public String getShortDescription() {
        return "Momentové tlačidlo - v kľude výstup O a slabý pull-up na 1; stlačením pretiahne vstup (0/1) na výstup";
    }
}