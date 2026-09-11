package sk.uniza.fri.cp.SchematicSim.Wire;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import sk.uniza.fri.cp.SchematicSim.GridSystem;
import sk.uniza.fri.cp.SchematicSim.Movable;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.LinkedList;

/**
 * Spájač/zlom na kábliku. Prevzaté z BreadboardSim.Wire.Joint bezo zmeny logiky lámania
 * vodiča - pridané sú iba {@link #getExitSide()} a {@link #getConnectionPoint()}, ktoré
 * potrebuje {@link OrthogonalRouter} na vedenie vodiča v Manhattan štýle.
 *
 * @author Tomáš Hianik (pôvodný autor), rozšírenie pre ortogonálny routing v SchematicSim
 */
public class Joint extends Movable {

    private static final Color DEFAULT_COLOR = Color.DARKGRAY;

    private final LinkedList<Node> disabledNodes = new LinkedList<>();

    // na indexe 0 je vždy segment, aj po odstránení druhého
    WireSegment[] wireSegments;

    private final Wire wire;
    private Circle joint;
    private Circle colorizer;
    private double radius;

    public Joint(SchematicSheet sheet, Wire wire) {
        super(sheet);
        this.wire = wire;
        this.wireSegments = new WireSegment[2];

        GridSystem grid = getSheet().getGrid();

        Rectangle boundingBox = new Rectangle(grid.getSizeX(), grid.getSizeY());
        boundingBox.setOpacity(0);
        boundingBox.setLayoutX(-grid.getSizeX() / 2.0);
        boundingBox.setLayoutY(-grid.getSizeY() / 2.0);

        this.radius = grid.getSizeMin() / 3.7;
        Group graphic = generateJointGraphic(radius);

        this.getChildren().addAll(boundingBox, graphic);

        this.setOnMouseDragged(event -> {
            getSheet().addSelect(getWire());
            getWire().select();
        });

        this.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) this.delete();
            else getSheet().addSelect(getWire());
        });

        this.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
        this.addEventFilter(MouseEvent.MOUSE_RELEASED, this::onMouseReleased);
    }

    public void onMouseDragged(MouseEvent event) {
        Node node = event.getPickResult().getIntersectedNode();
        if (node instanceof Line) {
            disabledNodes.addLast(node);
            node.setMouseTransparent(true);
        }
    }

    public void onMouseReleased(MouseEvent event) {
        if (!disabledNodes.isEmpty()) {
            disabledNodes.forEach(node -> node.setMouseTransparent(false));
            disabledNodes.clear();
        }
    }

    public void connectWireSegment(WireSegment segment) {
        if (this.wireSegments[0] == null) this.wireSegments[0] = segment;
        else this.wireSegments[1] = segment;
    }

    boolean removeWireSegment(WireSegment segment) {
        if (wireSegments[0] == segment) {
            wireSegments[0] = wireSegments[1];
            wireSegments[1] = null;
        } else if (wireSegments[1] == segment) {
            wireSegments[1] = null;
        } else return false;
        return true;
    }

    public Wire getWire() {
        return this.wire;
    }

    WireSegment getPrimaryWireSegment() {
        return this.wireSegments[0];
    }

    WireSegment getSecondaryWireSegment() {
        return this.wireSegments[1];
    }

    public void setColor(Color color) {
        if (Platform.isFxApplicationThread()) {
            this.colorizer.setFill(color);
            this.colorizer.setOpacity(1);
        } else {
            Platform.runLater(() -> {
                this.colorizer.setFill(color);
                this.colorizer.setOpacity(1);
            });
        }
    }

    public void setDefaultColor() {
        if (Platform.isFxApplicationThread()) this.colorizer.setOpacity(0);
        else Platform.runLater(() -> this.colorizer.setOpacity(0));
    }

    void incRadius() {
        radius *= 1.2;
        this.joint.setRadius(radius);
    }

    /**
     * Viditeľnosť základného (tmavého) bodu tohto zlomu. {@link WireEnd} ho skrýva - koniec vodiča
     * sa vykresľuje bez krúžku; čierny krúžok tak ostáva len na {@link WireJunction}, kde sa
     * stretávajú dva vodiče. Krúžok sa iba skryje, nie odstráni, aby naďalej fungoval
     * {@link #incRadius()} aj pick-test.
     */
    protected void setJointDotVisible(boolean visible) {
        this.joint.setVisible(visible);
    }

    protected Group generateJointGraphic(double radius) {
        Group graphics = new Group();

        this.joint = new Circle(radius, radius, radius, DEFAULT_COLOR);
        this.joint.setTranslateX(-radius);
        this.joint.setLayoutY(-radius);

        this.colorizer = new Circle(0, 0, radius, Color.RED);
        this.colorizer.setOpacity(0);

        graphics.getChildren().addAll(this.joint, this.colorizer);
        return graphics;
    }

    /**
     * Bod, v ktorom sa vodič má napojiť na tento zlom - stred jointu, v súradniciach plochy.
     * Používa {@link OrthogonalRouter}.
     */
    public Point2D getConnectionPoint() {
        return new Point2D(getLayoutX(), getLayoutY());
    }

    /**
     * Smer, ktorým MUSÍ vodič z tohto bodu vychádzať kolmo. Voľný zlom (nie WireEnd pripojený
     * na pin) smer nepozná - vracia null, {@link OrthogonalRouter} v tom prípade zvolí voľný ohyb.
     */
    public Side getExitSide() {
        return null;
    }

    @Override
    public Pane getDescription() {
        return getWire().getDescription();
    }
}
