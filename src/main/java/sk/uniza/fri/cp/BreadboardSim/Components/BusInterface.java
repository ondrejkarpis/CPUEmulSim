package sk.uniza.fri.cp.BreadboardSim.Components;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.BoardEvent;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputOutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.InputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.OutputPin;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.LightEmitter;
import sk.uniza.fri.cp.BreadboardSim.SchoolBreadboard;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;
import sk.uniza.fri.cp.BreadboardSim.Socket.SocketsFactory;
import sk.uniza.fri.cp.Bus.Bus;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Komponent pre pripojenie na zbernice - adresnú, dátovú, kontrolnú.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class BusInterface extends Component {
    private static final Color ADDRESS_LED_ON = Color.RED;
    private static final Color ADDRESS_LED_OFF = Color.DARKRED;
    private static final Color DATA_LED_ON = Color.LIME;
    private static final Color DATA_LED_OFF = Color.rgb(54, 122, 84); // Color.SEAGREEN
    private static final Color CONTROL_LED_ON = Color.YELLOW;
    private static final Color CONTROL_LED_OFF = Color.rgb(100, 100, 0); // Color.OLIVE;

    private AddressBusCommunicator[] addressBusCommunicators = new AddressBusCommunicator[16];
    private DataBusCommunicator[] dataBusCommunicators = new DataBusCommunicator[8];
    private ControlBusCommunicator[] controlBusCommunicators = new ControlBusCommunicator[9];

    private Socket[] addressSockets;
    private Socket[] dataSockets;
    private Socket[] controlSockets;

    private Bus bus;

    //zapnutie / vypnutie svetielok pri zmene stavu simulacie
    private final ChangeListener<Boolean> onSimulationStateChange = new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if (newValue.booleanValue()) {
                //spustenie az po zapnuti PowerSoketov, inak sa skratuju hned po spusteni
                Thread runLater = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bus.getBus().waitForSteadyState();

                        AddressBusCommunicator.setAddress(bus.getAddressBus());
                        DataBusCommunicator.setData(bus.getDataBus());
                        ControlBusCommunicator.setControls(bus.getControlBus());

                        for (int i = 0; i < 16; i++)
                            addressBusCommunicators[i].update();

                        for (int i = 4; i < 9; i++)  //iba pre vystupne signaly
                            controlBusCommunicators[i].update();

                        for (int i = 0; i < 8; i++)
                            dataBusCommunicators[i].update();
                    }
                });

                runLater.setDaemon(true);
                runLater.start();
            } else {
                for (int i = 0; i < 16; i++)
                    addressBusCommunicators[i].reset();

                for (int i = 4; i < 9; i++)  //iba pre vystupne signaly
                    controlBusCommunicators[i].reset();

                for (int i = 0; i < 8; i++)
                    dataBusCommunicators[i].reset();
            }
        }
    };
    //rekcie na zmeny casti zbernice
    private final ChangeListener<Number> onAddressBusChange = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            AddressBusCommunicator.setAddress(newValue.intValue());
            for (int i = 0; i < 16; i++) {
                addressBusCommunicators[i].update();
            }
        }
    };
    private final ChangeListener<Number> onDataBusChange = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            //nastavenie aktualnych dat na datovej zbernici pre vsetky instancie komunikatora (ledky)

            if ((bus.getControlBus() & 0xB0) == 0xB0) {
                //ak nie je zapnute citanie -> ak je, tak sa riadime iba podla simulacie,
                //nie podla dat ulozenych na zbernici
                DataBusCommunicator.setData(newValue.intValue());

                for (int i = 0; i < 8; i++) {
                    dataBusCommunicators[i].update();
                }
            }

        }
    };

    private volatile boolean lastReadStateOn = false;
    private volatile boolean lastWriteStateOn = false;
    private final ChangeListener<Number> onControlBusChange = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            ControlBusCommunicator.setControls(newValue.intValue());

            //0x140 -> 1 0100 0000 => signal zapisu MW/IW je v nule
            if ((newValue.intValue() & 0x140) != 0x140 && !lastWriteStateOn) {
                DataBusCommunicator.setToWrite(true);

                Bus.getBus().waitForSteadyState(); // koli skratu

                //zapis
                for (int i = 0; i < 8; i++)
                    dataBusCommunicators[i].update();

                lastWriteStateOn = true;
            }

            //MR/IR/IA
            if ((newValue.intValue() & 0xB0) != 0xB0 && !lastReadStateOn) {
                //citanie
                Bus.getBus().waitForSteadyState(); // koli skratu

                for (int i = 0; i < 8; i++)
                    ((DataBusCommunicator) dataBusCommunicators[i]).setToRead(true);

                lastReadStateOn = true;
            } else if (lastReadStateOn) {
                for (int i = 0; i < 8; i++)
                    ((DataBusCommunicator) dataBusCommunicators[i]).setToRead(false);

                lastReadStateOn = false;
            }

            for (int i = 4; i < 9; i++) { //iba pre vystupne signaly
                controlBusCommunicators[i].update();
            }

            if ((newValue.intValue() & 0x140) == 0x140 && lastWriteStateOn) {
                Bus.getBus().waitForSteadyState(); // koli skratu

                DataBusCommunicator.setToWrite(false);
                for (int i = 0; i < 8; i++) {
                    dataBusCommunicators[i].update();
                    //dataBusCommunicators[i].update();
                }
                getBoard().addEvent(new BoardEvent(null));
                lastWriteStateOn = false;
            }

        }
    };

    /**
     * Vytvorenie pripojenia na zbernicu.
     *
     * @param board Plocha simulatora, na ktoru bude objekt pridany.
     */
    public BusInterface(Board board) {
        super(board);
        this.bus = Bus.getBus();

        //grafika
        GridSystem grid = getBoard().getGrid();

        this.gridWidth = grid.getSizeX() * 66; //68
        this.gridHeight = grid.getSizeY() * 5;
        Rectangle background = new Rectangle(this.gridWidth, this.gridHeight, SchoolBreadboard.BACKGROUND_COLOR);

        //GND
        Group leftGndSockets = SocketsFactory.getHorizontalPower(this, 2, Potential.Value.LOW, getPowerSockets());
        Text leftGndText = Board.getLabelText("GND", grid.getSizeMin());
        leftGndText.setLayoutX(-grid.getSizeX() / 2.0);
        leftGndText.setLayoutY(-grid.getSizeY());
        leftGndSockets.getChildren().add(leftGndText);
        leftGndSockets.setLayoutX(grid.getSizeX() * 8);
        leftGndSockets.setLayoutY(grid.getSizeY() * 4);

        Group rightGndSockets = SocketsFactory.getHorizontalPower(this, 2, Potential.Value.LOW, getPowerSockets());
        Text rightGndText = Board.getLabelText("GND", grid.getSizeMin());
        rightGndText.setLayoutX(-grid.getSizeX() / 2.0);
        rightGndText.setLayoutY(-grid.getSizeY());
        rightGndSockets.getChildren().add(rightGndText);
        rightGndSockets.setLayoutX(grid.getSizeX() * 57);
        rightGndSockets.setLayoutY(grid.getSizeY() * 4);

        this.getChildren().addAll(background,    leftGndSockets, rightGndSockets);
        generateInterface();

        //registracia vsetkych soketov
        this.addAllSockets(getPowerSockets());
        //zvysna sa pridavaju pocas vytvarania v getnerateInterface

        registerListeners();
    }

    @Override
    public void delete() {
        if (addressBusCommunicators[0].getPin() == null) {
            //ak sa nejedna o prvu dosku, ta nemoze byt zmazana
            super.delete();

            unregisterListeners();

            for (AddressBusCommunicator addressBusCommunicator : addressBusCommunicators)
                addressBusCommunicator.delete();

            for (DataBusCommunicator dataBusCommunicator : dataBusCommunicators)
                dataBusCommunicator.delete();

            for (ControlBusCommunicator controlBusCommunicator : controlBusCommunicators)
                controlBusCommunicator.delete();
        }

    }

    private void registerListeners() {
        if (addressBusCommunicators[0].getPin() != null) {
            //iba hlavny pocuva na zmeny
            this.getBoard().simRunningProperty().addListener(onSimulationStateChange);
            this.bus.addressBusProperty().addListener(onAddressBusChange);
            this.bus.dataBusProperty().addListener(onDataBusChange);
            this.bus.controlBusProperty().addListener(onControlBusChange);

            //prvotna inicializacia
            this.bus.setRandomAddress();
            this.bus.setRandomData();
        } else BusCommunicator.MAIN_COMMUNICATORS.forEach(BusCommunicator::update);
    }

    private void unregisterListeners() {
        this.getBoard().simRunningProperty().removeListener(onSimulationStateChange);
        this.bus.addressBusProperty().removeListener(onAddressBusChange);
        this.bus.dataBusProperty().removeListener(onDataBusChange);
        this.bus.controlBusProperty().removeListener(onControlBusChange);
    }

    /**
     * Generovanie grafiky komponentu
     */
    private void generateInterface(){
        GridSystem grid = getBoard().getGrid();
        Text name;

        //ADRESNA ZBERNICA
        addressSockets = new Socket[16];

        Group addressInterface = new Group();
        addressInterface.setLayoutX(grid.getSizeX() * 15);
        addressInterface.setLayoutY(grid.getSizeY() * 4);

        //generovanie soketov, komunikatorov a oznaceni soketov
        for (int i = 0; i < 16; i++) {
            double centerX = grid.getSizeX() * (16-i);

            //soket
            Socket socket = new Socket(this);
            socket.setLayoutX(centerX);
            addressSockets[i] = socket;

            //komunikator so zbernicou
            AddressBusCommunicator communicator = new AddressBusCommunicator(getBoard(), addressSockets[i], i);
            communicator.setLayoutX(centerX);
            communicator.setLayoutY(-grid.getSizeY());
            addressBusCommunicators[i] = communicator;

            //oznacenie
            Text label = Board.getLabelText(String.valueOf(i), grid.getSizeMin());
            label.setLayoutX(centerX - label.getBoundsInLocal().getWidth()/2.0);
            label.setLayoutY(-grid.getSizeY()*2);

            addressInterface.getChildren().addAll(socket, communicator, label);
        }

        //ozancenie
        name = Board.getLabelText("AB", grid.getSizeMin());
        name.setLayoutX(-grid.getSizeX() * 1.1);
        addressInterface.getChildren().add(name);


        //DATOVA ZBERNICA
        dataSockets = new Socket[8];
        Group dataInterface = new Group();
        dataInterface.setLayoutX(grid.getSizeX() * 36);
        dataInterface.setLayoutY(grid.getSizeY() * 4);

        //generovanie soketov, komunikatorov a oznaceni soketov
        for (int i = 0; i < 8; i++) {
            double centerX = grid.getSizeX() * (8-i);

            //soket
            Socket socket = new Socket(this);
            socket.setLayoutX(centerX);
            dataSockets[i] = socket;

            //komunikator so zbernicou
            DataBusCommunicator communicator = new DataBusCommunicator(getBoard(), dataSockets[i], i);
            communicator.setLayoutX(centerX);
            communicator.setLayoutY(-grid.getSizeY());
            dataBusCommunicators[i] = communicator;

            //oznacenie
            Text label = Board.getLabelText(String.valueOf(i), grid.getSizeMin());
            label.setLayoutX(centerX - label.getBoundsInLocal().getWidth()/2.0);
            label.setLayoutY(-grid.getSizeY()*2);

            dataInterface.getChildren().addAll(socket, communicator, label);
        }

        //ozancenie
        name = Board.getLabelText("DB", grid.getSizeMin());
        name.setLayoutX(-grid.getSizeX() * 1.1);
        dataInterface.getChildren().add(name);

        //RIADIACA ZBERNICA
        controlSockets = new Socket[9];
        Group controlInterface = new Group();
        controlInterface.setLayoutX(grid.getSizeX() * 45);
        controlInterface.setLayoutY(grid.getSizeY() * 4);

        //generovanie soketov, komunikatorov a oznaceni soketov
        for (int i = 0; i < 9; i++) {
            double centerX = grid.getSizeX() * (9-i);

            //soket
            Socket socket = new Socket(this);
            socket.setLayoutX(centerX);
            controlSockets[i] = socket;

            //komunikator so zbernicou
            ControlBusCommunicator communicator = new ControlBusCommunicator(getBoard(), controlSockets[i], i);
            communicator.setLayoutX(centerX);
            communicator.setLayoutY(-grid.getSizeY());
            controlBusCommunicators[i] = communicator;

            //oznacenie
            String lb = "";
            switch (i){
                case 0:	lb = "BA";
                    break;
                case 1:	lb = "BQ";
                    break;
                case 2:	lb = "RY";
                    break;
                case 3:	lb = "IT";
                    break;
                case 4:	lb = "IA";
                    break;
                case 5:	lb = "IR";
                    break;
                case 6:	lb = "IW";
                    break;
                case 7:	lb = "MR";
                    break;
                case 8:	lb = "MW";
                    break;
            }
            Text label = Board.getLabelText(lb, grid.getSizeMin());
            label.setLayoutX(centerX - label.getBoundsInLocal().getWidth() / 2.0);
            label.setLayoutY(-2*grid.getSizeY());

            if(i % 2 != 0) label.setLayoutY(-3*grid.getSizeY());	//posunutie oznacenia hore

            //negovacia ciarka nad oznacenim
            if(i >= 4) {
                Line negLogic = new Line(label.getLayoutX(),
                        label.getLayoutY() - grid.getSizeX() + 2,
                        label.getLayoutX() + label.getBoundsInLocal().getWidth(),
                        label.getLayoutY() - grid.getSizeX() + 2);
                negLogic.setId("breadboardLabel");
                controlInterface.getChildren().add(negLogic);
            }

            controlInterface.getChildren().addAll(socket, communicator, label);
        }

        this.getChildren().addAll(addressInterface, dataInterface, controlInterface);
        //registracia soketov
        this.addAllSockets(addressSockets);
        this.addAllSockets(dataSockets);
        this.addAllSockets(controlSockets);
    }


    /**
     * Zariadenie komunikujuce so zbernicou.
     *
     * vysvetlenie prepojenia
     * [COMMON_PIN} -> [COMMON_SOCKET] ~ [1. InterfaceSocket]
     *                                 ~ [2. InterfaceSocket] ...
     * -> pripojenie pinu k soketu
     * ~  potenciál
     */
    private abstract static class BusCommunicator extends Device{

        private static final double RADIUS_COEF = 0.4;

        //vsetky hlavne komunikatory, ktore komunikuju so zbernicou a riadia vystup
        private static final ArrayList<BusCommunicator> MAIN_COMMUNICATORS = new ArrayList<>(33);

        //spolocne sokety kazdeho interface pre kazdy komunikator zvlast
        private static final Pin[] COMMON_PINS_ADDRESS = new Pin[16];
        private static final Pin[] COMMON_PINS_DATA = new Pin[8];
        private static final Pin[] COMMON_PINS_CONTROL = new Pin[9];

        private static final Socket[] COMMON_SOCKETS_ADDRESS = new Socket[16];
        private static final Socket[] COMMON_SOCKETS_DATA = new Socket[8];
        private static final Socket[] COMMON_SOCKETS_CONTROL = new Socket[9];

        private static final List<List<LightEmitter>> ADDRESS_LIGHT_EMITTERS =
                Stream.generate(LinkedList<LightEmitter>::new).limit(16).collect(Collectors.toList());
        private static final List<List<LightEmitter>> DATA_LIGHT_EMITTERS =
                Stream.generate(LinkedList<LightEmitter>::new).limit(8).collect(Collectors.toList());
        private static final List<List<LightEmitter>> CONTROL_LIGHT_EMITTERS =
                Stream.generate(LinkedList<LightEmitter>::new).limit(9).collect(Collectors.toList());

        //kazdy komunikator ma vlastne
        private final Circle indicator;
        private final LightEmitter lightEmitter;
        private final Color onColor;
        private final Color offColor;

        private final Potential connectingPotential;
        private final Pin pin; //iba jeden komunikator ma pin, ten hlavny, ten prvy, ktory vsetko riadi
        private final List<LightEmitter> byteEmittersList; //ma ho iba hlavny
        private final int byteNr; //cislo bitu na zbernici od najnizsieho radu

        public BusCommunicator(Board board, Socket interfaceSocket, int byteNr, Color onColor, Color offColor) {
            super(board);

            this.byteNr = byteNr;
            this.onColor = onColor;
            this.offColor = offColor;

            GridSystem grid = board.getGrid();
            this.indicator = new Circle(grid.getSizeMin() * RADIUS_COEF, offColor);
            this.lightEmitter = new LightEmitter(board, this.indicator, this.onColor, this.offColor);

            //vyber spravych strukturo podla vytvaranej instancie
            Pin[] commonPins = null;
            Socket[] commonSockets = null;
            Pin pin = null;
            List<LightEmitter> thisByteEmittersList = null;

            if (this instanceof AddressBusCommunicator) {
                commonPins = COMMON_PINS_ADDRESS;
                commonSockets = COMMON_SOCKETS_ADDRESS;
                pin = new OutputPin(this, "Address Output Pin " + byteNr);

                thisByteEmittersList = ADDRESS_LIGHT_EMITTERS.get(byteNr);
            } else if (this instanceof DataBusCommunicator) {
                commonPins = COMMON_PINS_DATA;
                commonSockets = COMMON_SOCKETS_DATA;
                pin = new InputOutputPin(this, "Data IO Pin " + byteNr);

                thisByteEmittersList = DATA_LIGHT_EMITTERS.get(byteNr);
            } else if (this instanceof ControlBusCommunicator) {
                commonPins = COMMON_PINS_CONTROL;
                commonSockets = COMMON_SOCKETS_CONTROL;
                if (this.getByteNr() == 2 || this.getByteNr() == 3)
                    pin = new InputPin(this, "Control Input Pin " + byteNr);
                else
                    pin = new OutputPin(this, "Control Output Pin " + byteNr);

                thisByteEmittersList = CONTROL_LIGHT_EMITTERS.get(byteNr);
            }

            thisByteEmittersList.add(this.lightEmitter);

            //inicializacia spolocneho pinu a soketu (ak este neprebehla)
            if (commonPins[byteNr] == null) {
                //inicializacia jedineho pinu komunikatora
                commonPins[byteNr] = pin;

                //inicializacia jedineho spolocneho soketu ku ktoremu sa pripajaju vsetky sokety rozhani
                commonSockets[byteNr] = new Socket(interfaceSocket.getComponent());

                //pripojenie pinu k soketu
                commonSockets[byteNr].connect(commonPins[byteNr]);

                //priradenie vytvoreneho pinu jedinemu komunikatoru
                this.pin = commonPins[byteNr];
                this.byteEmittersList = thisByteEmittersList;

                MAIN_COMMUNICATORS.add(this);
            } else {
                //komunikator je iba podriadeny, zobrazuje informacie z hlavneho komunikatora, nema svoj pin
                this.pin = null;
                this.byteEmittersList = null;
            }

            //potencial spajajuci soket na interface (na doske) s vnutornym spolocnym soketom
            this.connectingPotential = new Potential(commonSockets[byteNr], interfaceSocket);

            this.getChildren().addAll(indicator);
            this.makeImmovable();
        }

        /**
         * Aktualizácia hodnoty komunikátora.
         */
        abstract public void update();

        protected final int getByteNr() {
            return this.byteNr;
        }

        protected final Pin getPin() {
            return this.pin;
        }

        @Override
        public boolean isHigh(Pin inputPin) {
            if (super.isHigh(inputPin)) {
                this.turnIndicatorOn();
                return true;
            } else {
                return false;
            }
        }

        protected final void setHigh() {
            this.setPinForce(this.pin, Pin.PinState.HIGH);
            turnIndicatorOn();
        }

        @Override
        public boolean isLow(Pin inputPin) {
            if (super.isLow(inputPin)) {
                this.turnIndicatorOff();
                return true;
            } else {
                return false;
            }
        }

        protected final void setLow() {
            this.setPinForce(this.pin, Pin.PinState.LOW);
            turnIndicatorOff();
        }

        protected final void setHighImpedance(boolean turnIndicatorOn) {
            this.setPinForce(this.pin, Pin.PinState.HIGH_IMPEDANCE);
            if (turnIndicatorOn)
                turnIndicatorOn();
            else
                turnIndicatorOff();
        }


        protected final void turnIndicatorOn() {
            if (this.byteEmittersList != null) {
                this.byteEmittersList.forEach(emitter -> emitter.turnOn());
            }
        }

        protected final void turnIndicatorOff() {
            if (this.byteEmittersList != null) {
                this.byteEmittersList.forEach(emitter -> emitter.turnOff());
            }
        }

        @Override
        public List<Pin> getPins() {
            return null;
        }

        @Override
        public void delete() {
            super.delete();
            this.lightEmitter.delete();
            this.connectingPotential.delete();
        }

        @Override
        public void reset() {
            this.pin.getSocket().setPotential(Potential.Value.NC);
            turnIndicatorOff();
        }
    }

	private static class AddressBusCommunicator extends BusCommunicator{

        private volatile static int address;

        /**
         * Nastavenie akutálnej adresy na zbernici
         *
         * @param newAddress Adresa na zbernici
         */
        public static void setAddress(int newAddress) {
            address = newAddress;
        }

        public AddressBusCommunicator(Board board, Socket interfaceSocket, int byteNr) {
            super(board, interfaceSocket, byteNr, ADDRESS_LED_ON, ADDRESS_LED_OFF);
        }

        @Override
        public void update() {
            if ((address & 1 << this.getByteNr()) != 0) {
                this.setHigh();
            } else {
                this.setLow();
            }
        }

        @Override
        public void simulate() {
        } //output, neprebieha simulacia
    }

	private static class DataBusCommunicator extends BusCommunicator{

        private volatile static int data; //aktualne data na zbernici
        private volatile static boolean read; //nastavenie ako vstupu
        private volatile static boolean write; //nastavenie ako vystupu

        public static void setData(int newData) {
            data = newData;
        }

        public void setToRead(boolean newRead) {
            read = newRead;
            Bus.getBus().dataIsChanging();
            if (read)
                this.setPinForce(this.getPin(), Pin.PinState.HIGH_IMPEDANCE);

            this.update();
        }

        public static void setToWrite(boolean newWrite) {
            write = newWrite;
        }

        public DataBusCommunicator(Board board, Socket interfaceSocket, int byteNr) {
            super(board, interfaceSocket, byteNr, DATA_LED_ON, DATA_LED_OFF);
            this.read = false;
            this.write = false;
        }

		/**
		 * Aktualizacia po zmene dat na zbernici
         */
		public void update(){
            if (write) {
                Bus.getBus().dataIsChanging();
                //zapis zo zbernice do externej pamate na zaklade
                if ((data & 1 << this.getByteNr()) != 0) {
                    this.setHigh();
                } else {
                    this.setLow();
                }

            } else if (read) {
                Bus.getBus().dataIsChanging();
                //citanie z externeho zariadenia a zapis na zbernicu
                if (this.isHigh(this.getPin())) {
                    //ak je na pine jednotka
                    Bus.getBus().setDataBus((byte) (data |= (1 << this.getByteNr())));
                } else if (this.isLow(this.getPin())) {
                    //ak nie tak sa predpoklada ze je tam nula (aj ked je nepripojeny, lebo CPU je odpojene)
                    Bus.getBus().setDataBus((byte) (data &= ~(1 << this.getByteNr())));
                }

            } else {
                //ANI ZAPIS ANI CITANIE -> TO DIVNE SPRAVANIE Z POVODNEHO SIMULATORA

                //zachovanie povodnej hodnoty
                Potential.Value oldValue = this.getPin().getSocket().getThisPotential().getValue();

                //odpojenie aby neovplyvnoval vyslednu hodnotu na potencialy pri zistovani kto ho riadi (potencial)
                if (this.getBoard().isSimulationRunning())
                    this.getPin().getSocket().setPotential(Potential.Value.NC);

                //zistenie, ci je potencial, na ktory je pripojeny riadeny nim (zbernicou) alebo nie
                if (this.getPin().getSocket().getPotential().getValue() != Potential.Value.NC) {
                    //VYSLEDNY POTENCIAL RIADI NIEKTO INY (lebo nie je not connected -> je tam nejaky vystup)

                    //ak hodnota potencialu nie je riadena zo zbernice, je riadena inym vystupom, ktory sa sem pripaja
                    //a teda podla neho iba nastavim ledku kontrolou ci je HIGH alebo LOW
                    if (!this.isHigh(this.getPin()))
                        this.isLow(this.getPin());
                } else {
                    //VYSLEDNY POTENCIAL JE RIADENY ZO ZBERNICE

                    //vratenie povodneho potencialu na sokete zbernice
                    this.getPin().getSocket().setPotential(oldValue);

                    //ak ja riadim hodnotu potencialu, tak ju riadim na zaklade dat zo zbernice
                    if ((data & 1 << this.getByteNr()) != 0) {
                        //mal by vysielat output HIGH
                        this.turnIndicatorOn();

                        //ak sa nova hodnota lisi od starej, vytvorim event pre simulaciu, inak sa zacyklim ako nikto
                        if (oldValue != Potential.Value.HIGH) {
                            this.setHigh();
                        }
                    } else {
                        this.turnIndicatorOff();

                        if (oldValue != Potential.Value.LOW) {
                            this.setLow();
                        }
                    }
                }
            }

		}

		@Override
		public void simulate() {
            update();
		}

    }

	private static class ControlBusCommunicator extends BusCommunicator{

        private volatile static int controls; //priznaky na zbernici

        public static void setControls(int newControls) {
            controls = newControls;
        }

        public ControlBusCommunicator(Board board, Socket interfaceSocket, int byteNr) {
            super(board, interfaceSocket, byteNr, CONTROL_LED_ON, CONTROL_LED_OFF);
        }

        @Override
        public void update() {
            Bus.getBus().dataIsChanging();
            if ((controls & 1 << this.getByteNr()) != 0) {
                this.setHigh();
            } else {
                this.setLow();
            }
        }

		@Override
		public void simulate() {
			//simulujeme zapis na zbernicu ak sa jedna o vstupny signal
            switch (this.getByteNr()) {
                case 2:
                    if (this.isHigh(this.getPin()))//kontrola isHigh zapina aj indikator
                        Bus.getBus().setRY(true);
                    else {
                        Bus.getBus().setRY(true);
                        this.turnIndicatorOff();
                    }
                    break;
                case 3:
                    if (this.isHigh(this.getPin())) {
                        Bus.getBus().setIT(true);
                    } else {
                        Bus.getBus().setIT(false);
                        this.turnIndicatorOff();
                    }
                    break;
            }
        }
	}
}