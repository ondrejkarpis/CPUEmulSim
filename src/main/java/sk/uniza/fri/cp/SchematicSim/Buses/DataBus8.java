package sk.uniza.fri.cp.SchematicSim.Buses;

import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

public class DataBus8 extends BusSymbol {

    private static final String[] PIN_NAMES = createPinNames();

    private static String[] createPinNames() {
        String[] names = new String[8];
        for (int index = 0; index < names.length; index++) {
            names[index] = "D" + index;
        }
        return names;
    }

    public DataBus8() {
        super();
    }

    public DataBus8(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected String getBusLabel() {
        return "DÁTOVÁ 8";
    }

    @Override
    protected String[] getBusPinNames() {
        return PIN_NAMES;
    }
}
