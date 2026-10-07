package sk.uniza.fri.cp.SchematicSim;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;

/**
 * Item je objekt na ploche schémy alebo v paletke dostupných objektov (ItemPicker).
 * Prevzaté z BreadboardSim.Item - narozdiel od pôvodnej verzie tu nie je výnimka pre Chip
 * (žiadny reálny čip so špecifickým vzhľadom pri výbere v tomto modeli neexistuje).
 *
 * @author Tomáš Hianik (pôvodný autor), adaptácia pre SchematicSim
 */
public abstract class Item extends Movable {

    /** Medza medzi hranicou súčiastky a čiarkovaným obrysom výberu. */
    private static final double SELECTION_OFFSET = 1;
    /** navyše k {@link #SELECTION_OFFSET} na ľavej a hornej strane. */
    private static final double SELECTION_LEFT_TOP_EXTRA = 1;
    /** navyše k {@link #SELECTION_OFFSET} na pravej a dolnej strane. */
    private static final double SELECTION_RIGHT_BOTTOM_EXTRA = 1;

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

        this.selectionShape = new Rectangle();
        this.selectionShape.setFill(null);
        for (double value : STROKE_DASH_ARRAY) this.selectionShape.getStrokeDashArray().add(value);
        this.selectionShape.setStrokeWidth(2);
        this.selectionShape.setStroke(Color.BLACK);
        this.selectionShape.setStrokeLineCap(StrokeLineCap.ROUND);
        this.selectionShape.setOpacity(0.8);
        applySelectionBounds();

        this.getChildren().add(this.selectionShape);
    }

    @Override
    public void deselect() {
        super.deselect();
        this.getChildren().remove(this.selectionShape);
    }

    /**
     * Hranice pre čiarkovaný obrys výberu: súčin vizuálnych potomkov (telo, popisky,
     * úchytky) bez pinov. Piny majú neviditeľnú hit-áreasu presahujúcu telo súčiastky
     * a keďže väčšinou visia na ľavom/hornom okraji, obrys by bol výrazne posunutý
     * doľava a hore namiesto symetrie okolo značky.
     *
     * <p>Počíta sa rekurzívne od listov, pretože hranice Regionu (Pane) sú orezané
     * na počiatočný bod (0,0): keď vizuál začína až s kladnými súradnicami
     * (LED, Input, kľúče MatrixKeyboard), obrys by bol posunutý doľava a hore.
     */
    private Bounds selectionBounds() {
        Bounds[] bounds = new Bounds[1];
        for (Node child : getChildren()) {
            accumulateSelectionBounds(child, 0, 0, bounds);
        }
        return bounds[0] != null ? bounds[0] : getBoundsInLocal();
    }

    /**
     * Rozšíri {@code bounds} o hranice uzla {@code node} v súradnicovom systéme
     * položky. {@code offsetX}/{@code offsetY} je offset predkov; vlastný layout
     * uzla už je zahrnutý v jeho {@code getBoundsInParent()}.
     */
    private void accumulateSelectionBounds(Node node, double offsetX, double offsetY, Bounds[] bounds) {
        if (node == this.selectionShape || node instanceof Pin) return;

        if (node instanceof Parent) {
            double childOffsetX = offsetX + node.getLayoutX();
            double childOffsetY = offsetY + node.getLayoutY();
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                accumulateSelectionBounds(child, childOffsetX, childOffsetY, bounds);
            }
            return;
        }

        Bounds shapeBounds = node.getBoundsInParent();
        if (shapeBounds.getWidth() <= 0 && shapeBounds.getHeight() <= 0) return;

        double minX = shapeBounds.getMinX() + offsetX;
        double minY = shapeBounds.getMinY() + offsetY;
        double maxX = shapeBounds.getMaxX() + offsetX;
        double maxY = shapeBounds.getMaxY() + offsetY;

        if (bounds[0] == null) {
            bounds[0] = new BoundingBox(minX, minY, maxX - minX, maxY - minY);
        } else {
            Bounds current = bounds[0];
            double newMinX = Math.min(current.getMinX(), minX);
            double newMinY = Math.min(current.getMinY(), minY);
            double newMaxX = Math.max(current.getMaxX(), maxX);
            double newMaxY = Math.max(current.getMaxY(), maxY);
            bounds[0] = new BoundingBox(newMinX, newMinY, newMaxX - newMinX, newMaxY - newMinY);
        }
    }

    /**
     * Prekreslenie čiarkovaného obdĺžnika výberu po zmene rozmerov súčiastky
     * (napr. predĺženie zbernice). Pôvodný obdĺžnik je len snímka rozmerov z okamihu
     * výberu, preto je ho treba pri zmene veľkosti prepočítať.
     */
    protected void refreshSelectionShape() {
        if (selectionShape == null) return;
        boolean removed = getChildren().remove(selectionShape);
        if (!removed) return;
        applySelectionBounds();

        getChildren().add(selectionShape);
    }

    /**
     * Umiestnenie a veľkosť čiarkovaného obrysu podľa aktuálnych hraníc súčiastky.
     * Ľavá a horná hrana dostane o {@link #SELECTION_LEFT_TOP_EXTRA} px väčšiu medzeru,
     * pravá a spodná o {@link #SELECTION_RIGHT_BOTTOM_EXTRA} px.
     */
    private void applySelectionBounds() {
        Bounds bounds = selectionBounds();
        this.selectionShape.setLayoutX(bounds.getMinX() - SELECTION_OFFSET - SELECTION_LEFT_TOP_EXTRA);
        this.selectionShape.setLayoutY(bounds.getMinY() - SELECTION_OFFSET - SELECTION_LEFT_TOP_EXTRA);
        this.selectionShape.setWidth(bounds.getWidth() + 2 * SELECTION_OFFSET
                + SELECTION_LEFT_TOP_EXTRA + SELECTION_RIGHT_BOTTOM_EXTRA);
        this.selectionShape.setHeight(bounds.getHeight() + 2 * SELECTION_OFFSET
                + SELECTION_LEFT_TOP_EXTRA + SELECTION_RIGHT_BOTTOM_EXTRA);
    }
}
