package sk.uniza.fri.cp.BreadboardSim.Board;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;
import sk.uniza.fri.cp.BreadboardSim.DescriptionPane;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.Item;
import sk.uniza.fri.cp.BreadboardSim.SchoolBreadboard;
import sk.uniza.fri.cp.BreadboardSim.Selectable;
import sk.uniza.fri.cp.BreadboardSim.Socket.PowerSocket;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Plocha simulátora. Na začiatku obsahuje jednu vývojovú dosku.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class Board extends ScrollPane {

    //rozmery v pixeloch
    private double widthPx;
    private double heightPx;

    //zakladne objeky
    private final BoardSimulator simulator;
    private final GridSystem gridSystem;
    private final BoardLayersManager layersManager;
    private DescriptionPane descriptionPane; //odkaz na panel s popisom

    private ArrayList<Selectable> selected; //vybrané objekty na ploche
    private Item addingItem; //pridávaný item z itempickera

    private boolean hasChanged = false; //udiala sa zmena na ploche od posledného uloženia?

    //zoom
    private double SCALE_DELTA = 1.1;
    private final SimpleDoubleProperty scale_total = new SimpleDoubleProperty(1);

    //EVENTY
    //pridávanie nového itemu
    private final EventHandler<MouseDragEvent> onMouseDragEnteredHandle = event -> {
        hasChanged = true;
        if (addingItem == null && event.getGestureSource() instanceof Item) {
            Item item = ((Item) event.getGestureSource());
            Board board = ((Board) event.getSource());

            try {
                addingItem = item.getClass().getConstructor(Board.class).newInstance(board);
                addItem(addingItem);
                Point2D point = this.getContent().sceneToLocal(event.getSceneX(), event.getSceneY());
                Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_DRAGGED, event.getSceneX(), event.getSceneY(),
                        event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
                        true, true, true, true, true,
                        true, true, true, true, null));

            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }


        }
    };
    //pohybovanie s pridávaným itemom
    private final EventHandler<MouseDragEvent> onMouseDragOverHandle = event -> {
        if (addingItem != null) {
            Point2D point = this.getContent().sceneToLocal(event.getSceneX(), event.getSceneY());
            Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_DRAGGED, event.getSceneX(), event.getSceneY(),
                    event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
                    true, true, true, true, true,
                    true, true, true, true, null));
        }
    };
    //ukončenie pridávania nového itemu
    private final EventHandler<MouseDragEvent> onMouseDragReleasedHandle = event -> {
        if (addingItem != null) {
            Point2D point = this.getContent().sceneToLocal(event.getSceneX(), event.getSceneY());
            Event.fireEvent(addingItem, new MouseEvent(MouseEvent.MOUSE_RELEASED, event.getSceneX(), event.getSceneY(),
                    event.getScreenX(), event.getScreenY(), MouseButton.PRIMARY, 1, true,
                    true, true, true, true, true,
                    true, true, true, true, null));
            addingItem = null;
        }
    };
    //keď myš opustí plochu s vytváraným objektom, znič ho
    private final EventHandler<MouseDragEvent> onMouseDragExitedHandle = event -> {
        if (addingItem != null) {
            addingItem.delete();
            addingItem = null;
        }

        event.consume();
    };

    /**
     * Vytvára text použitý na popis častí vývojovej dosky.
     *
     * @param text Obsah textu.
     * @param size Veľkosť písma.
     * @return Vytovrená nová inštancia textu s nastavenými parametrami pre css formátovanie.
     */
    public static Text getLabelText(String text, int size){
		Text out = new Text(text);
		out.setFont(Font.font(size));
		out.setId("breadboardLabel");
		out.setStrokeWidth(0);

		return out;
	}

    /**
     * Plocha simulátora.
     *
     * @param width      Šírka v pixeloch.
     * @param height     Výška v pixeloch.
     * @param gridSizePx Rozmer strany štvorčeka na ploche v pixeloch.
     */
    public Board(double width, double height, int gridSizePx) {
        //inicializacia atributov
		this.widthPx = width;
		this.heightPx = height;
		this.selected = new ArrayList<>();
		this.simulator = new BoardSimulator();
        this.gridSystem = new GridSystem(gridSizePx);
        Pane gridBackground = gridSystem.generateBackground(this.widthPx, this.heightPx, Color.WHITESMOKE, Color.GRAY);
		this.layersManager = new BoardLayersManager(gridBackground);

        //vytvorenie prvej vývojovej dosky a jej umiestnenie do stredu plochy
        SchoolBreadboard sb = new SchoolBreadboard(this);
        this.addItem(sb);
        sb.moveTo((int) (width / gridSizePx / 2d - sb.getGridWidth() / 2d), (int) (height / gridSizePx / 2d - sb.getGridHeight() / 2d));
        this.hasChanged = false;


        //EVENTY
        //po kliknuti primarnym tlacidlom na volnu plochu zrus vyber
        gridBackground.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton().name().equalsIgnoreCase("PRIMARY"))
                clearSelect();
        });

        this.addEventHandler(MouseDragEvent.MOUSE_DRAG_ENTERED, onMouseDragEnteredHandle);
        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_OVER, onMouseDragOverHandle);
        this.addEventFilter(MouseDragEvent.MOUSE_DRAG_RELEASED, onMouseDragReleasedHandle);
        this.addEventHandler(MouseDragEvent.MOUSE_DRAG_EXITED, onMouseDragExitedHandle);

        //ZOOM
        //credit: http://stackoverflow.com/questions/16680295/javafx-correct-scaling

        //novy zoom nema rad pannable, implementuje vlastne
        this.setPannable(false);

        final Group contentGroup = this.layersManager.getLayers();
        final StackPane zoomPane = new StackPane(contentGroup);
        final Group scrollContent = new Group(zoomPane);

        this.setContent(scrollContent);

        this.viewportBoundsProperty().addListener(new ChangeListener<Bounds>() {
            @Override
            public void changed(ObservableValue<? extends Bounds> observable,
                                Bounds oldValue, Bounds newValue) {
                zoomPane.setMinSize(newValue.getWidth(), newValue.getHeight());
            }
        });

        zoomPane.setOnScroll(event -> {
            event.consume();
            if (event.getDeltaY() == 0) return;

            double scaleFactor = (event.getDeltaY() > 0)
                    ? SCALE_DELTA
                    : 1 / SCALE_DELTA;

            if (scaleFactor * scale_total.get() >= 0.3 && scaleFactor * scale_total.get() <= 3) {
                // amount of scrolling in each direction in scrollContent coordinate units
                Point2D scrollOffset = figureScrollOffset(scrollContent, this);

                contentGroup.setScaleX(contentGroup.getScaleX() * scaleFactor);
                contentGroup.setScaleY(contentGroup.getScaleY() * scaleFactor);

                scale_total.setValue(scale_total.doubleValue() * scaleFactor);

                // move viewport so that old center remains in the center after the scaling
                repositionScroller(scrollContent, this, scaleFactor, scrollOffset);
            }
        });

        // Panning via drag....
        final ObjectProperty<Point2D> lastMouseCoordinates = new SimpleObjectProperty<>();
        scrollContent.setOnMousePressed(event ->
                lastMouseCoordinates.set(new Point2D(event.getX(), event.getY())));

        ScrollPane scroller = this;
        scrollContent.setOnMouseDragged(event -> {
            double deltaX = event.getX() - lastMouseCoordinates.get().getX();
            double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
            double deltaH = deltaX * (scroller.getHmax() - scroller.getHmin()) / extraWidth;
            double desiredH = scroller.getHvalue() - deltaH;
            scroller.setHvalue(Math.max(0, Math.min(scroller.getHmax(), desiredH)));

            double deltaY = event.getY() - lastMouseCoordinates.get().getY();
            double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
            double deltaV = deltaY * (scroller.getHmax() - scroller.getHmin()) / extraHeight;
            double desiredV = scroller.getVvalue() - deltaV;
            scroller.setVvalue(Math.max(0, Math.min(scroller.getVmax(), desiredV)));
        });
    }

    /**
     * Vráti aktuálne použité priblíženie.
     *
     * @return Faktor priblíženia <0.3 - 3>.
     */
    public double getAppliedScale() {
        return scale_total.getValue();
    }

    /**
     * Property aktuálne použitého faktoru priblíženia.
     *
     * @return Property objekt s faktorom priblíženia.
     */
    public SimpleDoubleProperty zoomScaleProperty() {
        return scale_total;
    }

    /**
     * Vracia informaciu o zmene na ploche od zavolania clearChange().
     *
     * @return Nastala zmena na ploche?
     */
    public boolean hasChanged() {
        return hasChanged;
    }

    /**
     * Zrušenie príznaku zmeny, napr. pri uložení.
     */
    public void clearChange() {
        this.hasChanged = false;
    }

    /**
     * Vráti použitý systém mriežkovania.
     * Dá sa použiť na nastavenie veľkostí podľa rozmerov mriežky.
     *
     * @return Použitý sustém mriežkovania.
     */
    public GridSystem getGrid(){
        return gridSystem;
    }

    /**
     * Šírka plochy v pixeloch.
     *
     * @return Šírka plochy.
     */
    public double getWidthPx(){
        return this.widthPx;
    }

    /**
     * Výška plochy v pixeloch.
     *
     * @return Výška plochy.
     */
    public double getHeightPx(){
        return this.heightPx;
    }

    /**
     * Prepočítanie súradníc zo scnény na súradnice na ploche podľa pozadia - mriežky.
     *
     * @param sceneX X-ová súradnica zo scény.
     * @param sceneY Y-ová súradnica zo scény.
     * @return Súradnice na ploche.
     */
    public Point2D sceneToBoard(double sceneX, double sceneY) {
        return layersManager.getLayer("background").sceneToLocal(sceneX, sceneY);
    }

    /**
     * Skytá plocha na x-ovej osy.
     *
     * @return Šírka skrytej plochy vľavo.
     */
    public double getOriginSceneOffsetX(){
        return layersManager.getLayer("background").getLocalToSceneTransform().getTx();
    }

    /**
     * Skytá plocha na y-ovej osy.
     *
     * @return Výška skrytej plochy hore.
     */
    public double getOriginSceneOffsetY(){
        return layersManager.getLayer("background").getLocalToSceneTransform().getTy();
    }

    /**
     * Nastaví odkaz na panel s popisom, na ktorom sa vudú zobrazovať informácie o objektoch po ich výbere.
     *
     * @param descriptionPane Odkaz na panel s popisom.
     */
    public void setDescriptionPane(DescriptionPane descriptionPane) {
        this.descriptionPane = descriptionPane;
    }

	/**
	 * Pridá položku do akutálneho výberu.
     * Pri výbere jediného itemu sa zobrazí jeho popis na panely s popisom.
     * Ak je vybraných viacero itemov, neobrazuje sa popis.
     *
	 * @param item Nová položka
     * @return True ak bola pridaná, false inak.
     */
	public boolean addSelect(Selectable item){
		if(((Group) item).getParent() instanceof SchoolBreadboard)
			item = (SchoolBreadboard) ((Group) item).getParent();

		//ak je toto jediný vybraný item, ukáž informácie o ňom
		if(selected.size() == 0)
            this.descriptionPane.setDescription(item);

		if(!selected.contains(item)) {
			item.select();
			return selected.add(item);
		}

		return false;
	}

	/**
	 * Odoberie položku z aktuálneho výberu
	 * 
	 * @param item Položka na odobratie
     * @return True ak sa ju podarilo odobrať, false inak.
     */
    public boolean removeSelect(Selectable item){
        item.deselect();
        return selected.remove(item);
    }

    /**
     * Vyčistí výber objektov.
     */
    public void clearSelect(){
        selected.forEach(item -> item.deselect());
        selected.clear();
        this.descriptionPane.clear();
    }

    /**
     * Odobratie vybraných objektov z plochy.
     */
    public void deleteSelect(){
        selected.forEach(item -> item.delete());
    }

	/**
	 * Pridá položku na plochu. Podla typu ju zaradí do odpovedajúcej vrstvy.
     *
	 * @param item Nová položka na ploche.
     * @return True ak bola pridaná, false inak.
     */
    public boolean addItem(Object item) {
        hasChanged = true;
        return layersManager.add(item);
    }

    /**
     * Odobratie položky z plochy.
     *
     * @param item Položka na odobratie.
     * @return True ak sa ju podarilo odobrať, false inak.
     */
    public boolean removeItem(Object item) {
        hasChanged = true;
        return layersManager.remove(item);
    }

    /**
     * Vyčistenie plochy. Ostane iba prvá vývojová doska.
     */
    public void clearBoard() {
        this.layersManager.clear();
    }

    /**
     * Zapnutie simulácie.
     * Zozbiera zo všetkých pridaných komponentov PowerSocket-y pre inicializáciu po spustení simulácie.
     */
    public void powerOn(){
        if (!simulator.runningProperty().getValue()) {
            List<PowerSocket> powerSockets = new LinkedList<>();

            layersManager.getComponents().forEach(component ->
                    powerSockets.addAll(component.getPowerSockets()));

            simulator.start(powerSockets);
        }
    }

    /**
     * Vypnutie simulácie.
     */
    public void powerOff(){
        simulator.stop();
    }

    /**
     * Beží simulácia?
     * @return True ak beží, false inak.
     */
    public boolean isSimulationRunning(){ return simRunningProperty().getValue();
    }

    /**
     * Property objekt behu simulácie.
     *
     * @return Property - true ak simulácia beží, false inak.
     */
    public ReadOnlyBooleanProperty simRunningProperty(){
        return simulator.runningProperty();
    }

    /**
     * Vrátia odkaz na simulátor.
     * @return
     */
    public BoardSimulator getSimulator() {
        return this.simulator;
    }

    /**
     * Pridanie udalosti do simulácie.
     *
     * @param event Nová udalosť.
     */
    public void addEvent(BoardEvent event){
        simulator.addEvent(event);
    }

    /**
     * Pozícia kurzoru nad plochou.
     *
     * @param event Event vygenerovaný pohybom kurzoru.
     * @return Súradnice miesta, nad ktorým sa kurzor nachádza.
     */
    public Point2D getMousePositionOnGrid(MouseEvent event) {
        Point2D local = layersManager.getLayer("background").sceneToLocal(event.getSceneX(), event.getSceneY());
        return gridSystem.pixelToGrid(local.getX(), local.getY());
    }

    /**
     * Uloženie zapojenia do súboru.
     *
     * @param file Súbor, do ktorého sa má zapojenie uložiť.
     * @return True, ak sa ukladanie podarilo, false inak.
     */
    public boolean save(File file) {
        return SchemeLoader.save(file, layersManager);
    }

    /**
     * Načítanie zapojenia zo súboru.
     *
     * @param file Súbor s uloženým zapojením.
     * @return True, ak sa podarilo načítať súbor, false inak.
     */
    public boolean load(File file) {
        return SchemeLoader.load(file, layersManager);
    }

    /*
    DETEKCIA KOLIZII -> PIN - SOKET
	 */

    /**
     * Kontroluje prienik bounds všetkých komponentov na ploche a daného zariadenia.
     *
     * @param device Zariadenie, pre ktoré sa má kontrolovať prienik
     * @return List komponentov s ktorými je v prieniku. Ak nie je v prieniku so žiadnym komponentom, vracia null.
     */
    public List<Component> checkForCollisionWithComponent(Device device) {
        List<Component> components = null;

        //skontroluj vsetky komponenty, ci sa s nimi zariadenie prekryva
        for (Component component : layersManager.getComponents()) {
            if (component.localToScene(component.getBoundsInLocal())
                    .intersects(device.localToScene(device.getBoundsInLocal()))) {
                //ak zoznam neexistuje, vytvor ho
                if (components == null)
                    components = new LinkedList<>();
                //ak ano, pridaj ho do zoznamu na zaciatok -> od najvrchnejsich k spodnym

                components.add(0, component);
            }
        }

        if (components != null && components.size() > 1) {
            //ak bolo najdenych viacero komponentov
            Component topMostComponent = components.get(0);
            if (topMostComponent.localToScene(topMostComponent.getBoundsInLocal())
                    .contains(device.localToScene(device.getBoundsInLocal()))) {
                //a ak zariadenie je cele nad najvyssim komponentom, vrat iba tento jeden komponent
                //to aby sa zariadenie nepripajalo skrze viacero komponentov
                components.clear();
                components.add(topMostComponent);
            }
        }

        return components;
    }

    /**
     * Kontrola na prienik pinu so soketom.
     *
     * @param component Komponent, na ktorom sa majú hľadať sokety.
     * @param pin       Pin, s ktorým sa má hľdať prienik.
     * @return Soket pod pinom, null ak taký neexistuje.
     */
    public Socket checkForCollisionWithSocket(Component component, Pin pin) {
        for (Socket socket : component.getSockets()) {
            if (socket.localToScene(socket.getBoundsInLocal()).intersects(pin.localToScene(pin.getBoundsInLocal())))
                return socket;
        }

        return null;
    }

    /**
     * ZOOM
     */
    private Point2D figureScrollOffset(Node scrollContent, ScrollPane scroller) {
        double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
        double hScrollProportion = (scroller.getHvalue() - scroller.getHmin()) / (scroller.getHmax() - scroller.getHmin());
        double scrollXOffset = hScrollProportion * Math.max(0, extraWidth);
        double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
        double vScrollProportion = (scroller.getVvalue() - scroller.getVmin()) / (scroller.getVmax() - scroller.getVmin());
        double scrollYOffset = vScrollProportion * Math.max(0, extraHeight);
        return new Point2D(scrollXOffset, scrollYOffset);
    }

    /**
     * ZOOM
     */
    private void repositionScroller(Node scrollContent, ScrollPane scroller, double scaleFactor, Point2D scrollOffset) {
        double scrollXOffset = scrollOffset.getX();
        double scrollYOffset = scrollOffset.getY();
        double extraWidth = scrollContent.getLayoutBounds().getWidth() - scroller.getViewportBounds().getWidth();
        if (extraWidth > 0) {
            double halfWidth = scroller.getViewportBounds().getWidth() / 2;
            double newScrollXOffset = (scaleFactor - 1) * halfWidth + scaleFactor * scrollXOffset;
            scroller.setHvalue(scroller.getHmin() + newScrollXOffset * (scroller.getHmax() - scroller.getHmin()) / extraWidth);
        } else {
            scroller.setHvalue(scroller.getHmin());
        }
        double extraHeight = scrollContent.getLayoutBounds().getHeight() - scroller.getViewportBounds().getHeight();
        if (extraHeight > 0) {
            double halfHeight = scroller.getViewportBounds().getHeight() / 2;
            double newScrollYOffset = (scaleFactor - 1) * halfHeight + scaleFactor * scrollYOffset;
            scroller.setVvalue(scroller.getVmin() + newScrollYOffset * (scroller.getVmax() - scroller.getVmin()) / extraHeight);
        } else {
            scroller.setHvalue(scroller.getHmin());
        }
    }
}