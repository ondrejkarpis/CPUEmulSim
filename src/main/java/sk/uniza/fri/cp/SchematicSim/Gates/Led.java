package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import sk.uniza.fri.cp.SchematicSim.Pin.InputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Samostatná LED dióda - obyčajný farebný kruh s jedným prípojným bodom {@code IN}.
 * Dióda svieti, keď je na vstupe logická 1 (HIGH), inak je zhasnutá. Je čisto pasívna -
 * nemá žiadne výstupné vývody, nikdy nehynie na zbernicu, a preto nemôže spôsobiť skrat.
 * Pravým tlačidlom myši sa otvorí kontextové menu s výberom umiestnenia vývodu
 * (vpravo/vľavo/hore/dole) a farby LED. Štandardne je vývod vľavo a farba červená.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class Led extends GateSymbol {

    private static final int GRID_WIDTH = 2;
    private static final int GRID_HEIGHT = 2;

    private static final Color LED_OFF = Color.DARKGRAY;

    private static final Map<String, Color> LED_COLORS = new LinkedHashMap<>();
    static {
        LED_COLORS.put("Červená", Color.RED);
        LED_COLORS.put("Zelená", Color.GREEN);
        LED_COLORS.put("Modrá", Color.BLUE);
        LED_COLORS.put("Žltá", Color.YELLOW);
        LED_COLORS.put("Oranžová", Color.ORANGE);
    }

    private Pin pinIn;
    private Circle light;
    private ContextMenu contextMenu;

    private volatile boolean on = false;
    private volatile Color ledColor = LED_COLORS.get("Červená");

    /** Konštruktor pre paletku (ItemPicker). */
    public Led() {
        super();
    }

    /** Konštruktor pre umiestnenie na plochu - volaný reflexiou zo SchematicSheet. */
    public Led(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected List<Pin> createPins() {
        pinIn = new InputPin(this, "IN", 0, 1, Side.LEFT);
        return Arrays.asList(pinIn);
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();
        double w = getGridWidth() * cell;
        double h = getGridHeight() * cell;

        light = new Circle(w / 2.0, h / 2.0, cell * 0.55, LED_OFF);
        light.setStroke(Color.BLACK);
        light.setStrokeWidth(1.5);
        light.setOnContextMenuRequested(event -> {
            showMenu(event.getScreenX(), event.getScreenY());
            event.consume();
        });

        return new Pane(light);
    }

    private void showMenu(double screenX, double screenY) {
        if (contextMenu == null) {
            Menu placement = new Menu("Vývod");
            ToggleGroup placementGroup = new ToggleGroup();
            placement.getItems().add(placementItem("vpravo", Side.RIGHT, placementGroup));
            placement.getItems().add(placementItem("vľavo", Side.LEFT, placementGroup));
            placement.getItems().add(placementItem("hore", Side.TOP, placementGroup));
            placement.getItems().add(placementItem("dole", Side.BOTTOM, placementGroup));

            Menu colorMenu = new Menu("Farba LED");
            ToggleGroup colorGroup = new ToggleGroup();
            for (Map.Entry<String, Color> entry : LED_COLORS.entrySet()) {
                RadioMenuItem item = new RadioMenuItem(entry.getKey());
                item.setToggleGroup(colorGroup);
                item.setUserData(entry.getValue());
                item.setOnAction(e -> setColor((Color) item.getUserData()));
                colorMenu.getItems().add(item);
            }
            colorMenu.getItems().get(0).setSelected(true);

            contextMenu = new ContextMenu(placement, new SeparatorMenuItem(), colorMenu);
        }
        contextMenu.show(light, screenX, screenY);
    }

    private RadioMenuItem placementItem(String label, Side side, ToggleGroup group) {
        RadioMenuItem item = new RadioMenuItem(label);
        item.setToggleGroup(group);
        item.setUserData(side);
        item.setOnAction(e -> placeInput((Side) item.getUserData()));
        if (side == pinIn.getExitSide()) item.setSelected(true);
        return item;
    }

    public void placeInput(Side side) {
        int cell = getSheet().getGrid().getSizeMin();
        pinIn.setLayoutX(placementOffsetX(side) * cell);
        pinIn.setLayoutY(placementOffsetY(side) * cell);
        pinIn.setSide(side);

        // ak na vývode už visí vodič, presuň jeho koniec na novú polohu a preveď rerouting
        if (pinIn.getWireEnd() != null) {
            pinIn.getWireEnd().refreshPosition();
        }
    }

    private static int placementOffsetX(Side side) {
        switch (side) {
            case LEFT: return 0;
            case RIGHT: return GRID_WIDTH;
            case TOP:
            case BOTTOM: return 1;
        }
        return 0;
    }

    private static int placementOffsetY(Side side) {
        switch (side) {
            case TOP: return 0;
            case BOTTOM: return GRID_HEIGHT;
            case LEFT:
            case RIGHT: return 1;
        }
        return 1;
    }

    @Override
    public void simulate() {
        // LED svieti, keď je na vstupe logická 1
        boolean lit = isHigh(pinIn);
        if (lit != on) {
            on = lit;
            refreshVisual();
        }
    }

    @Override
    public void reset() {
        if (pinIn != null) setPinForce(pinIn, Pin.PinState.NOT_CONNECTED);
    }

    /**
     * Aktualizácia vizuálu vždy na FX vlákne (simulácia beží na separátnom vlákne).
     * Zmeny sú skoalescované do jednej čakajúcej úlohy, aby sa pri rýchlej simulácii
     * FX vlákno nezahltilo radom a aplikácia (klávesy F4/F5/F10...) ostala odozvá.
     */
    private volatile boolean visualUpdateScheduled;

    private void refreshVisual() {
        if (light == null || visualUpdateScheduled) return;
        visualUpdateScheduled = true;
        Platform.runLater(() -> {
            visualUpdateScheduled = false;
            light.setFill(on ? ledColor : LED_OFF);
        });
    }

    public boolean isLit() {
        return on;
    }

    public Color getLedColor() {
        return ledColor;
    }

    /**
     * Priame nastavenie farby LED (ekvivalent voľby v kontextovom menu).
     */
    public void setColor(Color color) {
        ledColor = color;
        refreshVisual();
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
        return "LED";
    }

    @Override
    public String getShortDescription() {
        return "LED dióda - svieti pri logickej 1; pravým tlačidlom zvoľ umiestnenie vývodu a farbu";
    }
}