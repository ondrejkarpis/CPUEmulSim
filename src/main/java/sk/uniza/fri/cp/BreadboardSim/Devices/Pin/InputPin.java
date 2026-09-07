package sk.uniza.fri.cp.BreadboardSim.Devices.Pin;


import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

/**
 * Vstupný pin. Zisťuje hodnotu potenciálu.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public class InputPin extends Pin {

    /**
     * Vstupný pin.
     *
     * @param device Zariadenie, na ktorom sa nachádza.
     */
    public InputPin(Device device){
        super(device);
    }

    /**
     * Vstupný pin.
     *
     * @param device Zariadenie, na ktorom sa nachádza.
     * @param name   Názov pinu.
     */
    public InputPin(Device device, String name){
        super(device, name);
    }

}