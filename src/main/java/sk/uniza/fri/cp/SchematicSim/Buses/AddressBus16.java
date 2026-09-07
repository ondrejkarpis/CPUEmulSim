package sk.uniza.fri.cp.SchematicSim.Buses;

import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

public class AddressBus16 extends BusSymbol {

    private static final String[] PIN_NAMES = createPinNames();

    private static String[] createPinNames() {
        String[] names = new String[16];
        for (int index = 0; index < names.length; index++) {
            names[index] = "A" + index;
        }
        return names;
    }

    public AddressBus16() {
        super();
    }

    public AddressBus16(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected String getBusLabel() {
        return "ADRESNÁ 16";
    }

    @Override
    protected String[] getBusPinNames() {
        return PIN_NAMES;
    }
}
