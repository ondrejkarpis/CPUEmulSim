package sk.uniza.fri.cp.SchematicSim.Electrical;

/**
 * Typ budiča pripojeného k bodu siete (nahrádza pôvodný SocketType z BreadboardSim).
 * <p>
 * NC       - nepripojené, neovplyvňuje hodnotu potenciálu
 * IN       - vstup, neovplyvňuje hodnotu potenciálu (iba ju číta)
 * OUT      - klasický výstup (push-pull); pri spojení dvoch OUT s rôznou hodnotou nastáva skrat
 * IO       - vstupno/výstupný (napr. budúci tri-state driver)
 * TRI_OUT  - výstup s tromi stavmi (vysoká impedancia = "odpojené")
 * WEAK_OUT - slabý výstup (napr. budúci pull-up/pull-down rezistor)
 *
 * @author Tomáš Hianik (pôvodný autor SocketType), adaptácia pre SchematicSim
 */
public enum PinType {
    NC, IN, OUT, IO, TRI_OUT, WEAK_OUT
}
