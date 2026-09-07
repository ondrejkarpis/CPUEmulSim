package sk.uniza.fri.cp.BreadboardSim.Devices.Pin;


import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

/**
 * Vstupno-výstupný pin. Rozširuje výsupný pin a používa trojstavový výstup.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public class InputOutputPin extends OutputPin {

    /**
     * Vstupno-výstupný pin.
     *
     * @param device Zariadenie, na ktorom sa pin nachádza.
     */
    public InputOutputPin(Device device){
        super(device, PinDriver.TRI_STATE);
    }

    /**
     * Vstupno-výstupný pin.
     *
     * @param device Zariadenie, na ktorom sa pin nachádza.
     * @param name
     */
    public InputOutputPin(Device device, String name) {
        super(device, PinDriver.TRI_STATE, name);
    }

    /**
     * Vstupno-výstupný pin.
     *
     * @param device    Zariadenie, na ktorom sa pin nachádza.
     * @param pinDriver Budič pinu.
     */
    public InputOutputPin(Device device, PinDriver pinDriver){
        super(device, pinDriver);
    }

}