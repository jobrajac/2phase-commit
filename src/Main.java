import java.io.File;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

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
    public HashMap<String, HashMap<messageTypes, messageTypes>> scenarioStartFail() {
        HashMap<String, HashMap<messageTypes, messageTypes>> debugAnswers = new HashMap<>();

        HashMap<messageTypes, messageTypes> aAnswers = new HashMap<>();
        aAnswers.put(messageTypes.START, messageTypes.START_FAIL);
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
    public HashMap<String, HashMap<messageTypes, messageTypes>> scenarioCommitUndoFail() {
        HashMap<String, HashMap<messageTypes, messageTypes>> debugAnswers = new HashMap<>();

        HashMap<messageTypes, messageTypes> aAnswers = new HashMap<>();
        aAnswers.put(messageTypes.START, messageTypes.START_OK);
        aAnswers.put(messageTypes.COMMIT, messageTypes.COMMIT_FAIL);
        aAnswers.put(messageTypes.ROLLBACK, messageTypes.ROLLBACK_OK);
        aAnswers.put(messageTypes.UNDO, messageTypes.UNDO_OK);
        debugAnswers.put(Participant.A.name(), aAnswers);

        HashMap<messageTypes, messageTypes> bAnswers = new HashMap<>();
        bAnswers.put(messageTypes.START, messageTypes.START_OK);
        bAnswers.put(messageTypes.COMMIT, messageTypes.COMMIT_OK);
        bAnswers.put(messageTypes.ROLLBACK, messageTypes.ROLLBACK_OK);
        bAnswers.put(messageTypes.UNDO, messageTypes.UNDO_FAIL);
        debugAnswers.put(Participant.B.name(), bAnswers);

        return debugAnswers;
    }
    public static void main(String[] args) {
        while(true) {
            try {

                Scanner in = new Scanner(System.in);
                System.out.println("Enter the example you want to run [1 - 8]");
                int ex = in.nextInt();

                System.out.println("Sletter alle logger og alle lagrede tilstander.");
                Main main = new Main();
                main.cleanAllFolders();

                // Setup of nodes
                Participant[] rm = new Participant[]{Participant.A, Participant.B};
                TaskManager taskManager = new TaskManager(Participant.TM.getPort(), rm);
                Thread tm = new Thread(taskManager);
                tm.start();
                Thread a = new Thread(new ResourceManager(Participant.A.getPort(), Participant.TM.getHost(), Participant.TM.getPort(), "resources/a/booksA.txt", "saved_states/a/", "logs/a/"));
                a.start();
                Thread b = new Thread(new ResourceManager(Participant.B.getPort(), Participant.TM.getHost(), Participant.TM.getPort(), "resources/b/booksB.txt", "saved_states/b/", "logs/b/"));
                b.start();

                // Create the socket
                Socket clientSocket = new Socket(Participant.TM.getHost(), Participant.TM.getPort());
                // Create the input & output streams to the server
                ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

                switch (ex) {
                    case 1:
                        // Example 1. Every node replies with OK
                        HashMap<String, HashMap<messageTypes, messageTypes>> debugAnswers = main.scenarioAllOk();
                        Message send = new Message(1, Participant.OUTSIDE, messageTypes.TRANSACTION);
                        // Adding data to message
                        send.setData("Does not matter when debug = true.");
                        // Setting the RM's predefined answers
                        send.setDebug(true);
                        send.setDebugAnswers(debugAnswers);

                        outputStream.writeObject(send);
                        outputStream.flush();
                        Thread.sleep(5000);
                        tm.interrupt();
                        a.interrupt();
                        b.interrupt();
                        break;
                    case 2:
                        // Example 2. A replies with START_FAIL
                        debugAnswers = main.scenarioStartFail();
                        send = new Message(2, Participant.OUTSIDE, messageTypes.TRANSACTION);
                        // Adding data to message
                        send.setData("Does not matter when debugging.");
                        // Setting the RM's predefined answers
                        send.setDebug(true);
                        send.setDebugAnswers(debugAnswers);

                        outputStream.writeObject(send);
                        outputStream.flush();
                        Thread.sleep(5000);
                        tm.interrupt();
                        a.interrupt();
                        b.interrupt();
                        break;
                    case 3:
                        // Example 3. A replies with COMMIT_FAIL, and B replies with UNDO_FAIL
                        debugAnswers = main.scenarioCommitUndoFail();
                        send = new Message(3, Participant.OUTSIDE, messageTypes.TRANSACTION);
                        // Adding data to message
                        send.setData("Does not matter when debugging.");
                        // Setting the RM's predefined answers
                        send.setDebug(true);
                        send.setDebugAnswers(debugAnswers);

                        outputStream.writeObject(send);
                        outputStream.flush();
                        Thread.sleep(5000);
                        tm.interrupt();
                        a.interrupt();
                        b.interrupt();
                        break;
                    case 4:
                        // Example 4. Real test
                        send = new Message(4, Participant.OUTSIDE, messageTypes.TRANSACTION);
                        // Adding data to message
                        send.setData("The Lion, the Witch and the Wardrobe");

                        outputStream.writeObject(send);
                        outputStream.flush();
                        Thread.sleep(5000);
                        tm.interrupt();
                        a.interrupt();
                        b.interrupt();
                        break;
                    case 5:
                        taskManager.setMessageDelay(2000);
                        // Example 5. Like 4 but with 2 sec delay on every message from tm
                        send = new Message(4, Participant.OUTSIDE, messageTypes.TRANSACTION);
                        // Adding data to message
                        send.setData("The Lion, the Witch and the Wardrobe");

                        outputStream.writeObject(send);
                        outputStream.flush();
                        Thread.sleep(12000);
                        tm.interrupt();
                        a.interrupt();
                        b.interrupt();
                        taskManager.setMessageDelay(0);
                        break;
                    case 6:
                        taskManager.setMessageDelay(1000);
                        // Example 6. TaskManager crashes
                        debugAnswers = main.scenarioAllOk();
                        send = new Message(6, Participant.OUTSIDE, messageTypes.TRANSACTION);
                        // Adding data to message
                        send.setData("The Lion, the Witch and the Wardrobe");
                        send.setDebug(true);
                        send.setDebugAnswers(debugAnswers);
                        outputStream.writeObject(send);
                        outputStream.flush();
                        Thread.sleep(2000);
                        tm.interrupt();
                        Thread.sleep(2000);
                        tm = new Thread(new TaskManager(Participant.TM.getPort(), rm));
                        tm.start();
                        Thread.sleep(5000);
                        tm.interrupt();
                        a.interrupt();
                        b.interrupt();
                        taskManager.setMessageDelay(0);
                        break;
                    case 7:
                        // Example 7. B won't answer
                        b.interrupt();
                        Thread.sleep(300);
                        send = new Message(7, Participant.OUTSIDE, messageTypes.TRANSACTION);
                        // Adding data to message
                        send.setData("The Lion, the Witch and the Wardrobe");
                        outputStream.writeObject(send);
                        outputStream.flush();
                        Thread.sleep(10000);
                        tm.interrupt();
                        a.interrupt();
                        break;
                    case 8:
                        // Create the socket
                        Socket clientSocket2 = new Socket(Participant.TM.getHost(), Participant.TM.getPort());
                        // Create the input & output streams to the server
                        ObjectOutputStream outputStream2 = new ObjectOutputStream(clientSocket2.getOutputStream());
                        // Example 8. Two transactions at once
                        send = new Message(8, Participant.OUTSIDE, messageTypes.TRANSACTION);
                        // Adding data to message
                        send.setData("Anne of Green Gables, Lucy Maud Montgomery");
                        outputStream.writeObject(send);
                        outputStream.flush();

                        Message send2 = new Message(9, Participant.OUTSIDE, messageTypes.TRANSACTION);
                        send2.setData("Charlotte's Web, E.B. White");

                        outputStream2.writeObject(send2);
                        outputStream2.flush();
                        Thread.sleep(10000);
                        tm.interrupt();
                        a.interrupt();
                        b.interrupt();
                        break;

                }
                outputStream.close();
                clientSocket.close();
            }
            catch (Exception e) {
                System.out.println("An error occured");
                e.printStackTrace();
            }
        }
    }
}