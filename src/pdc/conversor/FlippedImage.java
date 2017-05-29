package pdc.conversor;

/**
 * Created by sebastian on 5/29/17.
 */
public class FlippedImage {

    private byte[] convertedImage;
    private int imageSize;
    private int initialPositionInMessage;
    private int byteCount, reverseCount;
    private byte[] pixel;
    private int turn;

    public FlippedImage(int imageSize, int initialPositionInMessage) {
        this.imageSize = imageSize;
        this.initialPositionInMessage = initialPositionInMessage;
        this.convertedImage = new byte[this.imageSize];
        this.byteCount = 0;
        this.reverseCount = imageSize - 1;
        this.pixel = new byte[3];
        this.turn = 0;
    }

    public void putByte(byte b, int pos) {

        if (turn >= 2) {
            if (this.byteCount < 3) {
                this.pixel[byteCount++] = b;
            }

            if (this.byteCount == 3) {
                for (int i = 0; i < 3; i++) {
                    this.convertedImage[reverseCount - 2 + i] = this.pixel[i];
                }
                this.byteCount = 0;
                this.reverseCount -= 3;
            }
        } else {
            this.convertedImage[pos-this.initialPositionInMessage] = b;
            System.out.println(String.format("%02X ", b));
        }

        if (b == '\n') {
            this.turn++;
        }
    }


    public int getInitialPositionInMessage() { return this.initialPositionInMessage; }
    public byte[] getConvertedImage() { return this.convertedImage; }
}
