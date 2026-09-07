package sk.uniza.fri.cp.BreadboardSim.Socket;

import sk.uniza.fri.cp.BreadboardSim.Board.BoardChangeEvent;
import sk.uniza.fri.cp.BreadboardSim.Components.Component;

/**
 * Napájací soket. Po spustení simulácie produkuje HIGH/LOW potenciál.
 * Ak je vytovrený a simulácia už beží, sám pridá zmenovú udalosť nad sebou.
 *
 * @author Tomáš Hianik
 * @created 24.3.2017
 */
public class PowerSocket extends Socket {

    private Potential.Value value;

    /**
     * Napájací soket.
     *
     * @param component      Komponent, na ktorom je umiestnený.
     * @param connectedValue Hodnota, ktorá sa má nastaviť po spustení simulácie. (HIGH pre VCC, LOW pre GND)
     */
    public PowerSocket(Component component, Potential.Value connectedValue) {
        super(component);
        this.setType(SocketType.OUT);
        this.value = connectedValue;

        if (component.getBoard().isSimulationRunning()) this.powerUp();
    }

    /**
     * Pridanie zmenovej udalosti s nastavním hodnoty potenciálu soketu.
     */
    public void powerUp(){
        getComponent().getBoard().addEvent(new BoardChangeEvent(this, value));
    }

    /**
     * Pridanie zmenovej udalosti s nastavením hodnoty nepripojeného potenciálu soketu.
     */
    public void powerDown(){
        getComponent().getBoard().addEvent(new BoardChangeEvent(this, Potential.Value.NC));
    }
}
