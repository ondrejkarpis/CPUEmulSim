package sk.uniza.fri.cp.BreadboardSim.Board;

import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Devices.LED;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

import java.util.Set;

/**
 * DOCASNE ci to vobec bude fungovat
 */
public class LedChangeEvent extends BoardEvent {

    private boolean newValue;
    private LED led;

    public LedChangeEvent(Socket socket, LED led, boolean turnOn) {
        super(socket);

        this.led = led;
        this.newValue = turnOn;
    }

    @Override
    public void process(Set<Device> devices) {
        if (this.newValue)
            this.led.turnOn();
        else
            this.led.turnOff();
    }
}
