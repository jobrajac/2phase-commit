import java.io.Serializable;
import java.util.HashMap;

// Class that gets sent over websockets between clients.
public class Message implements Serializable {
    private int transaction_id;
    private Participant client_id;
    public messageTypes message;
    private String data = null;

    // These two are for debugging purposes
    private boolean debug = false;
    private HashMap<String, HashMap<messageTypes, messageTypes>> debugAnswers = null;

    Message(int trans_id, Participant client_id, messageTypes message) {
        this.transaction_id = trans_id;
        this.client_id = client_id;
        this.message = message;
    }

    String getData() {
        return data;
    }

    void setData(String data) {
        this.data = data;
    }

    boolean isDebug() {
        return debug;
    }

    void setDebug(boolean debug) {
        this.debug = debug;
    }

    HashMap<String, HashMap<messageTypes, messageTypes>> getDebugAnswers() {
        return debugAnswers;
    }

    void setDebugAnswers(HashMap<String, HashMap<messageTypes, messageTypes>> debugAnswers) {
        this.debugAnswers = debugAnswers;
    }

    int getTransaction_id() {
        return transaction_id;
    }

    Participant getClient_id() {
        return client_id;
    }

    public messageTypes getMessage() {
        return message;
    }
}
