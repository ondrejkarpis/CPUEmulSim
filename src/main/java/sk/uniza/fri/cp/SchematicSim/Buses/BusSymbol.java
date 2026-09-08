package sk.uniza.fri.cp.SchematicSim.Buses;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.Bus.Bus;
import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Side;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstraktná zbernica na schéme - spája logické hradlá so zbernicou CPU
 * (adresnou, dátovou, riadiacou). Nahrádza {@code BusInterface} z BreadboardSim.
 *
 * @author Tomáš Hianik (pôvodný autor BusInterface), adaptácia pre SchematicSim
 */
public abstract class BusSymbol extends GateSymbol {

    protected BusSymbol() {
        super();
    }

    protected BusSymbol(SchematicSheet sheet) {
        super(sheet);
    }

    protected abstract String getBusLabel();

    protected abstract String[] getBusPinNames();

    protected Pin createBusPin(String name, int index, Pin.Direction direction) {
        return new BusPin(this, name, 0, index, Side.LEFT, direction);
    }

    @Override
    protected List<Pin> createPins() {
        List<Pin> pins = new ArrayList<>();
        String[] pinNames = getBusPinNames();
        for (int index = 0; index < pinNames.length; index++) {
            pins.add(createBusPin(pinNames[index], index, Pin.Direction.INOUT));
        }
        return pins;
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();
        Rectangle body = new Rectangle(getGridWidth() * cell, getGridHeight() * cell, Color.LIGHTYELLOW);
        body.setStroke(Color.DARKGOLDENROD);
        body.setStrokeWidth(1.5);

        Text title = new Text(getBusLabel());
        title.setLayoutX(cell * 1.2);
        title.setLayoutY(cell * 0.8);

        return new Pane(body, title);
    }

    /**
     * Obnova hodnôt zo zbernice na vývody. Volá sa pri zapojení (konštruktro) a
     * pri každom spustení simulácie (SchematicSimulator).
     */
    public abstract void syncFromBus();

    @Override
    public void simulate() {
    }

    @Override
    public void reset() {
    }

    @Override
    public int getGridWidth() {
        return 5;
    }

    @Override
    public int getGridHeight() {
        return getBusPinNames().length;
    }

    @Override
    public String getName() {
        return getBusLabel();
    }

    @Override
    public String getShortDescription() {
        return getBusLabel() + " zbernica (" + getBusPinNames().length + " bitov)";
    }

    public Bus getBus() {
        return Bus.getBus();
    }

    private static class BusPin extends Pin {
        BusPin(BusSymbol owner, String name, int gridOffsetX, int gridOffsetY, Side side, Direction direction) {
            super(owner, name, direction, gridOffsetX, gridOffsetY, side);
        }
    }
}
