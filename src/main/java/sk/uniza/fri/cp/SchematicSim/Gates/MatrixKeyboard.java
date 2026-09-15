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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Matricová klávesnica 4x4 - štyri vstupné riadky ({@code R0}..{@code R3}, vľavo) a štyri
 * výstupné stĺpce ({@code C0}..{@code C3}, hore). Každá z šestnástich kláves sa správa rovnako
 * ako samotné tlačidlo. Pravým tlačidlom myši sa otvorí kontextové menu s položkou
 * {@code Toggle}: ak je zapnutá, každé ľavé stlačenie klávesu trvalo prepne (latch),
 * ak je vypnutá, klávesa je stlačená len počas držania myši.
 * Ak je stlačená niektorá klávesa v stĺpci a jej riadok je v logickej 0, výstup stĺpca sa
 * pretiahne na 0, inak je na 1 (slabý pull-up). Výstupy sú typu {@link PinType#WEAK_OUT} -
 * silný push-pull vodič na sieti ich vždy pretiahne, klávesnica nemôže spôsobiť skrat.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class MatrixKeyboard extends GateSymbol {

    private static final int ROWS = 4;
    private static final int COLS = 4;
    /** Veľkosť jednej klávesy v bunkách mriežky - ako samostatné tlačidlo (2x2). */
    private static final int KEY_GRID = 2;
    private static final int GRID_WIDTH = COLS * KEY_GRID + 1;
    private static final int GRID_HEIGHT = ROWS * KEY_GRID + 1;

    private Pin[] pinIn;
    private Pin[] pinOut;
    private Circle[][] keyPlungers;
    private boolean[][] keyOn;
    private ContextMenu contextMenu;
    private CheckMenuItem toggleItem;

    private volatile int pressedRow = -1;
    private volatile int pressedCol = -1;
    private volatile boolean toggle = false;

    /** Konštruktor pre paletku (ItemPicker). */
    public MatrixKeyboard() {
        super();
    }

    /** Konštruktor pre umiestnenie na plochu - volaný reflexiou zo SchematicSheet. */
    public MatrixKeyboard(SchematicSheet sheet) {
        super(sheet);
    }

    @Override
    protected List<Pin> createPins() {
        // pole sa MUSIA vytvoriť tu, nie ako inštancové inicializátory - drawBody() sa volá
        // z konštruktora GateSymbol ešte PRED spustením inštancových inicializátorov
        keyPlungers = new Circle[ROWS][COLS];
        keyOn = new boolean[ROWS][COLS];

        List<Pin> pins = new ArrayList<>(ROWS + COLS);
        pinIn = new Pin[ROWS];
        for (int row = 0; row < ROWS; row++) {
            pinIn[row] = new InputPin(this, "R" + row, 1, 1 + row * KEY_GRID + KEY_GRID / 2, Side.LEFT);
            pins.add(pinIn[row]);
        }
        pinOut = new Pin[COLS];
        for (int col = 0; col < COLS; col++) {
            pinOut[col] = new OutputPin(this, "C" + col, 1 + col * KEY_GRID + KEY_GRID / 2, 1, Side.TOP);
            // slabý výstup (pull-up): idle HIGH, silný vodič na sieti ho môže pretiahnuť
            pinOut[col].getOwnedPotential().setType(PinType.WEAK_OUT);
            pins.add(pinOut[col]);
        }
        return pins;
    }

    @Override
    protected Pane drawBody() {
        int cell = getSheet().getGrid().getSizeMin();

        Pane pane = new Pane();

        // klávesy v matici 4x4 - každá klávesa má veľkosť/štýl samostatného tlačidla (2x2 bunky);
        // ľavým tlačidlom sa stláča - pri zapnutom Toggle sa každé stlačenie prepne (trvalo),
        // pri vypnutom je stlačená len počas držania myši. Pravým tlačidlom sa otvorí
        // kontextové menu s položkou Toggle.
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                double bx = (1 + col * KEY_GRID) * cell;
                double by = (1 + row * KEY_GRID) * cell;

                Rectangle keyBody = new Rectangle(bx, by, KEY_GRID * cell, KEY_GRID * cell);
                keyBody.setFill(Color.WHITESMOKE);
                keyBody.setStroke(Color.GRAY);
                keyBody.setStrokeWidth(1.5);

                Circle key = new Circle(bx + cell, by + cell, cell * 0.6, Color.BLACK);
                key.setStroke(Color.WHITE);
                key.setStrokeWidth(1.5);
                final int r = row;
                final int c = col;
                key.setOnMousePressed(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        if (toggle) {
                            keyOn[r][c] = !keyOn[r][c];
                            refreshVisual();
                            notifyStateChanged();
                        } else {
                            setPressed(r, c);
                        }
                    } else if (event.getButton() == MouseButton.SECONDARY) {
                        if (getSheet() != null && getSheet().isEditingEnabled()) showMenu(event.getScreenX(), event.getScreenY());
                        event.consume();
                    }
                });
                key.setOnMouseReleased(event -> {
                    if (event.getButton() == MouseButton.PRIMARY && !toggle) {
                        releasePressed(r, c);
                    }
                });
                key.setOnContextMenuRequested(event -> {
                    if (getSheet() == null || !getSheet().isEditingEnabled()) return;
                    if (contextMenu == null || !contextMenu.isShowing()) {
                        showMenu(event.getScreenX(), event.getScreenY());
                    }
                    event.consume();
                });
                keyPlungers[row][col] = key;
                pane.getChildren().addAll(keyBody, key);
            }
        }

        return pane;
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
        contextMenu.show(keyPlungers[0][0], screenX, screenY);
    }

    private boolean isActiveKey(int row, int col) {
        return (pressedRow == row && pressedCol == col) || keyOn[row][col];
    }

    @Override
    public void simulate() {
        // kľud: slabý pull-up drží stĺpce na 1; stlačená klávesa prenáša svoj riadok na stĺpec
        for (int col = 0; col < COLS; col++) {
            if (anyLowInColumn(col)) {
                setPin(pinOut[col], Pin.PinState.LOW);
            } else {
                setPin(pinOut[col], Pin.PinState.HIGH);
            }
        }
    }

    private boolean anyLowInColumn(int col) {
        for (int row = 0; row < ROWS; row++) {
            if (isActiveKey(row, col) && isLow(pinIn[row])) return true;
        }
        return false;
    }

    @Override
    public void reset() {
        pressedRow = -1;
        pressedCol = -1;
        for (boolean[] row : keyOn) {
            Arrays.fill(row, false);
        }
        for (int row = 0; row < ROWS; row++) {
            if (pinIn[row] != null) setPinForce(pinIn[row], Pin.PinState.NOT_CONNECTED);
        }
        for (int col = 0; col < COLS; col++) {
            if (pinOut[col] != null) setPinForce(pinOut[col], Pin.PinState.NOT_CONNECTED);
        }
    }

    /**
     * Momentové stlačenie klávesy (ekvivalent podržania ľavým tlačidlom myši).
     */
    public void setPressed(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return;
        pressedRow = row;
        pressedCol = col;
        refreshVisual();

        notifyStateChanged();
    }

    /**
     * Uvoľnenie stlačenej klávesy - v momentovom režime (vypnutý Toggle) zruší aj jej
     * prípadné trvalé zapnutie, presne ako pri samostatnom tlačidle.
     */
    public void releasePressed(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return;
        if (pressedRow != row || pressedCol != col) return;
        pressedRow = -1;
        pressedCol = -1;
        keyOn[row][col] = false;
        refreshVisual();

        notifyStateChanged();
    }

    /**
     * Prepnutie režimu Toggle: pri {@code true} každé ľavé stlačenie klávesu trvalo prepne,
     * pri {@code false} je klávesa stlačená len počas držania myši.
     */
    public void setToggle(boolean value) {
        if (toggle == value) return;
        toggle = value;
        // pri vypnutí Toggle sa všetky trvalo prepnuté klávesy vrátia do kľudu
        if (!toggle) {
            pressedRow = -1;
            pressedCol = -1;
            for (boolean[] row : keyOn) {
                Arrays.fill(row, false);
            }
            refreshVisual();
            notifyStateChanged();
        }
    }

    public boolean isToggle() {
        return toggle;
    }

    /**
     * Ak beží simulácia, okamžite prepočítaj vývody, aby sa zmena rozšírila do vodičov a LED.
     */
    private void notifyStateChanged() {
        if (getSheet() != null && getSheet().isSimulationRunning()) {
            simulate();
        }
    }

    public boolean isKeyOn(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return false;
        return keyOn[row][col];
    }

    /**
     * Aktualizácia vizuálu vždy na FX vlákne (simulácia beží na separátnom vlákne).
     * Zmeny sú skoalescované do jednej čakajúcej úlohy.
     */
    private volatile boolean visualUpdateScheduled;

    private void refreshVisual() {
        if (keyPlungers == null || visualUpdateScheduled) return;
        visualUpdateScheduled = true;
        Platform.runLater(() -> {
            visualUpdateScheduled = false;
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    Circle key = keyPlungers[row][col];
                    if (key != null) {
                        key.setFill(isActiveKey(row, col) ? Color.DARKGRAY : Color.BLACK);
                    }
                }
            }
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
        return "KBD";
    }

    @Override
    public String getShortDescription() {
        return "Klávesnica 4x4 - riadky R0-R3 (vľavo), stĺpce C0-C3 (hore); ľavým tlačidlom stlač, pravým tlačidlom Toggle (trvalé prepínanie)";
    }
}