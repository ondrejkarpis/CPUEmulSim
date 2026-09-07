package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import sk.uniza.fri.cp.SchematicSim.Pin.InputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.OutputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.List;
import java.util.Arrays;

/**
 * Abstraktný invertor (NOT). Vykreslený ako klasický trojuholník s negačným krúžkom.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class NotGate extends GateSymbol {

    private Pin a, y;

    public NotGate() {
        super();
    }

    public NotGate(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected List<Pin> createPins() {
        a = new InputPin(this, "A", 0, 1, Side.LEFT);
        y = new OutputPin(this, "Y", getGridWidth(), 1, Side.RIGHT);
        return Arrays.asList(a, y);
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();
        double w = getGridWidth() * cell;
        double h = getGridHeight() * cell;
        double bubble = cell / 4.0;

        Polygon triangle = new Polygon(0, 0, 0, h, w - 2 * bubble, h / 2.0);
        triangle.setFill(Color.WHITESMOKE);
        triangle.setStroke(Color.BLACK);
        triangle.setStrokeWidth(1.5);

        Circle negationBubble = new Circle(w - bubble, h / 2.0, bubble);
        negationBubble.setFill(Color.WHITESMOKE);
        negationBubble.setStroke(Color.BLACK);
        negationBubble.setStrokeWidth(1.5);

        return new Pane(triangle, negationBubble);
    }

    @Override
    public void simulate() {
        setPin(y, isHigh(a) ? Pin.PinState.LOW : Pin.PinState.HIGH);
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
        return 2;
    }

    @Override
    public String getName() {
        return "NOT";
    }

    @Override
    public String getShortDescription() {
        return "Invertor (Y = ¬A)";
    }
}
