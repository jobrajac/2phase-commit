import java.io.File;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

public class Main {
    public void cleanFolder(String path) {
        File dir = new File(path);
        for(File file: dir.listFiles())
            if (!file.isDirectory())
                file.delete();
    }
    public void cleanAllFolders() {
        cleanFolder("logs/a");
        cleanFolder("logs/b");
        cleanFolder("logs/tm");
        cleanFolder("saved_states/a");
        cleanFolder("saved_states/b");
        cleanFolder("saved_states/tm");
    }
    public HashMap<String, HashMap<messageTypes, messageTypes>> scenarioAllOk() {
        HashMap<String, HashMap<messageTypes, messageTypes>> debugAnswers = new HashMap<>();

        HashMap<messageTypes, messageTypes> aAnswers = new HashMap<>();
        aAnswers.put(messageTypes.START, messageTypes.START_OK);
        aAnswers.put(messageTypes.COMMIT, messageTypes.COMMIT_OK);
        aAnswers.put(messageTypes.ROLLBACK, messageTypes.ROLLBACK_OK);
        aAnswers.put(messageTypes.UNDO, messageTypes.UNDO_OK);
        debugAnswers.put(Participant.A.name(), aAnswers);

        HashMap<messageTypes, messageTypes> bAnswers = new HashMap<>();
        bAnswers.put(messageTypes.START, messageTypes.START_OK);
        bAnswers.put(messageTypes.COMMIT, messageTypes.COMMIT_OK);
        bAnswers.put(messageTypes.ROLLBACK, messageTypes.ROLLBACK_OK);
        bAnswers.put(messageTypes.UNDO, messageTypes.UNDO_OK);
        debugAnswers.put(Participant.B.name(), bAnswers);

        return debugAnswers;
    }
    public static void main(String[] args) {

        Main main = new Main();
        main.cleanAllFolders();
        // Setup of nodes
        Participant[] rm = new Participant[]{Participant.A, Participant.B};
        Thread tm = new Thread(new TaskManager(Participant.TM.getPort(), rm));
        tm.start();
        Thread a = new Thread(new ResourceManager(Participant.A.getPort(), Participant.TM.getHost(), Participant.TM.getPort(), "resources/a/booksA.txt", "saved_states/a/", "logs/a/"));
        a.start();
        Thread b = new Thread(new ResourceManager(Participant.B.getPort(), Participant.TM.getHost(), Participant.TM.getPort(), "resources/b/booksB.txt", "saved_states/b/", "logs/b/"));
        b.start();

        try {
            // Create the socket
            Socket clientSocket = new Socket(Participant.TM.getHost(), Participant.TM.getPort());
            // Create the input & output streams to the server
            ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());


            // Scenario 1. Every node replies with OK
            HashMap<String, HashMap<messageTypes, messageTypes>> debugAnswers = main.scenarioAllOk();
            Message send = new Message(1, Participant.OUTSIDE, messageTypes.TRANSACTION);
            // Adding data to message
            send.setData("The Lion, the Witch and the Wardrobe");
            // Setting the RM's predefined answers
//            send.setDebug(true);
//            send.setDebugAnswers(debugAnswers);

            outputStream.writeObject(send);
            outputStream.flush();

            Thread.sleep(5000);
//            tm.interrupt();
//            tm = new Thread(new TaskManager(Participant.TM.getPort(), rm));
//            tm.start();

            tm.interrupt();
            a.interrupt();
            b.interrupt();

            outputStream.close();
            clientSocket.close();
        }
        catch (Exception err) {
            System.err.println("Client Error: " + err.getMessage());
            System.err.println("Localized: " + err.getLocalizedMessage());
            System.err.println("Stack Trace: " + err.getStackTrace());
        }
    }

}