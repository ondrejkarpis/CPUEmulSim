package sk.uniza.fri.cp.SchematicSim;

import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

/**
 * Prevzaté bezo zmeny logiky z BreadboardSim.Selectable, iba typované na {@link SchematicSheet}.
 *
 * @author Tomáš Hianik (pôvodný autor), adaptácia pre SchematicSim
 */
public interface Selectable {

    double[] STROKE_DASH_ARRAY = {10d, 5d};

    SchematicSheet getSheet();

    Pane getDescription();

    void select();

    void deselect();

    void delete();

    boolean isSelectable();

    void setSelectable(boolean newValue);
}
