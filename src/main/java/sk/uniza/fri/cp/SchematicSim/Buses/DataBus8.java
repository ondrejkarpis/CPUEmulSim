package sk.uniza.fri.cp.SchematicSim.Buses;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.Bus.Bus;
import sk.uniza.fri.cp.SchematicSim.Pin.Pin;
import sk.uniza.fri.cp.SchematicSim.Sheet.SchematicSheet;
import sk.uniza.fri.cp.SchematicSim.Side;
import sk.uniza.fri.cp.SchematicSim.Wire.Wire;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Dátová 8-bitová zbernica zobrazená ako hrubá zvislá čiara ("lištová zbernica").
 * <p>
 * Chovanie (rovnako ako {@link AddressBus16}):
 * <ul>
 *     <li>dĺžku čiary možno meniť potiahnutím koncového rukoväťa (spodný koniec myšou);</li>
 *     <li>ľavé kliknutie na čiaru otvorí menu výberu vývodu D0..D7 a vytvorí vývod;</li>
 *     <li>natiahnutie spojenia (vodiča) na čiaru a pustenie myši tiež otvorí menu a pripojí vodič;</li>
 *     <li>každý vývod má meno Dx zobrazené nad spojením vedľa zbernice;</li>
 *     <li>z jednej zbernice možno ťahať viac rovnakých signálov - každé odbočenie tvorí
 *         vlastný samostatný vývod (potenciálová sieť má pre každý vývod vlastný list).</li>
 * </ul>
 * <p>
 * Podľa riadiacich signálov CPU zbernica buď premieta dáta zo zbernice na vývody
 * (zápis - vývody hná), alebo číta hodnotu z vývodov obvodu a ukladá ju na zbernicu
 * (čítanie - vývody sú v stave vysokej impedancie, aby mohol obvod odpovedať).
 *
 * @author Tomáš Hianik (pôvodný autor DataBusCommunicator), adaptácia pre SchematicSim,
 *         redizajn na lištovú zbernicu (Claude)
 */
public class DataBus8 extends BusSymbol {

    private static final String[] PIN_NAMES = createPinNames();

    private static String[] createPinNames() {
        String[] names = new String[8];
        for (int index = 0; index < names.length; index++) {
            names[index] = "D" + index;
        }
        return names;
    }

    private static final int RAIL_WIDTH = 2;
    private static final int DEFAULT_ROWS = 8;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 60;

    private static final Color RAIL_COLOR = Color.DARKRED;

    /** Hrúbka čiary relatívne k bunke (vodič má ~6 px, lišta teda o málo hrubšia). */
    private static final double RAIL_THICKNESS = 0.45;

    private int rows = DEFAULT_ROWS;

    private Pane railPane;
    private Rectangle line;
    private Circle resizeHandle;
    private boolean resizing;

    private List<Pin> pinsRef;
    private final List<TapPoint> taps = new ArrayList<>();

    private ContextMenu signalMenu;
    private MenuItem allMenuItem;
    private final RadioMenuItem[] menuItems = new RadioMenuItem[8];

    private volatile boolean visualsScheduled;

    private final ChangeListener<Number> onDataBusChange = (obs, oldValue, newValue) -> handleDataChange();
    private final ChangeListener<Number> onControlBusChange = (obs, oldValue, newValue) -> handleControlChange();

    private volatile boolean read;
    private volatile boolean write;
    private volatile int data;

    public DataBus8() {
        super();
    }

    @Override
    public Pane getImage() {
        Text label = new Text(getName());
        label.setLayoutY(label.getBoundsInLocal().getHeight());

        Image image = new Image(getClass().getResourceAsStream("/icons/bus.png"));
        ImageView view = new ImageView(image);
        view.setLayoutY(label.getBoundsInLocal().getHeight() + 5);

        label.layoutXProperty().bind(Bindings.createDoubleBinding(
                () -> (image.getWidth() - label.getBoundsInLocal().getWidth()) / 2.0,
                image.widthProperty(), label.boundsInLocalProperty()));

        return new Pane(label, view);
    }

