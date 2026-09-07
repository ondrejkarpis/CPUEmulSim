package sk.uniza.fri.cp.CPUEmul.Exceptions;

/**
 * Výnimka pri nesprávnom zápise inštrukcie v kóde.
 * Obsahuje pole indexov chybných riadkov a pole s informáciami, o aké chyby sa na jednotlivých riadkoch jedná.
 *
 * @author Tomáš Hianik
 * @created 8.2.2017
 */
public class InvalidCodeLinesException extends Exception {

    private int[] lines; //pole s indexami riadkov
    private String[] errors; //pole s popisom chýb

    /**
     * Vytvorenie výnimky s indexami chybných riadkov, popisom chýb a vlastnou správou posielanou predkovi.
     *
     * @param lines Indexy chybných riadkov.
     * @param errors Popis k jednotlivým riadkom.
     * @param msg Správa posielaná konštruktoru predka.
     */
    public InvalidCodeLinesException(int[] lines, String[] errors, String msg){
        super(msg);
        this.lines = lines.clone();
        this.errors = errors.clone();
    }

    /**
     * Vytvorenie výnimky s indexami chybných riadkov a popisom chýb.
     *
     * @param lines Indexy chybných riadkov.
     * @param errors Popis k jednotlivým riadkom.
     */
    public InvalidCodeLinesException(int[] lines, String[] errors){
        super("Some lines of code are invalid!");
        this.lines = lines.clone();
        this.errors = errors.clone();
    }

    /**
     * Reťazec s výpisom chýb.
     *
     * @return Reťazec s výpisom chýb.
     */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (String str :
                errors) {
            out.append(str).append("\n");
        }

        return out.toString();
    }

    /**
     * Pole s indexami chybných riadkov.
     *
     * @return Indexy chybných riadkov.
     */
    public int[] getLines() {
        return lines.clone();
    }

    /**
     * Posle s popisom k riadkom.
     *
     * @return Popis k jednotlivým chybným riadkom.
     */
    public String[] getErrors() {
        return errors.clone();
    }
}
