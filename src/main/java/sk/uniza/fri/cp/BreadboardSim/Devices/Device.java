package sk.uniza.fri.cp.BreadboardSim.Devices;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.BoardChangeEvent;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;
import sk.uniza.fri.cp.BreadboardSim.Devices.Chips.Chip;
import sk.uniza.fri.cp.BreadboardSim.Devices.Pin.Pin;
import sk.uniza.fri.cp.BreadboardSim.Item;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;
import sk.uniza.fri.cp.BreadboardSim.Socket.SocketType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Abstraktná trieda zariadenia.
 * Zariadenie sa pripája na sokety komponentov. Počas simulácie sa pri zmene hodnoty potenciálu
 * na vstupnom pine volá metóda simulácie funkčnosti zariadenia.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017 16:16:34
 */
public abstract class Device extends Item {

    private Component component;
    private ArrayList<Socket> socketsToConnectTo; //sokety ku ktorym sa moze pripojit

    private int lastGridPosX;
    private int lastGridPosY;

    private boolean moved = false;

    private Random rand = new Random();

    //hladanie soketov na pripojenie pri pohybe so zariadenim
    private EventHandler<MouseEvent> onMouseDraggedEventHandler = event -> {
        //ak sa zmenila pozicia zariadenia na ploche
        if (getGridPosX() != lastGridPosX || getGridPosY() != lastGridPosY) {
            moved = true;
            if (isConnected()) disconnectAllPins();

            //hladaj nove sokety na ktore by sa mohlo pripojit
            unhighlightConnectibleSockets();
            searchForSockets();
            highlightConnectibleSockets();

            lastGridPosX = getGridPosX();
            lastGridPosY = getGridPosY();
        }
    };
    //pripojenie zariadenia po pousteni na plochu
    private EventHandler<MouseEvent> onMouseReleasedEventHandler = event -> {
        //po umiestneni zariadenia na ploche sa pripoj k moznym soketom
        if (moved) {
            unhighlightConnectibleSockets();
            if (isConnected()) disconnectAllPins();
            tryToConnectToFoundSockets();
            moved = false;
        }
    };

    /**
     * Bezparametrický konštruktor pre itemPicker.
     */
    public Device(){}

