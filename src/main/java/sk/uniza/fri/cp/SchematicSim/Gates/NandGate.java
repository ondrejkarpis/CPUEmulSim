package sk.uniza.fri.cp.SchematicSim.Gates;

import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

/**
 * Logické hradlo NAND s premenlivým počtom vstupov (2, 3, 4 alebo 8 - cez kontextové menu).
 * Y = 0 práve vtedy, keď sú všetky vstupy vo vysokom stave (inak Y = 1).
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class NandGate extends MultiInputGate {

    public NandGate() {
        super();
    }

    public NandGate(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected String getSymbolText() {
        return "&";
    }

    @Override
    protected boolean negated() {
        return true;
    }

    @Override
    public void simulate() {
        boolean allHigh = true;
        for (Pin input : inputPins()) {
            if (!isHigh(input)) {
                allHigh = false;
                break;
            }
        }
        setPin(getOutput(), allHigh ? Pin.PinState.LOW : Pin.PinState.HIGH);
    }

    @Override
    public void reset() {
        setPinForce(getOutput(), Pin.PinState.NOT_CONNECTED);
    }

    @Override
    public String getName() {
        return "NAND";
    }

    @Override
    public String getShortDescription() {
        return "2-8 vstupové logické hradlo NAND (Y = NOT (A · B · ...))";
    }
}