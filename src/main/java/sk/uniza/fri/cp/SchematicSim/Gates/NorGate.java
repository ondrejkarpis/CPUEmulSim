package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.beans.binding.Bindings;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

/**
 * Logické hradlo NOR s premenlivým počtom vstupov (2, 3, 4 alebo 8 - cez kontextové menu).
 * Y = 0 práve vtedy, keď je aspoň jeden vstup vo vysokom stave (inak Y = 1).
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class NorGate extends MultiInputGate {

    public NorGate() {
        super();
    }

    @Override
    public Pane getImage() {
        Text label = new Text(getName());
        label.setLayoutY(label.getBoundsInLocal().getHeight());

        Image image = new Image(getClass().getResourceAsStream("/icons/nor.png"));
        ImageView view = new ImageView(image);
        view.setLayoutY(label.getBoundsInLocal().getHeight() + 5);

        label.layoutXProperty().bind(Bindings.createDoubleBinding(
                () -> (image.getWidth() - label.getBoundsInLocal().getWidth()) / 2.0,
                image.widthProperty(), label.boundsInLocalProperty()));

        return new Pane(label, view);
    }

    public NorGate(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected String getSymbolText() {
        return "\u2265" + "1";
    }

    @Override
    protected boolean negated() {
        return true;
    }

    @Override
    public void simulate() {
        boolean anyHigh = false;
        for (Pin input : inputPins()) {
            if (isHigh(input)) {
                anyHigh = true;
                break;
            }
        }
        setPin(getOutput(), anyHigh ? Pin.PinState.LOW : Pin.PinState.HIGH);
    }

    @Override
    public void reset() {
        setPinForce(getOutput(), Pin.PinState.NOT_CONNECTED);
    }

    @Override
    public String getName() {
        return "NOR";
    }

    @Override
    public String getShortDescription() {
        return "2-8 vstupové logické hradlo NOR (Y = NOT (A + B + ...))";
    }
}