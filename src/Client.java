import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract class Client implements Runnable {
    private Queue<Message> messageQueue = new LinkedList<>();
    private final Lock queueLock = new ReentrantLock();
    private Thread listener;
    WSServer server;

    Client(int PORT) {
        this.server = new WSServer(PORT, messageQueue, queueLock);
        listener = new Thread(server);
        listener.start();
    }
    @Override
    public void run() {
        try {
            while(true) {
                Thread.sleep(1000);
                if(Thread.interrupted()) {
                    System.out.println("Client interrupted. Quitting.");
                    server.close();
                    return;

                }
                queueLock.lock();
                Message msg = messageQueue.poll();
                queueLock.unlock();
                Thread.sleep(100);
                if (msg != null) {
                    System.out.println(msg.getClient_id().toString() + ": " + msg.getMessage().toString());
                    handleMessage(msg);
                }
            }
        }
        catch (InterruptedException e) {
            System.out.println("Client interrupted");
            server.close();
            return;
        }

    }
    abstract void handleMessage(Message msg);

    void saveFile(Object object, String path) {
        try {
            FileOutputStream fout = new FileOutputStream(path);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(object);
            oos.close();
            fout.close();
        }
        catch (Exception e) { // TODO fix this
            e.printStackTrace();
        }

    }
//    Object[] readFiles(String directory){
//        return Object[];
//    }
    void sendMessage(String HOST, int PORT, Message message) {
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
