package sk.uniza.fri.cp.SchematicSim;

import sk.uniza.fri.cp.SchematicSim.Gates.GateSymbol;

import java.util.HashSet;
import java.util.Set;
import java.util.function.LongConsumer;

/**
 * Evidencia obsadených buniek mriežky na ploche schémy.
 * Nahrádza pôvodnú kolíznu detekciu {@code Board.checkForCollisionWithComponent}/
 * {@code checkForCollisionWithSocket} z BreadboardSim, ktorá riešila prekrytie
 * pinov zariadenia s dierkami na doske. V schéme sa súčiastky umiestňujú voľne na
 * mriežku, takže stačí kontrolovať, či cieľové bunky nie sú už obsadené inou súčiastkou.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public class GridOccupancy {

    private final Set<Long> occupied = new HashSet<>();

    /**
     * Sú všetky bunky v danej oblasti voľné?
     */
    public boolean isFree(int gridX, int gridY, int width, int height) {
        for (int x = gridX; x < gridX + width; x++) {
            for (int y = gridY; y < gridY + height; y++) {
                if (occupied.contains(key(x, y))) return false;
            }
        }
        return true;
    }

    /**
     * Označí bunky pod danou súčiastkou (na jej AKTUÁLNEJ pozícii) ako obsadené.
     */
    public void occupy(GateSymbol gate) {
        forEachCell(gate, occupied::add);
    }

    /**
     * Uvoľní bunky pod danou súčiastkou (na jej AKTUÁLNEJ pozícii).
     */
    public void free(GateSymbol gate) {
        forEachCell(gate, occupied::remove);
    }

    private void forEachCell(GateSymbol gate, LongConsumer op) {
        int gx = (int) gate.getGridPos().getX();
        int gy = (int) gate.getGridPos().getY();
        for (int x = 0; x < gate.getGridWidth(); x++) {
            for (int y = 0; y < gate.getGridHeight(); y++) {
                op.accept(key(gx + x, gy + y));
            }
        }
    }

    private long key(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    public void clear() {
        occupied.clear();
    }
}
