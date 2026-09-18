package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.beans.binding.Bindings;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

/**
 * Logické hradlo XOR s premenlivým počtom vstupov (2, 3, 4 alebo 8 - cez kontextové menu).
 * Y = 1 práve vtedy, keď je počet vstupov vo vysokom stave nepárny.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class XorGate extends MultiInputGate {

    public XorGate() {
        super();
    }

    @Override
    public Pane getImage() {
        Text label = new Text(getName());
        label.setLayoutY(label.getBoundsInLocal().getHeight());

        Image image = new Image(getClass().getResourceAsStream("/icons/xor.png"));
        ImageView view = new ImageView(image);
        view.setLayoutY(label.getBoundsInLocal().getHeight() + 5);

        label.layoutXProperty().bind(Bindings.createDoubleBinding(
                () -> (image.getWidth() - label.getBoundsInLocal().getWidth()) / 2.0,
                image.widthProperty(), label.boundsInLocalProperty()));

        return new Pane(label, view);
    }

    public XorGate(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected String getSymbolText() {
        return "=1";
    }

    @Override
    public void simulate() {
        boolean oddHigh = false;
        for (Pin input : inputPins()) {
            if (isHigh(input)) {
                oddHigh = !oddHigh;
            }
        }
        setPin(getOutput(), oddHigh ? Pin.PinState.HIGH : Pin.PinState.LOW);
    }

    @Override
    public void reset() {
        setPinForce(getOutput(), Pin.PinState.NOT_CONNECTED);
    }

    @Override
    public String getName() {
        return "XOR";
    }

    @Override
    public String getShortDescription() {
        return "2-8 vstupové logické hradlo XOR (Y = A ⊕ B ⊕ ...)";
    }
}