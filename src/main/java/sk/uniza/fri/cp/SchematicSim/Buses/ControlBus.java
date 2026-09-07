package sk.uniza.fri.cp.SchematicSim.Buses;

import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

public class ControlBus extends BusSymbol {

    private static final String[] PIN_NAMES = {"MEMR", "MEMW", "IOR", "IOW"};

    public ControlBus() {
        super();
    }

    public ControlBus(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected String getBusLabel() {
        return "RIADIACA";
    }

    @Override
    protected String[] getBusPinNames() {
        return PIN_NAMES;
    }
}
