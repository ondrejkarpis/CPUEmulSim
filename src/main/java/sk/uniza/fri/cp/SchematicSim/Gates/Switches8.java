package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.SchematicSim.Pin.InputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * 8-bitový vstupný prepínač pre inštrukciu {@code INN}.
 * Stavy jednotlivých bitov sa prepínajú kliknutím na kruhy v tele súčiastky.
 * Svoje vývody D0..D7 naháňa IBA počas aktívneho riadiaceho signálu {@code IR_} (log. 0) -
 * vtedy na zbernici zastupuje vstupné zariadenie a DataBus8 jeho hodnotu prečíta.
 * V ostatných prípadoch sú vývody v stave high-impedance (tri-state), aby nezavadzali
 * ostatným zariadeniam na dátovej zbernici.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class Switches8 extends GateSymbol {

    private static final int GRID_WIDTH = 5;
    private static final int GRID_HEIGHT = 10;

    private static final Color SWITCH_ON = Color.RED;
    private static final Color SWITCH_OFF = Color.WHITESMOKE;

    private Pin[] dataPins;
    private Pin pinIR_;

    private final boolean[] switchState = new boolean[8];
    private Circle[] switches;
    private Text valueLabel;

    /** Konštruktor pre paletku (ItemPicker). */
    public Switches8() {
        super();
    }

    /** Konštruktor pre umiestnenie na plochu - volaný reflexiou zo SchematicSheet. */
    public Switches8(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected List<Pin> createPins() {
        List<Pin> pins = new ArrayList<>();

        dataPins = new Pin[8];
        for (int index = 0; index < 8; index++) {
            dataPins[index] = new IoPin(this, "D" + index, 0, index, Side.LEFT);
            pins.add(dataPins[index]);
        }
        pinIR_ = new InputPin(this, "IR_", 0, 8, Side.LEFT);
        pins.add(pinIR_);

        return pins;
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();
        Rectangle body = new Rectangle(GRID_WIDTH * cell, GRID_HEIGHT * cell, Color.LIGHTGREEN);
        body.setStroke(Color.BLACK);
        body.setStrokeWidth(1.5);

        Text title = new Text("SW 8");
        title.setLayoutX(cell * 0.6);
        title.setLayoutY(cell * 1.2);

        switches = new Circle[8];
        Pane bodyPane = new Pane(body, title);
        double radius = cell * 0.30;
        for (int index = 0; index < 8; index++) {
            Circle sw = new Circle(cell * 2.5, cell * (1.5 + index * 0.95), radius, SWITCH_OFF);
            sw.setStroke(Color.BLACK);
            final int bit = index;
            sw.setOnMouseClicked(event -> handleToggle(bit));
            switches[index] = sw;
            bodyPane.getChildren().add(sw);
        }

        valueLabel = new Text("0x00");
        valueLabel.setLayoutX(cell * 0.6);
        valueLabel.setLayoutY(cell * GRID_HEIGHT - cell * 0.6);
        bodyPane.getChildren().add(valueLabel);

        return bodyPane;
    }

    @Override
    public void simulate() {
        boolean active = isLow(pinIR_);
        for (int index = 0; index < 8; index++) {
            Pin.PinState state;
            if (active) {
                state = switchState[index] ? Pin.PinState.HIGH : Pin.PinState.LOW;
            } else {
                state = Pin.PinState.HIGH_IMPEDANCE;
            }
            setPin(dataPins[index], state);
        }
    }

    @Override
    public void reset() {
        if (getPins() == null) return;
        for (Pin pin : getPins()) pin.setState(Pin.PinState.NOT_CONNECTED);
    }

    private void handleToggle(int index) {
        switchState[index] = !switchState[index];
        refreshVisual();

        // ak beží simulácia a je IR_ zapojené, okamžite prepočítaj vývody (vytvorí zmenové udalosti)
        if (getSheet() != null && getSheet().isSimulationRunning()
                && pinIR_ != null && pinIR_.isConnected()) {
            simulate();
        }
    }

    private void refreshVisual() {
        if (switches == null) return;
        int value = 0;
        for (int index = 0; index < 8; index++) {
            if (switchState[index]) value |= 1 << index;
            switches[index].setFill(switchState[index] ? SWITCH_ON : SWITCH_OFF);
        }
        if (valueLabel != null) {
            valueLabel.setText(String.format("0x%02X", value));
        }
    }

    public boolean[] getSwitchState() {
        return switchState;
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
        return "SW 8";
    }

    @Override
    public String getShortDescription() {
        return "8 prepínačov (vstup pre INN) - klikom zmeň bit, na zbernicu púšťa dáta pri aktívnom IR_";
    }

    private static class IoPin extends Pin {
        IoPin(GateSymbol owner, String name, int gridOffsetX, int gridOffsetY, Side side) {
            super(owner, name, Direction.INOUT, gridOffsetX, gridOffsetY, side);
        }
    }
}