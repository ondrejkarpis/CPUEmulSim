package sk.uniza.fri.cp.SchematicSim.Gates;

import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

/**
 * Logické hradlo OR s premenlivým počtom vstupov (2, 3, 4 alebo 8 - cez kontextové menu).
 * Y = 1 práve vtedy, keď je aspoň jeden vstup vo vysokom stave.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class OrGate extends MultiInputGate {

    public OrGate() {
        super();
    }

    public OrGate(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected String getSymbolText() {
        return "\u2265" + "1";
    }

    @Override
    public void simulate() {
        boolean anyHigh = false;
        for (Pin input : inputPins()) {
            if (isHigh(input)) {
                anyHigh = true;
                break;
            }
        }
        setPin(getOutput(), anyHigh ? Pin.PinState.HIGH : Pin.PinState.LOW);
    }

    @Override
    public void reset() {
        setPinForce(getOutput(), Pin.PinState.NOT_CONNECTED);
    }

    @Override
    public String getName() {
        return "OR";
    }

    @Override
    public String getShortDescription() {
        return "2-8 vstupové logické hradlo OR (Y = A + B + ...)";
    }
}