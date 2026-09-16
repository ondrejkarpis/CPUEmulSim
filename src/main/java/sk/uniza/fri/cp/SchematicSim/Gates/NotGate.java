package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.beans.binding.Bindings;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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
 * Abstraktný invertor (NOT). Vykreslený ako obdĺžnik s „1" a negačným krúžkom podľa IEC 60617.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class NotGate extends GateSymbol {

    private Pin a, y;

    public NotGate() {
        super();
    }

    @Override
    public Pane getImage() {
        Text label = new Text(getName());
        label.setLayoutY(label.getBoundsInLocal().getHeight());

        Image image = new Image(getClass().getResourceAsStream("/icons/not.png"));
        ImageView view = new ImageView(image);
        view.setLayoutY(label.getBoundsInLocal().getHeight() + 5);

        label.layoutXProperty().bind(Bindings.createDoubleBinding(
                () -> (image.getWidth() - label.getBoundsInLocal().getWidth()) / 2.0,
                image.widthProperty(), label.boundsInLocalProperty()));

        return new Pane(label, view);
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

        Rectangle body = new Rectangle(w - 2 * bubble, h, Color.WHITESMOKE);
        body.setStroke(Color.BLACK);
        body.setStrokeWidth(1.5);

        Text label = new Text("1");
        label.setLayoutX((w - 2 * bubble) / 2 - 4);
        label.setLayoutY(h / 2 + 5);

        Circle negationBubble = new Circle(w - bubble, h / 2.0, bubble);
        negationBubble.setFill(Color.WHITESMOKE);
        negationBubble.setStroke(Color.BLACK);
        negationBubble.setStrokeWidth(1.5);

        return new Pane(body, label, negationBubble);
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
        return 2;
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
