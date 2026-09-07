package sk.uniza.fri.cp.SchematicSim.Pin;

import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;
import sk.uniza.fri.cp.SchematicSim.Side;

/**
 * Výstupný vývod súčiastky (push-pull). Prevzaté z BreadboardSim.OutputPin, adaptované do nového modelu.
 * Pre budúce tri-state hradlá by sem pribudol PinDriver parameter ako v origináli.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class OutputPin extends Pin {

    public OutputPin(GateSymbol owner, String name, int gridOffsetX, int gridOffsetY, Side side) {
        super(owner, name, Direction.OUTPUT, gridOffsetX, gridOffsetY, side);
    }
}
