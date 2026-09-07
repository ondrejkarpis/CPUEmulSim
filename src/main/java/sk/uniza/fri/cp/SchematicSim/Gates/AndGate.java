package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.SchematicSim.Pin.InputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.OutputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.List;
import java.util.Arrays;

/**
 * Abstraktné 2-vstupové AND hradlo. Nahrádza reálny 74xx čip (napr. Gen7408) z BreadboardSim -
 * bez VCC/GND, bez viacerých hradiel v jednom puzdre, iba čistá logická funkcia.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class AndGate extends GateSymbol {

    private Pin a, b, y;

    /** Konštruktor pre paletku (ItemPicker). */
    public AndGate() {
        super();
    }

    /** Konštruktor pre umiestnenie na plochu - volaný reflexiou zo SchematicSheet. */
    public AndGate(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected List<Pin> createPins() {
        a = new InputPin(this, "A", 0, 0, Side.LEFT);
        b = new InputPin(this, "B", 0, 2, Side.LEFT);
        y = new OutputPin(this, "Y", getGridWidth(), 1, Side.RIGHT);
        return Arrays.asList(a, b, y);
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();
        Rectangle body = new Rectangle(getGridWidth() * cell, getGridHeight() * cell, Color.WHITESMOKE);
        body.setStroke(Color.BLACK);
        body.setStrokeWidth(1.5);

        Text label = new Text("&");
        label.setLayoutX(body.getWidth() / 2 - 4);
        label.setLayoutY(body.getHeight() / 2 + 5);

        return new Pane(body, label);
    }

    @Override
    public void simulate() {
        setPin(y, (isHigh(a) && isHigh(b)) ? Pin.PinState.HIGH : Pin.PinState.LOW);
    }

    @Override
    public void reset() {
        setPinForce(y, Pin.PinState.NOT_CONNECTED);
    }

    @Override
    public int getGridWidth() {
        return 3;
    }

    @Override
    public int getGridHeight() {
        return 3;
    }

    @Override
    public String getName() {
        return "AND";
    }

    @Override
    public String getShortDescription() {
        return "2-vstupové logické hradlo AND (Y = A · B)";
    }
}
