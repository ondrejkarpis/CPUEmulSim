package sk.uniza.fri.cp.BreadboardSim.Components;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import sk.uniza.fri.cp.BreadboardSim.Board.Board;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Devices.LED;
import sk.uniza.fri.cp.BreadboardSim.SchoolBreadboard;
import sk.uniza.fri.cp.BreadboardSim.Socket.Potential;
import sk.uniza.fri.cp.BreadboardSim.Socket.Socket;

/**
 * Skúšač napätia v obvode.
 *
 * @author Tomáš Hianik
 * @created 22.3.2017.
 */
public class Probe extends Component {

    private static final Color PROBE_ON_COLOR = Color.RED;
    private static final Color PROBE_OFF_COLOR = Color.YELLOW;
    private static final double RADIUS_COEF = 0.4;

    private Rectangle background;
    private Potential innerPotential;

    /**
     * Konštruktor pre itemPicker.
     */
    public Probe() {
    }

    /**
     * Konštruktor pre vytvorenie objektu určeného pre plochu siumlátora.
     *
     * @param board Plocha simulátora
     */
    public Probe(Board board) {
        super(board);

        GridSystem grid = board.getGrid();

        this.background = new Rectangle(grid.getSizeX() * 5, grid.getSizeY() * 4, SchoolBreadboard.BACKGROUND_COLOR);

        //sokety
        Socket inputSocket = new Socket(this);
        inputSocket.setLayoutX(grid.getSizeX());
        inputSocket.setLayoutY(grid.getSizeY());

        //vnutorny soket na uzemnenie LEDky
        Socket gndSocket = new Socket(this, Potential.Value.LOW);

        Circle emitter = new Circle(grid.getSizeMin() * RADIUS_COEF, PROBE_OFF_COLOR);
        LED led = new LED(board, emitter, PROBE_ON_COLOR, this);
        led.setLayoutX(grid.getSizeX() * 3);
        led.setLayoutY(grid.getSizeY());
        led.makeImmovable();

        Text probeText = Board.getLabelText("PROBE", grid.getSizeMin());
        probeText.setLayoutX(grid.getSizeX() * 3 - probeText.getBoundsInParent().getWidth() / 2); //centrovanie pod LEDku
        probeText.setLayoutY(grid.getSizeY() + probeText.getBoundsInParent().getHeight());

        //vytvorenie vnútorného soketu
        Socket innerSocket = new Socket(this);
        innerSocket.connect(led.getAnode());
        this.innerPotential = new Potential(innerSocket, inputSocket);

        gndSocket.connect(led.getCathode());

        this.getChildren().addAll(this.background, inputSocket, led, probeText);
        this.addSocket(inputSocket);
    }

    /**
     * Odstránenie pozadia z komponentu.
     */
    public void removeBackground() {
        if (this.background != null) {
            this.getChildren().removeAll(this.background);
            this.background = null;
        }
    }

    @Override
    public void delete() {
        super.delete();
        this.innerPotential.delete();
    }

    @Override
    public Pane getImage() {
        return new Pane(new ImageView(new Image("/icons/components/probe.png")));
    }
}
