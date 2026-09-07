package sk.uniza.fri.cp.SchematicSim.Pin;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import sk.uniza.fri.cp.SchematicSim.Connectable;
import sk.uniza.fri.cp.SchematicSim.Electrical.PinType;
import sk.uniza.fri.cp.SchematicSim.Electrical.Potential;
import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;
import sk.uniza.fri.cp.SchematicSim.Item;
import sk.uniza.fri.cp.SchematicSim.Side;
import sk.uniza.fri.cp.SchematicSim.Wire.Wire;
import sk.uniza.fri.cp.SchematicSim.Wire.WireEnd;

/**
 * Vývod (pin) logickej súčiastky. Nahrádza dvojicu Pin+Socket z BreadboardSim - narozdiel od nich
 * je priamo Connectable: nesie vlastný {@link Potential} a je priamym koncovým bodom vodiča,
 * bez medzičlánku "socket na doske".
 * <p>
 * Pozícia pinu je vždy pevná voči vlastníkovi (GateSymbol) - žiadna kolízna detekcia,
 * žiadne "hľadanie soketu pod pinom".
 *
 * @author Claude (návrh podľa SchematicSim architektúry, elektrická logika prevzatá z pôvodného Socket)
 */
public abstract class Pin extends Group implements Connectable {

    public enum Direction { INPUT, OUTPUT, INOUT }

    public enum PinState { HIGH, LOW, HIGH_IMPEDANCE, NOT_CONNECTED }

    private static final Color CORE_COLOR = Color.rgb(60, 60, 60);

    private final GateSymbol owner;
    private final String name;
    private final Direction direction;
    private final int gridOffsetX, gridOffsetY;
    private final Side side;

    private final Potential potential;
    private PinState state = PinState.NOT_CONNECTED;
    private WireEnd connectedWireEnd;

    private final Circle colorizer;
    private final boolean[] activeHighlights = new boolean[5];

    private static Wire creatingWire; // rozpracovaný vodič, spoločný pre všetky piny (ako pôvodne v Socket)

    private final EventHandler<MouseEvent> onMouseEntered = event -> highlight(INFO);
    private final EventHandler<MouseEvent> onMouseExited = event -> unhighlight(INFO);

    private final EventHandler<MouseEvent> onMouseDragged = event -> {
        if (!event.isPrimaryButtonDown()) return;
        if (creatingWire != null) {
            Point2D sheetXY = owner.getSheet().sceneToSheet(event.getSceneX(), event.getSceneY());
            creatingWire.catchFreeEnd().moveTo(sheetXY.getX(), sheetXY.getY());
        }
        event.consume();
    };

    private final EventHandler<MouseEvent> onMouseReleased = event -> {
        if (creatingWire != null) {
            creatingWire.setMouseTransparent(false);
            creatingWire.setOpacity(1);
            if (!creatingWire.areBothEndsConnected()) creatingWire.delete();
            creatingWire = null;
        }
        event.consume();
    };

    private final EventHandler<MouseEvent> onMouseDragDetected = event -> {
        if (!event.isPrimaryButtonDown()) return;
        startFullDrag();

        creatingWire = new Wire(this);
        creatingWire.setMouseTransparent(true);
        creatingWire.setOpacity(0.5);
        owner.getSheet().addItem(creatingWire);

        event.consume();
    };

    private final EventHandler<MouseDragEvent> onMouseDragReleased = event -> {
        if (creatingWire != null) {
            if (this != event.getGestureSource()) {
                creatingWire.catchFreeEnd().connect(this);
            } else {
                creatingWire.delete();
            }
        } else if (event.getGestureSource() instanceof WireEnd) {
            ((WireEnd) event.getGestureSource()).connect(this);
        }
        event.consume();
    };

