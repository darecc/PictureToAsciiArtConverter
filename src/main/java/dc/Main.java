package dc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    private static String DEFAULT_CHARS = "@Xx*+-':. ";

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);

        System.out.print("Get file name PNG (np. Darek.png): ");
        String inputFile = sc.nextLine().trim();
        inputFile = inputFile + ".png";
        String outputFile = inputFile + ".out.png";

        System.out.print("Get columns (eg. 240–680, Enter = 360): ");
        int cols = getIntInput(sc, 360, 240, 800);

        System.out.print("Gamma correction (0.6–1.2, Enter = 0.8): ");
        double gamma = getDoubleInput(sc, 0.8, 0.5, 1.8);

        System.out.print("Negative (Y/N): ");
        boolean negative = sc.nextLine().trim().equalsIgnoreCase("Y");

        String fontName = "Courier New";
        int fontSize = 8;

        BufferedImage inputImage = ImageIO.read(new File(inputFile));

        String[] asciiData = imageToAscii(inputImage, cols, DEFAULT_CHARS, gamma, negative);
        String[] asciiData2 = imageToAsciiEdges(inputImage, cols);
        BufferedImage outputImage = renderAsciiToImage(asciiData, fontName, fontSize, negative);
        BufferedImage outputImage2 = renderAsciiToImage(asciiData2, fontName, fontSize, negative);
        ImageIO.write(outputImage, "png", new File(outputFile));
        System.out.println("✅ Ready! Written as " + outputFile);
        ImageIO.write(outputImage2, "png", new File(outputFile + ".png"));
        System.out.println("✅ Ready! Written as linear" + outputFile + ".png");
    }

    private static String[] imageToAscii(BufferedImage image, int cols, String chars, double gamma, boolean negative) {
        int width = image.getWidth();
        int height = image.getHeight();
        double aspectRatio = 0.5;
        int rows = (int) (height * cols / (double) width * aspectRatio);

        String[] asciiRows = new String[rows];

        for (int y = 0; y < rows; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < cols; x++) {
                int pixelX = x * width / cols;
                int pixelY = y * height / rows;

                int rgb = image.getRGB(pixelX, pixelY);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // percepcyjna jasność
                double pixel = 0.299 * r + 0.587 * g + 0.114 * b;

                // delikatna korekta gamma (Twoja metoda)
                pixel = Math.pow(pixel / 255.0, gamma) * 255.0;

                if (negative) pixel = 255 - pixel;

                int index = (int) ((pixel / 255.0) * (chars.length() - 1));
                char c = chars.charAt(index);
                sb.append(c);
            }
            asciiRows[y] = sb.toString();
        }
        return asciiRows;
    }

    private static BufferedImage renderAsciiToImage(String[] asciiRows, String fontName, int fontSize, boolean negative) {
        int cols = asciiRows[0].length();
        int rows = asciiRows.length;

        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = temp.createGraphics();
        g2d.setFont(new Font(fontName, Font.PLAIN, fontSize));
        FontMetrics fm = g2d.getFontMetrics();
        int charWidth = fm.charWidth('A');
        int charHeight = fm.getHeight();
        g2d.dispose();

        BufferedImage output = new BufferedImage(cols * charWidth, rows * charHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();

        if (negative) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, output.getWidth(), output.getHeight());
            g.setColor(Color.WHITE);
        } else {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, output.getWidth(), output.getHeight());
            g.setColor(Color.BLACK);
        }

        g.setFont(new Font(fontName, Font.PLAIN, fontSize));

        for (int y = 0; y < rows; y++) {
            g.drawString(asciiRows[y], 0, (y + 1) * charHeight);
        }

        g.dispose();
        return output;
    }

    private static int getIntInput(Scanner sc, int defaultValue, int min, int max) {
        try {
            String s = sc.nextLine().trim();
            if (s.isEmpty()) return defaultValue;
            int v = Integer.parseInt(s);
            return Math.max(min, Math.min(max, v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static double getDoubleInput(Scanner sc, double defaultValue, double min, double max) {
        try {
            String s = sc.nextLine().trim();
            if (s.isEmpty()) return defaultValue;
            double v = Double.parseDouble(s);
            return Math.max(min, Math.min(max, v));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** Tworzy ASCII-art oparty na krawędziach obrazu (linie / \ - | .)
     *  Poprawiona wersja: wypełnia wszystkie wiersze i ma dokładnie `cols` znaków na wiersz.
     */
    private static String[] imageToAsciiEdges(BufferedImage image, int cols) {
        int width = image.getWidth();
        int height = image.getHeight();
        double aspectRatio = 0.5;
        int rows = (int) (height * cols / (double) width * aspectRatio);
        if (rows < 3) rows = 3;
        if (cols < 3) cols = 3;

        // przeskaluj obraz do mniejszej rozdzielczości dla ASCII (GRAY)
        BufferedImage scaled = new BufferedImage(cols, rows, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = scaled.createGraphics();
        g2d.drawImage(image, 0, 0, cols, rows, null);
        g2d.dispose();

        // wczytaj jasności pikseli
        int[][] gray = new int[rows][cols];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int rgb = scaled.getRGB(x, y) & 0xFF; // grayscale => lower byte
                gray[y][x] = rgb;
            }
        }

        String[] ascii = new String[rows];

        // próg magnitudy krawędzi - możesz go wyeksponować jako parametr
        final double EDGE_THRESHOLD = 40.0;

        for (int y = 0; y < rows; y++) {
            StringBuilder sb = new StringBuilder(cols);
            for (int x = 0; x < cols; x++) {
                // obsługa brzegów: użyj klamping (powiel skrajny piksel)
                int xm1 = Math.max(0, x - 1), xp1 = Math.min(cols - 1, x + 1);
                int ym1 = Math.max(0, y - 1), yp1 = Math.min(rows - 1, y + 1);

                int gx =
                        -1 * gray[ym1][xm1] + 1 * gray[ym1][xp1]
                                + -2 * gray[y][xm1]     + 2 * gray[y][xp1]
                                + -1 * gray[yp1][xm1] + 1 * gray[yp1][xp1];

                int gy =
                        -1 * gray[ym1][xm1] + -2 * gray[ym1][x] + -1 * gray[ym1][xp1]
                                +  1 * gray[yp1][xm1] +  2 * gray[yp1][x] +  1 * gray[yp1][xp1];

                double mag = Math.sqrt((double)gx * gx + (double)gy * gy);

                char c;
                if (mag < EDGE_THRESHOLD) {
                    c = ' '; // brak wyraźnej krawędzi
                } else {
                    double angle = Math.toDegrees(Math.atan2(gy, gx));
                    if (angle < 0) angle += 180; // rozmiar kąta 0..180 wystarczy
                    // rozdzielamy kąty w 4 sektory: -, /, |, \
                    if (angle < 22.5 || angle >= 157.5) c = '-';
                    else if (angle < 67.5) c = '/';
                    else if (angle < 112.5) c = '|';
                    else c = '\\';
                }
                sb.append(c);
            }
            // zapewniamy że każda linia ma dokładnie cols znaków
            while (sb.length() < cols) sb.append(' ');
            if (sb.length() > cols) sb.setLength(cols);
            ascii[y] = sb.toString();
        }

        return ascii;
    }
}
