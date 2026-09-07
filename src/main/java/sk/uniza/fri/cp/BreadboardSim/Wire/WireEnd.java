package sk.uniza.fri.cp.BreadboardSim.Wire;


import javafx.beans.value.ChangeListener;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.BoardEvent;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

/**
 * Koniec káblika, rozširuje Joint. Na rozdiel od Joint-u môže byť pripojený k soketu.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public class WireEnd extends Joint {

	private Socket socket;

	private double lastPosX = -1;
	private double lastPosY = -1;

    private boolean moved;

    //pri zmene pozicie soketu sa posunie aj koniec
    private ChangeListener<Transform> socketPositionChangeListener = (observable, oldValue, newValue) -> {
        if(lastPosX == -1){
            lastPosX = getLayoutX();
            lastPosY = getLayoutY();
        }

        setLayoutX(socket.getBoardX() / getBoard().getAppliedScale());
        setLayoutY(socket.getBoardY() / getBoard().getAppliedScale());

        getWire().moveJointsWithEnd(this, getLayoutX() - lastPosX, getLayoutY() - lastPosY);

        lastPosX = getLayoutX();
        lastPosY = getLayoutY();
    };

    WireEnd(Board board, Wire wire) {
        super(board, wire);

		this.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			getWire().setMouseTransparent(true);
			getWire().setOpacity(0.5);
        });

        this.addEventFilter(MouseEvent.DRAG_DETECTED, event -> {
            startFullDrag();

            this.moved = true;

            if (this.getSocket() != null)
                this.disconnectSocket();

            this.setDefaultColor();
        });

        this.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            getWire().setMouseTransparent(false);
            getWire().setOpacity(1);

            if (!this.moved)
                getBoard().addSelect(getWire());

            this.moved = false;

            event.consume();
        });

        this.incRadius();
    }

    /**
     * Kontrola, či je koniec pripojený k soketu.
     *
     * @return True ak je koniec pripojený k soketu, false inak.
     */
    public boolean isConnected() {
        return this.socket != null;
    }

    /**
     * Pripojenie konca kábliku k soketu.
     * Ak bol predtým niekde pripojený, najprv sa odpojí. Ak sa nepodarí pripojiť, sfarbí sa na červeno.
     *
     * @param socket Soket, ku ktorému sa má pripojiť.
     */
    public void connectSocket(Socket socket){
        if(this.socket == null) {
            if (socket != null && socket.connect(this)) {
                this.socket = socket;

                this.socket.getItem().localToParentTransformProperty().addListener(socketPositionChangeListener);

                //TODO FATALNA CHYBA - pri "setrnejsom" localToScene nastava chyba pri zoome a jointoch na kabliku (vid zoom hadika)
                //this.socket.getItem().localToSceneTransformProperty().addListener(socketPositionChangeListener);

                setLayoutX(socket.getBoardX() / getBoard().getAppliedScale());
                setLayoutY(socket.getBoardY() / getBoard().getAppliedScale());

                lastPosX = getLayoutX();
                lastPosY = getLayoutY();

                this.setColor(this.getWire().getColor().brighter());

                this.getWire().updatePotential();
            } else {
                this.setColor(Color.RED);
            }
        } else {
            disconnectSocket();
            connectSocket(socket);
        }
    }

    /**
     * Vráti soket, ku ktorému je koniec pripojený.
     *
     * @return Soket, ku ktorému je koniec pripojený.
     */
    public Socket getSocket() {
        return socket;
    }

    @Override
    public void setDefaultColor() {
        if (this.isConnected()) this.setColor(this.getWire().getColor().brighter());
        else super.setDefaultColor();
    }

    @Override
    public void connectWireSegment(WireSegment segment) {
        this.wireSegments[0] = segment;
    }

    @Override
    public void delete() {
        super.delete();
        this.getWire().delete();
    }

    /**
     * Odpojenie konca zo soketu.
     */
    protected void disconnectSocket() {
        if(this.socket == null) return;
		Socket socketToUpdate = this.socket;

        this.socket.getItem().localToParentTransformProperty().removeListener(socketPositionChangeListener);

        //this.socket.getItem().localToSceneTransformProperty().removeListener(socketPositionChangeListener);
        this.socket.disconnect();
		this.socket = null;
		this.getWire().updatePotential();

		this.setDefaultColor();

		//ak simulacia bezi, pridaj novy event pre update potencialu odpojeneho soketu
		if(getBoard().simRunningProperty().getValue())
			getBoard().addEvent(new BoardEvent(socketToUpdate));
	}
}