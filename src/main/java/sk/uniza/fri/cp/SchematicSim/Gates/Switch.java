package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.SchematicSim.Pin.OutputPin;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.Arrays;
import java.util.List;

/**
 * Samostatný prepínač - biely kruh s jedným prípojným bodom {@code O}. Kliknutím na kruh
 * sa stav prepne a vnútri kruhu sa zobrazí logická hodnota vývodu (1 = HIGH, 0 = LOW).
 * Pravým tlačidlom myši sa otvorí kontextové menu na výber umiestnenia vývodu
 * (vpravo/vľavo/hore/dole); štandardne je vývod vpravo. Vývod {@code O} trvalo generuje
 * HIGH pre zapnutý prepínač a LOW pre vypnutý.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class Switch extends GateSymbol {

    private static final int GRID_WIDTH = 2;
    private static final int GRID_HEIGHT = 2;

    private Pin outPin;
    private Circle button;
    private Text valueText;
    private ContextMenu contextMenu;

    private volatile boolean on = false;

    /** Konštruktor pre paletku (ItemPicker). */
    public Switch() {
        super();
    }

    /** Konštruktor pre umiestnenie na plochu - volaný reflexiou zo SchematicSheet. */
    public Switch(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected List<Pin> createPins() {
        outPin = new OutputPin(this, "O", GRID_WIDTH, 1, Side.RIGHT);
        return Arrays.asList(outPin);
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();
        double w = getGridWidth() * cell;
        double h = getGridHeight() * cell;

        button = new Circle(w / 2.0, h / 2.0, cell * 0.55, Color.WHITE);
        button.setStroke(Color.BLACK);
        button.setStrokeWidth(1.5);
        button.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                handleToggle();
            }
        });
        // menu sa otvára na pravom tlačidle myši; MOUSE_PRESSED je spoľahlivejší ako
        // CONTEXT_MENU_REQUESTED (ten sa negeneruje, ak pravý stlač počas cesty niekto skonzumuje)
        button.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                showPlacementMenu(event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });
        button.setOnContextMenuRequested(event -> {
            if (contextMenu == null || !contextMenu.isShowing()) {
                showPlacementMenu(event.getScreenX(), event.getScreenY());
            }
            event.consume();
        });

        valueText = new Text("0");
        valueText.setTextOrigin(VPos.CENTER);
        valueText.setLayoutY(h / 2.0);
        valueText.setFont(Font.font(cell * 0.6));
        valueText.setMouseTransparent(true);
        valueText.setLayoutX(w / 2.0 - valueText.getBoundsInLocal().getWidth() / 2.0);

        return new Pane(button, valueText);
    }

    private void showPlacementMenu(double screenX, double screenY) {
        if (contextMenu == null) {
            contextMenu = new ContextMenu(
                    new MenuItem("Vývod vpravo"),
                    new MenuItem("Vývod vľavo"),
                    new MenuItem("Vývod hore"),
                    new MenuItem("Vývod dole"));
            contextMenu.getItems().get(0).setOnAction(e -> placeOutput(Side.RIGHT));
            contextMenu.getItems().get(1).setOnAction(e -> placeOutput(Side.LEFT));
            contextMenu.getItems().get(2).setOnAction(e -> placeOutput(Side.TOP));
            contextMenu.getItems().get(3).setOnAction(e -> placeOutput(Side.BOTTOM));
        }
        contextMenu.show(button, screenX, screenY);
    }

    public void placeOutput(Side side) {
        int cell = getSheet().getGrid().getSizeMin();
        outPin.setLayoutX(placementOffsetX(side) * cell);
        outPin.setLayoutY(placementOffsetY(side) * cell);
        outPin.setSide(side);

        // ak na vývode už visí vodič, presuň jeho koniec na novú polohu a preveď rerouting
        if (outPin.getWireEnd() != null) {
            outPin.getWireEnd().refreshPosition();
        }
    }

    private static int placementOffsetX(Side side) {
        switch (side) {
            case LEFT: return 0;
            case RIGHT: return GRID_WIDTH;
            case TOP:
            case BOTTOM: return 1;
        }
        return GRID_WIDTH;
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
        // prepínač generuje výstup stále
        setPin(outPin, on ? Pin.PinState.HIGH : Pin.PinState.LOW);
    }

    @Override
    public void reset() {
        if (outPin != null) setPinForce(outPin, Pin.PinState.NOT_CONNECTED);
    }

    private void handleToggle() {
        setOn(!on);
    }

    /**
     * Priame nastavenie stavu prepínača (ekvivalent kliknutia).
     */
    public void setOn(boolean value) {
        on = value;
        refreshVisual();

        // ak beží simulácia, okamžite prepočítaj vývody, aby sa zmena rozšírila do vodičov a LED
        if (getSheet() != null && getSheet().isSimulationRunning()) {
            simulate();
        }
    }

    /**
     * Aktualizácia vizuálu vždy na FX vlákne (simulácia beží na separátnom vlákne).
     * Zmeny sú skoalescované do jednej čakajúcej úlohy, aby sa pri rýchlej simulácii
     * FX vlákno nezahltilo radom a aplikácia (klávesy F4/F5/F10...) ostala odozvá.
     */
    private volatile boolean visualUpdateScheduled;

    private void refreshVisual() {
        if (button == null || valueText == null || visualUpdateScheduled) return;
        visualUpdateScheduled = true;
        Platform.runLater(() -> {
            visualUpdateScheduled = false;
            String text = on ? "1" : "0";
            valueText.setText(text);
            valueText.setLayoutX(wCenter() - valueText.getBoundsInLocal().getWidth() / 2.0);
        });
    }

    private double wCenter() {
        return getSheet().getGrid().getSizeMin() * getGridWidth() / 2.0;
    }

    public boolean isOn() {
        return on;
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
        return "SW";
    }

    @Override
    public String getShortDescription() {
        return "Prepínač - klikom prepni, pravým tlačidlom vyber umiestnenie vývodu O (vpravo/vľavo/hore/dole)";
    }
}