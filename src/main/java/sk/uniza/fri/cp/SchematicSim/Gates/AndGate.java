package sk.uniza.fri.cp.SchematicSim.Gates;

import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

/**
 * Logické hradlo AND s premenlivým počtom vstupov (2, 3, 4 alebo 8 - cez kontextové menu).
 * Y = 1 práve vtedy, keď sú všetky vstupy vo vysokom stave.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class AndGate extends MultiInputGate {

    public AndGate() {
        super();
    }

    public AndGate(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected String getSymbolText() {
        return "&";
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
        setPin(getOutput(), allHigh ? Pin.PinState.HIGH : Pin.PinState.LOW);
    }

    @Override
    public void reset() {
        setPinForce(getOutput(), Pin.PinState.NOT_CONNECTED);
    }

    @Override
    public String getName() {
        return "AND";
    }

    @Override
    public String getShortDescription() {
        return "2-8 vstupové logické hradlo AND (Y = A · B · ...)";
    }
}