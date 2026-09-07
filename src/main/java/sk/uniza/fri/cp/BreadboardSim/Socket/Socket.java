package sk.uniza.fri.cp.BreadboardSim.Socket;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.effect.BoxBlur;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import sk.uniza.fri.cp.BreadboardSim.Board.BoardChangeEvent;
import sk.uniza.fri.cp.BreadboardSim.Board.BoardEvent;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputOutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.Item;
import sk.uniza.fri.cp.BreadboardSim.Wire.Wire;
import sk.uniza.fri.cp.BreadboardSim.Wire.WireEnd;

import java.util.LinkedList;

/**
 * Soket reprezentuje kontaktný bod na komponente, ku ktorému sa môžu pripájať piny zariadení alebo konce spojení.
 * Uchováva si odkaz na svoj potenciál, ktorý tvorí strom spojení. Podľa tohto stromu sa zisťuje výsledný potenciál
 * na pripojenom pine zariadenia.
 * Soket pripája pin, nie naopak.
 *
 * Nové spojenie sa vytvára kliknutím na soket a ťahaním kurzora po ploche.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public class Socket extends Group {

    private static final Color TOP_BORDER_DEF_COLOR = Color.rgb(190, 190, 190);
    private static final Color BOTTOM_BORDER_DEF_COLOR = Color.rgb(230, 230, 230);

	//HIGHLIGHT KONSTANTY
    public static final int WARNING = 4;
    public static final int COMMON_POTENTIAL_OTHER = 3;
    public static final int COMMON_POTENTIAL = 2;
    public static final int OK = 1;
	public static final int INFO = 0;

    private final boolean[] activeHighlights = new boolean[5]; //aktívne zvýraznenie soketu

    //zoznam soketov, ktore boli zvyraznene (aby ich bolo mozne odvyraznit)
    private final LinkedList<Socket> highlightedConnectedSockets = new LinkedList<>(); //sokety spojene s tymto soketom
    private final LinkedList<Socket> highlightedWireEndSockets = new LinkedList<>(); //sokety na konci vytvaraneho kablika

    private final Component component; //komponent, na ktorom sa soket nachádza
    private final Potential potential; //potenciál soketu
    private Pin pin; //pin, ak je pripojený
    private WireEnd connectedWireEnd; //koniec káblika, ak je pripojený

    private final Circle colorizer; //zafarbuje soket

    //EVENTY
    private static Wire creatingWire; //spojenie, ktoré sa vytvára, ak sa vytvára
    // zvyraznenie
    private EventHandler<MouseEvent> onMouseEntered = event -> highlight(INFO);
    private EventHandler<MouseEvent> onMouseExited = event -> unhighlight(INFO);
    //pohybovanie s novovytvoreným koncom káblika
    private EventHandler<MouseEvent> onMouseDragged = event -> {
        if (!event.isPrimaryButtonDown()) return;

        if (creatingWire != null) {
            Socket socket = (Socket) event.getSource();
            Point2D boardXY = socket.component.getBoard().sceneToBoard(event.getSceneX(), event.getSceneY());
            creatingWire.catchFreeEnd().moveTo(boardXY.getX(), boardXY.getY());

            // stransparentnenie objektov nad ktorymi koniec kablika prechadza
            creatingWire.catchFreeEnd().onMouseDragged(event);
        }

        event.consume();
    };
    //zvýraznenie prepojených soketov
    private EventHandler<MouseEvent> onMousePressed = event ->
            highlightConnectedSockets(highlightedConnectedSockets);
    //ukončenie vytvárania káblika a odvýraznenie prepojených soketov
    private EventHandler<MouseEvent> onMouseReleased = event -> {
        if (creatingWire != null) {
            creatingWire.setMouseTransparent(false);
            creatingWire.setOpacity(1);
            if (!creatingWire.areBothEndsConnected()) creatingWire.delete();

            // odtransparentnenie objektov nad ktorymi presiel koniec kablika
            creatingWire.catchFreeEnd().onMouseReleased(event);
            creatingWire = null;
        }

        unhighlightConnectedSockets(highlightedConnectedSockets);
        event.consume();
    };

    //FULLDRAG
    //vytvorenie nového káblika
    private EventHandler<MouseEvent> onMouseDragDetected = event -> {
        if (!event.isPrimaryButtonDown()) return;

        startFullDrag();

        Socket socket = (Socket) event.getSource();

        creatingWire = new Wire(socket);
        creatingWire.setMouseTransparent(true);
        creatingWire.setOpacity(0.5);

        socket.getComponent().getBoard().addItem(creatingWire);

        event.consume();
    };
    //zvyraznenie pripojenych soketov pod koncom káblika
    private EventHandler<MouseDragEvent> onMouseDragEntered = event -> {
        highlightConnectedSockets(highlightedWireEndSockets);
        event.consume();
    };
    //zrusenie zvyraznenia pripojenych socketov pod koncom kablika
    private EventHandler<MouseDragEvent> onMouseDragExited = event -> {
        unhighlightConnectedSockets(highlightedWireEndSockets);
        event.consume();
    };
    //pripojenie konca káblika na mna, ked je pustené tlačidlo
    private EventHandler<MouseDragEvent> onMouseDragReleased = event -> {
        unhighlightConnectedSockets(highlightedWireEndSockets);

        if (creatingWire != null) {
            //ak sa vytvara kablik
            Socket socket = (Socket) event.getSource();

            if (socket != event.getGestureSource())
                //pripoj novy koniec na seba
                creatingWire.catchFreeEnd().connectSocket(socket);
            else
                creatingWire.delete();

        } else if (event.getGestureSource() instanceof WireEnd) {
            //ak sa presuva koniec kablika
            WireEnd end = (WireEnd) event.getGestureSource();
            Socket socket = (Socket) event.getSource();
            end.connectSocket(socket);
        }

        event.consume();
    };

	/**
     * Vytvorenie soketu pre komponent.
     *
     * @param component Komponent, na ktorom sa soket nachádza.
     */
    public Socket(Component component) {
		this.component = component;

		GridSystem grid = this.component.getBoard().getGrid();
        Rectangle boundingBox = new Rectangle(grid.getSizeX(), grid.getSizeY());
        boundingBox.setOpacity(0);
		boundingBox.setLayoutX(-grid.getSizeX()/2.0);
        boundingBox.setLayoutY(-grid.getSizeY()/2.0);
        double coreRadius = grid.getSizeX() * 3.0 / 16.1;
        double borderRadius = grid.getSizeX() * 5.3 / 15.0;

		//grafika
        Circle core = new Circle(borderRadius, borderRadius, coreRadius);
        core.setFill(Color.BLACK);

        Arc topBorder = new Arc(borderRadius, borderRadius, borderRadius, borderRadius, 0.0, 180.0);
        topBorder.setFill(TOP_BORDER_DEF_COLOR);
        topBorder.setType(ArcType.ROUND);

        Arc bottomBorder = new Arc(borderRadius, borderRadius, borderRadius, borderRadius, 180.0, 180.0);
        bottomBorder.setFill(BOTTOM_BORDER_DEF_COLOR);
        bottomBorder.setType(ArcType.ROUND);

        this.colorizer = new Circle(borderRadius, borderRadius, borderRadius + borderRadius * 0.3);
        this.colorizer.setFill(Color.ORANGE);
        this.colorizer.setMouseTransparent(true);
        this.colorizer.setOpacity(0);
        BoxBlur blur = new BoxBlur(borderRadius * 0.7, borderRadius * 0.7, 3);
        this.colorizer.setEffect(blur);

        this.potential = new Potential(this, null);

		//pre relativny offset k stredu socketu
		Group graphics = new Group(topBorder, bottomBorder, core, colorizer);
		graphics.setTranslateX(-borderRadius);
		graphics.setTranslateY(-borderRadius);

		this.getChildren().addAll(boundingBox, graphics);

		this.addEventFilter(MouseEvent.MOUSE_ENTERED, onMouseEntered);
		this.addEventFilter(MouseEvent.MOUSE_EXITED, onMouseExited);
		this.addEventFilter(MouseEvent.MOUSE_DRAGGED, onMouseDragged);
        this.addEventFilter(MouseEvent.MOUSE_PRESSED, onMousePressed);
        this.addEventFilter(MouseEvent.MOUSE_RELEASED, onMouseReleased);
        this.addEventFilter(MouseEvent.DRAG_DETECTED, onMouseDragDetected);
        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_ENTERED, onMouseDragEntered);
        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_EXITED, onMouseDragExited);
        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_RELEASED, onMouseDragReleased);
	}

	/**
     * Vytvorenie soketu s prednastavenou hodnotou potenciálu.
     *
     * @param component Komponent, na ktorom sa soket nachádza.
     * @param potentialValue Hodnota potenciálu.
     */
    public Socket(Component component, Potential.Value potentialValue) {
        this(component);
        this.potential.setValue(potentialValue);
	}

    /**
     * Nastavenie typu soketu.
     *
     * @param socketType Nový typ soketu.
     */
    public void setType(SocketType socketType) {
        this.potential.setType(socketType);
    }

    /**
     * Vráti nastavený typ soketu.
     *
     * @return Typ soketu.
     */
    public SocketType getType() {
        return this.getPotential().getType();
    }

    /**
     * Nastavenie hodnoty potencialu priradeneho tomuto soketu
     *
     * @param potentialValue
     */
    public boolean setPotential(Potential.Value potentialValue) {
        return this.potential.setValue(potentialValue);
    }

    /**
     * Vráti potenciál na vrchu stromu, teda výsledný.
     *
     * @return Potenciál, podľa ktorého sa riadi celý strom spojení, ku ktorému je soket pripojený.
     */
    public Potential getPotential() {
        if (this.potential != null)
            return this.potential.getPotential();

        return null;
    }

    /**
     * Vracia potenciál tohoto soketu.
     *
     * @return Potenciál priradený k soketu.
     */
    public Potential getThisPotential() {
        return this.potential;
    }

    /**
     * Vráti X-ovú súradnicu na ploche simulátora v pixeloch.
     *
     * @return X-ová súradnica na ploche simulátora v pixeloch.
     */
    public double getBoardX(){
        return this.getLocalToSceneTransform().getTx() - component.getBoard().getOriginSceneOffsetX();
    }

    /**
     * Vráti Y-ovú súradnicu na ploche simulátora v pixeloch.
     *
     * @return Y-ová súradnica na ploche simulátora v pixeloch.
     */
    public double getBoardY(){
        return this.getLocalToSceneTransform().getTy() - component.getBoard().getOriginSceneOffsetY();
    }

    /**
     * Pripojenie pinu k soketu. Pri pripojení sa aktualizuje typ soketu na základe pripojeného pinu.
     * Pin sa nepripojí, ak je k nemu pripojený už iný pin alebo káblik.
     *
     * @param pin Pin a pripojenie. Soket na základe neho mení svoj typ.
     * @return True ak sa podarilo pripojiť pin, false inak.
     */
    public boolean connect(Pin pin) {
        if (this.isOccupied())
            return false;

        this.pin = pin;
        this.pin.setSocket(this);
        if (pin instanceof InputPin) {
            this.setType(SocketType.IN);
        } else if (pin instanceof InputOutputPin) {
            this.setType(SocketType.IO);
        } else if (pin instanceof OutputPin) {
            switch (((OutputPin) pin).getPinDriver()){
                case TRI_STATE:
                    this.setType(SocketType.TRI_OUT);
                    break;
                case PUSH_PULL:
                    this.setType(SocketType.OUT);
                    break;
                default:
            }
        } else this.setType(SocketType.NC);

        //ak simulacia bezi, pridaj zmenu na sokete
        if(this.component.getBoard().isSimulationRunning()) {
            this.component.getBoard().addEvent(new BoardEvent(this));
        }

        return true;
    }

    /**
     * Pripojenie konca káblika.
     *
     * @param wireEnd Koniec káblika
     * @return True ak sa pripojil, false inak.
     */
    public boolean connect(WireEnd wireEnd) {
        if (this.isOccupied()) return false;
        this.connectedWireEnd = wireEnd;
        this.component.addWire(wireEnd.getWire());
        return true;
    }

    /**
     * Odpojenie pripojeného pinu alebo káblika.
     *
     * @return True ak bol odpojený, false nikdy.
     */
    public boolean disconnect() {
        //ak je pripojeny kablik, odpoj ho
        if (this.connectedWireEnd != null) {
            this.component.removeWire(this.connectedWireEnd.getWire());
            this.connectedWireEnd = null;
            return true;
        }

        //ak nie je pripojeny kablik ale pin odpoj ho
        if (this.pin != null) {
            this.pin.setSocket(null);
            this.pin = null;
        }

        //a nastav typ soketu na nepripojeny
        this.setType(SocketType.NC);

        //ak simulacia bezi, pridaj zmenu na sokete
        if (this.component.getBoard().isSimulationRunning()) {
            this.component.getBoard().addEvent(new BoardChangeEvent(this, Potential.Value.NC));
        }

        return true;
    }

    /**
     * Kontrola, či je k soketu pripojený pin.
     *
     * @return True, ak je k soketu pripojený pin, false inak.
     */
    public boolean hasConnectedPin() {
        return this.pin != null;
    }

    /**
     * Kontrola, či je k soketu pripojený káblik.
     *
     * @return True, ak je k soketu pripojený koniec káblika, false inak.
     */
    public boolean hasConnectedWire() {
        return this.connectedWireEnd != null;
    }

    /**
     * Kontrola, či je k soketu niečo pripojené (pin alebo káblik).
     *
     * @return True, ak je k soketu pripojený pin alebo káblik, false inak.
     */
    public boolean isOccupied() {
        return this.hasConnectedPin() || this.hasConnectedWire();
    }

    /**
     * Vráti pripojený koniec káblika, ak taký je.
     *
     * @return Koniec káblika alebo null.
     */
    public WireEnd getWireEnd() {
        return this.connectedWireEnd;
    }

    /**
     * Vráti pripojený pin, ak taký je.
     *
     * @return Pripojený pin alebo null.
     */
    public Pin getPin() {
        return this.pin;
    }

    /**
     * Vráti pripojené zariadenia, ak je pripojený pin.
     *
     * @return Pripojené zariadenie alebo null.
     */
    public Device getDevice() {
        if (this.pin != null) {
            return this.pin.getDevice();
        }

        return null;
    }

    /**
     * Vráti komponent, na ktorom je soket umiestnený.
     *
     * @return Komponent na ktorom je soket.
     */
    public Component getComponent() {
        return component;
    }

    /**
     * Pohyblivý item. Ak je komponent na ktorom je soket scholbreadboard, vrat ten, inak komponent samotny.
     *
     * @return Item, na ktorom je soket.
     */
    public Item getItem() {
        Parent parent = this.component;
        while (parent.getParent() instanceof Item)
            parent = parent.getParent();

        return (Item) parent;
    }

    /**
     * Zvýraznenie soketu. Každý typ zvýraznenia má svoju prioritu. Ak je použíté zvýraznenie s vyššou prioritou,
     * nezobrazí sa zvýraznenie s nižšou prioritou.
     * <p>
     * Typy podľa priority:
     * WARNING - Červené zvýraznenie pri závažnej chybe na sokete (napr. skrat na výstup). Mení farbu aj pripojeného
     * konca káblika, ak nejaký je
     * COMMON_POTENTIAL - Žltá farba. Soket je pripojený na spoločný potenciál s inými soketmi.
     * OK - Zelená farba. Akcia nad soketom je povolená.
     * INFO - Oranžová farba. Iné
     *
     * @param highlightType Typ zvýraznenia.
     */
    public void highlight(int highlightType) {
        this.activeHighlights[highlightType] = true;
        this.updateHighlight();
    }

    /**
     * Zrušenie zvýraznenia soketu
     */
    public void unhighlight(int highlightType) {
        this.activeHighlights[highlightType] = false;
        this.updateHighlight();
    }

    private void updateHighlight() {
        if (this.activeHighlights[WARNING]) {
            changeColor(this.colorizer, Color.RED, 0.8);
            if (this.connectedWireEnd != null) {
                this.connectedWireEnd.setColor(Color.RED);
            }
            return;
        } else if (this.activeHighlights[COMMON_POTENTIAL] || this.activeHighlights[COMMON_POTENTIAL_OTHER]) {
            changeColor(this.colorizer, Color.YELLOW, 0.5);
        } else if (this.activeHighlights[OK]) {
            changeColor(this.colorizer, Color.GREEN, 0.5);
        } else if (this.activeHighlights[INFO]) {
            changeColor(this.colorizer, Color.ORANGE, 0.5);
        } else {
            //ak nie je nic na zvyrazenie
            changeColor(this.colorizer, Color.WHITE, 0);
        }

        if (this.connectedWireEnd != null)
            this.connectedWireEnd.setDefaultColor();
    }

    private void changeColor(Shape shape, Color color, double opacity) {
        if (Platform.isFxApplicationThread()) {
            shape.setFill(color);
            shape.setOpacity(opacity);
        } else {
            Platform.runLater(() -> {
                shape.setFill(color);
                shape.setOpacity(opacity);
            });
        }
    }

    /**
     * Zvýraznenie prepojených soketov.
     *
     * @param list List na naplnenie.
     */
    private void highlightConnectedSockets(LinkedList<Socket> list) {
        if (list == highlightedConnectedSockets) {
            getPotential().getConnectedSockets(list);
            list.forEach(socket -> socket.highlight(COMMON_POTENTIAL_OTHER));
        } else {
            getPotential().getConnectedSockets(list);
            list.forEach(socket -> socket.highlight(COMMON_POTENTIAL));
        }
    }

    private void unhighlightConnectedSockets(LinkedList<Socket> list) {
        if (!list.isEmpty()) {
            if (list == highlightedConnectedSockets) {
                list.forEach(socket -> socket.unhighlight(COMMON_POTENTIAL_OTHER));
            } else {
                list.forEach(socket -> socket.unhighlight(COMMON_POTENTIAL));
            }
            list.clear();
        }
    }
}