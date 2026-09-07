package sk.uniza.fri.cp.BreadboardSim.Devices.Pin;

import sk.uniza.fri.cp.BreadboardSim.Devices.Device;

/**
 * Nepripojený pin. Nemá vplyv na potenciál.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 24.3.2017
 */
public class NotConnectedPin extends Pin {

    /**
     * Nepripojený pin.
     *
     * @param device Zariadenie, na ktorom sa nachádza.
     */
    public NotConnectedPin(Device device) {
        super(device);
    }

    @Override
    public String getName() {
        return "NC";
    }
}
