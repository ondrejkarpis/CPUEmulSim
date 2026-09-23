package sk.uniza.fri.cp.SchematicSim;

import javafx.geometry.Bounds;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

/**
 * Item je objekt na ploche schémy alebo v paletke dostupných objektov (ItemPicker).
 * Prevzaté z BreadboardSim.Item - narozdiel od pôvodnej verzie tu nie je výnimka pre Chip
 * (žiadny reálny čip so špecifickým vzhľadom pri výbere v tomto modeli neexistuje).
 *
 * @author Tomáš Hianik (pôvodný autor), adaptácia pre SchematicSim
 */
public abstract class Item extends Movable {

    private Rectangle selectionShape;

    public Item(SchematicSheet sheet) {
        super(sheet);
    }

    public Item(SchematicSheet sheet, int gridPosX, int gridPosY) {
        this(sheet);
        moveTo(gridPosX, gridPosY);
    }

    /**
     * Bezparametrický konštruktor slúži na vytvorenie inštancie objektu pre ItemPicker (paletku).
     */
    public Item() {
        this(null);
        makeImmovable();
        this.getChildren().add(getImage());
    }

    /**
     * Obrázok zobrazený v paletke ItemPicker. Konkrétne súčiastky (napr. GateSymbol potomkovia)
     * toto prekrývajú svojou schematickou značkou.
     */
    public Pane getImage() {
        Text itemName = new Text(this.getClass().getSimpleName());
        itemName.setLayoutX(5);
        itemName.setLayoutY(itemName.getBoundsInParent().getHeight());
        return new Pane(new Rectangle(itemName.getBoundsInParent().getWidth() + 10, 30, Color.WHITESMOKE), itemName);
    }

    @Override
    public void select() {
        super.select();
        if (!this.isSelectable()) return;

        double offset = 1;
        Bounds bounds = this.getBoundsInLocal();
        this.selectionShape = new Rectangle(bounds.getWidth() + 2 * offset, bounds.getHeight() + 2 * offset);
        this.selectionShape.setFill(null);
        for (double value : STROKE_DASH_ARRAY) this.selectionShape.getStrokeDashArray().add(value);
        this.selectionShape.setStrokeWidth(2);
        this.selectionShape.setStroke(Color.BLACK);
        this.selectionShape.setStrokeLineCap(StrokeLineCap.ROUND);
        this.selectionShape.setOpacity(0.8);
        this.selectionShape.setLayoutX(bounds.getMinX() - offset);
        this.selectionShape.setLayoutY(bounds.getMinY() - offset);

        this.getChildren().add(this.selectionShape);
    }

    @Override
    public void deselect() {
        super.deselect();
        this.getChildren().remove(this.selectionShape);
    }

    /**
     * Prekreslenie čiarkovaného obdĺžnika výberu po zmene rozmerov súčiastky
     * (napr. predĺženie zbernice). Pôvodný obdĺžnik je len snímka rozmerov z okamihu
     * výberu, preto je ho treba pri zmene veľkosti prepočítať.
     */
    protected void refreshSelectionShape() {
        if (selectionShape == null) return;
        double offset = 1;
        boolean removed = getChildren().remove(selectionShape);
        if (!removed) return;
        Bounds bounds = getBoundsInLocal();
        selectionShape.setLayoutX(bounds.getMinX() - offset);
        selectionShape.setLayoutY(bounds.getMinY() - offset);
        selectionShape.setWidth(bounds.getWidth() + 2 * offset);
        selectionShape.setHeight(bounds.getHeight() + 2 * offset);

        getChildren().add(selectionShape);
    }
}
