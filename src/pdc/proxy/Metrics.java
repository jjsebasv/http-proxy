package pdc.proxy;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sebastian on 5/25/17.
 */
public class Metrics {

    private long totalAccesses;
    private long receivedBytes;
    private long transferredBytes;
    private long convertedCharacters;
    private long flippedImages;
    private static Metrics instance;
    private Map<String, Long> methods;

    private Metrics() {
        this.totalAccesses = 0;
        this.receivedBytes = 0;
        this.transferredBytes = 0;
        this.convertedCharacters = 0;
        this.flippedImages = 0;
        this.methods = new HashMap<String, Long>();
    }

    /**
     * Returns a new Metrics object if it wasn't initialized before, or the previous instance.
     *
     * @return active instance of Metrics
     */
    public static Metrics getInstance() {
        if (instance == null) {
            instance = new Metrics();
        }
        return instance;
    }

    /**
     * Returns a string with the total ammount of accesses.
     *
     * @return string with number of accesses
     */
    public String getTotalAccesses() {
        return String.format("Total accesses: %d",totalAccesses);
    }

    /**
     * Returns a string with the total ammount of received bytes.
     *
     * @return string with number of received bytes
     */
    public String getReceivedBytes() {
        return String.format("Received Bytes: %d",receivedBytes);
    }

    /**
     * Returns a string with the total ammount of transferred bytes.
     *
     * @return string with number of transferred bytes
     */
    public String getTransferredBytes() {
        return String.format("Transferred Bytes: %d", transferredBytes);
    }

    /**
     * Returns a string with the ammount of characters converted.
     *
     * @return string with number of characters converted
     */
    public String getConvertedChars() {
        return String.format("Converted Chars: %d",convertedCharacters);
    }

    /**
     * Returns a string with the total ammount of images flipped.
     *
     * @return string with number of images flipped
     */
    public String getFlippedImages() {
        return String.format("Flipped Images: %d",flippedImages);
    }

    /**
     * Returns a string with the total ammount of HEAD, GET and POST realized.
     *
     * @return string with number of HEAD, GET and POST done
     */
    public String getMethodHistograms() {
        String answer = String.format("HEAD requests: %d\nGET requests: %d\nPOST requests: %d",
                methods.containsKey("HEAD") ? methods.get("HEAD") : 0,
                methods.containsKey("GET") ? methods.get("GET") : 0,
                methods.containsKey("POST") ? methods.get("POST") : 0);
        for (String method: methods.keySet()) {
            if (!method.equals("GET") && !method.equals("POST") && !method.equals("HEAD"))
                answer = answer + String.format("\n%s requests: %d", method, methods.get(method));
        }
        return answer;
    }

    /**
     * Returns a string detailing all metrics.
     *
     * @return string with all metrics
     */
    public String getAll() {
        return String.format("%s\r\n%s\r\n%s\r\n%s\r\n%s\r\n%s",
                getReceivedBytes(),
                getTotalAccesses(),
                getTransferredBytes(),
                getConvertedChars(),
                getFlippedImages(),
                getMethodHistograms());
    }

    /**
     * Adds one to the total number of accesses.
     */
    public void addAccess() {
        this.totalAccesses++;
    }

    /**
     * Adds certain number of received bytes to the total number of received bytes.
     *
     * @param receivedBytes
     */
    public void addReceivedBytes(long receivedBytes) {
        this.receivedBytes += receivedBytes;
    }

    /**
     * Adds certain number of transferred bytes to the total number of transferred bytes.
     *
     * @param transferredBytes
     */
    public void addTransferredBytes(long transferredBytes) {
        this.transferredBytes += transferredBytes;
    }

    /**
     * Adds one to the total number of converted characters.
     */
    public void addConvertedCharacter() {
        this.convertedCharacters ++;
    }

    /**
     * Adds one to the total number of times certain method was used.
     *
     * @param method
     */
    public void addMethod(String method) {
        Long cant = this.methods.containsKey(method) ? this.methods.get(method) : 0;
        this.methods.put(method, cant + 1);
    }

    public void addFlippedImage() {
        this.flippedImages ++;
    }
    
}
