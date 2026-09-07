package sk.uniza.fri.cp.SchematicSim.Pin;

import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;
import sk.uniza.fri.cp.SchematicSim.Side;

/**
 * Vstupný vývod súčiastky. Prevzaté z BreadboardSim.InputPin, adaptované do nového modelu.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class InputPin extends Pin {

    public InputPin(GateSymbol owner, String name, int gridOffsetX, int gridOffsetY, Side side) {
        super(owner, name, Direction.INPUT, gridOffsetX, gridOffsetY, side);
    }
}
