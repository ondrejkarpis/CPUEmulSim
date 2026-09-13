package sk.uniza.fri.cp.SchematicSim.Wire;

import javafx.geometry.Point2D;
import sk.uniza.fri.cp.SchematicSim.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * Výpočet ortogonálnej (Manhattan) trasy vodiča medzi dvomi bodmi. Ak niektorý bod má pevne
 * daný smer vývodu ({@link Side}), vodič z neho vychádza kolmo na túto stranu. Voľné body
 * (napr. ručne pridaný {@code Joint}, ktorý nie je pripojený k pinu) smer nemajú - v tom
 * prípade sa použije jediný voľný ohyb.
 * <p>
 * Ide o heuristický router (max. 3 zalomenia), nie o obchádzajúci (obstacle-avoiding) pathfinder -
 * ten by musel riešiť aj prekrývanie s telami iných súčiastok, čo je zámerne mimo rozsahu.
 * Používateľ si trasu môže kedykoľvek doladiť manuálne pridaním/presunom zlomu.
 *
 * @author Claude (návrh podľa SchematicSim architektúry)
 */
public final class OrthogonalRouter {

    private OrthogonalRouter() {
    }

    public static List<Point2D> route(Point2D p0, Side side0, Point2D p1, Side side1) {
        return route(p0, side0, p1, side1, null, GRID);
    }

    /**
     * Výpočet trasy s preferovaným smerom prvej úsečky. Používa sa pri ťahaní odbočky zo
     * spájača na vodiči, aby vodič vychádzal kolmo na kmeň v smere kurzora (a neprekrýval
     * pôvodný vodič), namiesto predvoleného "najprv vodorovne".
     *
     * @param preferredFirst ak nie je null, prvá úsečka ide v tomto smere (TOP/BOTTOM alebo LEFT/RIGHT)
     */
    public static List<Point2D> route(Point2D p0, Side side0, Point2D p1, Side side1, Side preferredFirst) {
        return route(p0, side0, p1, side1, preferredFirst, GRID);
    }

    /**
     * Výpočet trasy s preferovaným smerom prvej úsečky a snapovaním zlomov na mriežku.
     * Keď je {@code grid} <= 0, zlomy sa nesnapujú (plynulý ťah rozpracovaného vodiča).
     *
     * @param grid veľkosť mriežky v px - zlomy (zalomenia) trasy sa zaokrúhľujú na jej násobok
     */
    public static List<Point2D> route(Point2D p0, Side side0, Point2D p1, Side side1, Side preferredFirst, int grid) {
        // ak sú oba body na tej istej priamke, priamka bez zalomenia stačí (aj bez pevných smerov)
        if (side0 == null && side1 == null) {
            List<Point2D> pts = new ArrayList<>();
            pts.add(p0);
            addElbow(pts, p0, p1, preferredFirst, grid);
            pts.add(p1);
            return collapseColinear(pts);
        }

        Point2D dir0 = side0 == null ? null : side0.vector();
        Point2D dir1 = side1 == null ? null : side1.vector();

        List<Point2D> pts = new ArrayList<>();
        pts.add(p0);

        if (dir0 != null && dir1 == null) {
            Point2D s0 = p0.add(dir0.multiply(STUB));
            pts.add(s0);
            addFreeElbow(pts, s0, p1, grid);
            pts.add(p1);
        } else if (dir0 == null) {
            // voľný začiatok (spájač na vodiči): najprv preferovanou osou ku stubu pinu,
            // posledná úsečka je kolmý vývod z tela súčiastky (STUB)
            Point2D s1 = dir1 == null ? p1 : p1.add(dir1.multiply(STUB));
            addElbow(pts, p0, s1, preferredFirst, grid);
            pts.add(s1);
            if (dir1 != null) pts.add(p1);
        } else {
            routeFixedToFixed(pts, p0, dir0, p1, dir1, grid);
            pts.add(p1);
        }

        return collapseColinear(pts);
    }

    /** Dĺžka "pahýľa" tesne pri pine, aby vodič vychádzal kolmo na telo súčiastky. */
    private static final double STUB = 20;

    /** Veľkosť mriežky v px použitá pri predvolených (bezgridových) volaniach routra. */
    private static final int GRID = 20;

    /**
     * Zaokrúhlenie súradnice na najbližší násobok mriežky. Pri {@code grid <= 0} sa súradnica
     * nemení - používa sa pri plynulom ťahaní rozpracovaného vodiča.
     */
    private static double snapToGrid(double value, int grid) {
        if (grid <= 0) return value;
        return Math.round(value / grid) * grid;
    }

