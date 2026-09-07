package sk.uniza.fri.cp.SchematicSim.Sheet;

import sk.uniza.fri.cp.SchematicSim.Electrical.Potential;
import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;

import java.util.Set;

/**
 * Aktualizačná udalosť nad pinom v simulácii. Nahrádza {@code BoardEvent} z BreadboardSim,
 * Socket -> Pin.
 *
 * @author Tomáš Hianik (pôvodný autor BoardEvent), adaptácia pre SchematicSim
 */
public class SheetEvent {

    private final Pin pin;

    public SheetEvent(Pin pin) {
        this.pin = pin;
    }

    public Pin getPin() {
        return pin;
    }

    public void process(Set<GateSymbol> gatesToUpdate) {
        if (gatesToUpdate != null && pin != null) {
            Potential potential = pin.getPotential();
            if (potential != null) potential.getGatesWithInputs(gatesToUpdate);
        }
    }
}
