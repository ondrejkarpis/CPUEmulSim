package sk.uniza.fri.cp.SchematicSim.Gates;

import javafx.application.Platform;
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
 * ako samotné tlačidlo - ľavým tlačidlom myši sa stláča momentovo, pravým sa stav trvalo prepne.
 * Ak je stlačená niektorá klávesa v stĺpci a jej riadok je v logickej 0, výstup stĺpca sa
 * pretiahne na 0, inak je na 1 (slabý pull-up). Výstupy sú typu {@link PinType#WEAK_OUT} -
 * silný push-pull vodič na sieti ich vždy pretiahne, klávesnica nemôže spôsobiť skrat.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class MatrixKeyboard extends GateSymbol {

    private static final int ROWS = 4;
    private static final int COLS = 4;
    private static final int GRID_WIDTH = COLS + 1;
    private static final int GRID_HEIGHT = ROWS + 1;

    private Pin[] pinIn;
    private Pin[] pinOut;
    private final Circle[][] keyPlungers;
    private final boolean[][] latched;

    private volatile int pressedRow = -1;
    private volatile int pressedCol = -1;

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
        latched = new boolean[ROWS][COLS];

        List<Pin> pins = new ArrayList<>(ROWS + COLS);
        pinIn = new Pin[ROWS];
        for (int row = 0; row < ROWS; row++) {
            pinIn[row] = new InputPin(this, "R" + row, 0, row + 1, Side.LEFT);
            pins.add(pinIn[row]);
        }
        pinOut = new Pin[COLS];
        for (int col = 0; col < COLS; col++) {
            pinOut[col] = new OutputPin(this, "C" + col, col + 1, 0, Side.TOP);
            // slabý výstup (pull-up): idle HIGH, silný vodič na sieti ho môže pretiahnuť
            pinOut[col].getOwnedPotential().setType(PinType.WEAK_OUT);
            pins.add(pinOut[col]);
        }
        return pins;
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

        Pane pane = new Pane(body);

        // klávesy v matici 4x4 - ľavým tlačidlom stlač a drž, pustením sa vráti do kľudu;
        // pravým tlačidlom sa stav klávesy trvalo prepne (toggle)
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Circle key = new Circle((col + 1.5) * cell, (row + 1.5) * cell, cell * 0.3, Color.BLACK);
                key.setStroke(Color.WHITE);
                key.setStrokeWidth(1.0);
                final int r = row;
                final int c = col;
                key.setOnMousePressed(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        setPressed(r, c);
                    } else if (event.getButton() == MouseButton.SECONDARY) {
                        setLatched(r, c, !latched[r][c]);
                    }
                });
                key.setOnMouseReleased(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        releasePressed(r, c);
                    }
                });
                keyPlungers[row][col] = key;
                pane.getChildren().add(key);
            }
        }

        return pane;
    }

    private boolean isActiveKey(int row, int col) {
        return (pressedRow == row && pressedCol == col) || latched[row][col];
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
        for (boolean[] row : latched) {
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

        if (getSheet() != null && getSheet().isSimulationRunning()) {
            simulate();
        }
    }

    /**
     * Uvoľnenie momentovo stlačenej klávesy.
     */
    public void releasePressed(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return;
        if (pressedRow != row || pressedCol != col) return;
        pressedRow = -1;
        pressedCol = -1;
        refreshVisual();

        if (getSheet() != null && getSheet().isSimulationRunning()) {
            simulate();
        }
    }

    /**
     * Trvalé prepnutie (toggle) klávesy - ekvivalent pravého kliknutia.
     */
    public void setLatched(int row, int col, boolean value) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return;
        latched[row][col] = value;
        refreshVisual();

        if (getSheet() != null && getSheet().isSimulationRunning()) {
            simulate();
        }
    }

    public boolean isKeyLatched(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return false;
        return latched[row][col];
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
        return "Klávesnica 4x4 - riadky R0-R3 (vľavo), stĺpce C0-C3 (hore); ľavým tlačidlom stlač, pravým trvalo prepneš";
    }
}