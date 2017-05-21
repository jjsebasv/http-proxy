package tcp.client.echo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by sebastian on 5/21/17.
 */
public class TCPEchoClient {
    private static final String SERVER = "127.0.0.1";
    private static final int PORT = 9090;
    private static final String WELCOME_MESSAGE = "Welcome to Echo Server";
    private static final String GOODBYE_MESSAGE = "Thanks for using Echo Server";
    private static final int ALLOWED_MSG_LEN = 15;

    public static void main(String[] args) throws IOException {

        // Server name or IP address
        String server = SERVER;

        // Convert argument String to bytes using the default character encoding
        byte[] data = (WELCOME_MESSAGE + "\n").getBytes();
        int servPort = PORT;

        int totalReceivedBytes = 0;
        // Receive the same string back from the server
        int receivedBytes = 0;

        // Create socket that is connected to server on specified port
        Socket socket = new Socket(server, servPort);
        System.out.println("Connected to server...sending echo string");

        InputStream in = socket.getInputStream(); // From the Server to here
        OutputStream out = socket.getOutputStream(); // From here to the Server

        // Send the encoded string to the server
        out.write(data);
        out.write(("You are allowed to send messages up to " + ALLOWED_MSG_LEN + " chars\nConnection is closed when 'close' is sent\n").getBytes());

        // Bytes received in last read
        while (receivedBytes < data.length) {

            byte[] received = new byte[ALLOWED_MSG_LEN];
            receivedBytes = in.read(received) - 1; // to take the \n away

            if (receivedBytes == -1) {
                throw new SocketException("Connection closed prematurely");
            }

            byte[] receivedAllocated = cast(received, receivedBytes);
            String receivedString = new String(receivedAllocated);
            System.out.println("received " + receivedString);
            totalReceivedBytes += receivedBytes;
            out.write((receivedString + " to you!\n").getBytes());

            if (receivedString.toLowerCase().equals("close")) break;
        }
        // data array is full
        System.out.println("Bytes received: " + totalReceivedBytes);
        out.write((GOODBYE_MESSAGE + "\n").getBytes());
        // Close the socket and its streams
        socket.close();
    }

    private static byte[] cast(byte[] byteArray, int receivedBytes) {
        byte[] answer = new byte[receivedBytes];
        for (int i = 0; i < receivedBytes; i++) {
            answer[i] = byteArray[i];
        }
        return answer;
    }


}