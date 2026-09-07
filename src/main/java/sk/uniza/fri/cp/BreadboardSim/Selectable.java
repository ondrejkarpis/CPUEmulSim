package sk.uniza.fri.cp.BreadboardSim;


import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;

/**
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:35
 */
public interface Selectable {

	double[] STROKE_DASH_ARRAY = {10d, 5d};

    /**
     * Vráti plochu simulátora.
     *
     * @return Plocha simulátora.
     */
    Board getBoard();

    /**
     * Vráti panel s popisom objektu.
     *
     * @return Panel s popisom objektu.
     */
    Pane getDescription();

    /**
     * Zvýraznenie objektu po jeho vybratí.
     */
    void select();

    /**
     * Zrušenie zvýraznenia objektu.
     */
    void deselect();

    /**
     * Zmazanie objektu z plochy simulátora.
     */
    void delete();

    /**
     * Kontrola, či je možné objekt vybrať.
     *
     * @return True ak áno, false inak.
     */
    boolean isSelectable();

    /**
     * Nastavenie možnosti vybrať objekt.
     *
     * @param newValue True - je možné objekt vybrať, false - nie je možné objekt vybrať.
     */
    void setSelectable(boolean newValue);
}