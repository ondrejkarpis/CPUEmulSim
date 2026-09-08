package sk.uniza.fri.cp.SchematicSim.Buses;

import javafx.beans.value.ChangeListener;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

import java.util.List;

/**
 * Adresná 16-bitová zbernica. Výstupy A0..A15 sú tvorené CPU - zbernica ich
 * len premieta na vývody, ktoré sa pripájajú k logickým hradlám.
 *
 * @author Tomáš Hianik (pôvodný autor AddressBusCommunicator), adaptácia pre SchematicSim
 */
public class AddressBus16 extends BusSymbol {

    private static final String[] PIN_NAMES = createPinNames();

    private static String[] createPinNames() {
        String[] names = new String[16];
        for (int index = 0; index < names.length; index++) {
            names[index] = "A" + index;
        }
        return names;
    }

    private Pin[] addressPins;

    private final ChangeListener<Number> onAddressBusChange = (obs, oldValue, newValue) -> syncFromBus();

    public AddressBus16() {
        super();
    }

    public AddressBus16(SchematicSheet sheet) {
        super(sheet);
        getBus().addressBusProperty().addListener(onAddressBusChange);
        syncFromBus();
    }

    @Override
    protected List<Pin> createPins() {
        List<Pin> pins = new java.util.ArrayList<>();
        Pin[] arr = new Pin[16];
        for (int index = 0; index < arr.length; index++) {
            arr[index] = createBusPin("A" + index, index, Pin.Direction.OUTPUT);
            pins.add(arr[index]);
        }
        this.addressPins = arr;
        return pins;
    }

    @Override
    protected String getBusLabel() {
        return "ADRESNÁ 16";
    }

    @Override
    protected String[] getBusPinNames() {
        return PIN_NAMES;
    }

    @Override
    public void syncFromBus() {
        if (addressPins == null) return;
        int address = Short.toUnsignedInt(getBus().getAddressBus());
        for (int index = 0; index < addressPins.length; index++) {
            setPinForce(addressPins[index],
                    ((address & (1 << index)) != 0) ? Pin.PinState.HIGH : Pin.PinState.LOW);
        }
    }

    @Override
    public void simulate() {
    }

    @Override
    public void reset() {
        if (addressPins == null) return;
        for (Pin p : addressPins) p.setState(Pin.PinState.NOT_CONNECTED);
    }
}
