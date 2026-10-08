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
import sk.uniza.fri.cp.SchematicSim.Wire.WireJunction;

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

    private GateSymbol owner;
    private final String name;
    private final Direction direction;
    private final int gridOffsetX, gridOffsetY;
    private Side side;

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
            creatingWire.updateCreationDrag(sheetXY.getX(), sheetXY.getY());
        } else if (WireEnd.getGrabbedEnd() != null) {
            Point2D sheetXY = owner.getSheet().sceneToSheet(event.getSceneX(), event.getSceneY());
            WireEnd.moveGrabbed(sheetXY.getX(), sheetXY.getY());
        }
        event.consume();
    };

    private final EventHandler<MouseEvent> onMouseReleased = event -> {
        if (creatingWire != null) {
            finishInProgressWire();
            event.consume();
        } else if (WireEnd.getGrabbedEnd() != null) {
            WireEnd.finishGrab();
            event.consume();
        }
    };

    private final EventHandler<MouseEvent> onMouseDragDetected = event -> {
        if (!event.isPrimaryButtonDown()) return;
        if (owner.getSheet() != null && !owner.getSheet().isEditingEnabled()) return;
        startFullDrag();

        if (this.connectedWireEnd != null) {
            // obsadený pin: ťah uvoľní koniec z pine a ťahá ho ako náhľad
            WireEnd.beginGrab(this.connectedWireEnd);
        } else {
            beginWireCreation(this);
        }

        event.consume();
    };

    /**
     * Založenie rozpracovaného vodiča z daného pinu (bez natívneho drag gesta - používa to
     * samotný {@link #onMouseDragDetected} aj testy zbernicovej lišty). Vodič sa zaregistruje
     * ako aktuálny rozpracovaný ({@link #getInProgressWire()}) a pridá na plochu; zároveň sa
     * zapne L-tvarový náhľad ukotvený na pine (os sa zamkne pri prvom väčšom pohybe myši).
     */
    public static Wire beginWireCreation(Pin source) {
        creatingWire = new Wire(source);
        creatingWire.setMouseTransparent(true);
        creatingWire.setOpacity(0.5);
        source.owner.getSheet().addItem(creatingWire);
        anchorFreeEnd(creatingWire);
        creatingWire.beginPreview(0, null);
        return creatingWire;
    }

    /**
     * Založenie rozpracovaného vodiča z existujúceho spájača (WireJunction) na vodiči.
     * Umožňuje začínať nové vodiče na existujúcich vodičoch (odbočky).
     */
    public static Wire beginWireCreation(WireJunction source) {
        creatingWire = new Wire(source);
        creatingWire.setMouseTransparent(true);
        creatingWire.setOpacity(0.5);
        source.getSheet().addItem(creatingWire);
        anchorFreeEnd(creatingWire);
        creatingWire.beginPreview(0, null);
        return creatingWire;
    }

    /**
     * Pred prvým ťahom sa voľný koniec prichytí na miesto vzniku (kotvu) - náhľad
     * tak neletí do roha plátna a pri okamžitom pustení vznikne nulový vodič (zruší sa).
     */
    private static void anchorFreeEnd(Wire wire) {
        Point2D anchor = wire.getEnds()[0].getConnectionPoint();
        wire.getEnds()[1].moveTo(anchor.getX(), anchor.getY());
    }

    /**
     * Rozpracovaný vodič, ktorý sa práve ťahá z nejakého pinu. Prístup pre komponenty,
     * ktoré odchyťujú ukončenie ťahania mimo pin (napr. zbernicová lišta).
     */
    public static Wire getInProgressWire() {
        return creatingWire;
    }

    /**
     * Ukončenie rozpracovaného vodiča: obnoví jeho vzhľad, vypne náhľad a usadí
     * geometriu. Nedokončené ťahanie (pustenie mimo pripojenia) vodič NEZRUŠÍ -
     * ostáva na ploche ako vodič s voľným koncom. Zruší sa iba nulový vodič
     * (pustenie na mieste vzniku = omyl). Volá sa z viacerých miest pri pustení
     * myši - je idempotentné (prvé volanie vyčistí rozpracovaný stav).
     */
    public static void finishInProgressWire() {
        Wire wire = creatingWire;
        if (wire == null) return;
        creatingWire = null;

        wire.endPreview();
        wire.setMouseTransparent(false);
        wire.setOpacity(1);

        if (wire.getParent() == null) return;

        wire.completeCreation();

        if (!wire.areBothEndsConnected()) {
            Point2D start = wire.getEnds()[0].getConnectionPoint();
            Point2D finish = wire.getEnds()[1].getConnectionPoint();
            if (start.distance(finish) < 1e-6) {
                wire.delete();
            }
        }
    }

    private final EventHandler<MouseDragEvent> onMouseDragReleased = event -> {
        if (creatingWire != null) {
            if (this != event.getGestureSource()) {
                creatingWire.catchFreeEnd().connect(this);
            } else {
                creatingWire.delete();
            }
        } else if (WireEnd.getGrabbedEnd() != null) {
            // uchopený koniec pustený na pine: zdrojom gesta je PIN (nie WireEnd),
            // preto sa tu pripojí priamo; pustenie na zdrojovej pine necháme na
            // tolerančný návrat vo finishGrab (mikro-ťah sa má dať zrušiť)
            if (this != event.getGestureSource()) {
                WireEnd.getGrabbedEnd().connect(this);
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
        double r = gridPx / 8.0;

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

    /**
     * Vlastný potenciál tohto pinu - na rozdiel od {@link #getPotential()} nevracia agregát
     * celej siete (vodiča), ale samotný list spojení. Zmena hodnoty sa zapisuje práve na
     * list a odtiaľ sa cez {@link Potential#setValue} rozšíri do agregovaného potenciálu siete.
     */
    public Potential getOwnedPotential() {
        return potential;
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

    /**
     * Zmena strany, na ktorej vývod leží (napr. pri premiestnení vývodu cez kontextové menu).
     * Ovplyvňuje smer, ktorým z pinu vychádza vodič pri ortogonálnom routingu. Pozícia pinu
     * sa pritom na súčiastke presúva cez {@link #setLayoutX}/{@link #setLayoutY} u volajúceho.
     */
    public void setSide(Side side) {
        this.side = side;
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
