package sk.uniza.fri.cp.BreadboardSim.Components;


import javafx.scene.layout.Pane;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Devices.Device;
import sk.uniza.fri.cp.BreadboardSim.Item;
import sk.uniza.fri.cp.BreadboardSim.SchoolBreadboard;
import sk.uniza.fri.cp.BreadboardSim.Socket.PowerSocket;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;
import sk.uniza.fri.cp.BreadboardSim.Wire.Wire;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstraktná trieda pre komponenty.
 * Komponent má sokety, ku ktorým sa priájajú ine zariadenia.
 * Po vytvorení soketov je nutné ich registrovať pomocou addSocket,
 * aby ich bolo mozne vyhladat a pripojit k nim zariadenia.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17-mar-2017 16:16:34
 */
public abstract class Component extends Item {

    protected int gridWidth;
    protected int gridHeight;

	private LinkedList<ConnectedDevice> connectedDevices;

    private LinkedList<Wire> connectedWires;
    private ArrayList<Socket> sockets; //pole vsetkych soketov na komponente
    private LinkedList<PowerSocket> powerSockets;

    /**
     * Konštruktor pre itemPicker
     */
    public Component() {
    }

    /**
     * Konśtruktor pre vytvorenie objektu, ktorý sa umiestni na plochu simulátora.
     *
     * @param board Plocha simulátora
     */
    public Component(Board board) {
        super(board);
        this.powerSockets = new LinkedList<>();
        this.sockets = new ArrayList<>();
        this.connectedDevices = new LinkedList<>();
        this.connectedWires = new LinkedList<>();
    }

    /**
     * Vráti použitý zoznam PowerSocket-ov, nie kópiu.
     *
     * @return Zoznam powerSocketov komponentu.
     */
    public List<PowerSocket> getPowerSockets(){
        return powerSockets;
    }

    /**
     * Registrácia soketu viditeľného na komponente. Soketu je pri registrácií priradené unkiátne id v rámci komponentu.
     *
     * @param socket Soket viditeľný a prístupný na komponente.
     */
    void addSocket(Socket socket) {
        //id soketu na zaklade velkosti pola soketov -> id je jeho index
        socket.setId(Integer.toString(this.sockets.size()));
        this.sockets.add(socket);
    }

    /**
     * Registrácia soketu viditeľného na komponente. Soketu je pri registrácií priradené unkiátne id v rámci komponentu.
     *
     * @param sockets Sokety viditeľné a prístupné na komponente.
     */
    protected void addAllSockets(Socket... sockets) {
        for (Socket socket : sockets)
            addSocket(socket);
    }

    /**
     * Registrácia soketu viditeľného na komponente. Soketu je pri registrácií priradené unkiátne id v rámci komponentu.
     *
     * @param sockets Sokety viditeľné a prístupné na komponente.
     */
    protected void addAllSockets(List<? extends Socket> sockets) {
        for (Socket socket : sockets)
            addSocket(socket);
    }

    /**
     * Vráti sokety komponentu.
     *
     * @return Sokety na komponente.
     */
    public ArrayList<Socket> getSockets() {
        return sockets;
    }

    /**
     * Vráti soket na základe jeho ID -> idnex v rámci komponentu.
     *
     * @param id ID soketu.
     * @return Soket na indexe podľa ID.
     */
    public Socket getSocket(int id) {
        return this.sockets.get(id);
    }

    /**
     * Registrácia zariadenia, ktoré je pripojené ku komponentu.
     *
     * @param device Zariadenie pripojené na komponent.
     * @return True ak bolo zariadenie pridané do zoznamu, false inak.
     */
    public boolean addDevice(Device device){
        return connectedDevices.add(new ConnectedDevice(device));
    }

    /**
     * Odobratie zariadenia zo zoznamu pripojených zariadení ku kompoentu.
     *
     * @param device Odoberané zariadenie.
     * @return True ak bolo odobraté, false inak.
     */
    public boolean removeDevice(Device device){
        return connectedDevices.removeIf(cd -> cd.getDevice() == device);
    }

    /**
     * Registrácia spojenia, ktoré je pripojené ku komponentu.
     *
     * @param wire Spojenie pripojené na komponent.
     * @return True ak bolo spojenie pridané do zoznamu, false inak.
     */
    public boolean addWire(Wire wire){ return this.connectedWires.add(wire); }

    /**
     * Odobratie spojenia zo zoznamu pripojených spojení ku kompoentu.
     *
     * @param wire Odoberané spojenie.
     * @return True ak bolo odobraté, false inak.
     */
    public boolean removeWire(Wire wire){
        return connectedWires.remove(wire);
    }

    /**
     * Aktualizácia pozície pripojených zariadení. Zariadenia sa nepohybuju automaticky pri zmene polohy komponentu.
     */
    public void updateConnectedDevicesPosition(){
        connectedDevices.forEach(ConnectedDevice::updatePos);
    }

    /**
     * Šírka komponentu v jednotkách mriežky.
     *
     * @return Šírka komponentu.
     */
    public int getGridWidth() {
        return gridWidth;
    }

    /**
     * Výška komponentu v jednotkách mriežky.
     *
     * @return Výška komponentu.
     */
    public int getGridHeight() {
        return gridHeight;
    }

	@Override
	public void moveTo(int gridPosX, int gridPosY) {
		super.moveTo(gridPosX, gridPosY);

		updateConnectedDevicesPosition();
	}

    /**
     * Porovnanie dvoch objektov, ak sú komponenty umiestnené na rovnakej vývojovej doske, vracia true.
     *
     * @param obj Objekt pre porovnanie.
     * @return True ak sa jedná o rovnaký objekt alebo ak sú oba na rovnakej vývojovej doske.
     */
    @Override
	public boolean equals(Object obj) {
		//ak maju oba objekty ako predka rovnaky SchoolBreadboard, berieme ze je to jeden komponent
		if(obj instanceof Component
				&& ((Component) obj).getParent() instanceof SchoolBreadboard
				&& this.getParent() instanceof SchoolBreadboard ){
			if(((Component) obj).getParent() == this.getParent())
				return true;
		}

		return super.equals(obj);
	}

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public void delete() {
        super.delete();

        //zmazanie pripojenych zariadeni
        this.connectedDevices.forEach(ConnectedDevice::delete);

        //zamaznie pripojenych kablikov
        //kedze su v zozname z ktoreho cerpame a zaroven ich mazeme, treba povolat pomoc
        for (Wire wire : connectedWires.toArray(new Wire[0]))
            wire.delete();

    }

    @Override
    public Pane getImage() {
        return super.getImage();
    }

    @Override
    public Pane getDescription() {
        return super.getDescription();
    }

    private class ConnectedDevice{

        private Device device;
        private int deviceGridOffsetX;
        private int deviceGridOffsetY;

        ConnectedDevice(Device device){
            this.device = device;
            this.deviceGridOffsetX = device.getGridPosX() - getGridPosX();
            this.deviceGridOffsetY = device.getGridPosY() - getGridPosY();
        }

        void updatePos(){
            device.moveTo(getGridPosX() + deviceGridOffsetX, getGridPosY() + deviceGridOffsetY);
        }

        Device getDevice() {
            return device;
        }

        void delete(){
            this.device.delete();
        }
    }
}