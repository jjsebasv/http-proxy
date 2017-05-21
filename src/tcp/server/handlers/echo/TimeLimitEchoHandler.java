package tcp.server.handlers.echo;

/**
 * Created by sebastian on 5/21/17.
 */

import tcp.server.handlers.ConnectionHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Connection handler que implementa un servicio ECHO limitado en tiempo
 */
public class TimeLimitEchoHandler implements ConnectionHandler {
    private static final int BUFSIZE = 32; // Size (bytes) of buffer
    private static final String TIMELIMIT = "10000"; // Default limit (ms)
    private static final String TIMELIMITPROP = "Timelimit"; // Property
    private int timelimit;

    /**
     * Creates the TimeLimitEchoHandler.
     */
    public TimeLimitEchoHandler() {
        // Get the time limit from the System properties or take the default
        timelimit = Integer.parseInt(System.getProperty(TIMELIMITPROP, TIMELIMIT));
    }

    /**
     * @see ConnectionHandler#handle(Socket)
     */
    @Override
    public void handle(Socket s) throws IOException {
        try {
            // Get the input
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            int recvMsgSize;
            int totalBytesEchoed = 0; // Bytes received from client
            byte[] echoBuffer = new byte[BUFSIZE]; // Receive buffer
            long endTime = System.currentTimeMillis() + timelimit;
            int timeBoundMillis = timelimit;
            s.setSoTimeout(timeBoundMillis);
            // Receive until client closes connection, indicated by -1
            while ((timeBoundMillis > 0) && // catch zero values
                    ((recvMsgSize = in.read(echoBuffer)) != -1)) {
                out.write(echoBuffer, 0, recvMsgSize);
                totalBytesEchoed += recvMsgSize;
                timeBoundMillis = (int) (endTime - System.currentTimeMillis());
                s.setSoTimeout(timeBoundMillis);
            }
            System.out.println("Client " + s.getRemoteSocketAddress() + ", echoed " + totalBytesEchoed
                    + " bytes.");
        } catch (IOException ex) {
            System.out.println("Exception in echo protocol: " + ex);
        }

    }

}