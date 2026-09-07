package sk.uniza.fri.cp.SchematicSim.Buses;

import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

public class DataBus8 extends BusSymbol {

    private static String[] pinNames() {
        String[] names = new String[8];
        for (int index = 0; index < names.length; index++) {
            names[index] = "D" + index;
        }
        return names;
    }

    public DataBus8() {
        super("DÁTOVÁ 8", pinNames());
    }

    public DataBus8(SchematicSheet sheet) {
        super(sheet, "DÁTOVÁ 8", pinNames());
    }
}
