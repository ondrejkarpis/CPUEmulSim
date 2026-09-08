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
            // Nová hodnota sa zapíše na vlastný list pinu (getOwnedPotential()). Odtiaľ sa
            // cez Potential.setValue rozšíri do agregovaného potenciálu siete (vodiča), ktorý
            // počíta svoju hodnotu z hodnôt svojich predkov (listov). Keby sa zapisovalo na
            // pin.getPotential() (agregát), nikdy by sa zmena neprejavila - agregát iba
            // zoskupuje hodnoty predkov a tie sa nikde neplnili, takže vodič ostával "Z".
            Potential owned = pin.getOwnedPotential();
            Potential aggregate = pin.getPotential();

            if (owned != null && aggregate != null) {
                Potential.Value oldValue = aggregate.getValue();

                // ak nenastal skrat (a ak by nastal, nedoplnia sa súčiastky, ktoré ho vyvolali - cyklenie)
                if (owned.setValue(newValue)) {
                    // a ak sa hodnota v sieti zmenila, doplnia sa súčiastky so vstupmi na tejto sieti
                    if (oldValue != aggregate.getValue()) {
                        aggregate.getGatesWithInputs(gatesToUpdate);
                    }
                }
            }
        }
    }
}
