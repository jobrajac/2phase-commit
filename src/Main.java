import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        // Set a scenario. Decide what the nodes will reply.
        HashMap<String, HashMap<messageTypes, messageTypes>> debugAnswers = new HashMap<>();

        HashMap<messageTypes, messageTypes> aAnswers = new HashMap<>();
        aAnswers.put(messageTypes.START, messageTypes.START_OK);
        aAnswers.put(messageTypes.COMMIT, messageTypes.COMMIT_OK);
        aAnswers.put(messageTypes.ROLLBACK, messageTypes.ROLLBACK_OK);
        aAnswers.put(messageTypes.UNDO, messageTypes.UNDO_FAIL);
        debugAnswers.put(Participant.A.name(), aAnswers);

        HashMap<messageTypes, messageTypes> bAnswers = new HashMap<>();
        bAnswers.put(messageTypes.START, messageTypes.START_OK);
        bAnswers.put(messageTypes.COMMIT, messageTypes.COMMIT_FAIL);
        bAnswers.put(messageTypes.ROLLBACK, messageTypes.ROLLBACK_OK);
        bAnswers.put(messageTypes.UNDO, messageTypes.UNDO_OK);
        debugAnswers.put(Participant.B.name(), bAnswers);

        Participant[] rm = new Participant[]{Participant.A, Participant.B};
        Thread tm = new Thread(new TaskManager(Participant.TM.getPort(), rm));
        tm.start();
        Thread a = new Thread(new ResourceManager(Participant.A.getPort(), Participant.TM.getHost(), Participant.TM.getPort(), "resources/a/", "saved_states/a/"));
        a.start();
        Thread b = new Thread(new ResourceManager(Participant.B.getPort(), Participant.TM.getHost(), Participant.TM.getPort(), "resources/b/", "saved_states/b/"));
        b.start();
        try {
            // Create the socket
            Socket clientSocket = new Socket(Participant.TM.getHost(), Participant.TM.getPort());
            // Create the input & output streams to the server
            ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            Message send = new Message(2, Participant.OUTSIDE, messageTypes.TRANSACTION);
            // Adding data to message
            send.setData("INSERT THIS INTO DATABASE");
//            // Setting the answers defined above.
//            send.setDebug(true);
//            send.setDebugAnswers(debugAnswers);

            outputStream.writeObject(send);
            outputStream.flush();
            outputStream.close();
            clientSocket.close();

        } catch (Exception err) {
            System.err.println("Client Error: " + err.getMessage());
            System.err.println("Localized: " + err.getLocalizedMessage());
            System.err.println("Stack Trace: " + err.getStackTrace());
        }
    }

}
