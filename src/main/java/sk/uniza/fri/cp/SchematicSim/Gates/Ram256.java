package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.beans.binding.Bindings;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.SchematicSim.Pin.InputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * 256×8-bitová pamäť RAM komunikujúca so zbernicou CPU.
 * Umožňuje spúšťať programy s pamäťovými inštrukciami (LMI/LMR/SMI/SMR) na schéme.
 * <p>
 * Čítanie: počas aktívneho {@code MR_} zariadenie naháňa na vývody D0..D7 dáta
 * z adresovanej bunky. Zápis: počas aktívneho {@code MW_} sa dáta z vývodov D0..D7
 * zapíšu do adresovanej bunky, vývody sú v tom čase v stave high-impedance (tri-state),
 * aby po zbernici mohol hnať dáta {@code DataBus8}.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class Ram256 extends GateSymbol {

    private static final int GRID_WIDTH = 5;
    private static final int GRID_HEIGHT = 11;

    private final byte[] memory = new byte[256];

    private Pin[] addressPins;
    private Pin[] dataPins;
    private Pin pinMR_;
    private Pin pinMW_;

    /** Konštruktor pre paletku (ItemPicker). */
    public Ram256() {
        super();
    }

    @Override
    public Pane getImage() {
        Text label = new Text("8kB RAM");
        label.setLayoutY(label.getBoundsInLocal().getHeight());

        Image image = new Image(getClass().getResourceAsStream("/icons/ram.png"));
        ImageView view = new ImageView(image);
        view.setLayoutY(label.getBoundsInLocal().getHeight() + 5);

        label.layoutXProperty().bind(Bindings.createDoubleBinding(
                () -> (image.getWidth() - label.getBoundsInLocal().getWidth()) / 2.0,
                image.widthProperty(), label.boundsInLocalProperty()));

        return new Pane(label, view);
    }

    /** Konštruktor pre umiestnenie na plochu - volaný reflexiou zo SchematicSheet. */
    public Ram256(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected List<Pin> createPins() {
        List<Pin> pins = new ArrayList<>();

        // lavy okraj: adresové vývody a riadiace signály
        addressPins = new Pin[8];
        for (int index = 0; index < 8; index++) {
            addressPins[index] = new InputPin(this, "A" + index, 0, index, Side.LEFT);
            pins.add(addressPins[index]);
        }
        pinMR_ = new InputPin(this, "MR_", 0, 8, Side.LEFT);
        pinMW_ = new InputPin(this, "MW_", 0, 9, Side.LEFT);
        pins.add(pinMR_);
        pins.add(pinMW_);

        // pravy okraj: dátové vývody (tri-state)
        dataPins = new Pin[8];
        for (int index = 0; index < 8; index++) {
            dataPins[index] = new IoPin(this, "D" + index, GRID_WIDTH, index, Side.RIGHT);
            pins.add(dataPins[index]);
        }

        return pins;
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();
        Rectangle body = new Rectangle(GRID_WIDTH * cell, GRID_HEIGHT * cell, Color.LIGHTBLUE);
        body.setStroke(Color.BLACK);
        body.setStrokeWidth(1.5);

        Text title = new Text("RAM 256×8");
        title.setLayoutX(cell * 0.4);
        title.setLayoutY(cell * 2.2);

        Text addrLabel = new Text("A0..A7");
        addrLabel.setLayoutX(cell * 0.4);
        addrLabel.setLayoutY(cell * 4.6);

        Text dataLabel = new Text("D0..D7");
        dataLabel.setLayoutX(cell * 2.2);
        dataLabel.setLayoutY(cell * 4.6);

        Text mrlLabel = new Text("MR_");
        mrlLabel.setLayoutX(cell * 0.9);
        mrlLabel.setLayoutY(cell * 8.4);

        Text mwlLabel = new Text("MW_");
        mwlLabel.setLayoutX(cell * 0.9);
        mwlLabel.setLayoutY(cell * 9.4);

        return new Pane(body, title, addrLabel, dataLabel, mrlLabel, mwlLabel);
    }

    @Override
    public void simulate() {
        if (isLow(pinMR_)) {
            // čítanie - pamäť naháňa dáta na vývody D0..D7
            driveData(memory[readAddress()] & 0xFF);
        } else if (isLow(pinMW_)) {
            // zápis - dáta zachytíme a vývody uvoľníme (tri-state), po zbernici hnie DataBus8
            memory[readAddress()] = (byte) readData();
            setDataImpedance();
        } else {
            setDataImpedance();
        }
    }

    @Override
    public void reset() {
        if (getPins() == null) return;
        for (Pin pin : getPins()) pin.setState(Pin.PinState.NOT_CONNECTED);
    }

    private int readAddress() {
        int address = 0;
        for (int index = 0; index < 8; index++) {
            if (isHigh(addressPins[index])) address |= 1 << index;
        }
        return address;
    }

    private int readData() {
        int value = 0;
        for (int index = 0; index < 8; index++) {
            if (isHigh(dataPins[index])) value |= 1 << index;
        }
        return value & 0xFF;
    }

    private void driveData(int value) {
        for (int index = 0; index < 8; index++) {
            setPin(dataPins[index],
                    ((value & (1 << index)) != 0) ? Pin.PinState.HIGH : Pin.PinState.LOW);
        }
    }

    private void setDataImpedance() {
        for (int index = 0; index < 8; index++) {
            setPin(dataPins[index], Pin.PinState.HIGH_IMPEDANCE);
        }
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
        return "RAM 256";
    }

    @Override
    public String getShortDescription() {
        return "256×8-bitová pamäť (LMI/LMR/SMI/SMR) - číta pri MR_, zapisuje pri MW_";
    }

    private static class IoPin extends Pin {
        IoPin(GateSymbol owner, String name, int gridOffsetX, int gridOffsetY, Side side) {
            super(owner, name, Direction.INOUT, gridOffsetX, gridOffsetY, side);
        }
    }
}