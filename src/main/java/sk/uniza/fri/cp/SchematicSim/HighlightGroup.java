package sk.uniza.fri.cp.SchematicSim;

import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

/**
 * Výber a zvýraznenie objektov na ploche. Prevzaté z BreadboardSim.HighlightGroup,
 * iba Board -> SchematicSheet.
 *
 * @author Tomáš Hianik (pôvodný autor), adaptácia pre SchematicSim
 */
public abstract class HighlightGroup extends Group implements Selectable {

    private Pane cachedDescription;
    private boolean isSelectable;
    private boolean isSelected;

    protected HighlightGroup() {
        this.isSelectable = true;

        EventHandler<MouseEvent> onMouseClickEventHandler = event -> {
            if (!event.isPrimaryButtonDown()) return;
            SchematicSheet sheet = getSheet();

            if (sheet != null) {
                if (event.isShiftDown()) {
                    if (isSelected) {
                        sheet.removeSelect(this);
                    } else {
                        sheet.addSelect(this);
                    }
                } else if (!isSelected) {
                    sheet.clearSelect();
                    sheet.addSelect(this);
                }
            }
        };

        this.addEventHandler(MouseEvent.MOUSE_PRESSED, onMouseClickEventHandler);
    }

    public Pane getDescription() {
        return this.cachedDescription;
    }

    protected void cacheDescription(Pane descriptionPane) {
        this.cachedDescription = descriptionPane;
    }

    @Override
    public void select() {
        this.isSelected = true;
    }

    @Override
    public void deselect() {
        this.isSelected = false;
    }

    @Override
    public void delete() {
        this.isSelected = false;
    }

    @Override
    public boolean isSelectable() {
        return this.isSelectable;
    }

    @Override
    public void setSelectable(boolean newValue) {
        this.isSelectable = newValue;
    }

    public boolean isSelected() {
        return this.isSelected;
    }
}
