package sk.uniza.fri.cp.SchematicSim.Buses;

import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

public class AddressBus16 extends BusSymbol {

    private static String[] pinNames() {
        String[] names = new String[16];
        for (int index = 0; index < names.length; index++) {
            names[index] = "A" + index;
        }
        return names;
    }

    public AddressBus16() {
        super("ADRESNÁ 16", pinNames());
    }

    public AddressBus16(SchematicSheet sheet) {
        super(sheet, "ADRESNÁ 16", pinNames());
    }
}
