package pdc.conversor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by NEGU on 6/3/2017.
 */


public class FlippedImage {
    private int fileSize;                       // File Size
    private int initialPositionInMessage;
    private ByteArrayOutputStream flippedImage; // Flipped Image
    private String type;

    private int count;                          // byte counter -- just for debug

    public FlippedImage(int initialPositionInMessage, String type) {
        this.initialPositionInMessage = initialPositionInMessage;
        this.flippedImage = new ByteArrayOutputStream();
        this.type = type;
        this.count = 0;
    }

    public void putByte(byte b) {
        flippedImage.write(b);
        count ++;
    }

    public int getInitialPositionInMessage() { return this.initialPositionInMessage; }
    public byte[] getConvertedImage() throws IOException {
        return flip(this.flippedImage.toByteArray());
    }

    private byte[] flip(byte[] orig) throws IOException {
        // Esta es la posta! lo que anda!
        ByteArrayInputStream data = new ByteArrayInputStream(orig);
        BufferedImage image;
        image = ImageIO.read(data);

        BufferedImage i2 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

        int width = image.getWidth();
        int height = image.getHeight();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                i2.setRGB(col, (height - 1) - row, image.getRGB(col, row));
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (this.type.equals("PNG")) {
            ImageIO.write( i2, "png", baos );
        } else {
            ImageIO.write( i2, "jpeg", baos );
        }
        baos.flush();
        return baos.toByteArray();
    }
}