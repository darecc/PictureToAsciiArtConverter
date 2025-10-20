package dc;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapa gęstości glyphów (znaków), używana do dopasowania znaków do jasności obrazu.
 */
public class GlyphMap {
    public final LinkedHashMap<Character, Double> charToDensity;
    public final int charWidth;
    public final int charHeight;

    public GlyphMap(LinkedHashMap<Character, Double> charToDensity, int charWidth, int charHeight) {
        this.charToDensity = charToDensity;
        this.charWidth = charWidth;
        this.charHeight = charHeight;
    }

    /** Znajdź znak o gęstości najbliższej podanej wartości (0..1). */
    public char findClosestChar(double target) {
        char best = ' ';
        double bestDiff = Double.MAX_VALUE;
        for (Map.Entry<Character, Double> e : charToDensity.entrySet()) {
            double diff = Math.abs(e.getValue() - target);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = e.getKey();
            }
        }
        return best;
    }
}