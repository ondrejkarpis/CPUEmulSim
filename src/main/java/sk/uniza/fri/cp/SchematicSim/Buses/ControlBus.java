package sk.uniza.fri.cp.SchematicSim.Buses;

import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

public class ControlBus extends BusSymbol {

    private static final String[] PIN_NAMES = {"MEMR", "MEMW", "IOR", "IOW"};

    public ControlBus() {
        super("RIADIACA", PIN_NAMES);
    }

    public ControlBus(SchematicSheet sheet) {
        super(sheet, "RIADIACA", PIN_NAMES);
    }
}
