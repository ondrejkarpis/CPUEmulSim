package sk.uniza.fri.cp.App.CPUControl.CodeEditor;

import org.fxmisc.richtext.StyleClassedTextArea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Pomocné funkcie pre editor kódu využívajúci RichTextFX.
 *
 * @author Tomáš Hianik
 * @created 13.2.2017.
 */
public final class RichTextFXHelpers {
    private RichTextFXHelpers(){}

    /**
     * Pridanie štýlu paragrafu.
     *
     * @param ta TextArea s paragrafom.
     * @param paragraphIndex Index paragrafu.
     * @param style Štýl, ktorý sa má pridať paragrafu.
     * @return Ture ak sa poradilo nájsť paragraf, false inak.
     */
    public static boolean addParagraphStyle(StyleClassedTextArea ta, int paragraphIndex, String style){
        try {
            Collection<String> styles = new ArrayList<>(ta.getParagraph(paragraphIndex).getParagraphStyle());
            styles.add(style);
            ta.setParagraphStyle(paragraphIndex, styles);

            return  true;
        } catch (IndexOutOfBoundsException e){
            return false;
        }
    }

    /**
     * Odobratie štýlu paragrafu.
     *
     * @param ta TextArea s paragrafom.
     * @param paragraphIndex Index paragrafu.
     * @param style Štýl, ktorý sa má paragrafu odobrať.
     * @return True ak sa podarilo paragrafu odobrať štýl, false inak.
     */
    public static boolean tryRemoveParagraphStyle(StyleClassedTextArea ta, int paragraphIndex, String style){
        try {
            //zober vsetky styly, ktore su aplikovane na paragraf okrem odoberaneho stylu
            java.util.List<String> styles = ta.getParagraph(paragraphIndex).getParagraphStyle()
                    .stream().filter(st-> !st.equals(style)).collect(Collectors.toList());
            //nastav vsetky okrem daneho stylu
            ta.setParagraphStyle(paragraphIndex, styles);

            return true;
        } catch (IndexOutOfBoundsException e){
            return false;
        }
    }
}
