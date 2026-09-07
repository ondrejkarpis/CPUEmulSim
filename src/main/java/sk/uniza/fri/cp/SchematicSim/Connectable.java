package sk.uniza.fri.cp.SchematicSim;

import javafx.geometry.Point2D;
import sk.uniza.fri.cp.SchematicSim.Electrical.Potential;

/**
 * Elektrický koncový bod, ktorý môže niesť {@link Potential} a byť pripojený vodičom.
 * V tomto modeli ho implementuje výhradne {@link sk.uniza.fri.cp.SchematicSim.Pin.Pin}
 * (nahrádza pôvodný {@code Socket} z BreadboardSim).
 * <p>
 * POZOR - toto zámerne NEimplementuje {@code Joint}/{@code WireEnd}: presne ako
 * v pôvodnom kóde ({@code Wire.updatePotential()}) sa {@link Potential} vytvára vždy
 * medzi dvomi pinmi, ku ktorým sú konce vodiča pripojené, nie medzi samotnými koncami
 * vodiča. Joint/WireEnd sú čisto grafické body na trase vodiča.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public interface Connectable {

    int WARNING = 4;
    int COMMON_POTENTIAL_OTHER = 3;
    int COMMON_POTENTIAL = 2;
    int OK = 1;
    int INFO = 0;

    /**
     * Potenciál na vrchu stromu spojení, ku ktorému je bod aktuálne pripojený.
     */
    Potential getPotential();

    /**
     * Pozícia bodu v súradniciach plochy schémy (v pixeloch), pre vykresľovanie vodičov.
     */
    Point2D getSceneGridPosition();

    /**
     * Smer, ktorým musí vodič z tohto bodu vychádzať kolmo (pre ortogonálny router).
     */
    Side getExitSide();

    /**
     * Je už na tento bod pripojený vodič?
     */
    boolean isOccupied();

    void highlight(int highlightType);

    void unhighlight(int highlightType);

    /**
     * Item vlastniaci tento bod (súčiastka) - kvôli výberu/popisu.
     */
    Item getOwnerItem();
}
