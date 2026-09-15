package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.application.Platform;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import sk.uniza.fri.cp.SchematicSim.Electrical.PinType;
import sk.uniza.fri.cp.SchematicSim.Pin.InputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.OutputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.Arrays;
import java.util.List;

/**
 * Tlačidlo - v kľude je výstup {@code O} (slabý push/pull so slabým pull-upom)
 * na logickej 1, po stlačení sa vstup {@code I} behaviorálne prepája na výstup: ak je vstup
 * v logickej 0, výstup sa pretiahne na 0, inak zostane 1. Ľavým tlačidlom myši sa tlačidlo
 * stláča momentovo (stlač a drž). Pravým tlačidlom sa otvorí kontextové menu s položkou
 * {@code Toggle}: ak je zapnutá, každé ľavé stlačenie opakovane prepína stav (sticky/trvalo),
 * ak je vypnutá, tlačidlo je stlačené len počas držania myši.
 * Vývod {@code O} je typu {@link PinType#WEAK_OUT} - na spoločnej sieti ho vždy pretiahne
 * silný (push-pull) výstup, takže tlačidlo nemôže spôsobiť skrat.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class PushButton extends GateSymbol {

    private static final int GRID_WIDTH = 2;
    private static final int GRID_HEIGHT = 2;

    private Pin pinIn;
    private Pin pinOut;
    private Circle plunger;
    private ContextMenu contextMenu;
    private CheckMenuItem toggleItem;

    private volatile boolean pressed = false;
    private volatile boolean toggle = false;

    /** Konštruktor pre paletku (ItemPicker). */
    public PushButton() {
        super();
    }

    /** Konštruktor pre umiestnenie na plochu - volaný reflexiou zo SchematicSheet. */
    public PushButton(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected List<Pin> createPins() {
        pinIn = new InputPin(this, "I", 0, 1, Side.LEFT);
        pinOut = new OutputPin(this, "O", 1, 0, Side.TOP);
        // slabý výstup (pull-up): idle HIGH, silný vodič na sieti ho môže pretiahnuť
        pinOut.getOwnedPotential().setType(PinType.WEAK_OUT);
        return Arrays.asList(pinIn, pinOut);
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();
        double w = getGridWidth() * cell;
        double h = getGridHeight() * cell;

        Rectangle body = new Rectangle(w, h);
        body.setFill(Color.WHITESMOKE);
        body.setStroke(Color.GRAY);
        body.setStrokeWidth(1.5);

        // otlačené tlačidlo; ľavým tlačidlom sa stláča - pri zapnutom Toggle sa každé
        // stlačenie prepne (trvalo), pri vypnutom je stlačené len počas držania myši.
        // Pravým tlačidlom sa otvorí kontextové menu s položkou Toggle.
        plunger = new Circle(w / 2.0, h / 2.0, cell * 0.6, Color.BLACK);
        plunger.setStroke(Color.WHITE);
        plunger.setStrokeWidth(1.5);
        plunger.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (toggle) {
                    setPressedState(!pressed);
                } else {
                    setPressedState(true);
                }
            } else if (event.getButton() == MouseButton.SECONDARY) {
                if (getSheet() != null) showMenu(event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });
        plunger.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY && !toggle) {
                setPressedState(false);
            }
        });
        plunger.setOnContextMenuRequested(event -> {
            if (getSheet() == null) return;
            if (contextMenu == null || !contextMenu.isShowing()) {
                showMenu(event.getScreenX(), event.getScreenY());
            }
            event.consume();
        });

        return new Pane(body, plunger);
    }

    private void showMenu(double screenX, double screenY) {
        if (contextMenu == null) {
            toggleItem = new CheckMenuItem("Toggle");
            toggleItem.setSelected(toggle);
            toggleItem.setOnAction(e -> setToggle(toggleItem.isSelected()));
            contextMenu = new ContextMenu(toggleItem);
        } else {
            toggleItem.setSelected(toggle);
        }
        contextMenu.show(plunger, screenX, screenY);
    }

    @Override
    public void simulate() {
        // kľud: slabý pull-up drží výstup na 1; stlačenie prenáša vstup na výstup
        if (pressed && isLow(pinIn)) {
            setPin(pinOut, Pin.PinState.LOW);
        } else {
            setPin(pinOut, Pin.PinState.HIGH);
        }
    }

    @Override
    public void reset() {
        pressed = false;
        if (pinIn != null) setPinForce(pinIn, Pin.PinState.NOT_CONNECTED);
        if (pinOut != null) setPinForce(pinOut, Pin.PinState.NOT_CONNECTED);
    }

    /**
     * Priame nastavenie stavu stlačenia (ekvivalent kliknutia tlačidla).
     */
    public void setPressedState(boolean value) {
        pressed = value;
        refreshVisual();

        // ak beží simulácia, okamžite prepočítaj vývody, aby sa zmena rozšírila do vodičov a LED
        if (getSheet() != null && getSheet().isSimulationRunning()) {
            simulate();
        }
    }

    public boolean isPressedState() {
        return pressed;
    }

    /**
     * Prepnutie režimu Toggle: pri {@code true} každé ľavé stlačenie trvalo prepne stav,
     * pri {@code false} je tlačidlo stlačené len počas držania myši.
     */
    public void setToggle(boolean value) {
        if (toggle == value) return;
        toggle = value;
        // pri vypnutí Toggle počas držania sa momentovo stlačené tlačidlo vráti do kľudu
        if (!toggle && pressed) {
            setPressedState(false);
        }
    }

    public boolean isToggle() {
        return toggle;
    }

    /**
     * Aktualizácia vizuálu vždy na FX vlákne (simulácia beží na separátnom vlákne).
     * Zmeny sú skoalescované do jednej čakajúcej úlohy.
     */
    private volatile boolean visualUpdateScheduled;

    private void refreshVisual() {
        if (plunger == null || visualUpdateScheduled) return;
        visualUpdateScheduled = true;
        Platform.runLater(() -> {
            visualUpdateScheduled = false;
            plunger.setFill(pressed ? Color.DARKGRAY : Color.BLACK);
        });
    }

    public boolean isContextMenuShowing() {
        return contextMenu != null && contextMenu.isShowing();
    }

    @Override
    public int getGridWidth() {
        return GRID_WIDTH;
    }

    @Override
    public int getGridHeight() {
        return GRID_HEIGHT;
    }

    @Override
    public String getName() {
        return "BTN";
    }

    @Override
    public String getShortDescription() {
        return "Tlačidlo - ľavým tlačidlom stlačíš (0/1), pravým tlačidlom Toggle (trvalé prepínanie)";
    }
}