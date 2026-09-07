package sk.uniza.fri.cp.App.CPUControl;

/**
 * Trieda obsahujúca statické metódy na prevod hodnôt do decimálnej, binárnej hexadecimálnej sústavy alebo
 * do ASCII. Obsahuje tiež enum možných výsledných prevodov.
 *
 * @author Tomáš Hianik
 * @created  17.2.2017.
 */
final class DataRepresentation {

    /**
     * Vymenovanie možných reprezentácií.
     */
    public enum eRepresentation{
        Dec,Hex,Bin,ASCII
    }

    /**
     * Prevod hodnoty byte do požadovanej reprezentácie.
     *
     * @param value Hodnota na prevod
     * @param displayState Požadovaná reprezentácia
     * @return String s reprezentáciou hodnoty v danej sústave, resp. ASCII znak
     */
    static String getDisplayRepresentation(byte value, eRepresentation displayState){
        switch (displayState){
            case Dec:
                return Integer.toString(value & 0xFF );
            case Bin:
                return Integer.toBinaryString((value & 0xFF)  + 0x100).substring(1);
            case Hex:
                return "0x" + Integer.toHexString((value & 0xFF)  + 0x100).substring(1).toUpperCase();
            case ASCII:
                switch (value) {
                    case 8:
                        return "[BACKSPACE]";
                    case 9:
                        return "[TAB]";
                    case 10:
                        return "[LF]";
                    case 13:
                        return "[CR]";
                    case 27:
                        return "[ESC]";
                    case 32:
                        return "[SPACE]";
                    case 127:
                        return "[DEL]";
                    default:
                        return String.valueOf((char) value);
                }
        }
        return "";
    }

    /**
     * Prevod hodnoty short do požadovanej reprezentácie.
     *
     * @param value Hodnota na prevod
     * @param displayState Požadovaná reprezentácia
     * @return String s reprezentáciou hodnoty v danej sústave, resp. ASCII znak
     */
    static String getDisplayRepresentation(short value, eRepresentation displayState){
        switch (displayState){
            case Dec:
                return Integer.toString(value & 0xFFFF);
            case Bin:
                return Integer.toBinaryString((value & 0xFFFF) + 0x10000).substring(1);
            case Hex:
                return "0x" + Integer.toHexString((value & 0xFFFF) + 0x10000).substring(1).toUpperCase();
            case ASCII:
                char L = (char) value;
                char H = (char) (value >> 4);
                return String.format("%c%c", H, L);
        }
        return "";
    }

    private DataRepresentation(){}
}
