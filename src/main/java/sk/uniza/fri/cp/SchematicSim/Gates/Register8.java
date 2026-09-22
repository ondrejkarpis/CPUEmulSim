package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.beans.binding.Bindings;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.SchematicSim.Pin.InputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.OutputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;
import sk.uniza.fri.cp.SchematicSim.Electrical.Potential;

import java.util.ArrayList;
import java.util.List;

/**
 * 8-bitový register (D-type latch, transparentný pri LE=1).
 * <p>
 * Dátové vstupy D0..D7 sa počas aktívneho {@code LE} (log. 1) transparentne premietajú
 * do registra; po zániku LE si register hodnotu podrží. Výstupy Q0..Q7 sú tri-state:
 * pri aktívnom {@code OE_} (log. 0) hnané obsahom registra, pri neaktívnom (log. 1)
 * v stave vysokej impedancie (Z), aby mohol po zbernici hnať dáta iný zdroj.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class Register8 extends GateSymbol {

    private static final int GRID_WIDTH = 4;
    private static final int GRID_HEIGHT = 11;

    private Pin[] dataPins;
    private Pin[] outputPins;
    private Pin pinOE_;
    private Pin pinLE;

    private int latch = 0;

    /** Konštruktor pre paletku (ItemPicker). */
    public Register8() {
        super();
    }

    @Override
    public Pane getImage() {
        Text label = new Text("REG");
        label.setLayoutY(label.getBoundsInLocal().getHeight());

        Image image = new Image(getClass().getResourceAsStream("/icons/register.png"));
        ImageView view = new ImageView(image);
        view.setLayoutY(label.getBoundsInLocal().getHeight() + 5);

        label.layoutXProperty().bind(Bindings.createDoubleBinding(
                () -> (image.getWidth() - label.getBoundsInLocal().getWidth()) / 2.0,
                image.widthProperty(), label.boundsInLocalProperty()));

        return new Pane(label, view);
    }

    /** Konštruktor pre umiestnenie na plochu - volaný reflexiou zo SchematicSheet. */
    public Register8(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected List<Pin> createPins() {
        List<Pin> pins = new ArrayList<>();

        // lavy okraj: dátové vstupy a riadiace signály (prvý riadok je prázdny)
        dataPins = new Pin[8];
        for (int index = 0; index < 8; index++) {
            dataPins[index] = new InputPin(this, "D" + index, 0, index + 1, Side.LEFT);
            pins.add(dataPins[index]);
        }
        pinOE_ = new InputPin(this, "OE_", 0, 9, Side.LEFT);
        pinLE = new InputPin(this, "LE", 0, 10, Side.LEFT);
        pins.add(pinOE_);
        pins.add(pinLE);

        // pravy okraj: výstupy (tri-state)
        outputPins = new OutputPin[8];
        for (int index = 0; index < 8; index++) {
            outputPins[index] = new OutputPin(this, "Q" + index, GRID_WIDTH, index + 1, Side.RIGHT);
            pins.add(outputPins[index]);
        }

        return pins;
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();
        Rectangle body = new Rectangle(GRID_WIDTH * cell, GRID_HEIGHT * cell, Color.WHITE);
        body.setStroke(Color.BLACK);
        body.setStrokeWidth(1.5);

        Pane pane = new Pane();
        pane.getChildren().add(body);

        Text title = new Text("REGISTER");
        title.setLayoutX((GRID_WIDTH * cell - title.getBoundsInLocal().getWidth()) / 2.0);
        title.setLayoutY(-cell * 0.2);
        title.setFont(Font.font(cell * 0.6));
        pane.getChildren().add(title);

        for (int index = 0; index < 8; index++) {
            Text dLabel = new Text("D" + index);
            dLabel.setLayoutX(cell * 0.4);
            dLabel.setLayoutY((index + 1) * cell + cell * 0.25);

            Text qLabel = new Text("Q" + index);
            qLabel.setLayoutX(cell * 2.6);
            qLabel.setLayoutY((index + 1) * cell + cell * 0.25);

            pane.getChildren().add(dLabel);
            pane.getChildren().add(qLabel);
        }

        Text oeLabel = new Text("OE_");
        oeLabel.setLayoutX(cell * 0.4);
        oeLabel.setLayoutY(cell * 9.3);

        Text leLabel = new Text("LE");
        leLabel.setLayoutX(cell * 0.4);
        leLabel.setLayoutY(cell * 10.3);

        pane.getChildren().add(oeLabel);
        pane.getChildren().add(leLabel);

        return pane;
    }

    @Override
    public void simulate() {
        // Transparentný latch: počas LE=1 sa každá zmena dátových vstupov zapíše
        // do registra; po zániku LE si register hodnotu podrží (vzorkovanie úrovňou,
        // nie hranou). Simulácia je bezpečná - zbernice zapisujú potenciály okamžite,
        // takže počas LE už hradlá nevidia NC a nedochádza k zápisu čiastočných dát.
        if (isHigh(pinLE)) {
            int newLatch = readData();
            if (newLatch != latch) {
                latch = newLatch;
                logLatchChange();
            }
        }

        // pri aktívnom OE_ (log. 0) outputs hná obsah registra, inak sú v Z
        if (isLow(pinOE_)) {
            driveOutputs(latch);
        } else {
            setOutputsImpedance();
        }
    }

    /**
     * DIAGNOSTIKA (dočasná): pri zmene obsahu registra vypíše novú hodnotu, aktuálnu
     * úroveň LE, stav CA oboch 7-segmentových displejov a obsah druhého registra.
     */
    private void logLatchChange() {
        StringBuilder sb = new StringBuilder("[Diag] ");
        sb.append(getName()).append('@').append(getGridPosX()).append(',').append(getGridPosY());
        sb.append(" latch=0x").append(String.format("%02X", latch & 0xFF));
        sb.append(" LE=").append(isHigh(pinLE) ? '1' : '0');
        Potential.Value leNet = pinLE.getPotential() != null ? pinLE.getPotential().getValue() : null;
        sb.append(" LEnet=").append(leNet != null ? leNet.name() : "null");
        sb.append(" IOC=").append(leNet != null && leNet == Potential.Value.NC ? "X" : "ok");
        if (getSheet() != null) {
            for (GateSymbol gate : getSheet().getGates()) {
                if (gate == this || !(gate instanceof Register8)) continue;
                sb.append(" | otherREG=0x").append(String.format("%02X", ((Register8) gate).getLatch() & 0xFF));
            }
            for (GateSymbol gate : getSheet().getGates()) {
                if (!(gate instanceof SevenSegmentDisplay)) continue;
                SevenSegmentDisplay d = (SevenSegmentDisplay) gate;
                sb.append(" | CA").append('(').append(d.getGridPosX()).append(',').append(d.getGridPosY()).append(')')
                        .append(d.isCaActive() ? "=ON" : "=off");
            }
        }
        System.out.println(sb);
    }

    @Override
    public void reset() {
        if (getPins() == null) return;
        for (Pin pin : getPins()) pin.setState(Pin.PinState.NOT_CONNECTED);
    }

    private int readData() {
        int value = 0;
        for (int index = 0; index < 8; index++) {
            if (isHigh(dataPins[index])) value |= 1 << index;
        }
        return value & 0xFF;
    }

    private void driveOutputs(int value) {
        for (int index = 0; index < 8; index++) {
            setPin(outputPins[index],
                    ((value & (1 << index)) != 0) ? Pin.PinState.HIGH : Pin.PinState.LOW);
        }
    }

    private void setOutputsImpedance() {
        for (int index = 0; index < 8; index++) {
            setPin(outputPins[index], Pin.PinState.HIGH_IMPEDANCE);
        }
    }

    /**
     * Aktuálny obsah registra (iba na čítanie v testoch).
     */
    public int getLatch() {
        return latch;
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
        return "Register 8";
    }

    @Override
    public String getShortDescription() {
        return "8-bitový register (D-type latch) - LE=1 zapisuje dáta, OE_=0 povoľuje výstupy";
    }
}