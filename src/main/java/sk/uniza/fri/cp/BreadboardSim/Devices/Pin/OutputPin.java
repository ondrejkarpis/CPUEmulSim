package sk.uniza.fri.cp.BreadboardSim.Devices.Pin;


import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

/**
 * Výstupný pin. Riadi hodnotu potenciálu.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public class OutputPin extends Pin {

	private PinDriver driver;

    /**
     * Výstupný pin.
     *
     * @param device Zariadenie, na ktorom sa pin nachádza.
     */
    public OutputPin(Device device){
        super(device);
        this.driver = PinDriver.PUSH_PULL;
    }

    /**
     * Výstupný pin.
     *
     * @param device Zariadenie, na ktorom sa pin nachádza.
     * @param name   Názov pinu.
     */
    public OutputPin(Device device, String name){
        super(device, name);
        this.driver = PinDriver.PUSH_PULL;
    }

    /**
     * Výstupný pin.
     *
     * @param device Zariadenie, na ktorom sa pin nachádza.
     * @param pinDriver Budič pinu.
     */
    public OutputPin(Device device, PinDriver pinDriver){
        super(device);
        this.driver = pinDriver;
    }

    /**
     * Výstupný pin.
     *
     * @param device Zariadenie, na ktorom sa pin nachádza.
     * @param pinDriver Budič pinu.
     * @param name Názov pinu.
     */
    public OutputPin(Device device, PinDriver pinDriver, String name) {
        super(device, name);
        this.driver = pinDriver;
    }

    /**
     * Vráti použitý budič.
     *
     * @return Budič pinu.
     */
    public PinDriver getPinDriver(){
        return driver;
    }

}