    public DataBus8(SchematicSheet sheet) {
        super(sheet);
        getBus().dataBusProperty().addListener(onDataBusChange);
        getBus().controlBusProperty().addListener(onControlBusChange);
        this.data = Byte.toUnsignedInt(getBus().getDataBus());
        initRail();
        syncFromBus();
    }

    @Override
    protected List<Pin> createPins() {
        this.pinsRef = new ArrayList<>();
        return this.pinsRef;
    }

    @Override
    protected String getBusLabel() {
        return "DB";
    }

    @Override
    protected String[] getBusPinNames() {
        return PIN_NAMES;
    }

    @Override
    protected Pane drawBody() {
        this.railPane = new Pane();
        return this.railPane;
    }

    private void initRail() {
        int cell = getSheet().getGrid().getSizeMin();
        double railCenterX = RAIL_WIDTH * cell / 2.0;
        double thickness = Math.max(4, cell * RAIL_THICKNESS);

        line = new Rectangle(railCenterX - thickness / 2.0, 0, thickness, rows * cell);
        line.setFill(RAIL_COLOR);
        line.setStroke(Color.BLACK);
        line.setStrokeWidth(1);
        line.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleRailClick);
        line.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleRailRelease);

        // neviditeľný rukoväť na zmenu dĺžky - cítiť ho je len kurzorom, samotný nie je vidieť
        resizeHandle = new Circle(railCenterX, rows * cell, cell * 0.35, Color.TRANSPARENT);
        resizeHandle.setStroke(Color.TRANSPARENT);
        resizeHandle.setCursor(Cursor.N_RESIZE);
        resizeHandle.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleResizePressed);
        resizeHandle.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleResizeDragged);
        resizeHandle.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleResizeReleased);

        Text title = new Text(getBusLabel());
        title.setLayoutX(railCenterX - cell * 0.55);
        title.setLayoutY(-cell * 0.15);
        title.setFont(Font.font(cell * 0.6));
        title.setFill(RAIL_COLOR);
        title.setMouseTransparent(true);

        railPane.getChildren().addAll(line, title, resizeHandle);
    }

    /**
     * Vytvorenie vývodu "D{bit}" na riadku s lokálnou súradnicou localY. Vracia pin
     * vývodu (aby sa naň dal pripojiť test/vodič). Verejné kvôli testom.
     */
    public Pin createTap(int bit, int localY) {
        int cell = getSheet().getGrid().getSizeMin();
        return createTap(bit, (double) localY);
    }

    public Pin createTap(int bit, double localY) {
        int cell = getSheet().getGrid().getSizeMin();
        double x = RAIL_WIDTH * cell / 2.0;
        double y = snap(clamp(localY, cell, rows * cell), cell);

        TapPin pin = new TapPin(this, "D" + bit, bit);
        pin.setLayoutX(x);
        pin.setLayoutY(y);
        this.getChildren().add(pin);

        Circle dot = new Circle(x, y, cell * 0.28, Color.GRAY);
        dot.setStroke(Color.BLACK);
        dot.setStrokeWidth(1);
        // dot je len vizuálny - myš ide skrz na pin, aby ťahaním z vývodu vzniklo spojenie
        dot.setMouseTransparent(true);

        Text label = new Text("D" + bit);
        label.setLayoutX(x + cell * 0.6);
        label.setLayoutY(y - cell * 0.4);
        label.setFont(Font.font(cell * 0.5));
        label.setFill(RAIL_COLOR);
        label.setMouseTransparent(true);

        TapPoint tap = new TapPoint(pin, dot, label, bit);
        getChildren().addAll(dot, label);
        taps.add(tap);
        pinsRef.add(pin);

        // pravým tlačidlom na vývode sa dá zmeniť, ktorý signál zbernice reprezentuje
        pin.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                openSignalMenu(newBit -> changeTapBit(tap, newBit), this::createAllTaps, bit, e.getScreenX(), e.getScreenY());
            }
        });

        driveTap(tap);
        return pin;
    }

    private void changeTapBit(TapPoint tap, int bit) {
        tap.bit = bit;
        tap.label.setText("D" + bit);
        driveTap(tap);
    }

    private void driveTap(TapPoint tap) {
        if (write) {
            boolean high = (data & (1 << tap.bit)) != 0;
            setPin(tap.pin, high ? Pin.PinState.HIGH : Pin.PinState.LOW);
        } else {
            setPin(tap.pin, Pin.PinState.HIGH_IMPEDANCE);
        }
        scheduleTapVisuals();
    }

    private void scheduleTapVisuals() {
        if (visualsScheduled) return;
        visualsScheduled = true;
        Platform.runLater(() -> {
            visualsScheduled = false;
            for (TapPoint tap : taps) {
                boolean high = tap.pin.getState() == Pin.PinState.HIGH;
                tap.dot.setFill(high ? Color.LIME : Color.DARKGRAY);
            }
        });
    }

    /**
     * Vytvorenie vývodov pre všetky bity zbernice naraz. Vynecháva bity, ktoré už vývod majú,
     * a nové vývody umiestni na prvé voľné riadky (aby neprekrývali už existujúce). Ak je lišta
     * príliš krátka, predĺži ju. Vracia prvý vytvorený vývod (alebo prvý už
     * existujúci), aby sa naň dal pripojiť ťahaný vodič.
     */
    private Pin createAllTaps() {
        int cell = getSheet().getGrid().getSizeMin();
        int count = menuItems.length;
        Pin first = null;
        Set<Integer> usedRows = new HashSet<>();
        for (TapPoint tap : taps) {
            usedRows.add((int) Math.round(tap.pin.getLayoutY() / cell));
        }
        int nextFreeRow = 1;
        for (int i = 0; i < count; i++) {
            if (hasTap(i)) continue;
            while (usedRows.contains(nextFreeRow)) {
                nextFreeRow++;
            }
            if (nextFreeRow >= rows) {
                setRows(nextFreeRow + 1);
            }
            usedRows.add(nextFreeRow);
            Pin pin = createTap(i, nextFreeRow * cell);
            if (first == null) first = pin;
            nextFreeRow++;
        }
        if (first == null && !pinsRef.isEmpty()) {
            first = pinsRef.get(0);
        }
        return first;
    }

    private boolean hasTap(int bit) {
        for (TapPoint tap : taps) {
            if (tap.bit == bit) return true;
        }
        return false;
    }

    // === kliknutie na čiaru / odovzdanie vodiča ===

    private void handleRailClick(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;
        if (Pin.getInProgressWire() != null) return;
        final double localY = toLocalY(event);
        openSignalMenu(bit -> createTap(bit, localY), this::createAllTaps, -1, event.getScreenX(), event.getScreenY());
    }

    private void handleRailRelease(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;
        Wire creating = Pin.getInProgressWire();
        if (creating == null) return;
        if (creating.areBothEndsConnected()) return;

        final double localY = toLocalY(event);
        double thickness = Math.max(4, getSheet().getGrid().getSizeMin() * RAIL_THICKNESS);
        final boolean leftWired = event.getX() < thickness / 2.0;

        openSignalMenu(bit -> {
            Pin tapPin = createTap(bit, localY);
            tapPin.setSide(leftWired ? Side.LEFT : Side.RIGHT);
            creating.catchFreeEnd().connect(tapPin);
            creating.setMouseTransparent(false);
            creating.setOpacity(1);
            Pin.finishInProgressWire();
        }, () -> {
            Pin tapPin = createAllTaps();
            if (tapPin != null) {
                tapPin.setSide(leftWired ? Side.LEFT : Side.RIGHT);
                creating.catchFreeEnd().connect(tapPin);
                creating.setMouseTransparent(false);
                creating.setOpacity(1);
                Pin.finishInProgressWire();
            }
        }, -1, event.getScreenX(), event.getScreenY());
    }

    // === zmena dĺžky čiary ===

    private void handleResizePressed(MouseEvent event) {
        if (!event.isPrimaryButtonDown()) return;
        resizing = true;
        event.consume();
    }

    private void handleResizeDragged(MouseEvent event) {
        if (!resizing) return;
        int cell = getSheet().getGrid().getSizeMin();
        double localY = toLocalY(event);
        int newRows = clamp((int) Math.round(localY / cell), Math.max(MIN_ROWS, minRowsForTaps()), MAX_ROWS);
        if (newRows != rows) {
            rows = newRows;
            line.setHeight(rows * cell);
            resizeHandle.setCenterY(rows * cell);
        }
        event.consume();
    }

    private void handleResizeReleased(MouseEvent event) {
        if (!resizing) return;
        resizing = false;
        event.consume();
    }

    // === menu výberu vývodu ===

    private void openSignalMenu(Consumer<Integer> onSelect, Runnable onSelectAll, int preselected, double screenX, double screenY) {
        ContextMenu menu = ensureMenu();
        allMenuItem.setOnAction(event -> {
            if (onSelectAll != null) onSelectAll.run();
        });
        for (int i = 0; i < menuItems.length; i++) {
            final int bit = i;
            RadioMenuItem item = menuItems[i];
            item.setSelected(i == preselected);
            item.setOnAction(event -> {
                if (onSelect != null) onSelect.accept(bit);
            });
        }
        menu.setOnHidden(event -> cancelInProgressWire());
        menu.show(railPane, screenX, screenY);
    }

    private ContextMenu ensureMenu() {
        if (signalMenu != null) return signalMenu;
        signalMenu = new ContextMenu();
        allMenuItem = new MenuItem("Všetky");
        signalMenu.getItems().addAll(allMenuItem, new SeparatorMenuItem());
        ToggleGroup group = new ToggleGroup();
        for (int i = 0; i < menuItems.length; i++) {
            RadioMenuItem item = new RadioMenuItem("D" + i);
            item.setToggleGroup(group);
            menuItems[i] = item;
            signalMenu.getItems().add(item);
        }
        return signalMenu;
    }

    // === testovacie prístupy (package-private, nepatria do verejného API) ===

    ContextMenu getSignalMenu() {
        return signalMenu;
    }

    Rectangle getRailLine() {
        return line;
    }

    Circle getResizeHandle() {
        return resizeHandle;
    }

    private void cancelInProgressWire() {
        Wire creating = Pin.getInProgressWire();
        if (creating != null) {
            creating.setMouseTransparent(false);
            creating.setOpacity(1);
            if (!creating.areBothEndsConnected()) creating.delete();
            Pin.finishInProgressWire();
        }
    }

    // === pomocné ===

    private double toLocalY(MouseEvent event) {
        // lišta aj rukoväť majú lokálny počiatok v ľavom hornom rohu súčiastky,
        // takže getY() udalosti je priamo súradnica v lokálnom priestore súčiastky
        return event.getY();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double snap(double value, double cell) {
        return Math.round(value / cell) * cell;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // === komunikácia so zbernicou ===

    @Override
    public void syncFromBus() {
        int ctrl = getBus().getControlBus();
        boolean readActive = (ctrl & 0xB0) != 0xB0;
        boolean writeActive = (ctrl & 0x140) != 0x140;
        this.data = Byte.toUnsignedInt(getBus().getDataBus());
        this.read = readActive;
        this.write = writeActive;
        for (TapPoint tap : taps) {
            driveTap(tap);
        }
    }

    /**
     * Nastavenie počtu riadkov (dĺžky) lištovej zbernice. Ekvivalent potiahnutia rukoväťa.
     */
    public void setRows(int newRows) {
        int cell = getSheet().getGrid().getSizeMin();
        int clamped = clamp(newRows, Math.max(MIN_ROWS, minRowsForTaps()), MAX_ROWS);
        rows = clamped;
        line.setHeight(clamped * cell);
        resizeHandle.setCenterY(clamped * cell);
    }

    /**
     * Najmenší možný počet riadkov tak, aby bola lišta stále pod všetkými vývodmi
     * (plus rezervný riadok kvôli rukoväti na zmenu dĺžky).
     */
    private int minRowsForTaps() {
        int cell = getSheet().getGrid().getSizeMin();
        int maxRow = 0;
        for (TapPoint tap : taps) {
            maxRow = Math.max(maxRow, (int) Math.round(tap.pin.getLayoutY() / cell));
        }
        return maxRow + 1;
    }

    @Override
    public Map<String, String> saveProperties() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("rows", Integer.toString(rows));

        StringBuilder tapsValue = new StringBuilder();
        for (TapPoint tap : taps) {
            if (tapsValue.length() > 0) tapsValue.append(';');
            tapsValue.append(tap.bit).append(':').append(Math.round(tap.pin.getLayoutY()));
        }
        properties.put("taps", tapsValue.toString());
        return properties;
    }

    @Override
    public void loadProperties(Map<String, String> properties) {
        String rowsValue = properties.get("rows");
        if (rowsValue != null) {
            try {
                setRows(Integer.parseInt(rowsValue.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        String tapsValue = properties.get("taps");
        if (tapsValue != null && !tapsValue.isEmpty()) {
            for (String entry : tapsValue.split(";")) {
                String[] part = entry.split(":");
                if (part.length != 2) continue;
                try {
                    int bit = Integer.parseInt(part[0].trim());
                    double y = Double.parseDouble(part[1].trim());
                    createTap(bit, y);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    private void handleDataChange() {
        // pri čítaní sa riadime iba podľa simulácie (obvod odpovedá na zbernicu)
        if ((getBus().getControlBus() & 0xB0) != 0xB0) return;
        this.data = Byte.toUnsignedInt(getBus().getDataBus());
        for (TapPoint tap : taps) {
            boolean high = (data & (1 << tap.bit)) != 0;
            setPin(tap.pin, high ? Pin.PinState.HIGH : Pin.PinState.LOW);
        }
        scheduleTapVisuals();
    }

    private void handleControlChange() {
        int ctrl = getBus().getControlBus();
        boolean writeActive = (ctrl & 0x140) != 0x140;
        boolean readActive = (ctrl & 0xB0) != 0xB0;

        if (writeActive && !write) {
            // začiatok zápisu - premietneme dáta zo zbernice na vývody
            write = true;
            Bus.getBus().dataIsChanging();
            for (TapPoint tap : taps) {
                driveTap(tap);
            }
        } else if (!writeActive && write) {
            // koniec zápisu - dáta na vývodoch necháme podržané; zmenia sa až pri setRandomData()
            write = false;
            Bus.getBus().dataIsChanging();
        }

        if (readActive && !read) {
            // začiatok čítania - vývody prepneme do stavu vysokej impedancie,
            // aby mohol obvod odpovedať na zbernicu
            read = true;
            Bus.getBus().dataIsChanging();
            for (TapPoint tap : taps) {
                setPin(tap.pin, Pin.PinState.HIGH_IMPEDANCE);
            }
        } else if (!readActive && read) {
            read = false;
            Bus.getBus().dataIsChanging();
            for (TapPoint tap : taps) {
                setPin(tap.pin, Pin.PinState.HIGH_IMPEDANCE);
            }
        }
    }

    private void readPinsIntoBus() {
        int result = this.data;
        for (TapPoint tap : taps) {
            if (isHigh(tap.pin)) {
                result |= (1 << tap.bit);
            } else if (isLow(tap.pin)) {
                result &= ~(1 << tap.bit);
            }
        }
        if (result != this.data) {
            this.data = result;
            getBus().setDataBus((byte) result);
        }
    }

    @Override
    public void simulate() {
        if (read) {
            readPinsIntoBus();
        } else if (write) {
            for (TapPoint tap : taps) {
                driveTap(tap);
            }
        }
    }

    @Override
    public void reset() {
        for (TapPoint tap : taps) {
            tap.pin.setState(Pin.PinState.NOT_CONNECTED);
        }
    }

    @Override
    public int getGridWidth() {
        return RAIL_WIDTH;
    }

    @Override
    public int getGridHeight() {
        return rows;
    }

    private static class TapPoint {
        final Pin pin;
        final Circle dot;
        final Text label;
        int bit;

        TapPoint(Pin pin, Circle dot, Text label, int bit) {
            this.pin = pin;
            this.dot = dot;
            this.label = label;
            this.bit = bit;
        }
    }

    private static class TapPin extends Pin {
        TapPin(DataBus8 owner, String name, int index) {
            super(owner, name, Direction.INOUT, 0, 0, Side.RIGHT);
        }
    }
}