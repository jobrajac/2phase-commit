import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
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
    private final int TIMEOUT = 2000;
    private int messageDelay = 0;
    String logsFolderPath;

    Client(int PORT, String logsFolderPath) {
        this.logsFolderPath = logsFolderPath;
        this.server = new WSServer(PORT, messageQueue, queueLock);
        listener = new Thread(server);
        listener.start();
    }
    @Override

    // Polls queue for new received messages.
    public void run() {
        try {
            while(true) {
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


    // After a message is received it needs to be handled.
    abstract void handleMessage(Message msg);

    public void setMessageDelay(int delay) {
        this.messageDelay = delay;
    }
    // Append text to logfile
    void appendLog(String append, int trans_id) {
        try {
            File log = new File(logsFolderPath + trans_id + ".txt");
            log.createNewFile(); // if file already exists this will do nothing
            FileWriter writer = new FileWriter(log, true);
            writer.append(append + "\n");
            writer.close();

        }
        catch (Exception e) {
            System.out.println("Error writing to logfile.");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Localized: " + e.getLocalizedMessage());
            System.err.println("Stack Trace: " + e.getStackTrace());
        }
    }


    // Save object to file
    void saveFile(Object object, String path) {
        try {
            FileOutputStream fout = new FileOutputStream(path);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(object);
            oos.close();
            fout.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }


    // Send websocket message
    // Has a timeout if a connection can not be made.
    boolean sendMessage(String HOST, int PORT, Message message) {
        try {
            if(messageDelay != 0) {
                Thread.sleep(messageDelay);
            }
            // Create the socket
            Socket clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);

            // Create the input & output streams to the server
            ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

            outputStream.writeObject(message);
            outputStream.flush();
            outputStream.close();
            clientSocket.close();
            return true;

        }
        catch (Exception e) {
            System.err.println("Client Error: " + e.getMessage());
            System.err.println("Localized: " + e.getLocalizedMessage());
            System.err.println("Stack Trace: " + e.getStackTrace());
            return false;
        }
    }
}
