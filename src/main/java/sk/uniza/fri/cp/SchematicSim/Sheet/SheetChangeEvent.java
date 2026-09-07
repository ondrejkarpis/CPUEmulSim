package sk.uniza.fri.cp.SchematicSim.Sheet;

import sk.uniza.fri.cp.SchematicSim.Electrical.Potential;
import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;

import java.util.Set;

/**
 * Zmenová udalosť v simulácii - zmena hodnoty potenciálu na výstupnom pine.
 * Nahrádza {@code BoardChangeEvent} z BreadboardSim, Socket -> Pin. Logika nezmenená.
 *
 * @author Tomáš Hianik (pôvodný autor BoardChangeEvent), adaptácia pre SchematicSim
 */
public class SheetChangeEvent extends SheetEvent {

    private final Potential.Value newValue;

    public SheetChangeEvent(Pin pin, Potential.Value newValue) {
        super(pin);
        this.newValue = newValue;
    }

    public Potential.Value getValue() {
        return newValue;
    }

    @Override
    public void process(Set<GateSymbol> gatesToUpdate) {
        Pin pin = getPin();
        if (gatesToUpdate != null && pin != null) {
            Potential potential = pin.getPotential();

            if (potential != null) {
                Potential.Value oldValue = potential.getValue();

                // ak nenastal skrat (a ak by nastal, nedoplnia sa súčiastky, ktoré ho vyvolali - cyklenie)
                if (potential.setValue(newValue)) {
                    // a ak sa hodnota zmenila
                    if (oldValue != potential.getValue()) {
                        potential.getGatesWithInputs(gatesToUpdate);
                    }
                }
            }
        }
    }
}
