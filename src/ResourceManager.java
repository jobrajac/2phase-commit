import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.FileLock;
import java.util.HashMap;

public class ResourceManager extends Client {
    private String TM_HOST;
    private int TM_PORT;
    private HashMap<Integer, RM_State> saved_states = new HashMap<>();
    private final String stateFolderPath;
    private final String resourceFilePath;
    private java.nio.channels.FileLock lock;

    public ResourceManager(int PORT, String TM_HOST, int TM_PORT, String resourceFilePath, String stateFolderPath) {
        super(PORT);
        this.TM_HOST = TM_HOST;
        this.TM_PORT = TM_PORT;
        this.stateFolderPath = stateFolderPath;
        this.resourceFilePath = resourceFilePath;
    }

    private RM_State getSaved_state(int id) {
        return saved_states.get(id);
    }

    private void setSaved_state(RM_State saved_state, int id) {
        saved_states.put(id, saved_state);
    }

    public FileLock getLock() {
        return lock;
    }

    public void setLock(FileLock lock) {
        this.lock = lock;
    }

    @Override
    public void handleMessage(Message msg) {
        // For debugging. Ekko the value specified in debug.
        // If the value is NOREPLY don't reply.
        if(msg.isDebug()) {
            messageTypes ans = msg.getDebugAnswers().get(msg.getClient_id().name()).get(msg.message);
            if(ans != messageTypes.NO_REPLY) {
                sendMessage(TM_HOST, TM_PORT, new Message(msg.getTransaction_id(), msg.getClient_id(), ans));
            }
            return;
        }
        switch (msg.getMessage()) {
            case START:
                start(msg);
                break;
            case COMMIT:
                commit(msg);
                break;
            case ROLLBACK:
                rollback(msg);
                break;
            case UNDO:
                undo(msg);
        }
    }
    private void start(Message msg) {
        // Create and save state
        RM_State transaction_state = new RM_State(msg.getTransaction_id(), msg.getData());
        setSaved_state(transaction_state, msg.getTransaction_id());
        setState(0, msg.getTransaction_id());

        // Try to hold resources
        try {
            FileInputStream in = new FileInputStream(resourceFilePath);
            java.nio.channels.FileLock lock = in.getChannel().lock();
            setLock(lock);
        }
        catch (Exception e) {
            System.out.println("ERROR reading logs");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Localized: " + e.getLocalizedMessage());
            System.err.println("Stack Trace: " + e.getStackTrace());
            // Report failed
            send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.START_FAIL));
        }
        //Report ok
        send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.START_OK));
    }
    private void commit(Message msg) {
        send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.COMMIT_OK));
    }
    private void rollback(Message msg) {
        send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.ROLLBACK_OK));
    }
    private void undo(Message msg) {
        send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.UNDO_OK));
    }

    private void saveState(int trans_id) {
        saveFile(getSaved_state(trans_id), stateFolderPath + trans_id + ".ser");
    }

    private void setState(int newStep, int trans_id) {
        RM_State tmp = getSaved_state(trans_id);
        tmp.setState(newStep);
        setSaved_state(tmp, trans_id);
        saveState(trans_id);
    }
    private void send(Message msg) {
        sendMessage(TM_HOST, TM_PORT, msg);
    }

}
