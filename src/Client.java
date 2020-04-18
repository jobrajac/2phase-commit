import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract class Client {
    private Queue<Message> messageQueue = new LinkedList<>();
    private final Lock queueLock = new ReentrantLock();
    private final Thread listener;

    public Client(int PORT) {
        WSServer server = new WSServer(PORT, messageQueue, queueLock);
        listener = new Thread(server);
        listener.start();
        this.checkQueue();
    }
    private void checkQueue() {
        while(true) {
            queueLock.lock();
            Message msg = messageQueue.poll();
            queueLock.unlock();
            if (msg != null) {
                handleMessage(msg);
            }
        }
    }
    abstract void handleMessage(Message msg);

    //TODO lag metode for å lagre fil - state

    public void sendMessage(String HOST, int PORT, Message message) {
        System.out.println("Sending message");
        try {
            // Create the socket
            Socket clientSocket = new Socket(HOST, PORT);
            // Create the input & output streams to the server
            ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

            outputStream.writeObject(message);
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
