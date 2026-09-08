package sk.uniza.fri.cp.SchematicSim.Buses;

import javafx.beans.value.ChangeListener;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

import java.util.List;

/**
 * Riadiaca zbernica. Výstupné signály (MW_, MR_, IW_, IR_, IA_) sú riadené CPU
 * a premietajú sa na vývody, ktoré ovládajú logiku obvodu. Vstupné signály
 * (IT, RY) sú riadené obvodom a CPU ich číta zo zbernice.
 *
 * @author Tomáš Hianik (pôvodný autor ControlBusCommunicator), adaptácia pre SchematicSim
 */
public class ControlBus extends BusSymbol {

    private static final String[] PIN_NAMES = {"MW_", "MR_", "IW_", "IR_", "IA_", "RY", "IT"};

    // bity riadiacej zbernice (pozri Bus.mapSignal)
    private static final int[] BIT_OF_PIN = {8, 7, 6, 5, 4, 2, 3};
    // ktoré vývody sú vstupné (obvod -> CPU) - RY a IT
    private static final boolean[] INPUT_PIN = {false, false, false, false, false, true, true};

    private Pin[] controlPins;

    private final ChangeListener<Number> onControlBusChange = (obs, oldValue, newValue) -> syncFromBus();

    public ControlBus() {
        super();
    }

    public ControlBus(SchematicSheet sheet) {
        super(sheet);
        getBus().controlBusProperty().addListener(onControlBusChange);
        syncFromBus();
    }

    @Override
    protected List<Pin> createPins() {
        List<Pin> pins = new java.util.ArrayList<>();
        Pin[] arr = new Pin[PIN_NAMES.length];
        for (int index = 0; index < arr.length; index++) {
            arr[index] = createBusPin(PIN_NAMES[index], index,
                    INPUT_PIN[index] ? Pin.Direction.INPUT : Pin.Direction.OUTPUT);
            pins.add(arr[index]);
        }
        this.controlPins = arr;
        return pins;
    }

    @Override
    protected String getBusLabel() {
        return "RIADIACA";
    }

    @Override
    protected String[] getBusPinNames() {
        return PIN_NAMES;
    }

    @Override
    public void syncFromBus() {
        if (controlPins == null) return;
        int ctrl = getBus().getControlBus();
        for (int index = 0; index < controlPins.length; index++) {
            if (INPUT_PIN[index]) continue; // vstupné signály sa nenastavujú zo zbernice
            boolean high = (ctrl & (1 << BIT_OF_PIN[index])) != 0;
            setPinForce(controlPins[index], high ? Pin.PinState.HIGH : Pin.PinState.LOW);
        }
    }

    @Override
    public void simulate() {
        if (controlPins == null) return;
        // simulujeme zápis na zbernicu pre vstupné signály (RY, IT)
        for (int index = 0; index < controlPins.length; index++) {
            if (!INPUT_PIN[index]) continue;
            boolean high = isHigh(controlPins[index]);
            if (BIT_OF_PIN[index] == 2) getBus().setRY(high);
            else if (BIT_OF_PIN[index] == 3) getBus().setIT(high);
        }
    }

    @Override
    public void reset() {
        if (controlPins == null) return;
        for (Pin p : controlPins) p.setState(Pin.PinState.NOT_CONNECTED);
    }
}