    /**
     * Konštruktor pre objekt pridávaný na plochu simulátora.
     *
     * @param board Plocha simulátora.
     */
    public Device(Board board){
        super(board);

        socketsToConnectTo = new ArrayList<>();

        this.addEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
        this.addEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleasedEventHandler);

    }

    /**
     * Simulačná metóda zariadenia. Odzrkadľuje jeho funkciu.
     */
    public abstract void simulate();

    /**
     * Resetovanie stavu zariadenia.
     */
    public abstract void reset();

    /**
     * Vráti piny určené na pripojenie do soketov.
     *
     * @return List pinov zariadenia
     */
    public abstract List<Pin> getPins();

    @Override
    public void makeImmovable() {
        super.makeImmovable();

        if(getBoard() != null) {
            this.removeEventHandler(MouseEvent.MOUSE_DRAGGED, onMouseDraggedEventHandler);
            this.removeEventHandler(MouseEvent.MOUSE_RELEASED, onMouseReleasedEventHandler);
        }
    }

    /**
     * Kontrola, či je na pine zariadenia hodnota iná ako NOT_CONNECTED.
     *
     * @param pin Pin zariadenia.
     * @return True, ak je pin pripojený k potenciálu s hodnotou HIGH/LOW.
     */
	public boolean isConnected(Pin pin){
        if (pin != null && pin.getSocket() != null) {
            return pin.getSocket().getPotential().getValue() != Potential.Value.NC;
        } else {
            return false;
        }
    }

    /**
     * Kontrola a aktualizácia aktuálnej hodnoty na pine podľa potenciálu socketu ku ktorému je pripojený.
     *
     * @param inputPin Vstupný pin, ktorého stavová hodnota sa má aktualizovať a vratiť
     * @return True ak je hodnota HIGH, false inak.
     */
    public boolean isHigh(Pin inputPin){
        if (inputPin == null) return false;
        if(!inputPin.isConnected()) return false;

        Potential.Value value = inputPin.getSocket().getPotential().getValue();

        if(value == Potential.Value.HIGH){
            inputPin.setState(Pin.PinState.HIGH);
            return true;
        } else if (value == Potential.Value.LOW){
            inputPin.setState(Pin.PinState.LOW);
        } else {
            SocketType stype = inputPin.getSocket().getPotential().getType();
            if (stype == SocketType.WEAK_OUT)
                inputPin.setState(Pin.PinState.NOT_CONNECTED);
            else {
                // ostatne nezapojene vstupy su nahodne
                if (rand.nextBoolean()) {
                    inputPin.setState(Pin.PinState.HIGH);
                    return true;
                } else
                    inputPin.setState(Pin.PinState.LOW);
            }
        }


        return false;
    }

    /**
     * Kontrola a aktualizácia aktuálnej hodnoty na pine podľa potenciálu socketu ku ktorému je pripojený.
     *
     * @param inputPin Vstupný pin, ktorého stavová hodnota sa má aktualizovať a vratiť
     * @return True ak je hodnota LOW, false inak.
     */
    public boolean isLow(Pin inputPin){
        if (inputPin == null) return false;
        if (!inputPin.isConnected()) return false;

        Potential.Value value = inputPin.getSocket().getPotential().getValue();

        if (value == Potential.Value.LOW) {
            inputPin.setState(Pin.PinState.LOW);
            return true;
        } else if (value == Potential.Value.HIGH) {
            inputPin.setState(Pin.PinState.HIGH);
        } else {
            SocketType stype = inputPin.getSocket().getPotential().getType();
            if (stype == SocketType.WEAK_OUT)
                inputPin.setState(Pin.PinState.NOT_CONNECTED);
            else {
                // ostatne nezapojene vstupy su nahodne
                if (rand.nextBoolean())
                    inputPin.setState(Pin.PinState.HIGH);
                else {
                    inputPin.setState(Pin.PinState.LOW);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Nastavenie hodnoty na výstupnom pine a zaznamenanie zmenovej udalosti pre simuláciu, ak sa hodnota zmenila.
     *
     * @param pin Pin zariadenia, ktorého výstupná hodnota sa má zmeniť.
     * @param state Nový výstupný stav.
     */
    public void setPin(Pin pin, Pin.PinState state){
        if (pin.getState() != state) {
            pin.setState(state);

            switch (state) {
                case HIGH:
                    getBoard().addEvent(new BoardChangeEvent(pin.getSocket(), Potential.Value.HIGH));
                    break;
                case LOW:
                    getBoard().addEvent(new BoardChangeEvent(pin.getSocket(), Potential.Value.LOW));
                    break;
                case HIGH_IMPEDANCE:
                case NOT_CONNECTED:
                    getBoard().addEvent(new BoardChangeEvent(pin.getSocket(), Potential.Value.NC));
            }
        }
    }

    /**
     * Nastavenie hodnoty na výstupnom pine a zaznamenanie zmenovej udalosti pre simuláciu.
     * Nezáleží, či sa hodnota zmenila alebo nie, udalosť sa aj tak vytvorí.
     *
     * @param pin   Pin zariadenia, ktorého výstupná hodnota sa má zmeniť.
     * @param state Nový výstupný stav.
     */
    public void setPinForce(Pin pin, Pin.PinState state) {
        pin.setState(state);

        switch (state) {
            case HIGH:
                getBoard().addEvent(new BoardChangeEvent(pin.getSocket(), Potential.Value.HIGH));
                break;
            case LOW:
                getBoard().addEvent(new BoardChangeEvent(pin.getSocket(), Potential.Value.LOW));
                break;
            case HIGH_IMPEDANCE:
            case NOT_CONNECTED:
                getBoard().addEvent(new BoardChangeEvent(pin.getSocket(), Potential.Value.NC));
        }
    }

    /*
    DETAKCIA KOLIZII -> PRIPOJENIA PINOV NA SOKETY
     */

    /**
     * Vyhľadá sokety umiestnené pod pinmi zariadenia a zapamätá si ich vo vlastnom poli pre ďalšie operácie.
     *
     * @return Výsledok hľadania. True ak našiel aspoň jeden soket pre pin.
     */
    public boolean searchForSockets(){
        boolean found = false;
        List<Component> collisionComponents = getBoard().checkForCollisionWithComponent(this);

        //vycistenie pola pre sokety ku ktorym sa moze pripojit
        this.socketsToConnectTo.clear();

        //ak existuje komponent s ktorym sa toto zariadenie prekryva
        //a zaroven, ak sa jedna o IC, musi byt prave jeden komponet, lebo sa neda pripojit cez viacero
        if ((collisionComponents != null) && (!(this instanceof Chip) || (collisionComponents.size() == 1))) {
            List<Pin> pins = getPins();
            Socket[] sockets = new Socket[pins.size()];

            //pre kazdy z komponentov
            for (Component component : collisionComponents) {
                //hladaj ci sa nachadza pin nad nejakym volnym soketom
                for (int i = 0; i < pins.size(); i++) {
                    //ak bol uz najdeny soket pre tento pin, preskoc hladanie
                    if(sockets[i] != null) continue;

                    //TODO berie sokety aj cez prekryvajuci komponent
                    Socket socket = getBoard().checkForCollisionWithSocket(component, pins.get(i));
                    //ak ano pridaj ho do zoznamu soketov na pripojenie
                    if(socket != null){
                        found = true;
                        sockets[i] = socket;
                    }
                }
            }

            if(found) this.socketsToConnectTo.addAll(Arrays.asList(sockets));
        }

        return found;
    }

    /**
     * Pokúsi sa pripojiť zariadenie k nájdeným soketom. Pripojenie sa podarí, iba ak je možné zapojiť všetky piny
     * zariadenia naraz.
     *
     * @return True ak sa podarilo pripojiť všetky piny zariadenia k soketom, false inak.
     */
    public boolean tryToConnectToFoundSockets() {
        List<Pin> pins = getPins();

        //ak sa mozu pripojit vsetky piny zariadenia
        if (this.socketsToConnectTo.size() == pins.size()) {
            for (int i = 0; i < this.socketsToConnectTo.size(); i++) {
                Socket socket = this.socketsToConnectTo.get(i);
                if (socket != null && !socket.isOccupied()) {
                    socket.connect(pins.get(i));

                    //ak nie si priradeny ku komponentu, prirad sa ku komponentu prveho pripojeneho soketu
                    if(this.component == null) {
                        this.component = socket.getComponent();
                        this.component.addDevice(this);
                    }
                } else {
                    //odpoj pripojene piny
                    this.disconnectAllPins();
                    return false;
                }
            }
        } else return false;

        return true;
    }

    /**
     * Odpojenie všetkých pinov zariadenia od komponentu.
     */
    public void disconnectAllPins(){
        List<Pin> pins = getPins();
        if (pins != null)
            pins.forEach(Pin::disconnect);

        if(this.component != null){
            this.component.removeDevice(this);
            this.component = null;
        }
    }

    /**
     * Je aspoň jeden pin pripojený k soketu?
     *
     * @return True ak áno, dalse inak.
     */
    public boolean isConnected(){
        List<Pin> pins = getPins();
        for (Pin pin : pins) {
            if(pin.isConnected()) return true;
        }

        return false;
    }

    /**
     * Zvýrazní sokety, ku ktorým sa môže zariadenie pripojiť.
     */
    public void highlightConnectibleSockets(){
        socketsToConnectTo.forEach((socket) -> {
            if (socket != null) socket.highlight(!socket.isOccupied() ? Socket.OK : Socket.WARNING);
        });
    }

    /**
     * Zruší zvýraznenie soketov ku ktorým sa môže pripojiť.
     */
    public void unhighlightConnectibleSockets(){
        socketsToConnectTo.forEach((socket) -> {
            if (socket != null) socket.unhighlight(!socket.isOccupied() ? Socket.OK : Socket.WARNING);
        });
    }

    @Override
    public void delete() {
        super.delete();

        this.disconnectAllPins();
        unhighlightConnectibleSockets();
    }


}