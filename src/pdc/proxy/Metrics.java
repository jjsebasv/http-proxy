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

    public static Metrics getInstance() {
        if (instance == null) {
            instance = new Metrics();
        }
        return instance;
    }

    public String getTotalAccesses() {
        return String.format("Total accesses: %d",totalAccesses);
    }

    public String getReceivedBytes() {
        return String.format("Received Bytes: %d",receivedBytes);
    }

    public String getTransferredBytes() {
        return String.format("Transferred Bytes: %d", transferredBytes);
    }

    public String getConvertedChars() {
        return String.format("Converted Chars: %d",convertedCharacters);
    }

    public String getFlippedImages() {
        return String.format("Flipped Images: %d",flippedImages);
    }

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

    public void addTransferredBytes(long transferredBytes) {
        this.transferredBytes += transferredBytes;
    }

    public void addConvertedCharacter() {
        this.convertedCharacters ++;
    }

    public void addMethod(String method) {
        Long cant = this.methods.containsKey(method) ? this.methods.get(method) : 0;
        this.methods.put(method, cant + 1);
    }

    public void addFlippedImage() {
        this.flippedImages ++;
    }
    
}
