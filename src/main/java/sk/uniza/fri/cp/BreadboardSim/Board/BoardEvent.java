package sk.uniza.fri.cp.BreadboardSim.Board;


import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

import java.util.Set;

/**
 * Objekt aktualizačnej udalosti nad soketom v simulácií.
 * Aktualizácia nemení hodnotu potenciálu, iba aktualizuje zariadenia k nemu pripojené.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public class BoardEvent {
	private Socket socket;

    /**
     * Aktualizačná udalosť.
     *
     * @param socket Soket, ktorý sa má aktualizovať.
     */
    public BoardEvent(Socket socket){
        this.socket = socket;
    }

    /**
     * Soket priradený k udalosti.
     *
     * @return Soket udalosti.
     */
    public Socket getSocket(){
        return socket;
    }

    /**
     * Spracovanie udalosti.
     *
     * @param devices Zariadenia s napojenými vstupmi k potenciálu soketu, ktoré sa majú aktualizovať.
     */
    public void process(Set<Device> devices) {
        if(devices != null && socket != null){
            Potential potential = socket.getPotential();
            if(potential != null)
                potential.getDevicesWithInputs(devices);
        }
    }

}