    /**
     * Voľný ohyb medzi bodmi a a b. Zlom sa tvorí z oboch súradníc koncových bodov;
     * voľná (vzdialenejšia) súradnica sa snapuje na mriežku, spoločná (v najbližšom
     * koncovom bode) ostáva na mieste, aby ohyb neporušil pravouhlosť trasy.
     */
    private static void addFreeElbow(List<Point2D> pts, Point2D a, Point2D b, int grid) {
        if (a.getX() != b.getX() && a.getY() != b.getY()) {
            pts.add(new Point2D(snapToGrid(b.getX(), grid), a.getY()));
        }
    }

    /**
     * Voľný ohyb medzi bodmi a a b. Bez preferencie vedie najprv vodorovne a potom zvislo;
     * s preferenciou TOP/BOTTOM vedie najprv zvislo (kolmo), s LEFT/RIGHT najprv vodorovne.
     */
    private static void addElbow(List<Point2D> pts, Point2D a, Point2D b, Side preferred, int grid) {
        if (a.getX() == b.getX() || a.getY() == b.getY()) return;
        if (preferred == Side.TOP || preferred == Side.BOTTOM) {
            pts.add(new Point2D(a.getX(), snapToGrid(b.getY(), grid)));
        } else {
            pts.add(new Point2D(snapToGrid(b.getX(), grid), a.getY()));
        }
    }

    private static void routeFixedToFixed(List<Point2D> pts, Point2D p0, Point2D dir0, Point2D p1, Point2D dir1, int grid) {
        Point2D s0 = p0.add(dir0.multiply(STUB));
        Point2D s1 = p1.add(dir1.multiply(STUB));
        boolean horiz0 = dir0.getX() != 0;
        boolean horiz1 = dir1.getX() != 0;

        pts.add(s0);

        if (horiz0 != horiz1) {
            // kolmé smery - jeden roh, ak sedí znamienko oboch smerov
            Point2D corner = horiz0
                    ? new Point2D(snapToGrid(s1.getX(), grid), s0.getY())
                    : new Point2D(s0.getX(), snapToGrid(s1.getY(), grid));
            if (signMatches(s0, corner, dir0) && signMatches(corner, s1, dir1.multiply(-1))) {
                pts.add(corner);
            } else {
                detour(pts, s0, dir0, s1, dir1, grid);
            }
        } else {
            // rovnobežné smery - priamka, ak sú zarovnané, inak Z-tvar cez stred
            // stredová súradnica sa snapuje na mriežku, aby zalomenie ležalo na čiare mriežky
            boolean aligned = horiz0 ? s0.getY() == s1.getY() : s0.getX() == s1.getX();
            if (!aligned) {
                if (horiz0) {
                    double midX = snapToGrid((s0.getX() + s1.getX()) / 2.0, grid);
                    pts.add(new Point2D(midX, s0.getY()));
                    pts.add(new Point2D(midX, s1.getY()));
                } else {
                    double midY = snapToGrid((s0.getY() + s1.getY()) / 2.0, grid);
                    pts.add(new Point2D(s0.getX(), midY));
                    pts.add(new Point2D(s1.getX(), midY));
                }
            }
        }

        pts.add(s1);
    }

    /** Kontrola, či presun z a do b prebieha v smere dir (nie opačne). */
    private static boolean signMatches(Point2D a, Point2D b, Point2D dir) {
        double d = dir.getX() != 0 ? (b.getX() - a.getX()) * dir.getX() : (b.getY() - a.getY()) * dir.getY();
        return d >= 0;
    }

    /** Núdzový obchádzací manéver, keď oba pahýle mieria "od seba". */
    private static void detour(List<Point2D> pts, Point2D s0, Point2D dir0, Point2D s1, Point2D dir1, int grid) {
        Point2D e0 = s0.add(dir0.multiply(STUB));
        Point2D e1 = s1.add(dir1.multiply(STUB));
        pts.add(e0);
        addFreeElbow(pts, e0, e1, grid);
        pts.add(e1);
    }

    private static List<Point2D> collapseColinear(List<Point2D> pts) {
        List<Point2D> out = new ArrayList<>();
        for (Point2D p : pts) {
            if (!out.isEmpty() && out.get(out.size() - 1).equals(p)) continue;
            if (out.size() >= 2) {
                Point2D a = out.get(out.size() - 2), b = out.get(out.size() - 1);
                boolean sameLine = (a.getX() == b.getX() && b.getX() == p.getX())
                        || (a.getY() == b.getY() && b.getY() == p.getY());
                if (sameLine) out.remove(out.size() - 1);
            }
            out.add(p);
        }
        return out;
    }
}
