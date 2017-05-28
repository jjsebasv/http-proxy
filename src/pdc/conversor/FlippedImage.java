package pdc.conversor;

/**
 * Created by sebastian on 5/28/17.
 */
public class FlippedImage {

    private byte[] convertedImage;
    private int imageSize;
    private int initialPositionInMessage;
    private int enters, lastEnter;

    public FlippedImage(int imageSize, int initialPositionInMessage) {
        this.imageSize = imageSize;
        this.initialPositionInMessage = initialPositionInMessage;
        this.convertedImage = new byte[this.imageSize];
        this.enters = 0;
        this.lastEnter = 0;

    }

    public void putByte(int originalPosition, byte b) {
        this.convertedImage[(originalPosition - this.initialPositionInMessage)] = b;
        /*
        if (b == '\n' && enters <= 3) {
            enters++;
            this.lastEnter = originalPosition;
        } else if (enters > 3) {
            this.convertedImage[this.imageSize - (originalPosition - this.lastEnter)] = b;
        } else {
            this.convertedImage[(originalPosition - this.initialPositionInMessage)] = b;
        }
        */
    }

    public int getInitialPositionInMessage() { return this.initialPositionInMessage; }
    public byte[] getConvertedImage() { return this.convertedImage; }
}
