package pdc.conversor;

import pdc.proxy.Metrics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by sebastian on 5/27/17.
 */
public class Conversor {

    public static byte leet(char a) {
        switch (a) {
            case 'a':
                Metrics.getInstance().addConvertedCharacter();
                return (byte)'4';
            case 'e':
                Metrics.getInstance().addConvertedCharacter();
                return (byte)'3';
            case 'i':
                Metrics.getInstance().addConvertedCharacter();
                return (byte)'1';
            case 'o':
                Metrics.getInstance().addConvertedCharacter();
                return (byte)'0';
            case 'c':
                Metrics.getInstance().addConvertedCharacter();
                return (byte)'<';
            default:
                return (byte)a;
        }
    }

    public static void flipImage(FlippedImage image, ByteBuffer message, int position) {
        ByteArrayInputStream bais = new ByteArrayInputStream(image.getConvertedImage());
        /*
        try {
            BufferedImage bi = ImageIO.read(bais);

            AffineTransform at = new AffineTransform();
            at.concatenate(AffineTransform.getScaleInstance(1, -1));
            at.concatenate(AffineTransform.getTranslateInstance(0, -bi.getHeight()));

            File outputfile = new File("image2.png");
            ImageIO.write(createTransformed(bi, at), "png", outputfile);
            bais.close();
        } catch (IOException e) {
            e.printStackTrace();
        } */
        int initialPos = image.getInitialPositionInMessage();
        for (int i = initialPos; i < position; i++) {
            message.put(i, image.getConvertedImage()[i-initialPos]);
        }
        FileOutputStream fos;
        try {
            fos = new FileOutputStream("image"+position+".png");
            fos.write(image.getConvertedImage());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static BufferedImage createTransformed (BufferedImage image, AffineTransform at) {
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.transform(at);
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return newImage;
    }

}
