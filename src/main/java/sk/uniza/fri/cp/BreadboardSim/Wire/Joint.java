package sk.uniza.fri.cp.BreadboardSim.Wire;


import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Movable;

import java.util.LinkedList;

/**
 * Spájač častí káblikov.
 * Hodnoty layoutX a layoutY sú jeho stredom.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public class Joint extends Movable {

    private static final Color DEFAULT_COLOR = Color.DARKGRAY;

    // zoznam vypnutych node-ov pri tahani konca kablika
    private final LinkedList<Node> disabledNodes = new LinkedList<>();

    //Na indexe 0 wireSegments je vždy segment. Aj pri odstránení
    WireSegment[] wireSegments; //segmenty ktore spaja

	private Wire wire;
    private Circle joint;
    private Circle colorizer;
	private Rectangle boundingBox;
	private double radius;

    /**
     * Spájač častí káblikov.
     *
     * @param board Plocha simulátora.
     * @param wire  Káblik, na ktorom je umiestnený.
     */
    public Joint(Board board, Wire wire){
        super(board);
        this.wire = wire;

        this.wireSegments = new WireSegment[2];

        GridSystem grid = getBoard().getGrid();

        boundingBox = new Rectangle(grid.getSizeX(), grid.getSizeY());
        boundingBox.setOpacity(0);
        boundingBox.setLayoutX(-grid.getSizeX()/2.0);
        boundingBox.setLayoutY(-grid.getSizeY()/2.0);

        this.radius = grid.getSizeMin() / 3.7;
        Group graphic = generateJointGraphic(radius);

        this.getChildren().addAll(boundingBox, graphic);

        this.setOnMouseDragged(event -> {
            getBoard().addSelect(getWire());
            getWire().select();
        });

        this.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                this.delete();
            } else getBoard().addSelect(getWire());
        });

        this.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
        this.addEventFilter(MouseEvent.MOUSE_RELEASED, this::onMouseReleased);
    }

    /**
     * Akcia pri pohybe so spájačom/koncom káblika.
     * Vytvorené predovšetkým pre odchytenie a stransparentnenie segmentov káblika, ktorý by prekážal pri zakpájaní do
     * soketu.
     *
     * @param event MouseEvent
     */
    public void onMouseDragged(MouseEvent event) {
        Node node = event.getPickResult().getIntersectedNode();
        if (node != null && node instanceof Line) {
            disabledNodes.addLast(node);
            event.getPickResult().getIntersectedNode().setMouseTransparent(true);
        }
    }

    /**
     * Ukončenie pohybu spájača.
     *
     * @param event MouseEvent
     */
    public void onMouseReleased(MouseEvent event) {
        if (!disabledNodes.isEmpty()) {
            disabledNodes.forEach(node -> node.setMouseTransparent(false));
        }
    }

    /**
     * Pripojenie jointu na segment. Ak nie je pripojený ani jeden segment, hodí ho ako primárny,
     * inak ako sekundárny segment.
     *
     * @param segment Segment káblika.
     */
    public void connectWireSegment(WireSegment segment){
		if(this.wireSegments[0] == null)
			this.wireSegments[0] = segment;
		else
			this.wireSegments[1] = segment;
	}

    /**
     * Odstránenie segmentu ktorý spája.
     *
     * @param segment Segment na odstránenie.
     * @return True ak taký segment poznal a bol odstránený, inak false.
     */
    boolean removeWireSegment(WireSegment segment){
        if(wireSegments[0] == segment) {
			wireSegments[0] = wireSegments[1];
			wireSegments[1] = null;
		}
		else if(wireSegments[1] == segment)
			wireSegments[1] = null;
		else return false;

		return true;
	}

    /**
     * Vráti káblik na ktorom je zlom umiestnený.
     *
     * @return Káblik ku ktorému patrí.
     */
    public Wire getWire(){
        return this.wire;
    }

    WireSegment getPrimaryWireSegment(){
        return this.wireSegments[0];
	}

    WireSegment getSecondaryWireSegment(){
        return this.wireSegments[1];
	}

    /**
     * Zmena farby.
     *
     * @param color Nová farba.
     */
    public void setColor(Color color){
        if (Platform.isFxApplicationThread()) {
            this.colorizer.setFill(color);
            this.colorizer.setOpacity(1);
        } else {
            Platform.runLater(() -> {
                this.colorizer.setFill(color);
                this.colorizer.setOpacity(1);
            });
        }
    }

    /**
     * Nastavenie pôvodnej farby.
     */
    public void setDefaultColor(){
        if (Platform.isFxApplicationThread()) {
            this.colorizer.setOpacity(0);
        } else {
            Platform.runLater(() -> this.colorizer.setOpacity(0));
        }
    }

    /**
     * Zväčšenie grafického rádiusu zlomu.
     */
    void incRadius() {
        radius *= 1.2;
        this.joint.setRadius(radius);
    }

    /**
     * Zmenšenie grafického rádiusu zlomu.
     */
    void decRadius() {
        radius *= 1.2;
        this.joint.setRadius(radius);
    }

	protected Group generateJointGraphic(double radius){

		Group graphics = new Group();

        this.joint = new Circle(radius, radius, radius, DEFAULT_COLOR);
        this.joint.setTranslateX(-radius);
        this.joint.setLayoutY(-radius);


        this.colorizer = new Circle(0,0,radius, Color.RED);
        this.colorizer.setOpacity(0);

        graphics.getChildren().addAll(this.joint, this.colorizer);


		return graphics;
	}

    @Override
    public Pane getDescription() {
        return getWire().getDescription();
    }
}