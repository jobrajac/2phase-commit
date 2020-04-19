import java.io.ObjectOutputStream;
import java.net.Socket;

public class Tester {
    public static void main(String[] args) {
        try {
            // Create the socket
            Socket clientSocket = new Socket(Participant.TM.getHost(), Participant.TM.getPort());
            // Create the input & output streams to the server
            ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            Message send = new Message(1, Participant.OUTSIDE, messageTypes.TRANSACTION);
            outputStream.writeObject(send);
            outputStream.flush();
            outputStream.close();
            clientSocket.close();

        } catch (Exception e) {
            System.err.println("Client Error: " + e.getMessage());
            System.err.println("Localized: " + e.getLocalizedMessage());
            System.err.println("Stack Trace: " + e.getStackTrace());
        }
    }
}