    /**
     * @param owner        Súčiastka, na ktorej sa pin nachádza.
     * @param name         Názov pinu (napr. "A", "Y").
     * @param direction    Smer signálu.
     * @param gridOffsetX  Pozícia pinu v jednotkách mriežky voči ľavému hornému rohu súčiastky.
     * @param gridOffsetY  Pozícia pinu v jednotkách mriežky voči ľavému hornému rohu súčiastky.
     * @param side         Strana súčiastky, na ktorej vývod je (pre ortogonálny routing vodičov).
     */
    protected Pin(GateSymbol owner, String name, Direction direction, int gridOffsetX, int gridOffsetY, Side side) {
        this.owner = owner;
        this.name = name;
        this.direction = direction;
        this.gridOffsetX = gridOffsetX;
        this.gridOffsetY = gridOffsetY;
        this.side = side;
        this.potential = new Potential(this, null);
        this.potential.setType(defaultTypeFor(direction));

        int gridPx = owner.getSheet().getGrid().getSizeMin();
        double r = gridPx * 3.0 / 16.0;

        Rectangle hitArea = new Rectangle(gridPx, gridPx);
        hitArea.setOpacity(0);
        hitArea.setLayoutX(-gridPx / 2.0);
        hitArea.setLayoutY(-gridPx / 2.0);

        Circle core = new Circle(r, CORE_COLOR);

        this.colorizer = new Circle(r * 1.8, Color.ORANGE);
        this.colorizer.setMouseTransparent(true);
        this.colorizer.setOpacity(0);
        this.colorizer.setEffect(new BoxBlur(r, r, 2));

        this.getChildren().addAll(hitArea, colorizer, core);
        this.setLayoutX(gridOffsetX * gridPx);
        this.setLayoutY(gridOffsetY * gridPx);

        registerEvents();
    }

    private static PinType defaultTypeFor(Direction d) {
        switch (d) {
            case INPUT: return PinType.IN;
            case OUTPUT: return PinType.OUT;
            default: return PinType.IO;
        }
    }

    private void registerEvents() {
        this.addEventFilter(MouseEvent.MOUSE_ENTERED, onMouseEntered);
        this.addEventFilter(MouseEvent.MOUSE_EXITED, onMouseExited);
        this.addEventFilter(MouseEvent.MOUSE_DRAGGED, onMouseDragged);
        this.addEventFilter(MouseEvent.MOUSE_RELEASED, onMouseReleased);
        this.addEventFilter(MouseEvent.DRAG_DETECTED, onMouseDragDetected);
        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_RELEASED, onMouseDragReleased);
    }

    public GateSymbol getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public Direction getDirection() {
        return direction;
    }

    public PinState getState() {
        return state;
    }

    /**
     * Nastavenie stavu pinu. NEmení hodnotu potenciálu - o to sa stará GateSymbol.setPin().
     */
    public void setState(PinState state) {
        this.state = state;
    }

    public boolean isConnected() {
        return connectedWireEnd != null;
    }

    // === Connectable ===

    @Override
    public Potential getPotential() {
        return potential != null ? potential.getPotential() : null;
    }

    @Override
    public Point2D getSceneGridPosition() {
        return new Point2D(
                this.getLocalToSceneTransform().getTx() - owner.getSheet().getOriginSceneOffsetX(),
                this.getLocalToSceneTransform().getTy() - owner.getSheet().getOriginSceneOffsetY());
    }

    @Override
    public Side getExitSide() {
        return side;
    }

    @Override
    public boolean isOccupied() {
        return connectedWireEnd != null;
    }

    /**
     * Interná väzba na pripojený koniec vodiča. Volá {@link WireEnd#connect(Pin)}/{@code disconnect()},
     * verejné kvôli krížovému balíku (Wire.WireEnd), ale mimo neho by sa volať nemalo.
     */
    public void setWireEnd(WireEnd end) {
        this.connectedWireEnd = end;
    }

    public WireEnd getWireEnd() {
        return connectedWireEnd;
    }

    public void clearWireEnd() {
        this.connectedWireEnd = null;
        this.potential.update();
    }

    @Override
    public void highlight(int highlightType) {
        activeHighlights[highlightType] = true;
        updateHighlight();
    }

    @Override
    public void unhighlight(int highlightType) {
        activeHighlights[highlightType] = false;
        updateHighlight();
    }

    private void updateHighlight() {
        Color color;
        double opacity;
        if (activeHighlights[WARNING]) { color = Color.RED; opacity = 0.8; }
        else if (activeHighlights[COMMON_POTENTIAL] || activeHighlights[COMMON_POTENTIAL_OTHER]) { color = Color.YELLOW; opacity = 0.5; }
        else if (activeHighlights[OK]) { color = Color.GREEN; opacity = 0.5; }
        else if (activeHighlights[INFO]) { color = Color.ORANGE; opacity = 0.5; }
        else { color = Color.WHITE; opacity = 0; }

        if (Platform.isFxApplicationThread()) {
            colorizer.setFill(color);
            colorizer.setOpacity(opacity);
        } else {
            Platform.runLater(() -> { colorizer.setFill(color); colorizer.setOpacity(opacity); });
        }
    }

    @Override
    public Item getOwnerItem() {
        return owner;
    }
}
