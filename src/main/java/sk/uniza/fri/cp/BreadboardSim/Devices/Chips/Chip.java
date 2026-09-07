package sk.uniza.fri.cp.BreadboardSim.Devices.Chips;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.LightEmitter;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstraktná trieda pre všetky integrované obvody.
 * Grafika obodu sa generuje dynamicky na základe pinov, preto je v metóde fillPins
 * potrebné všetky piny registrovať pomocou registerPin.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public abstract class Chip extends Device {

	private Pin[] pins;
    private int gridHeight = 3;

    private static InspectionPinsUpdater ipu = new InspectionPinsUpdater();
    private Stage inspectorStage;

    private EventHandler<MouseEvent> onMouseDoubleClick = event -> {
        if (event.getClickCount() >= 2) {
            Stage stage = this.getInspectionWindow();
            stage.show();
            stage.toFront();
        }
    };

    /**
     * Vytovrenie objektu chipu pre itemPicker.
     *
     * @param pinsCount Pocet výstupov obovdu.
     */
    public Chip(int pinsCount) {
        this.pins = new Pin[pinsCount];
        fillPins();
    }

    /**
     * Vytvorenie objektu pre plochu simulátora.
     * Defaultná výška obvodu sú 3 jednotky mriežky.
     *
     * @param board     Plocha simulátora.
     * @param pinsCount Počet výstupov obvodu.
     */
    public Chip(Board board, int pinsCount){
        super(board);
        this.pins = new Pin[pinsCount];

        fillPins();

        this.getChildren().add(generateGraphic());

        this.addEventHandler(MouseEvent.MOUSE_CLICKED, onMouseDoubleClick);
    }

    /**
     * Vytvorenie objektu pre plochu simulátora s definovaím jeho výšky.
     *
     * @param board Plocha simulátora.
     * @param pinsCount Počet výstupov obvodu.
     * @param chipGridHeight Výška obvodu.
     */
    public Chip(Board board, int pinsCount, int chipGridHeight) {
        super(board);
        this.pins = new Pin[pinsCount];
        this.gridHeight = chipGridHeight;

        fillPins();

        this.getChildren().add(generateGraphic());

        this.addEventHandler(MouseEvent.MOUSE_CLICKED, onMouseDoubleClick);
    }

    /**
     * Vráti pin podľa jeho indexu.
     *
     * @param index Index pinu.
     * @return Pin obodu s daným indexom.
     */
    public Pin getPin(int index){
        if(index < pins.length)
            return pins[index];
        return null;
    }

	@Override
    public List<Pin> getPins() {
        return Arrays.asList(this.pins);
    }

    /**
     * Kontrola, či je číslo pinu validné.
     *
     * @param pinNumber Číslo pinu na overenie.
     * @return True ak obvod obsehuje taký pin, false inak.
     */
    public boolean isPinNumberValid(int pinNumber){
        return pinNumber > 0 && pinNumber <= this.pins.length;
    }

    /**
     * Kontrola, či je obvod napájaný.
     *
     * @param vccPinNumber Číslo VCC pinu napájania.
     * @param gndPinNumber Číslo GND pinu napájania.
     * @return True ak sú piny napojené na +5V/0V potenciály.
     */
    public boolean isPowered(int vccPinNumber, int gndPinNumber){
        if(this.isPinNumberValid(vccPinNumber) && this.isPinNumberValid(gndPinNumber)) {
            Socket vccSocket = this.pins[vccPinNumber - 1].getSocket();
            Socket gndSocket = this.pins[gndPinNumber - 1].getSocket();
            if(vccSocket != null && gndSocket != null)
                return vccSocket.getPotential().getValue() == Potential.Value.HIGH
                        && gndSocket.getPotential().getValue() == Potential.Value.LOW;
        }
        return false;
    }

    /**
     * Kontrola, či je na pine s daným číslom HIGH hodnota potenciálu.
     *
     * @param inputPinNumber Číslo pinu.
     * @return True, ak je pripojený k hodnote potenciálu HIGH.
     */
    public boolean isHigh(int inputPinNumber) {
        return this.isPinNumberValid(inputPinNumber) && isHigh(this.pins[inputPinNumber - 1]);
    }

    /**
     * Kontrola, či je na pine s daným číslom LOW hodnota potenciálu.
     *
     * @param inputPinNumber Číslo pinu.
     * @return True, ak je pripojený k hodnote potenciálu LOW.
     */
    public boolean isLow(int inputPinNumber) {
        return this.isPinNumberValid(inputPinNumber) && isLow(this.pins[inputPinNumber - 1]);
    }

    /**
     * Vráti názov obvodu.
     *
     * @return Názov obvodu.
     */
    public abstract String getName();

    /**
     * Vráti krátky popis obvodu.
     *
     * @return Krátky popis.
     */
    public abstract String getShortStringDescription();

    /**
     * Pred vytvorenim grafiky je nutne naplnit pole pinov pomocou registerPin().
     */
	protected abstract void fillPins();

    protected Stage getInspectionWindow() {
        if (inspectorStage != null) return inspectorStage;

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);

        Text chipName = new Text(getName());
        chipName.setFont(Font.font(20));
        root.getChildren().addAll(chipName, generateInspectionImage());

        Stage stage = new Stage();
        Scene scene = new Scene(root);

        stage.setScene(scene);

        stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> {
            Arrays.stream(this.pins).forEach(pin -> ipu.remove(pin));
            inspectorStage = null;
        });

        return inspectorStage = stage;
    }

    /**
     * Zaregistruje obvodu pin s daným čislom.
     *
     * @param pinNumber Číslo pinu v obvode.
     * @param pin Pin, ktorý sa má registrovať.
     */
    protected void registerPin(int pinNumber, Pin pin) {
        if (this.isPinNumberValid(pinNumber))
            this.pins[pinNumber-1] = pin;
    }

    /**
     * Nastavenie logickej hodnoty na pine.
     *
     * @param pinNumber Číslo pinu.
     * @param state Nová hodnota.
     */
    protected void setPin(int pinNumber, Pin.PinState state) {
        if (this.isPinNumberValid(pinNumber))
            setPin(this.pins[pinNumber-1], state);
    }

    /**
     * Zaregistruje postupne všetky piny od daného čísla.
     * @param fromNumber Číslo prvého pinu
     * @param pins Piny na registráciu
     */
	boolean setAllPins(int fromNumber, Pin... pins){
		if(fromNumber > 0 && fromNumber + pins.length <= this.pins.length) {
			System.arraycopy(pins, 0, this.pins, fromNumber-1, pins.length);
			return true;
		}
		return false;
    }

    /**
     * Vráti dodatočný popis obvodu, ktorý môže obsahovať napr. funkčné tabuľky.
     *
     * @return Dlhší popis obvodu.
     */
    public HBox getMoreDescription() {
        HBox wrapper = new HBox(new ImageView(new javafx.scene.image.Image("/descriptions/chips/" + getName() + ".png")));
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    @Override
    public Pane getDescription() {
        Pane cached = super.getDescription();
        if (cached == null) {
            VBox wrapper = new VBox();
            wrapper.setAlignment(Pos.CENTER);
            wrapper.setPadding(new Insets(3, 3, 0, 3));
            wrapper.setStyle("-fx-background-color: white");
            AnchorPane.setRightAnchor(wrapper, 0d);
            AnchorPane.setLeftAnchor(wrapper, 0d);

            //nazov chipu
            Text chipName = new Text(getName());
            chipName.setFont(Font.font(25));

            //strucny popis chipu
            Text chipShortDescription = new Text(getShortStringDescription());
            chipShortDescription.setFont(Font.font(12));

            Group image = this.generateDescriptionImage();
            VBox.setMargin(image, new Insets(20, 0, 10, 0));

            HBox moreDescription = this.getMoreDescription();

            wrapper.getChildren().addAll(chipName, chipShortDescription, image, moreDescription);

            AnchorPane descriptionPane = new AnchorPane(wrapper);

            this.cacheDescription(descriptionPane);
            return descriptionPane;
        } else return cached;
    }

    @Override
    public void delete() {
        super.delete();

        //ak je otvoreny inspektor pre chip, zatvor ho pri zmazani
        if (inspectorStage != null && inspectorStage.isShowing()) {
            inspectorStage.close();
        }
    }

    //GENEROVANIE GRAFIKY

    /**
     * Generovanie grafiky obvodu na plochu simulatora.
     *
     * @return Grafika obvodu.
     */
    private Group generateGraphic(){
        Group graphic = new Group();
        GridSystem grid = getBoard().getGrid();

        //double gridHeight = 3; //vyska cipu v jednotkach policok
        double heightReduction = grid.getSizeY() * 0.3 ; //zmensenie na jednej strane

        double width = grid.getSizeX() * (this.pins.length / 2) + grid.getSizeX();
        double height = grid.getSizeY() * gridHeight - heightReduction*2;

        double topBevel = grid.getSizeY()*0.3;
        double leftBevel = grid.getSizeX()*0.2;
        Rectangle bodyLayer0 = new Rectangle(width, height, Color.BLACK);
        Rectangle bodyLayer1 = new Rectangle(width-leftBevel, height-topBevel, Color.rgb(64,64,64));
        Rectangle bodyLayer2 = new Rectangle(width-2*leftBevel, height-2*topBevel, Color.rgb(48,48,48));
        bodyLayer2.setLayoutX(leftBevel);
        bodyLayer2.setLayoutY(topBevel);

        Arc bodyMarkTop = new Arc(0, height/2, grid.getSizeY()*0.5, grid.getSizeY()*0.5, 0, 90);
        bodyMarkTop.setFill(Color.rgb(28,28,28));
        bodyMarkTop.setType(ArcType.ROUND);
        Arc bodyMarkBottom = new Arc(0, height/2, grid.getSizeY()*0.5, grid.getSizeY()*0.5, -90, 90);
        bodyMarkBottom.setFill(Color.rgb(56,56,56));
        bodyMarkBottom.setType(ArcType.ROUND);
        Arc bodyMarkMiddle = new Arc(0, height/2, grid.getSizeY()*0.25, grid.getSizeY()*0.25, -90, 180);
        bodyMarkMiddle.setFill(Color.rgb(38,38,38));
        bodyMarkMiddle.setType(ArcType.ROUND);
        bodyMarkMiddle.setStrokeWidth(0);

        Group body = new Group(bodyLayer0, bodyLayer1, bodyLayer2, bodyMarkTop, bodyMarkBottom, bodyMarkMiddle);
        body.setLayoutX(-grid.getSizeX());
        body.setLayoutY(heightReduction);

        Group legs = new Group();
        final double legWidth = grid.getSizeX()*2.0/3.0;
        final double legHeight = grid.getSizeY()*0.7;
        for (int i = 0; i < this.pins.length; i++){
            Polygon legBase = new Polygon(0, 0,
                    legWidth, 0,
                    legWidth, legHeight/4.0,
                    legWidth/2.0, legHeight/2.0,
                    0, legHeight/4.0);
            legBase.setFill(Color.rgb(140,140,140));
            Rectangle legPin = new Rectangle(legWidth*0.5, legHeight);
            legPin.setFill(Color.rgb(140,140,140));
            legPin.setLayoutX(legWidth/2 - legPin.getWidth()/2.0);

            //nastavenie pozicie odpovedajuceho pinu pre pripojenie k soketu
            this.pins[i].setCenterX(legWidth/2.0);
            this.pins[i].setCenterY(legHeight*0.7);
            Group leg = new Group(legBase, legPin, this.pins[i]);

            if(i < this.pins.length/2){
                //spodne nozicky
                leg.setLayoutX(grid.getSizeX() * i - legWidth/2);
                leg.setLayoutY(grid.getSizeY() * gridHeight - legHeight*0.7);
            } else {
                //horne nozicky
                leg.setRotate(180);
                leg.setLayoutX(grid.getSizeX() * ( this.pins.length - i - 1 ) - legWidth/2);
                leg.setLayoutY(-legHeight+legHeight*0.7);
            }
            legs.getChildren().add(leg);
        }

        Text label = new Text(getName());
        label.setFont(Font.font(grid.getSizeY()*1.5));
        label.setFill(Color.WHITE);
        label.setLayoutY(grid.getSizeY()*2);

        graphic.getChildren().addAll(body, legs, label);

        this.getChildren().add(new Pane(graphic));

        return graphic;
    }

    /**
     * Generovanie grafiky pre itemPicker s výškou obvodu 3 jednotky.
     *
     * @param chipName Názov obvodu.
     * @param pinsCount Počet vývodov obvodu.
     * @return Panel s obrázkom.
     */
    protected static Pane generateItemImage(String chipName, int pinsCount) {
        return generateItemImage(chipName, pinsCount, 3);
    }

    /**
     * Generovanie grafiky pre itemPicker s definovanou výškou obvodu.
     *
     * @param chipName Názov obvodu.
     * @param pinsCount Počet vývodov obvodu.
     * @param chipHeight Výška obvodu.
     * @return Panel s obrázkom.
     */
    protected static Pane generateItemImage(String chipName, int pinsCount, int chipHeight) {

        Group graphic = new Group();

        double width = 10 * (pinsCount / 2);
        double height = 10 * chipHeight;

        double topBevel = 10*0.3;
        double leftBevel = 10*0.2;
        Rectangle bodyLayer0 = new Rectangle(width, height, Color.BLACK);
        Rectangle bodyLayer1 = new Rectangle(width-leftBevel, height-topBevel, Color.rgb(64,64,64));
        Rectangle bodyLayer2 = new Rectangle(width-2*leftBevel, height-2*topBevel, Color.rgb(48,48,48));
        bodyLayer2.setLayoutX(leftBevel);
        bodyLayer2.setLayoutY(topBevel);

        Arc bodyMarkTop = new Arc(0, height/2, 10*0.5, 10*0.5, 0, 90);
        bodyMarkTop.setFill(Color.rgb(28,28,28));
        bodyMarkTop.setType(ArcType.ROUND);
        Arc bodyMarkBottom = new Arc(0, height/2, 10*0.5, 10*0.5, -90, 90);
        bodyMarkBottom.setFill(Color.rgb(56,56,56));
        bodyMarkBottom.setType(ArcType.ROUND);
        Arc bodyMarkMiddle = new Arc(0, height/2, 10*0.25, 10*0.25, -90, 180);
        bodyMarkMiddle.setFill(Color.rgb(38,38,38));
        bodyMarkMiddle.setType(ArcType.ROUND);
        bodyMarkMiddle.setStrokeWidth(0);

        Group body = new Group(bodyLayer0, bodyLayer1, bodyLayer2, bodyMarkTop, bodyMarkBottom, bodyMarkMiddle);
        body.setLayoutX(-10/2);

        Group legs = new Group();
        final double legWidth = 5*2/3;
        final double legHeight = 5*0.7;
        for (int i = 0; i < pinsCount; i++){
            Polygon legBase = new Polygon(0, 0,
                    legWidth, 0,
                    legWidth, legHeight/4.0,
                    legWidth/2.0, legHeight/2.0,
                    0, legHeight/4.0);
            legBase.setFill(Color.rgb(140,140,140));
            Rectangle legPin = new Rectangle(legWidth*0.5, legHeight);
            legPin.setFill(Color.rgb(140,140,140));
            legPin.setLayoutX(legWidth/2 - legPin.getWidth()/2.0);

            Group leg = new Group(legBase, legPin);

            if(i < pinsCount/2){
                //spodne nozicky
                leg.setLayoutX(10 * i - legWidth/2);
                leg.setLayoutY(height - legHeight * 0.7);
            } else {
                //horne nozicky
                leg.setRotate(180);
                leg.setLayoutX(10 * ( pinsCount - i - 1 ) - legWidth/2);
                leg.setLayoutY(-legHeight+legHeight*0.7);
            }
            legs.getChildren().add(leg);
        }

        Text label = new Text(chipName);
        label.setFill(Color.WHITE);
        label.setFont(Font.font(9 * 1.5));
        label.setLayoutY(10*2);
        label.setLayoutX(5);

        graphic.getChildren().addAll(body, legs, label);
        graphic.setLayoutX(10/2);

        return new Pane(graphic);
    }

    /**
     * Generovanie popisu obvodu pre panel s popisom na záklde pinov a ich názvov
     *
     * @return Skupina obsahujúca obrázk s popisom.
     */
    protected Group generateDescriptionImage() {
        Group imageGroup = new Group();

        List<Pin> pins = getPins();

        double padding = 10;
        double pinHeight = 10;
        double pinWidth = 15;
        double pinMargin = 6;
        double textMargin = 5;
        double fontSize = 10;
        double height = 70;
        double width = 2 * padding + (pinWidth + 2 * pinMargin) * pins.size() / 2 - 2 * pinMargin;
        double strokeWidth = 1.5;

        Rectangle body = new Rectangle(width, height, Color.WHITE);
        body.setStroke(Color.BLACK);
        body.setStrokeWidth(strokeWidth);
        imageGroup.getChildren().add(body);

        Arc mark = new Arc(0, height / 2d, 8, 5, -90, 180);
        mark.setType(ArcType.ROUND);
        mark.setFill(Color.WHITE);
        mark.setStroke(Color.BLACK);
        mark.setStrokeWidth(strokeWidth);
        imageGroup.getChildren().add(mark);

        //piny
        for (int i = 0; i < pins.size(); i++) {
            Group pinGroup = new Group();
            Rectangle pinBody = new Rectangle(pinWidth, pinHeight, Color.WHITE);
            pinBody.setStroke(Color.BLACK);
            pinBody.setStrokeWidth(strokeWidth);

            Text pinName = new Text(pins.get(i).getName());
            pinName.setFont(Font.font(fontSize));
            pinName.setLayoutX(pinWidth / 2.0 - pinName.getBoundsInParent().getWidth() / 2.0); //centrovanie textu pod pin

            Text pinNumber = new Text(Integer.toString(i + 1));
            pinNumber.setFont(Font.font(fontSize));
            pinNumber.setLayoutX(pinWidth / 2.0 - pinNumber.getBoundsInParent().getWidth() / 2.0); //centrovanie cisla pod pin

            pinGroup.getChildren().addAll(pinBody, pinName, pinNumber);

            if (i < pins.size() / 2) {
                //piny dole
                pinNumber.setLayoutY(-pinNumber.getBoundsInParent().getHeight() + pinNumber.getBaselineOffset() - textMargin);
                pinName.setLayoutY(pinHeight + pinName.getBoundsInParent().getHeight());

                //posunutie celej grupy
                pinGroup.setLayoutX(padding + i * (pinWidth + 2 * pinMargin));
                pinGroup.setLayoutY(height);
            } else {
                //piny hore
                pinNumber.setLayoutY(pinHeight + pinName.getBoundsInParent().getHeight());
                pinName.setLayoutY(-pinNumber.getBoundsInParent().getHeight() + pinName.getBaselineOffset() - textMargin);

                pinGroup.setLayoutX(padding + (pins.size() - i - 1) * (pinWidth + 2 * pinMargin));
                pinGroup.setLayoutY(-pinHeight);
            }

            imageGroup.getChildren().add(pinGroup);
        }

        return imageGroup;
    }


    /**
     * Generovanie popisu obvodu pre okno s informáciami o obvode v reálnom čase.
     *
     * @return Skupina obsahujúca obrázk s popisom.
     */
    protected Group generateInspectionImage() {
        Group imageGroup = new Group();

        List<Pin> pins = getPins();

        double padding = 10;
        double pinHeight = 10;
        double pinWidth = 15;
        double pinMargin = 8;
        double textMargin = 5;
        double fontSize = 10;
        double height = 70;
        double width = 2 * padding + (pinWidth + 2 * pinMargin) * pins.size() / 2 - 2 * pinMargin;
        double strokeWidth = 1.5;

        Rectangle body = new Rectangle(width, height, Color.WHITE);
        body.setStroke(Color.BLACK);
        body.setStrokeWidth(strokeWidth);
        imageGroup.getChildren().add(body);

        Arc mark = new Arc(0, height / 2d, 8, 5, -90, 180);
        mark.setType(ArcType.ROUND);
        mark.setFill(Color.WHITE);
        mark.setStroke(Color.BLACK);
        mark.setStrokeWidth(strokeWidth);
        imageGroup.getChildren().add(mark);

        //piny
        for (int i = 0; i < pins.size(); i++) {
            Group pinGroup = new Group();
            InspectionPin pinBody = new InspectionPin(pinWidth, pinHeight, Color.WHITE, pins.get(i));
            ipu.add(pinBody);
            pinBody.setStroke(Color.BLACK);
            pinBody.setStrokeWidth(strokeWidth);

            Text pinName = new Text(pins.get(i).getName());
            pinName.setFont(Font.font(fontSize));
            pinName.setLayoutX(pinWidth / 2.0 - pinName.getBoundsInParent().getWidth() / 2.0); //centrovanie textu pod pin

            Text pinNumber = new Text(Integer.toString(i + 1));
            pinNumber.setFont(Font.font(fontSize));
            pinNumber.setLayoutX(pinWidth / 2.0 - pinNumber.getBoundsInParent().getWidth() / 2.0); //centrovanie cisla pod pin

            pinGroup.getChildren().addAll(pinBody, pinName, pinNumber);

            if (i < pins.size() / 2) {
                //piny dole
                pinNumber.setLayoutY(-pinNumber.getBoundsInParent().getHeight() + pinNumber.getBaselineOffset() - textMargin);
                pinName.setLayoutY(pinHeight + pinName.getBoundsInParent().getHeight());

                //posunutie celej grupy
                pinGroup.setLayoutX(padding + i * (pinWidth + 2 * pinMargin));
                pinGroup.setLayoutY(height);
            } else {
                //piny hore
                pinNumber.setLayoutY(pinHeight + pinName.getBoundsInParent().getHeight());
                pinName.setLayoutY(-pinNumber.getBoundsInParent().getHeight() + pinName.getBaselineOffset() - textMargin);

                pinGroup.setLayoutX(padding + (pins.size() - i - 1) * (pinWidth + 2 * pinMargin));
                pinGroup.setLayoutY(-pinHeight);
            }

            imageGroup.getChildren().add(pinGroup);
        }

        return imageGroup;
    }

    /**
     * Trieda pre pin zobrazeny v inspektorovi IC.
     * Meni farbu na zaklade stavu pinu, podla ktoreho bola instancia vytvorena.
     */
    private static class InspectionPin extends Rectangle {
        private Pin pin;

        public InspectionPin(double width, double height, Paint fill, Pin pin) {
            super(width, height, fill);
            this.pin = pin;
        }

        public Pin.PinState getState() {
            if (this.pin.getSocket() == null) return Pin.PinState.NOT_CONNECTED;
            if (this.pin.getState() == Pin.PinState.HIGH_IMPEDANCE) return Pin.PinState.HIGH_IMPEDANCE;

            //hodnota podla soketu, hodnota pinu sa neupdateuje vzdy
            switch (this.pin.getSocket().getPotential().getValue()) {
                case HIGH:
                    return Pin.PinState.HIGH;
                case LOW:
                    return Pin.PinState.LOW;
                default:
                    return Pin.PinState.NOT_CONNECTED;
            }
        }

        public Pin getPin() {
            return this.pin;
        }
    }

    private static class InspectionPinsUpdater {
        private static final Color HIGH_COLOR = Color.RED;
        private static final Color LOW_COLOR = Color.BLUE;
        private static final Color HiZ_COLOR = Color.ORANGE;
        private static final Color NC_COLOR = Color.GRAY;

        private LinkedList<InspectionPin> pins = new LinkedList<>();
        //"skrtiace" vlakno RunLater poziadaviek
        private static Thread thread;
        //pocitadlo pre pristup k RunLater
        private AtomicLong counter = new AtomicLong(-1);

        public void add(InspectionPin pin) {
            if (!this.pins.contains(pin))
                this.pins.add(pin);

            if (this.pins.size() == 1) this.startUpdating();
        }

        public void remove(InspectionPin pin) {
            this.pins.remove(pin);
            if (this.pins.size() == 0) this.stopUpdating();
        }

        public void remove(Pin pin) {
            Iterator<InspectionPin> i = this.pins.iterator();
            while (i.hasNext()) {
                if (i.next().getPin() == pin) {
                    i.remove();
                    break;
                }
            }

            if (this.pins.size() == 0) this.stopUpdating();
        }

        private void startUpdating() {
            counter.set(-1);

            thread = new Thread(() -> {
                Board board = this.pins.get(0).getPin().getDevice().getBoard();
                while (!Thread.currentThread().isInterrupted()) {
                    if (board.isSimulationRunning()
                            && counter.getAndSet(1) == -1) {
                        updateUI(counter);
                    }
                }

                updateUI(counter); //posledny update -> vypnutie
            });

            thread.setName("InspectorWindowUpdater");
            thread.setDaemon(true);
            thread.start();
        }

        private void stopUpdating() {
            if (thread != null) thread.interrupt();
        }

        private void updateUI(final AtomicLong counter) {

            Platform.runLater(() -> {
                //pre kazdy emitter na ploche
                for (InspectionPin pin :
                        pins) {

                    switch (pin.getState()) {
                        case HIGH:
                            pin.setFill(HIGH_COLOR);
                            break;
                        case LOW:
                            pin.setFill(LOW_COLOR);
                            break;
                        case HIGH_IMPEDANCE:
                            pin.setFill(HiZ_COLOR);
                            break;
                        default:
                            pin.setFill(NC_COLOR);
                    }
                }

                counter.set(-1);
            });

        }
    }
}