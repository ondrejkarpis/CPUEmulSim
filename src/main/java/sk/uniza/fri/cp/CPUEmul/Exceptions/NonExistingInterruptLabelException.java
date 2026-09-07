package sk.uniza.fri.cp.CPUEmul.Exceptions;

/**
 * Výnimka pri neexistujúcom návestí prerušenia.
 *
 * @author Tomáš Hianik
 * @created 21.2.2017.
 */
public class NonExistingInterruptLabelException extends Exception {

    /**
     * Vytvorenie výnimky s číslom prerušenia v decimálnej sústave.
     *
     * @param intNumber Číslo prerušenia od 0 do 15.
     */
    public NonExistingInterruptLabelException(int intNumber) {
        // Karpis - uprava nazvu na int00 az int15
        super("Neexistujúce návestie prerušenia int" + ((intNumber < 10) ? "0" : "") + intNumber);
    }
}
