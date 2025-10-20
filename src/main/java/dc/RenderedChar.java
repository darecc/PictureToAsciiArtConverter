package dc;

import java.awt.image.BufferedImage;

public class RenderedChar {
    char c;
    double avgInk;
    BufferedImage image;
    RenderedChar(char c, double avgInk, BufferedImage image) {
        this.c = c; this.avgInk = avgInk; this.image = image;
    }
}
