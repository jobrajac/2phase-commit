import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.locks.Lock;

public class WSServer implements Runnable{
    private Queue<Message> messageQueue;
    private final Lock queueLock;
    private final int PORT;
    private ServerSocket serversocket;
    WSServer(int PORT, Queue<Message> messageQueue, Lock queueLock) {
        this.messageQueue = messageQueue;
        this.queueLock = queueLock;
        this.PORT = PORT;
    }
    void close() {
        try {
            serversocket.close();
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }
    @Override
    public void run() {
            try {
                System.out.println("Waiting for messages on port: " + PORT);
                serversocket = new ServerSocket(PORT);
                while (true) {
                    if(Thread.interrupted()) {
                        System.out.println("Listener interrupted. Quitting.");
                        return;
                    }
                    // Create socket for client
                    Socket clientSocket = serversocket.accept();
                    // Create input and output streams to client
                    ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());

                    /* Create Message object and retrieve information */
                    Message in = (Message) inputStream.readObject();
                    queueLock.lock();
                    messageQueue.add(in);
                    queueLock.unlock();
                    clientSocket.close();
                }

            } catch (Exception e) {
                System.err.println("Server Error: " + e.getMessage());
                System.err.println("Localized: " + e.getLocalizedMessage());
                System.err.println("Stack Trace: " + e.getStackTrace());
                System.err.println("To String: " + e.toString());
            }
    }
}
