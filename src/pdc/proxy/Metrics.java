package pdc.proxy;

/**
 * Created by sebastian on 5/25/17.
 */
public class Metrics {

    private long totalAccesses;
    private long receivedBytes;
    private long transferedBytes;
    private long convertedCharacters;
    private long flippedImages;
    private long getRequests;
    private long postRequests;
    private static Metrics instance;

    private Metrics() {
        this.totalAccesses = 0;
        this.receivedBytes = 0;
        this.transferedBytes = 0;
        this.convertedCharacters = 0;
        this.flippedImages = 0;
        this.getRequests = 0;
        this.postRequests = 0;
    }

    public static Metrics getInstance() {
        if (instance == null) {
            instance = new Metrics();
        }
        return instance;
    }

    public String getTotalAccesses() {
        return String.format("Total acceses: %d",totalAccesses);
    }

    public String getReceivedBytes() {
        return String.format("Received Bytes: %d",receivedBytes);
    }

    public String getTransferredBytes() {
        return String.format("Transferred Bytes: %d",transferedBytes);
    }

    public String getConvertedChars() {
        return String.format("Converted Chars: %d",convertedCharacters);
    }

    public String getFlippedImages() {
        return String.format("Flipped Images: %d",flippedImages);
    }

    public String getMethodHistograms() {
        // FIXME : El enunciado pide soportar más que sólo GET y POST....
        return String.format("GET requests: %d\nPOST requests: %d",getRequests, postRequests);
    }

    public String getAll() {
        return String.format("%s\r\n%s\r\n%s\r\n%s\r\n%s\r\n%s",
                getReceivedBytes(),
                getTotalAccesses(),
                getTransferredBytes(),
                getConvertedChars(),
                getFlippedImages(),
                getMethodHistograms());
    }

    public void addAccess() {
        this.totalAccesses++;
    }

    public void addReceivedBytes(long receivedBytes) {
        this.receivedBytes += receivedBytes;
    }

    public void addTransferedBytes(long transferedBytes) {
        this.transferedBytes += transferedBytes;
    }

    public void addConvertedCharacter() {
        this.convertedCharacters ++;
    }

    public void addFlippedImage() {
        this.flippedImages ++;
    }

    public void addGetRequest() {
        this.getRequests ++;
    }

    public void addPostRequest() {
        this.postRequests ++;
    }
}
