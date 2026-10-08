package sk.uniza.fri.cp.SchematicSim.Wire;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import sk.uniza.fri.cp.SchematicSim.Connectable;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Sheet.SheetEvent;

/**
 * Koniec vodiča. Pripája sa priamo na {@link Pin} súčiastky alebo na
 * {@link WireJunction} (bod, v ktorom sa stretávajú dva vodiče).
 * <p>
 * V novom modeli je vodič vždy priama úsečka - pohyb konca preto nesmie vznikať
 * ťahaním samotného jointu ({@link #makeImmovable()}), ale buď
 * <ul>
 *     <li>posunutím pinu/vlastníka (listener nižšie → {@link Wire#onEndMoved}),</li>
 *     <li>posunutím spájača ({@link Wire#onJunctionMoved}),</li>
 *     <li>alebo uchopením konca myšou (náhľad L-tvaru cez {@link Wire#beginPreview}).</li>
 * </ul>
 *
 * @author Tomáš Hianik (pôvodný autor), adaptácia pre SchematicSim
 */
public class WireEnd extends Joint {

    private Pin pin;
    private WireJunction junction;

    private double lastPosX = -1;
    private double lastPosY = -1;

    // pri zmene pozície vlastníka pinu (posun súčiastky) sa posunie aj koniec vodiča
    private final ChangeListener<Transform> pinPositionChangeListener = (observable, oldValue, newValue) -> {
        if (lastPosX == -1) {
            lastPosX = getLayoutX();
            lastPosY = getLayoutY();
        }

        Point2D p = pin.getSceneGridPosition();
        setLayoutX(p.getX() / getSheet().getAppliedScale());
        setLayoutY(p.getY() / getSheet().getAppliedScale());

        getWire().onEndMoved(this, getLayoutX() - lastPosX, getLayoutY() - lastPosY);

        lastPosX = getLayoutX();
        lastPosY = getLayoutY();
    };

    // pri zmene pozície spájača (WireJunction) sa posunie aj koniec vodiča
    private final ChangeListener<Number> junctionPositionChangeListener = (observable, oldValue, newValue) -> {
        if (this.junction == null) return;

        double deltaX = junction.getLayoutX() - lastPosX;
        double deltaY = junction.getLayoutY() - lastPosY;
        setLayoutX(junction.getLayoutX());
        setLayoutY(junction.getLayoutY());

        WireJunction.markDirty(this.junction);
        getWire().onJunctionMoved(this, deltaX, deltaY);

        lastPosX = getLayoutX();
        lastPosY = getLayoutY();
    };

    /** Uchopený koniec vodiča ťahaním z pine (null, ak žiadny). */
    private static WireEnd grabbedEnd;

    /** Pin, z ktorého bol koniec uchopený - pre návrat pri mikro-ťahu. */
    private static Pin grabSourcePin;

