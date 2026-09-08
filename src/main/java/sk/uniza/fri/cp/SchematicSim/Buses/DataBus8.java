package sk.uniza.fri.cp.SchematicSim.Buses;

import javafx.beans.value.ChangeListener;
import sk.uniza.fri.cp.Bus.Bus;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

import java.util.List;

/**
 * Dátová 8-bitová zbernica (dvojmerná). Podľa riadiacich signálov CPU buď
 * premieta dáta zo zbernice na vývody (zápis), alebo číta hodnotu z vývodov
 * obvodu a ukladá ju na zbernicu (čítanie). V čítaní sú vývody v stave vysokej
 * impedancie, aby mohol obvod odpovedať.
 *
 * @author Tomáš Hianik (pôvodný autor DataBusCommunicator), adaptácia pre SchematicSim
 */
public class DataBus8 extends BusSymbol {

    private static final String[] PIN_NAMES = createPinNames();

    private static String[] createPinNames() {
        String[] names = new String[8];
        for (int index = 0; index < names.length; index++) {
            names[index] = "D" + index;
        }
        return names;
    }

    private Pin[] dataPins;

    private volatile boolean read;
    private volatile boolean write;
    private volatile int data;

    private final ChangeListener<Number> onDataBusChange = (obs, oldValue, newValue) -> handleDataChange();
    private final ChangeListener<Number> onControlBusChange = (obs, oldValue, newValue) -> handleControlChange();

    public DataBus8() {
        super();
    }

    public DataBus8(SchematicSheet sheet) {
        super(sheet);
        getBus().dataBusProperty().addListener(onDataBusChange);
        getBus().controlBusProperty().addListener(onControlBusChange);
        this.data = Byte.toUnsignedInt(getBus().getDataBus());
        syncFromBus();
    }

    @Override
    protected List<Pin> createPins() {
        List<Pin> pins = new java.util.ArrayList<>();
        Pin[] arr = new Pin[8];
        for (int index = 0; index < arr.length; index++) {
            arr[index] = createBusPin("D" + index, index, Pin.Direction.INOUT);
            pins.add(arr[index]);
        }
        this.dataPins = arr;
        return pins;
    }

    @Override
    protected String getBusLabel() {
        return "DÁTOVÁ 8";
    }

    @Override
    protected String[] getBusPinNames() {
        return PIN_NAMES;
    }

    @Override
    public void syncFromBus() {
        if (dataPins == null) return;
        // pri čítaní sa vývody riadia obvodom, nie zbernicou - vývody necháme plávať
        int ctrl = getBus().getControlBus();
        boolean readActive = (ctrl & 0xB0) != 0xB0;
        boolean writeActive = (ctrl & 0x140) != 0x140;
        this.data = Byte.toUnsignedInt(getBus().getDataBus());
        this.read = readActive;
        this.write = writeActive;
        if (write) drivePins();
        else setPinsImpedance();
    }

    private void handleDataChange() {
        // ak nie je zapnuté čítanie -> riadime sa podľa dát na zbernici (zápis / pasívny stav)
        if ((getBus().getControlBus() & 0xB0) == 0xB0) {
            this.data = Byte.toUnsignedInt(getBus().getDataBus());
            if (write) drivePins();
        }
        // pri čítaní sa riadime iba podľa simulácie (obvod odpovedá na zbernicu)
    }

    private void handleControlChange() {
        int ctrl = getBus().getControlBus();
        boolean writeActive = (ctrl & 0x140) != 0x140;
        boolean readActive = (ctrl & 0xB0) != 0xB0;

        if (writeActive && !write) {
            // začiatok zápisu - premietneme dáta zo zbernice na vývody
            write = true;
            Bus.getBus().dataIsChanging();
            drivePins();
        } else if (!writeActive && write) {
            // koniec zápisu - vývody odpojíme (obvod si dáta už zachytil)
            write = false;
            Bus.getBus().dataIsChanging();
            setPinsImpedance();
        }

        if (readActive && !read) {
            // začiatok čítania - vývody prepneme do stavu vysokej impedancie,
            // aby mohol obvod odpovedať na zbernicu
            read = true;
            Bus.getBus().dataIsChanging();
            setPinsImpedance();
        } else if (!readActive && read) {
            // koniec čítania
            read = false;
            Bus.getBus().dataIsChanging();
            setPinsImpedance();
        }
    }

    private void drivePins() {
        if (dataPins == null) return;
        for (int i = 0; i < dataPins.length; i++) {
            setPinForce(dataPins[i], ((data & (1 << i)) != 0) ? Pin.PinState.HIGH : Pin.PinState.LOW);
        }
    }

    private void setPinsImpedance() {
        if (dataPins == null) return;
        for (int i = 0; i < dataPins.length; i++) {
            setPinForce(dataPins[i], Pin.PinState.HIGH_IMPEDANCE);
        }
    }

    private void readPinsIntoBus() {
        if (dataPins == null) return;
        int result = this.data;
        for (int i = 0; i < dataPins.length; i++) {
            if (isHigh(dataPins[i])) {
                result |= (1 << i);
            } else if (isLow(dataPins[i])) {
                result &= ~(1 << i);
            }
        }
        if (result != this.data) {
            this.data = result;
            getBus().setDataBus((byte) result);
        }
    }

    @Override
    public void simulate() {
        if (read) {
            Bus.getBus().dataIsChanging();
            readPinsIntoBus();
        } else if (write) {
            Bus.getBus().dataIsChanging();
            drivePins();
        }
    }

    @Override
    public void reset() {
        if (dataPins == null) return;
        for (Pin p : dataPins) p.setState(Pin.PinState.NOT_CONNECTED);
    }
}
