package sk.uniza.fri.cp.BreadboardSim.Devices.Pin;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

/**
 * Trieda reprezentujúca pin zariadenia.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public abstract class Pin extends Circle {

	/** stavy pinu na cipe
	 * HIGH - +5V
	 * LOW - 0V
	 * HIGH_IMPEDANCE - kvazi NOT_CONNECTED, pin nema vplyv na potencial
	 * NOT_CONNECTED - nepripojene piny a vsetky piny ak cip nie je napajany
	**/
	public enum PinState { HIGH, LOW, HIGH_IMPEDANCE, NOT_CONNECTED	}

	/** typy budicov vystupnych pinov
	 * PUSH_PULL - klasicky vystup, pri napojeni vysupv +5V a GND nedefinovana hodnota
	 * TRI_STATE - pridáva tretí stav vysokej impedancie, kedy sa "odpája" od obvodu a nemá naň vplyv -> InputOutputPin
	 */
    public enum PinDriver {
        PUSH_PULL, TRI_STATE
    }

	private Socket socket;
	private Device device;
	private PinState state = PinState.NOT_CONNECTED;

    private String name = "UNNAMED " + this.getClass().getSimpleName();

    /**
     * Vytvorenie pinu na zariadení.
     *
     * @param device Zariadenie, ku ktorému je pin priradený.
     */
    public Pin(Device device){
        this.device = device;

        //"grafika" pre vytvorenie boundary na koliziu so soketmi
        this.setRadius(1);
        this.setFill(Color.RED);
    }

    /**
     * Vytvorenie pinu na zariadení s názvom.
     *
     * @param device Zariadenie, ku ktorému je pin priradený.
     * @param name   Názov pinu
     */
    public Pin(Device device, String name){
        this(device);

        this.name = name;
    }

    /**
     * Vráti zariadenie, ku ktorému je pin priradený.
     *
     * @return Zariadenia pinu.
     */
    public Device getDevice() {
        return device;
    }

    /**
     * Vráti názov pinu.
     *
     * @return Názov pinu.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Vráti stav pinu.
     *
     * @return Stav pinu.
     */
    public PinState getState() {
        return state;
    }

    /**
     * Vráti soket, ku ktorému je pin pripojený.
     *
     * @return Pripojený soket, null ak ku žiadnemu nie je pipojený.
     */
    public Socket getSocket() {
        return socket;
    }

	/**
     * Nastavenie stavu výstupného pinu. NEZmení potenciál pripojeného soketu.
     *
     * @param state Nový stav pinu - High, Low, High Impedance, Not connected
     */
	public void setState(PinState state){
		this.state = state;
	}

    /**
     * Nastavenie soketu k pinu.
     * POZOR Nepripája soket k pinu! Preto by sa pripájanie malo robiť cez soket.
     *
     * @param socket Soket, ku ktorému je pin pripojený.
     */
    public void setSocket(Socket socket){
		this.socket = socket;
		this.setFill(Color.GREEN);
    }

    /**
     * Odpojenie pinu od soketu.
     */
    public void disconnect() {
        if (this.socket != null) this.socket.disconnect();
        this.setFill(Color.RED);
    }

    /**
     * Kontrola, či je pin pripojený k nejakému soketu.
     *
     * @return True ak je pripojený, false inak.
     */
    public boolean isConnected() {
        return this.socket != null;
    }
}