    public WireEnd(SchematicSheet sheet, Wire wire) {
        super(sheet, wire);

        // koniec vodiča sa vykresľuje bez čierneho krúžku - krúžok je len na spájači (WireJunction)
        this.setJointDotVisible(false);

        // konce vodičov sa NEŤAHAJÚ priamo (Movable by ich posúval mimo modelu vodiča)
        makeImmovable();

        this.addEventFilter(MouseEvent.DRAG_DETECTED, this::onDragDetected);

        this.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (grabbedEnd == this) {
                Point2D sheetXY = getSheet().sceneToSheet(event.getSceneX(), event.getSceneY());
                moveGrabbed(sheetXY.getX(), sheetXY.getY());
                event.consume();
                return;
            }
            Wire inProgress = Pin.getInProgressWire();
            if (inProgress != null) {
                Point2D sheetXY = getSheet().sceneToSheet(event.getSceneX(), event.getSceneY());
                inProgress.updateCreationDrag(sheetXY.getX(), sheetXY.getY());
                event.consume();
            }
        });

        this.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (grabbedEnd == this) {
                finishGrab();
                event.consume();
                return;
            }
            if (Pin.getInProgressWire() != null) {
                Pin.finishInProgressWire();
                event.consume();
            }
        });

        this.incRadius();
    }

    /**
     * Začiatok ťahania konca vodiča. Tri prípady podľa toho, kde koniec leží:
     * <ul>
     *     <li>na pine - koniec sa odpojí a ťahá sa ako náhľad (zvyšný koniec ostáva ukotvený),</li>
     *     <li>na spájači - začne sa nová odbočka zo spájača,</li>
     *     <li>voľný - v mieste konca sa založí spájač a odbočka sa z neho ťahá ďalej
     *         (pôvodný vodič ostáva stáť; pri zrušení ťahu sa spájač zruší).</li>
     * </ul>
     */
    private void onDragDetected(MouseEvent event) {
        if (!event.isPrimaryButtonDown()) return;
        if (!getSheet().isEditingEnabled()) return;

        if (this.pin != null) {
            beginGrab(this);
            startFullDrag();
        } else if (this.junction != null) {
            Pin.beginWireCreation(this.junction);
            startFullDrag();
        } else {
            WireJunction start = WireJunction.at(getSheet(), getConnectionPoint());
            this.connect(start);
            Pin.beginWireCreation(start);
            startFullDrag();
        }
        event.consume();
    }

    // === uchopenie konca (grab) ===

    public static void beginGrab(WireEnd end) {
        if (grabbedEnd != null || end == null) return;
        grabbedEnd = end;
        grabSourcePin = end.pin;
        if (end.pin != null) end.disconnect();

        Wire wire = end.getWire();
        int index = wire.getEnds()[0] == end ? 0 : 1;
        wire.setMouseTransparent(true);
        wire.setOpacity(0.5);
        wire.beginPreview(1 - index, wire.isHorizontal());
    }

    public static boolean moveGrabbed(double x, double y) {
        if (grabbedEnd == null) return false;
        grabbedEnd.getWire().updateCreationDrag(x, y);
        return true;
    }

    /**
     * Ukončenie uchopenia konca. Obnoví vzhľad vodiča, prípadne vráti koniec na
     * pôvodný pin (ak sa pri mikro-ťahu znova trafilo do jeho polohy) a naplánuje
     * usadenie geometrie. Volá sa z viacerých miest - je idempotentné.
     */
    public static void finishGrab() {
        WireEnd end = grabbedEnd;
        if (end == null) return;
        grabbedEnd = null;
        Pin sourcePin = grabSourcePin;
        grabSourcePin = null;

        Wire wire = end.getWire();
        if (wire == null) return;
        wire.endPreview();
        wire.setMouseTransparent(false);
        wire.setOpacity(1);

        if (!end.isConnected() && sourcePin != null && !sourcePin.isOccupied()) {
            Point2D p = sourcePin.getSceneGridPosition();
            double scale = wire.getSheet().getAppliedScale();
            double grid = wire.getSheet().getGrid().getSizeMin();
            if (end.getConnectionPoint().distance(p.getX() / scale, p.getY() / scale) <= grid / 2.0) {
                end.connect(sourcePin);
            }
        }
        wire.scheduleSettle(end);
    }

    public static WireEnd getGrabbedEnd() {
        return grabbedEnd;
    }

    static boolean isGrabbing(Wire wire) {
        return grabbedEnd != null && grabbedEnd.getWire() == wire;
    }

    // === pripojenie / odpojenie ===

    public boolean isConnected() {
        return this.pin != null || this.junction != null;
    }

    /**
     * Pripojenie konca vodiča k pinu alebo spájaču. Ak bol predtým pripojený inde,
     * najprv sa odpojí. Ak sa nepodarí pripojiť (pin je obsadený), zafarbí sa na červeno.
     */
    public void connect(Connectable target) {
        if (target instanceof Pin) {
            connectToPin((Pin) target);
        } else if (target instanceof WireJunction) {
            connectToJunction((WireJunction) target);
        }
    }

    private void connectToPin(Pin target) {
        if (this.pin == null && this.junction == null) {
            if (target != null && !target.isOccupied()) {
                this.pin = target;
                this.pin.setWireEnd(this);

                this.pin.getOwner().localToParentTransformProperty().addListener(pinPositionChangeListener);

                Point2D p = target.getSceneGridPosition();
                setLayoutX(p.getX() / getSheet().getAppliedScale());
                setLayoutY(p.getY() / getSheet().getAppliedScale());

                lastPosX = getLayoutX();
                lastPosY = getLayoutY();

                this.setColor(this.getWire().getColor().brighter());
                this.getWire().updateGeometry();
                this.getWire().updatePotentialNetwork();
            } else {
                this.setColor(Color.RED);
            }
        } else {
            disconnect();
            connectToPin(target);
        }
    }

    private void connectToJunction(WireJunction target) {
        if (this.pin == null && this.junction == null) {
            if (target != null) {
                this.junction = target;
                target.addWireEnd(this);

                this.junction.layoutXProperty().addListener(junctionPositionChangeListener);
                this.junction.layoutYProperty().addListener(junctionPositionChangeListener);

                Point2D p = target.getConnectionPoint();
                setLayoutX(p.getX());
                setLayoutY(p.getY());

                lastPosX = getLayoutX();
                lastPosY = getLayoutY();

                WireJunction.markDirty(target);
                this.setColor(this.getWire().getColor().brighter());
                this.getWire().updateGeometry();
                this.getWire().updatePotentialNetwork();
            } else {
                this.setColor(Color.RED);
            }
        } else {
            disconnect();
            connectToJunction(target);
        }
    }

    public Pin getPin() {
        return pin;
    }

    public WireJunction getJunction() {
        return junction;
    }

    /**
     * Prepočítanie pozície konca vodiča podľa aktuálnej polohy pinu (používa sa pri
     * premiestnení vývodu na súčiastke, kedy sa pin sám presunul - posun tela súčiastky
     * rieši listener vyššie).
     */
    public void refreshPosition() {
        if (pin == null) return;

        if (lastPosX == -1) {
            lastPosX = getLayoutX();
            lastPosY = getLayoutY();
        }

        Point2D p = pin.getSceneGridPosition();
        setLayoutX(p.getX() / getSheet().getAppliedScale());
        setLayoutY(p.getY() / getSheet().getAppliedScale());

        getWire().onEndMoved(this, getLayoutX() - lastPosX, getLayoutY() - lastPosY);

        lastPosX = getLayoutX();
        lastPosY = getLayoutY();
    }

    /**
     * Koniec vodiča sa nikdy nevykresľuje farebným krúžkom - ak by sme {@code setColor} nechali
     * na predkovi, po pripojení by na konci zasvietil krúžok. Krúžky tak ostávajú len na
     * {@link WireJunction}, kde sa stretávajú tri a viac vodičov.
     */
    @Override
    public void setColor(Color color) {
        // bez vizuálneho krúžku na konci vodiča
    }

    @Override
    public void delete() {
        this.disconnect();
        super.delete();
        Wire wire = getWire();
        if (wire != null) wire.delete();
    }

    /**
     * Odpojenie konca od pinu alebo spájaču. Odpojený spájač sa označí na
     * prepočítanie ({@link WireJunction#markDirty}) - až reconcile rozhodne, či sa
     * spájač zničí, alebo či sa zvyšné vodiče zlúčia do jednej priamky.
     */
    protected void disconnect() {
        if (this.pin != null) {
            Pin pinToUpdate = this.pin;
            this.pin.getOwner().localToParentTransformProperty().removeListener(pinPositionChangeListener);
            this.pin.clearWireEnd();
            this.pin = null;
            this.getWire().updatePotentialNetwork();
            this.setDefaultColor();
            if (getSheet().isSimulationRunning()) {
                getSheet().addEvent(new SheetEvent(pinToUpdate));
            }
        } else if (this.junction != null) {
            this.junction.layoutXProperty().removeListener(junctionPositionChangeListener);
            this.junction.layoutYProperty().removeListener(junctionPositionChangeListener);
            WireJunction orphanedJunction = this.junction;
            this.junction.removeWireEnd(this);
            this.junction = null;
            WireJunction.markDirty(orphanedJunction);
            orphanedJunction.refreshConnectedWires();
            this.getWire().updatePotentialNetwork();
            this.setDefaultColor();
        }
    }
}
