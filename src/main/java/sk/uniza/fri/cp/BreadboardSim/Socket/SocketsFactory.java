package sk.uniza.fri.cp.BreadboardSim.Socket;

import javafx.scene.Group;
import sk.uniza.fri.cp.BreadboardSim.Board.GridSystem;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;

import java.util.List;

/**
 * Faktorka na vytváranie skupín soketov.
 *
 * @author Tomáš Hianik
 * @version 1.0
 * @created 17.3.2017
 */
public class SocketsFactory {

    /**
     * Vytvorenie horizontálnej skupiny soketov.
     *
     * @param component  Komponent, na ktorom sú pridané.
     * @param count      Počet soketov.
     * @param collection Kolekcia, do ktorej sú sokety pridané.
     * @return Vytovrená skupina soketov.
     */
    public static Group getHorizontal(Component component, int count, List<Socket> collection) {
        Group sockets = new Group();
		GridSystem grid = component.getBoard().getGrid();
		int gridSpaceX = grid.getSizeX();

		for (int i = 0; i < count; i++) {
            Socket socket = new Socket(component);
            socket.setLayoutX(i*gridSpaceX);

			//spojenie socketov pomocou potencialu
			if(sockets.getChildren().size() > 0){
				new Potential((Socket) sockets.getChildren().get(0), socket);
			}

			sockets.getChildren().add(socket);
			collection.add(socket);
		}

		return sockets;
    }

    /**
     * Vytvorenie horizontálnej skupiny soketov.
     *
     * @param component Komponent, na ktorom sú pridané.
     * @param count Počet soketov.
     * @param numInStack Počet v skupinke.
     * @param spaceBetweenStack Medzera medzi skupinkami.
     * @param collection Kolekcia, do ktorej sú sokety pridané.
     * @return Vytovrená skupina soketov.
     */
    public static Group getHorizontal(Component component, int count, int numInStack, int spaceBetweenStack, List<Socket> collection) {
        Group sockets = new Group();
		GridSystem grid = component.getBoard().getGrid();
		int gridSpaceX = grid.getSizeX();

		int posX = 0;
		for (int i = 0; i < count; i++) {
            Socket socket = new Socket(component);
            socket.setLayoutX(posX*gridSpaceX);

			//spojenie socketov pomocou potencialu
			if(sockets.getChildren().size() > 0){
				new Potential((Socket) sockets.getChildren().get(0), socket);
			}

			sockets.getChildren().add(socket);
			collection.add(socket);

			if((i+1) % numInStack == 0)
				posX += spaceBetweenStack + 1;
			else
				posX++;
		}

		return sockets;
	}

    /**
     * Vytvorenie vertikálnej skupiny soketov.
     *
     * @param component Komponent, na ktorom sú pridané.
     * @param count Počet soketov.
     * @param collection Kolekcia, do ktorej sú sokety pridané.
     * @return Vytovrená skupina soketov.
     */
    public static Group getVertical(Component component, int count, List<Socket> collection) {
        Group sockets = new Group();
		GridSystem grid = component.getBoard().getGrid();
		int gridSpaceY = grid.getSizeY();

		for (int i = 0; i < count; i++) {
            Socket socket = new Socket(component);
            socket.setLayoutY(i*gridSpaceY);

			//spojenie socketov pomocou potencialu
			if(sockets.getChildren().size() > 0){
				new Potential((Socket) sockets.getChildren().get(0), socket);
			}

			sockets.getChildren().add(socket);
			collection.add(socket);
		}

		return sockets;
    }

    /**
     * Vytvorenie vertikálnej skupiny soketov.
     *
     * @param component Komponent, na ktorom sú pridané.
     * @param count Počet soketov.
     * @param numInStack Počet v skupinke.
     * @param spaceBetweenStack Medzera medzi skupinkami.
     * @param collection Kolekcia, do ktorej sú sokety pridané.
     * @return Vytovrená skupina soketov.
     */
    public static Group getVertical(Component component, int count, int numInStack, int spaceBetweenStack, List<Socket> collection) {
        Group sockets = new Group();
        GridSystem grid = component.getBoard().getGrid();
        int gridSpaceY = grid.getSizeY();

        int posX = 0;
        for (int i = 0; i < count; i++) {
            Socket socket = new Socket(component);
            socket.setLayoutY(posX*gridSpaceY);

            //spojenie socketov pomocou potencialu
            if(sockets.getChildren().size() > 0){
                new Potential((Socket) sockets.getChildren().get(0), socket);
            }

            sockets.getChildren().add(socket);
            collection.add(socket);

            if((i+1) % numInStack == 0)
                posX += spaceBetweenStack + 1;
            else
                posX++;
        }

        return sockets;
    }

    /**
     * Vytvorenie horizontálnej skupiny soketov, ktoré slúžia ako napájanie.
     *
     * @param component Komponent, na ktorom sú pridané.
     * @param count Počet soketov.
     * @param pwrValue Hodnota potenciálu po spustení simulácie.
     * @param collection Kolekcia, do ktorej sú sokety pridané.
     * @return Vytovrená skupina soketov.
     */
    public static Group getHorizontalPower(Component component, int count, Potential.Value pwrValue, List<PowerSocket> collection) {
        Group sockets = new Group();
        GridSystem grid = component.getBoard().getGrid();
        int gridSpaceX = grid.getSizeX();

        for (int i = 0; i < count; i++) {
            PowerSocket socket = new PowerSocket(component, pwrValue);
            socket.setLayoutX(i*gridSpaceX);
            sockets.getChildren().add(socket);
            collection.add(socket);
        }

        return sockets;
    }

    /**
     * Vytvorenie vertikálnej skupiny soketov, ktoré slúžia ako napájanie.
     *
     * @param component Komponent, na ktorom sú pridané.
     * @param count Počet soketov.
     * @param pwrValue Hodnota potenciálu po spustení simulácie.
     * @param collection Kolekcia, do ktorej sú sokety pridané.
     * @return Vytovrená skupina soketov.
     */
    public static Group getVerticalPower(Component component, int count, Potential.Value pwrValue, List<PowerSocket> collection) {
        Group sockets = new Group();
        GridSystem grid = component.getBoard().getGrid();
        int gridSpaceY = grid.getSizeY();

        for (int i = 0; i < count; i++) {
            PowerSocket socket = new PowerSocket(component, pwrValue);
            socket.setLayoutY(i*gridSpaceY);
            sockets.getChildren().add(socket);
            collection.add(socket);
        }

        return sockets;
    }

}